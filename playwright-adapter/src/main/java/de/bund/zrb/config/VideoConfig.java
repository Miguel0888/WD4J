package de.bund.zrb.config;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentrale, adapter-taugliche Recording-Konfiguration.
 * Wird NICHT aus der UI geladen – dafür ist die App-Schicht zuständig.
 * Statische, volatile Werte + einfache Helper.
 */
public final class VideoConfig {

    // ------------------------- Defaults -------------------------
    private static final boolean DEFAULT_ENABLED      = false;
    private static final int     DEFAULT_FPS          = 15;
    private static final String  DEFAULT_REPORTS      = "C:/Recordings";

    private static final String  DEFAULT_CONTAINER    = "matroska";   // "matroska", "avi", "mp4", ...
    private static final String  DEFAULT_CODEC        = "mjpeg";      // "mjpeg", "libx264", "libx265", ...
    private static final String  DEFAULT_PIXEL_FMT    = "yuv420p";
    private static final boolean DEFAULT_INTERLEAVED  = true;

    // Qualität: wähle genau EINES (qscale | crf | bitrate)
    private static final String  DEFAULT_QUALITY_MODE = "qscale";     // "qscale" | "crf" | "bitrate"
    private static final int     DEFAULT_QSCALE       = 3;            // 1 sehr gut .. 31 schlecht (für MJPEG)
    private static final int     DEFAULT_CRF          = 20;           // für x264/x265
    private static final int     DEFAULT_BITRATE_KBPS = 0;            // 0 = aus

    // Farbraum/-range
    private static final String  DEFAULT_COLOR_RANGE  = "pc";         // "pc" (full) | "tv" (limited)
    private static final String  DEFAULT_COLORSPACE   = "bt709";
    private static final String  DEFAULT_COLOR_TRC    = "bt709";
    private static final String  DEFAULT_COLOR_PRIM   = "bt709";

    // Filterkette (leerer String = keine). Beispiel:
    // "scale=in_range=pc:out_range=pc,format=yuv420p"
    private static final String  DEFAULT_VF           = "scale=in_range=pc:out_range=pc,format=yuv420p";

    // Sonstiges
    private static final boolean DEFAULT_ENFORCE_EVEN_DIMS = true;
    private static final int     DEFAULT_THREADS      = 0;            // 0 = ffmpeg wählt

    // Container-Fallback-Reihenfolge (wird probiert, wenn Start fehlschlägt)
    // Achtung: die Extension wird automatisch zum Container gemappt (s. mapContainerToExt)
    private static final List<String> DEFAULT_CONTAINER_FALLBACKS =
            Collections.unmodifiableList(Arrays.asList("avi", "mp4"));

    // ------------------------- Konfigurierbare Felder -------------------------
    private static volatile boolean enabled    = DEFAULT_ENABLED;
    private static volatile int     fps        = DEFAULT_FPS;
    private static volatile String  reportsDir = DEFAULT_REPORTS;

    private static volatile String  container  = DEFAULT_CONTAINER;
    private static volatile String  codec      = DEFAULT_CODEC;
    private static volatile String  pixelFmt   = DEFAULT_PIXEL_FMT;
    private static volatile boolean interleaved = DEFAULT_INTERLEAVED;

    private static volatile String  qualityMode = DEFAULT_QUALITY_MODE;  // "qscale"|"crf"|"bitrate"
    private static volatile int     qscale      = DEFAULT_QSCALE;
    private static volatile int     crf         = DEFAULT_CRF;
    private static volatile int     bitrateKbps = DEFAULT_BITRATE_KBPS;

    private static volatile String  colorRange  = DEFAULT_COLOR_RANGE;
    private static volatile String  colorspace  = DEFAULT_COLORSPACE;
    private static volatile String  colorTrc    = DEFAULT_COLOR_TRC;
    private static volatile String  colorPrim   = DEFAULT_COLOR_PRIM;

    private static volatile String  vf          = DEFAULT_VF;

    private static volatile boolean enforceEvenDims = DEFAULT_ENFORCE_EVEN_DIMS;
    private static volatile int     threads     = DEFAULT_THREADS;

