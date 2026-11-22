#!/usr/bin/env python3
"""Build the single HMG-API mod from the user's supplied original package."""
import argparse
import base64
import hashlib
import io
import json
import os
import secrets
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent
TOOLS = WORKSPACE / ".tools"
EXPECTED_ORIGINAL = "5be94836a030ce6e291d200626252aac53cdd496ce1b3d6d7636566664cee6fc"
PREFIXES = ("UsbNoticeFilter", "SurroundGeometry", "SurroundWindowController", "SurroundHooks", "SurroundBoundsLease", "SurroundFrontEvidence", "SurroundSensorObserver", "SurroundStatusPublisher", "SurroundDecision", "ArrivalInfoReader", "ArrivalInfoOverlay", "ArrivalInfoPolicy", "AppSlot", "NavKeySwap", "Diagnostic", "RadioArtBridge", "PrototypeViewport", "BottomMediaView",
            "BtCommandGate", "StockBtControlBridge", "BottomBarLayout", "BottomBarSurface",
            "GuidanceProxy", "GuideOverlay", "GlobalMediaNotice", "GlobalNoticePolicy", "OsdModePolicy", "ThemeMode", "WarmerSwipe", "VolumePanelHook", "NavScrollMonitor", "UniversalEntry", "TransientNotice")
EXACT = {"CcncMediaController.class", "NavScrollState.class", "MarqueeTiming.class", "CarPlayPlaceholder.class"}


