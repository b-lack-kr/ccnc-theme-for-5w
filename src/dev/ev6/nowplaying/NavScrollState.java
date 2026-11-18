package dev.ev6.nowplaying;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class NavScrollState {
    private static final Pattern LINE = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*CMapControl::setScrollMo: 14088\\].*setScrollMode session\\(Front\\), activity\\(Mainview\\), _bScrollMode\\(([01])\\),.*$");
    private static final Pattern GUIDANCE = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*CMapControl::isOverviewW: 55973\\].*checkOverviewWhileStop session\\(Front\\) activity\\(Mainview\\),.*isGuide\\(([01])\\),.*$");
    private static final Pattern GUIDE_CHANGE = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*CServiceShareData::updat: 1671\\].*Update Is Guidance Status \\[([01])\\].*$");

    private static final Pattern GUIDE_LIST = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*CMapControl::isOverviewW: 55973\\].*checkOverviewWhileStop session\\(Front\\) activity\\(Mainview\\),.*isShowTBT\\(([01])\\),.*$");

    private static final Pattern OFF_ROUTE = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*CMapControl::isOverviewW: 55973\\].*checkOverviewWhileStop session\\(Front\\) activity\\(Mainview\\),.*isOffRoute\\(([01])\\),.*$");

    public static Boolean parse(String str, int i, long j, long j2) {
        return parsePattern(LINE, str, i, j, j2);
    }

    private static final Pattern SCENE = Pattern.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d+)\\s+\\d+\\s+[VDIWEF]\\s+ExSL\\s*:.*top scene updated: screen\\(1\\),.*curScene\\(([^)]+)\\).*$");
    static String sceneName(String line, int pid, long earliest, long now) {
        if(line==null)return null;Matcher m=SCENE.matcher(line);if(!m.matches()||!Integer.toString(pid).equals(m.group(2)))return null;
        try{SimpleDateFormat year=new SimpleDateFormat("yyyy",Locale.US);SimpleDateFormat date=new SimpleDateFormat("yyyy MM-dd HH:mm:ss.SSS",Locale.US);date.setLenient(false);long t=date.parse(year.format(new Date(now))+" "+m.group(1)).getTime();return t>=earliest&&t<=now+1000?m.group(3):null;}catch(RuntimeException e){return null;}catch(ParseException e){return null;}
    }
    public static Boolean sceneBlocked(String line, int pid, long earliest, long now) {
        if (line == null) return null;
        Matcher m=SCENE.matcher(line); if(!m.matches() || !Integer.toString(pid).equals(m.group(2))) return null;
        try { SimpleDateFormat year=new SimpleDateFormat("yyyy",Locale.US); SimpleDateFormat date=new SimpleDateFormat("yyyy MM-dd HH:mm:ss.SSS",Locale.US); date.setLenient(false); long t=date.parse(year.format(new Date(now))+" "+m.group(1)).getTime();
            if(t<earliest||t>now+1000) return null; return Boolean.valueOf(!"Map_Normal".equals(m.group(3)));
        } catch(RuntimeException e){return null;} catch(ParseException e){return null;}
    }

    public static Boolean guidance(String str, int i, long j, long j2) {
        Boolean offRoute = parsePattern(OFF_ROUTE, str, i, j, j2);
        if (offRoute != null) GuidanceProxy.setOffRoute(offRoute);
        Boolean list = parsePattern(GUIDE_LIST, str, i, j, j2);
        if (list != null) GuidanceProxy.setExpanded(list);
        Boolean pattern = parsePattern(GUIDE_CHANGE, str, i, j, j2);
        return pattern != null ? pattern : parsePattern(GUIDANCE, str, i, j, j2);
    }

    private static Boolean parsePattern(Pattern pattern, String str, int i, long j, long j2) {
        if (str == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches() && Integer.toString(i).equals(matcher.group(2))) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy", Locale.US);
                SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy MM-dd HH:mm:ss.SSS", Locale.US);
                simpleDateFormat2.setLenient(false);
                long time = simpleDateFormat2.parse(simpleDateFormat.format(new Date(j2)) + " " + matcher.group(1)).getTime();
                if (time >= j && time <= j2 + 1000) {
                    return Boolean.valueOf("1".equals(matcher.group(3)));
                }
            } catch (RuntimeException | ParseException unused) {
            }
        }
        return null;
    }

    private NavScrollState() {
    }
}