    // x264/x265 Zusatz
    private static volatile String  preset      = null;   // z.B. "veryfast"
    private static volatile String  tune        = null;   // z.B. "zerolatency","animation"
    private static volatile String  profile     = null;   // z.B. "high","main","baseline"
    private static volatile String  level       = null;   // z.B. "4.1"

    // Beliebige zusätzliche FFmpeg-Video-Optionen (werden 1:1 weitergereicht)
    private static final Map<String,String> extraVideoOptions = new ConcurrentHashMap<>();

    // Container-Fallbacks (reihenfolge-wichtig)
    private static volatile List<String> containerFallbacks = new ArrayList<>(DEFAULT_CONTAINER_FALLBACKS);

    private VideoConfig() {}

    // ------------------------- Getter -------------------------
    public static boolean isEnabled() { return enabled; }
    public static int getFps() { return fps; }
    public static String getReportsDir() { return reportsDir; }

    public static String getContainer() { return container; }
    public static String getCodec() { return codec; }
    public static String getPixelFmt() { return pixelFmt; }
    public static boolean isInterleaved() { return interleaved; }

    public static String getQualityMode() { return qualityMode; }
    public static int getQscale() { return qscale; }
    public static int getCrf() { return crf; }
    public static int getBitrateKbps() { return bitrateKbps; }

    public static String getColorRange() { return colorRange; }
    public static String getColorspace() { return colorspace; }
    public static String getColorTrc() { return colorTrc; }
    public static String getColorPrimaries() { return colorPrim; }

    public static String getVf() { return vf; }

    public static boolean isEnforceEvenDims() { return enforceEvenDims; }
    public static int getThreads() { return threads; }

    public static String getPreset() { return preset; }
    public static String getTune() { return tune; }
    public static String getProfile() { return profile; }
    public static String getLevel() { return level; }

    public static Map<String,String> getExtraVideoOptions() { return extraVideoOptions; }

    public static List<String> getContainerFallbacks() { return Collections.unmodifiableList(containerFallbacks); }

    // ------------------------- Setter (+ Validation) -------------------------
    public static void setEnabled(boolean v) { enabled = v; }
    public static void setFps(int value) {
        if (value <= 0) throw new IllegalArgumentException("fps must be > 0");
        fps = value;
    }
    public static void setReportsDir(String dir) {
        if (dir == null || dir.trim().isEmpty())
            throw new IllegalArgumentException("reportsDir must not be empty");
        reportsDir = dir.trim();
    }

    public static void setContainer(String v) { container = nonEmptyLower(v, "container"); }
    public static void setCodec(String v) { codec = nonEmptyLower(v, "codec"); }
    public static void setPixelFmt(String v) { pixelFmt = nonEmptyLower(v, "pixelFmt"); }
    public static void setInterleaved(boolean v) { interleaved = v; }

    public static void setQualityMode(String v) {
        v = nonEmptyLower(v, "qualityMode");
        if (!v.equals("qscale") && !v.equals("crf") && !v.equals("bitrate"))
            throw new IllegalArgumentException("qualityMode must be qscale|crf|bitrate");
        qualityMode = v;
    }
    public static void setQscale(int v) {
        if (v < 1 || v > 31) throw new IllegalArgumentException("qscale must be 1..31");
        qscale = v;
    }
    public static void setCrf(int v) {
        if (v < 0 || v > 51) throw new IllegalArgumentException("crf must be 0..51");
        crf = v;
    }
    public static void setBitrateKbps(int v) { bitrateKbps = Math.max(0, v); }

    public static void setColorRange(String v) { colorRange = nonEmptyLower(v, "colorRange"); }
    public static void setColorspace(String v) { colorspace = nonEmptyLower(v, "colorspace"); }
    public static void setColorTrc(String v) { colorTrc = nonEmptyLower(v, "colorTrc"); }
    public static void setColorPrimaries(String v) { colorPrim = nonEmptyLower(v, "colorPrimaries"); }

    public static void setVf(String v) { vf = v == null ? "" : v.trim(); }

    public static void setEnforceEvenDims(boolean v) { enforceEvenDims = v; }
    public static void setThreads(int v) { threads = Math.max(0, v); }

