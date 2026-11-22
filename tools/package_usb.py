#!/usr/bin/env python3
"""Package the already signed HMG-API mod in the USB update layout."""
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
version = json.loads((ROOT / 'package/mod.json').read_text(encoding='utf-8'))['version']
source = ROOT / 'dist/ccnc-theme-for-5w-mod.zip'
if not source.is_file():
    raise SystemExit('Build the signed mod before packaging the USB update')
with zipfile.ZipFile(source) as z:
    meta = json.loads(z.read('mod.json'))
    if meta.get('id') != 'blackr-ccnc-theme-for-5w' or meta.get('version') != version:
        raise SystemExit('The built mod does not match the current package version')
versioned = ROOT / 'dist' / ('ccnc-theme-for-5w-' + version + '.zip')
shutil.copyfile(source, versioned)
usb = ROOT / 'dist' / ('ccnc-theme-for-5w-' + version + '-usb.zip')
readme = '''ccNC Theme for 5W 설치

1. 이 압축파일을 풀고 HMGAPI 폴더를 USB 최상위에 복사합니다.
2. 차량의 HMG-API에서 USB 업데이트를 확인하고 패키지를 설치합니다.
3. 필요한 경우 내비게이션을 재시작합니다.

HMG-API가 미리 설치되어 있어야 합니다.
아이오닉 5 (NE) 2023–2024 외의 차량은 검증되지 않았습니다.
'''
with zipfile.ZipFile(usb, 'w', zipfile.ZIP_DEFLATED) as z:
    z.write(versioned, 'HMGAPI/updates/' + versioned.name)
    z.writestr('설치안내.txt', readme)
with zipfile.ZipFile(usb) as z:
    if z.testzip() is not None:
        raise SystemExit('USB archive integrity check failed')
print('Signed mod:', versioned)
print('USB package:', usb)