def run(command, **kwargs):
    subprocess.run([str(x) for x in command], check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--original", type=Path, help="supplied original 5W ZIP for the full source build")
    parser.add_argument("--key", required=True, type=Path, help="HMG-API signing key (never committed)")
    parser.add_argument("--jdk", type=Path, default=TOOLS / "java-build/jdk-17.0.21+9/Contents/Home")
    parser.add_argument("--android-jar", type=Path, default=TOOLS / "android-sdk/platforms/android-37.0/android.jar")
    parser.add_argument("--d8", type=Path, default=TOOLS / "android-sdk/build-tools/36.0.0/d8")
    parser.add_argument("--dexlib", type=Path, default=TOOLS / "android-sdk/cmdline-tools/latest/lib/external/com/android/tools/smali/smali-dexlib2/3.0.9/smali-dexlib2-3.0.9.jar")
    parser.add_argument("--guava", type=Path, default=TOOLS / "android-sdk/cmdline-tools/latest/lib/external/com/google/guava/guava/33.4.0-jre/guava-33.4.0-jre.jar")
    parser.add_argument("--signing-classpath", type=Path, default=WORKSPACE / "simulation_source/build/signclasses")
    parser.add_argument("--release-base", type=Path, help="Apply release changes to the signed 0.1.34 mod")
    args = parser.parse_args()
    source_archive = args.release_base or args.original
    if source_archive is None:
        parser.error("supply --release-base or --original")
    for value in (source_archive, args.key, args.jdk / "bin/java", args.jdk / "bin/javac", args.android_jar, args.d8, args.dexlib, args.guava, args.signing_classpath):
        if not value.exists():
            parser.error("required input does not exist: " + str(value))
    build = ROOT / "build"
    if build.exists():
        shutil.rmtree(build)
    classes, dex, mod = build / "classes", build / "dex", build / "mod"
    for directory in (classes, dex, mod):
        directory.mkdir(parents=True)
    if args.release_base:
        if hashlib.sha256(args.release_base.read_bytes()).hexdigest() != "0ff018d86eff4396c3d1205a10fcbf39417d5e85369061e05edeef258bd54eff":
            raise SystemExit("Unexpected release baseline")
        with zipfile.ZipFile(args.release_base) as baseline:
            with zipfile.ZipFile(io.BytesIO(baseline.read("hooks.jar"))) as hook:
                original = hook.read("classes.dex")
    else:
        with zipfile.ZipFile(args.original) as outer:
            with zipfile.ZipFile(io.BytesIO(outer.read("INSTALL/mod.zip"))) as original_mod:
                with zipfile.ZipFile(io.BytesIO(original_mod.read("hooks.jar"))) as original_hook:
                    original = original_hook.read("classes.dex")
        if hashlib.sha256(original).hexdigest() != EXPECTED_ORIGINAL:
            raise SystemExit("Unsupported original hooks.dex; expected supplied 0.5.1 package")
    (build / "original.dex").write_bytes(original)
    javac, java = args.jdk / "bin/javac", args.jdk / "bin/java"
    sources = sorted((ROOT / "src").rglob("*.java")) + sorted((ROOT / "stubs").rglob("*.java"))
    placeholder = base64.b64encode((ROOT / "assets/carplay-placeholder.jpg").read_bytes()).decode("ascii")
    generated_placeholder = build / "CarPlayPlaceholder.java"
    generated_placeholder.write_text(
        'package dev.ev6.ccnc_media; final class CarPlayPlaceholder { static final String DATA = "'
        + placeholder + '"; }\n', encoding="utf-8")
    sources.append(generated_placeholder)
    tests = sorted((ROOT / "tests").glob("*.java"))
    run([javac, "-source", "8", "-target", "8", "-cp", args.android_jar, "-d", classes, *sources, *tests])
    for test in ("dev.ev6.ccnc_media.StateTest", "dev.ev6.ccnc_media.LayoutTest",
                 "dev.ev6.ccnc_media.MarqueeTimingTest", "dev.ev6.ccnc_media.ThemeModeTest", "dev.ev6.ccnc_media.GlobalNoticePolicyTest", "dev.ev6.ccnc_media.ArrivalInfoPolicyTest", "dev.ev6.ccnc_media.WarmerSwipeMathTest", "dev.ev6.edge.EdgePageMappingTest", "GestureTest", "GateTest"):
        run([java, "-cp", classes, test])
    package_classes = classes / "dev/ev6/ccnc_media"
    replacements = [p for p in sorted(package_classes.glob("*.class"))
                    if p.name in EXACT or any(p.name.startswith(prefix) for prefix in PREFIXES)]
    replacements += sorted((classes / "dev/ev6/edge").glob("EdgeHandle*.class"))
    if args.release_base:
        roots = {"VolumePanelHook", "BottomMediaView", "TransientNotice", "GlobalMediaNotice", "UsbNoticeFilter", "ArrivalInfoOverlay", "PrototypeViewport", "CarPlayPlaceholder"}
        replacements = [p for p in replacements if p.stem.split("$")[0] in roots or p.name == "UniversalEntry.class"]
    env = dict(os.environ, JAVA_HOME=str(args.jdk.resolve()))
    run([args.d8, "--min-api", "19", "--lib", args.android_jar, "--output", dex, *replacements], env=env)
    cp = os.pathsep.join((str(args.dexlib), str(args.guava)))
    run([javac, "-cp", cp, "-d", build, ROOT / "tools/Merge.java"])
    merge = [java, "-cp", os.pathsep.join((str(build), cp)), "Merge", build / "original.dex", dex / "classes.dex", build / "merged.dex"]
    if args.release_base:
        merge.append("--release")
    run(merge)
    # Two launcher activities in one locally signed APK. Keep this signing key for updates.
    local = ROOT / "local"
    local.mkdir(exist_ok=True)
    keyfile, passfile = local / "shortcuts.keystore", local / "shortcuts-password"
    if not keyfile.exists():
        if passfile.exists():
            raise SystemExit("Shortcut signing key is missing; restore it before building")
        passfile.write_text(secrets.token_urlsafe(32))
        passfile.chmod(0o600)
        keyenv = dict(os.environ, CCNC_SHORTCUT_PASSWORD=passfile.read_text().strip())
        run([args.jdk / "bin/keytool", "-genkeypair", "-keystore", keyfile, "-storetype", "PKCS12", "-alias", "ccnc", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000", "-dname", "CN=ccNC Theme for 5W", "-storepass:env", "CCNC_SHORTCUT_PASSWORD", "-keypass:env", "CCNC_SHORTCUT_PASSWORD"], env=keyenv)
    if not passfile.exists():
        raise SystemExit("Shortcut signing password is missing; restore it before building")
    apkbuild = build / "shortcut"
    apkclasses, apkdex = apkbuild / "classes", apkbuild / "dex"
    apkclasses.mkdir(parents=True); apkdex.mkdir()
    generated = apkbuild / "generated"; generated.mkdir()
    apptools = args.d8.parent
    raw = apkbuild / "unsigned.apk"
    run([apptools / "aapt", "package", "-f", "-M", ROOT / "shortcuts/AndroidManifest.xml", "-S", ROOT / "shortcuts/res", "-J", generated, "-I", args.android_jar, "-F", raw])
    apksources = sorted((ROOT / "shortcuts/src").rglob("*.java")) + sorted(generated.rglob("*.java"))
    run([javac, "-source", "8", "-target", "8", "-cp", args.android_jar, "-d", apkclasses, *apksources])
    run([args.d8, "--min-api", "19", "--lib", args.android_jar, "--output", apkdex, *sorted(apkclasses.rglob("*.class"))], env=env)
    with zipfile.ZipFile(raw, "a", zipfile.ZIP_DEFLATED) as archive:
        archive.write(apkdex / "classes.dex", "classes.dex")
    aligned = apkbuild / "aligned.apk"
    run([apptools / "zipalign", "-f", "4", raw, aligned])
    appdir = mod / "app"; appdir.mkdir()
    keyenv = dict(env, CCNC_SHORTCUT_PASSWORD=passfile.read_text().strip())
    run([apptools / "apksigner", "sign", "--ks", keyfile, "--ks-key-alias", "ccnc", "--ks-pass", "env:CCNC_SHORTCUT_PASSWORD", "--key-pass", "env:CCNC_SHORTCUT_PASSWORD", "--out", appdir / "Shortcuts.apk", aligned], env=keyenv)
    run([apptools / "apksigner", "verify", "--min-sdk-version", "19", appdir / "Shortcuts.apk"], env=env)
    for name in ("mod.json", "settings.json"):
        shutil.copy2(ROOT / "package" / name, mod / name)
    if args.release_base:
        with zipfile.ZipFile(args.release_base) as baseline:
            for name in ("app/Shortcuts.apk", "app/Shortcuts.apk.idsig"):
                (mod / name).write_bytes(baseline.read(name))
            meta = json.loads(baseline.read("mod.json"))
            current = json.loads((mod / "mod.json").read_text())
            for key in ("version", "description", "hooks"):
                meta[key] = current[key]
            (mod / "mod.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n")
    with zipfile.ZipFile(mod / "hooks.jar", "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(build / "merged.dex", "classes.dex")
    run([java, "-cp", args.signing_classpath, "hmgapi.engine.Signing", "sign", mod, args.key])
    release = ROOT / "dist"
    release.mkdir(exist_ok=True)
    out = release / "ccnc-theme-for-5w-mod.zip"
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as archive:
        for item in sorted(mod.rglob("*")):
            if item.is_file():
                archive.write(item, item.relative_to(mod).as_posix())
    print("Signed mod:", out)


if __name__ == "__main__":
    main()