    public static void setPreset(String v) { preset = emptyToNull(v); }
    public static void setTune(String v) { tune = emptyToNull(v); }
    public static void setProfile(String v) { profile = emptyToNull(v); }
    public static void setLevel(String v) { level = emptyToNull(v); }

    public static void putExtraVideoOption(String key, String value) {
        if (key != null && !key.trim().isEmpty() && value != null) {
            extraVideoOptions.put(key.trim(), value);
        }
    }
    public static void setContainerFallbacks(List<String> list) {
        if (list == null || list.isEmpty()) { containerFallbacks = new ArrayList<>(); return; }
        ArrayList<String> copy = new ArrayList<>(list.size());
        for (String s : list) if (s != null && !s.trim().isEmpty()) copy.add(s.trim().toLowerCase(Locale.ROOT));
        containerFallbacks = copy;
    }

    // ------------------------- System-Properties Import -------------------------
    /**
     * Initialisiert Felder aus System Properties (falls gesetzt).
     * Namen u.a.:
     *  - recording.enabled           (true/false)
     *  - recording.fps               (int > 0)
     *  - recording.reportsDir        (Pfad)
     *  - recording.container         (matroska|avi|mp4|...)
     *  - recording.codec             (mjpeg|libx264|libx265|...)
     *  - recording.pixfmt            (yuv420p|yuv444p|...)
     *  - recording.interleaved       (true/false)
     *  - recording.quality           (qscale|crf|bitrate)
     *  - recording.qscale            (1..31)
     *  - recording.crf               (0..51)
     *  - recording.bitrateKbps       (>=0)
     *  - recording.color.range       (pc|tv)
     *  - recording.color.space       (bt709|bt601|...)
     *  - recording.color.trc         (bt709|...)
     *  - recording.color.primaries   (bt709|...)
     *  - recording.vf                (Filterkette)
     *  - recording.enforceEvenDims   (true/false)
     *  - recording.threads           (0..)
     *  - recording.preset|tune|profile|level (für x264/x265)
     *  - recording.container.fallbacks (CSV, z.B. "avi,mp4")
     *  - recording.ffopts.<key>=<value>   (beliebige zusätzliche FFmpeg-Video-Optionen)
     */
    public static void initFromSystemProperties() {
        String p;
        if ((p = System.getProperty("recording.enabled")) != null) setEnabled(Boolean.parseBoolean(p));
        if ((p = System.getProperty("recording.fps")) != null) { try { setFps(Integer.parseInt(p)); } catch (Exception ignored) {} }
        if ((p = System.getProperty("recording.reportsDir")) != null && !p.trim().isEmpty()) setReportsDir(p);

        if ((p = System.getProperty("recording.container")) != null) setContainer(p);
        if ((p = System.getProperty("recording.codec")) != null) setCodec(p);
        if ((p = System.getProperty("recording.pixfmt")) != null) setPixelFmt(p);
        if ((p = System.getProperty("recording.interleaved")) != null) setInterleaved(Boolean.parseBoolean(p));

        if ((p = System.getProperty("recording.quality")) != null) setQualityMode(p);
        if ((p = System.getProperty("recording.qscale")) != null) { try { setQscale(Integer.parseInt(p)); } catch (Exception ignored) {} }
        if ((p = System.getProperty("recording.crf")) != null) { try { setCrf(Integer.parseInt(p)); } catch (Exception ignored) {} }
        if ((p = System.getProperty("recording.bitrateKbps")) != null) { try { setBitrateKbps(Integer.parseInt(p)); } catch (Exception ignored) {} }

        if ((p = System.getProperty("recording.color.range")) != null) setColorRange(p);
        if ((p = System.getProperty("recording.color.space")) != null) setColorspace(p);
        if ((p = System.getProperty("recording.color.trc")) != null) setColorTrc(p);
        if ((p = System.getProperty("recording.color.primaries")) != null) setColorPrimaries(p);

        if ((p = System.getProperty("recording.vf")) != null) setVf(p);

        if ((p = System.getProperty("recording.enforceEvenDims")) != null) setEnforceEvenDims(Boolean.parseBoolean(p));
        if ((p = System.getProperty("recording.threads")) != null) { try { setThreads(Integer.parseInt(p)); } catch (Exception ignored) {} }

        if ((p = System.getProperty("recording.preset")) != null) setPreset(p);
        if ((p = System.getProperty("recording.tune")) != null) setTune(p);
        if ((p = System.getProperty("recording.profile")) != null) setProfile(p);
        if ((p = System.getProperty("recording.level")) != null) setLevel(p);

        if ((p = System.getProperty("recording.container.fallbacks")) != null) {
            String[] parts = p.split(",");
            ArrayList<String> list = new ArrayList<>();
            for (String s : parts) if (s != null && !s.trim().isEmpty()) list.add(s.trim());
            setContainerFallbacks(list);
        }

        // recording.ffopts.<key>=<value>
        Properties sys = System.getProperties();
        final String prefix = "recording.ffopts.";
        for (String name : sys.stringPropertyNames()) {
            if (name.startsWith(prefix)) {
                String key = name.substring(prefix.length());
                putExtraVideoOption(key, sys.getProperty(name));
            }
        }
    }

    /**
     * Setzt wieder auf Defaults zurück (praktisch für Tests).
     */
    public static void resetToDefaults() {
        enabled    = DEFAULT_ENABLED;
        fps        = DEFAULT_FPS;
        reportsDir = DEFAULT_REPORTS;

        container  = DEFAULT_CONTAINER;
        codec      = DEFAULT_CODEC;
        pixelFmt   = DEFAULT_PIXEL_FMT;
        interleaved = DEFAULT_INTERLEAVED;

        qualityMode = DEFAULT_QUALITY_MODE;
        qscale      = DEFAULT_QSCALE;
        crf         = DEFAULT_CRF;
        bitrateKbps = DEFAULT_BITRATE_KBPS;

        colorRange = DEFAULT_COLOR_RANGE;
        colorspace = DEFAULT_COLORSPACE;
        colorTrc   = DEFAULT_COLOR_TRC;
        colorPrim  = DEFAULT_COLOR_PRIM;

        vf = DEFAULT_VF;

        enforceEvenDims = DEFAULT_ENFORCE_EVEN_DIMS;
        threads = DEFAULT_THREADS;

        preset = tune = profile = level = null;

        extraVideoOptions.clear();
        containerFallbacks = new ArrayList<>(DEFAULT_CONTAINER_FALLBACKS);
    }

    // ------------------------- Recorder-Helfer -------------------------

    /** Mappt Container auf Dateiendung. */
    public static String mapContainerToExt(String cont) {
        if (cont == null) return ".mkv";
        switch (cont.toLowerCase(Locale.ROOT)) {
            case "matroska": return ".mkv";
            case "mp4":      return ".mp4";
            case "avi":      return ".avi";
            case "mov":      return ".mov";
            case "mpegts":
            case "mpeg-ts":
            case "ts":       return ".ts";
            default:         return "." + cont.toLowerCase(Locale.ROOT);
        }
    }

    /** Mappt Codec-String auf FFmpeg-Codec-ID (soweit sinnvoll). */
    public static int mapCodecId(String codecName) {
        String c = codecName == null ? "" : codecName.toLowerCase(Locale.ROOT);
        switch (c) {
            case "mjpeg":   return avcodec.AV_CODEC_ID_MJPEG;
            case "libx264":
            case "h264":    return avcodec.AV_CODEC_ID_H264;
            case "libx265":
            case "hevc":    return avcodec.AV_CODEC_ID_HEVC;
            default:        return avcodec.AV_CODEC_ID_NONE; // FFmpeg entscheidet evtl. anhand von setVideoCodecName
        }
    }

    /**
     * Wendet *alle* Video-Optionen auf den Recorder an.
     * (Codec, PixFmt, FPS, Interleaving, Qualität, Farbraum, Filter, Threads, Extras)
     */
    public static void configureRecorder(FFmpegFrameRecorder rec, int fps) {
        rec.setFrameRate(fps);
        rec.setInterleaved(isInterleaved());

        // Codec
        int codecId = mapCodecId(getCodec());
        if (codecId != avcodec.AV_CODEC_ID_NONE) {
            rec.setVideoCodec(codecId);
        } else {
            // Fallback: explizit den Codec-String setzen
            rec.setVideoCodecName(getCodec());
        }

        // PixFmt
        rec.setPixelFormat(mapPixelFmt(getPixelFmt()));

        // Qualität
        switch (getQualityMode()) {
            case "qscale":
                rec.setVideoOption("qscale", String.valueOf(getQscale()));
                break;
            case "crf":
                rec.setVideoOption("crf", String.valueOf(getCrf()));
                break;
            case "bitrate":
                if (getBitrateKbps() > 0) rec.setVideoBitrate(getBitrateKbps() * 1000);
                break;
        }

        // Farbraum/-range
        if (notEmpty(getColorRange()))    rec.setVideoOption("color_range", getColorRange());
        if (notEmpty(getColorspace()))    rec.setVideoOption("colorspace", getColorspace());
        if (notEmpty(getColorTrc()))      rec.setVideoOption("color_trc", getColorTrc());
        if (notEmpty(getColorPrimaries()))rec.setVideoOption("color_primaries", getColorPrimaries());

        // Filter
        if (notEmpty(getVf()))            rec.setVideoOption("vf", getVf());

        // Threads
        if (getThreads() > 0)             rec.setVideoOption("threads", String.valueOf(getThreads()));

        // x264/x265 Extras
        if (notEmpty(getPreset()))  rec.setVideoOption("preset", getPreset());
        if (notEmpty(getTune()))    rec.setVideoOption("tune", getTune());
        if (notEmpty(getProfile())) rec.setVideoOption("profile", getProfile());
        if (notEmpty(getLevel()))   rec.setVideoOption("level", getLevel());

        // Beliebige Extra-Optionen
        for (Map.Entry<String,String> e : getExtraVideoOptions().entrySet()) {
            if (notEmpty(e.getKey()) && e.getValue() != null) {
                rec.setVideoOption(e.getKey(), e.getValue());
            }
        }
    }

    private static int mapPixelFmt(String pf) {
        if (pf == null) return avutil.AV_PIX_FMT_YUV420P;
        switch (pf.toLowerCase(Locale.ROOT)) {
            case "yuv420p":  return avutil.AV_PIX_FMT_YUV420P;
            case "yuvj420p": return avutil.AV_PIX_FMT_YUVJ420P;
            case "yuv422p":  return avutil.AV_PIX_FMT_YUV422P;
            case "yuv444p":  return avutil.AV_PIX_FMT_YUV444P;
            case "bgr24":    return avutil.AV_PIX_FMT_BGR24;
            case "rgb24":    return avutil.AV_PIX_FMT_RGB24;
            default:         return avutil.AV_PIX_FMT_YUV420P;
        }
    }

    private static String nonEmptyLower(String v, String name) {
        if (v == null || v.trim().isEmpty())
            throw new IllegalArgumentException(name + " must not be empty");
        return v.trim().toLowerCase(Locale.ROOT);
    }
    private static boolean notEmpty(String v) { return v != null && !v.trim().isEmpty(); }
    private static String emptyToNull(String v) { return (v == null || v.trim().isEmpty()) ? null : v.trim(); }

    @Override
    public String toString() {
        return "VideoConfig{" +
                "enabled=" + enabled +
                ", fps=" + fps +
                ", reportsDir='" + reportsDir + '\'' +
                ", container='" + container + '\'' +
                ", codec='" + codec + '\'' +
                ", pixelFmt='" + pixelFmt + '\'' +
                ", interleaved=" + interleaved +
                ", qualityMode='" + qualityMode + '\'' +
                ", qscale=" + qscale +
                ", crf=" + crf +
                ", bitrateKbps=" + bitrateKbps +
                ", colorRange='" + colorRange + '\'' +
                ", colorspace='" + colorspace + '\'' +
                ", colorTrc='" + colorTrc + '\'' +
                ", colorPrimaries='" + colorPrim + '\'' +
                ", vf='" + vf + '\'' +
                ", enforceEvenDims=" + enforceEvenDims +
                ", threads=" + threads +
                ", preset='" + preset + '\'' +
                ", tune='" + tune + '\'' +
                ", profile='" + profile + '\'' +
                ", level='" + level + '\'' +
                ", containerFallbacks=" + containerFallbacks +
                ", extraVideoOptions=" + extraVideoOptions +
                '}';
    }
}
