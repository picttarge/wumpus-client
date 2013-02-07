package htwge;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

public class SoundFactory {

    private static final ClassLoader loader = SoundFactory.class.getClassLoader();
    public static final String A_MUSIC = "music_darkestchild_incompetech.wav";
    public static final String A_HEART_FAST = "loop_heartbeat_fast.wav";
    public static final String A_HEART_MED = "loop_heartbeat.wav";
    public static final String A_HEART_SLOW = "loop_heartbeat_slow.wav";
    public static final String A_BEGIN = "begin.wav";
    public static final String A_SPLAT = "splat0.wav";
    public static final String A_SHOTGUN = "shotgun.wav";
    public static final String A_WUMPUS_DEATH = "wumpus_death.wav";
    public static final String A_RELOAD = "reload.wav";
    public static final String A_FOOTSTEPS = "loop_footsteps.wav";
    public static final String A_BUMP = "bump.wav";
    public static final String A_STATIC = "loop_static.wav";
    public static final String A_STEP = "step.wav";
    public static final String A_AMMO = "ammo.wav";
    public static final HashMap<String, Integer> sourcemap = new HashMap<String, Integer>();
    public static final HashSet<String> playing = new HashSet<String>();

    static class FileListFilter implements FilenameFilter {

        private String extension;

        public FileListFilter(String extension) {
            this.extension = extension;
        }

        public boolean accept(File directory, String filename) {
            boolean fileOK = true;

            if (extension != null) {
                fileOK &= filename.endsWith('.' + extension);
            }
            return fileOK;
        }
    }
    private static boolean initok = false;
    public static final HashSet<String> important = new HashSet<String>() {

        {
            // abysmal use of anon-inner class
            add(A_HEART_FAST);
            add(A_HEART_MED);
            add(A_HEART_SLOW);
            add(A_FOOTSTEPS);
        }
    };
    public static final HashMap<String, String> map = new HashMap<String, String>() {

        {
            // abysmal use of anon-inner class
            put("0", A_HEART_FAST);
            put("1", A_HEART_MED);
            put("2", A_HEART_SLOW);
            put("3", A_BEGIN);
            put("4", A_SPLAT);
            put("5", A_SHOTGUN);
            put("6", A_WUMPUS_DEATH);
            put("7", A_RELOAD);
            put("8", A_FOOTSTEPS);
            put("9", A_BUMP);
        }
    };
    public static HashMap<String, Float> gains = new HashMap<String, Float>() {

        {
            // abysmal use of anon-inner class
            put(A_MUSIC, 1.0f);
            put(A_HEART_FAST, 2.0f);
            put(A_HEART_MED, 0.9f);
            put(A_HEART_SLOW, 0.8f);
            put(A_BEGIN, 1.0f);
            put(A_SPLAT, 1.0f);
            put(A_SHOTGUN, 0.7f);
            put(A_WUMPUS_DEATH, 1.0f);
            put(A_RELOAD, 1.0f);
            put(A_FOOTSTEPS, 0.5f);
            put(A_BUMP, 1.0f);
            put(A_STATIC, 0.5f);
            put(A_STEP, 0.5f);
            put(A_AMMO, 1.0f);
        }
    };
    public static final int MAX_BUFFERS = 128;
    public static final ArrayList<String> soundsToProcess = new ArrayList<String>();
    public static final ArrayList<String> serverSoundList = new ArrayList<String>();
    private static String[] soundFiles;
    private static ArrayList<String> accessFiles = new ArrayList<String>();
    /** Buffers hold sound data. */
    private static IntBuffer buffer;
    /** Sources are points emitting sound. */
    private static IntBuffer source = BufferUtils.createIntBuffer(MAX_BUFFERS);
    /** Position of the source sound. */
    private static FloatBuffer sourcePos = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
    private static FloatBuffer sourcePosLeft = BufferUtils.createFloatBuffer(3).put(new float[]{-1.0f, 0.0f, 0.0f});
    private static FloatBuffer sourcePosRight = BufferUtils.createFloatBuffer(3).put(new float[]{1.0f, 0.0f, 0.0f});
    /** Velocity of the source sound. */
    private static FloatBuffer sourceVel = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
    /** Position of the listener. */
    private static FloatBuffer listenerPos = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
    /** Velocity of the listener. */
    private static FloatBuffer listenerVel = BufferUtils.createFloatBuffer(3).put(new float[]{0.0f, 0.0f, 0.0f});
    /** Orientation of the listener. (first 3 elements are "at", second 3 are "up")
    Also note that these should be units of '1'. */
    private static FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[]{0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f});

    static void init() throws IOException {

        // load list from jar (jnlp support)

        if (Main.isRunningJavaWebStart()) {
            // because Java webstart doesn't like the idea of listing a directory
            // from within a jar.
            CodeSource src = SoundFactory.class.getProtectionDomain().getCodeSource();

            if (src != null) {
                URL jar = src.getLocation();
                ZipInputStream stream = new ZipInputStream(jar.openStream());
                try {
                    ZipEntry entry;

                    ArrayList<String> soundEntries = new ArrayList<String>();
                    final String path = "sounds/";
                    while ((entry = stream.getNextEntry()) != null) {

                        if (entry.getName().startsWith(path) && entry.getSize() > 0) {
                            soundEntries.add(entry.getName().replaceAll(path, ""));
                            String s = String.format("Entry: %s len %d added %TD",
                                entry.getName(), entry.getSize(),
                                new Date(entry.getTime()));
                            Main.debug(s);
                        }
                    }

                    if (soundEntries.size() > 0) {
                        soundFiles = soundEntries.toArray(new String[soundEntries.size()]);
                    } else {
                        Main.debug("Unable to get sound entries");
                    }
                } finally {
                    // we must always close the zip file.
                    stream.close();
                }
            } else {
                System.err.println("Unable to get listing of sounds");
            }
        } else {
            URL url = SoundFactory.class.getClassLoader().getResource("sounds");

            if (url == null) {
                System.err.println("Unable to locate sounds and form URL");
                initok = false;
                return;
            }
            File directory = new File(url.getFile());
            FileListFilter filter = new FileListFilter("wav");
            soundFiles = directory.list(filter);
        }

        if (soundFiles != null) {
            buffer = BufferUtils.createIntBuffer(soundFiles.length);
        } else {
            System.err.println("Unable to locate sounds");
            initok = false;
            return;
        }

        sourcePos.flip();
        sourcePosLeft.flip();
        sourcePosRight.flip();
        sourceVel.flip();
        listenerPos.flip();
        listenerVel.flip();
        listenerOri.flip();

        try {
            AL.create(null, 15, 22500, true);
        } catch (LWJGLException le) {
            le.printStackTrace(System.err);
        }
        AL10.alGetError();

        // Load the wav data.
        if (loadALData() == AL10.AL_FALSE) {
            Main.debug("[AL] Error loading wave data.");
        }

        setListenerValues();

        initok = true;
    }

    static int loadALData() {

        // Load wav data into a buffer.
        AL10.alGenBuffers(buffer);

        int errcode = AL10.alGetError();
        if (errcode != AL10.AL_NO_ERROR) {
            Main.debug("[AL] A Error code: " + errcode);
            return AL10.AL_FALSE;
        }

        WaveData waveFile = null;

        for (int i = 0; i < soundFiles.length; i++) {
            Main.debug("[AL] Adding sound file: " + soundFiles[i]);
            accessFiles.add(i, soundFiles[i]);

            URL url = Main.class.getResource("/sounds/" + soundFiles[i]);
            waveFile = WaveData.create(url);

            if (buffer == null) {
                System.err.println("AL buffer is null");
                System.exit(1);
            }

            if (waveFile == null) {
                System.err.println("AL waveFile is null");
                System.exit(1);
            }

            AL10.alBufferData(buffer.get(i), waveFile.format, waveFile.data, waveFile.samplerate);
            waveFile.dispose();
        }
        // Do another error check and return.
        errcode = AL10.alGetError();
        if (errcode == AL10.AL_NO_ERROR) {
            return AL10.AL_TRUE;
        } else {
            Main.debug("[AL] B ERROR CODE: " + errcode);
            return AL10.AL_FALSE;
        }
    }

    static private void addSource(int type, boolean repeat, float definedgain, String file, FloatBuffer sp) {

        if (!initok) {
            return;
        }

        int position = source.position();
        Main.debug("[AL] source position for " + file + " " + position);
        source.limit(position + 1);
        AL10.alGenSources(source);

        int alerror = AL10.alGetError();
        if (alerror != AL10.AL_NO_ERROR) {
            System.out.println("[AL] Error generating audio source. " + alerror + " " + file + " " + source.get(position));
            Main.fakeTrace();
            System.exit(-1);
        }

        Main.debug("[AL] adding to sourcemap " + file + " " + source.get(position));
        sourcemap.put(file, source.get(position));

        AL10.alSourcei(source.get(position), AL10.AL_BUFFER, buffer.get(type));
        AL10.alSourcef(source.get(position), AL10.AL_PITCH, 1.0f);
        AL10.alSourcef(source.get(position), AL10.AL_GAIN, definedgain);

        AL10.alSource(source.get(position), AL10.AL_POSITION, sp);

        AL10.alSource(source.get(position), AL10.AL_VELOCITY, sourceVel);
        AL10.alSourcei(source.get(position), AL10.AL_LOOPING, ((repeat == true) ? AL10.AL_TRUE : AL10.AL_FALSE));
        AL10.alSourcePlay(source.get(position));
        playing.add(file);
        // next index
        source.position(position + 1);
    }

    static void setListenerValues() {
        AL10.alListener(AL10.AL_POSITION, listenerPos);
        AL10.alListener(AL10.AL_VELOCITY, listenerVel);
        AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
    }

    public static void killALData() {
        // set to 0, num_sources
        Main.debug("[AL] Cleaning up all OpenAL data");

        for (int i : sourcemap.values()) {
            AL10.alSourceStop(i);
        }

        if (source != null) {
            int position = source.position();
            source.position(0).limit(position);
            if (initok) {
                AL10.alDeleteSources(source);
                source.clear();
                AL10.alDeleteBuffers(buffer);
                buffer.clear();
            }
        }

        AL.destroy();

        Main.debug("[AL] Cleanup complete");
    }

    public static void play(String file, boolean loop, float gain) {

        if (!initok) {
            return;
        }

        Main.debug("[AL] >) (< CENTER playing (" + loop + ") " + file);

        if (sourcemap.containsKey(file)) {
            AL10.alSourcef(sourcemap.get(file), AL10.AL_GAIN, gain >= 0 ? gain : gains.get(file));
            AL10.alSource(sourcemap.get(file), AL10.AL_POSITION, sourcePos);
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
            Main.debug("playing already added source " + file + " " + sourcemap.get(file));
        } else {
            Main.debug("adding new source " + file + " " + sourcemap.get(file));
            addSource(accessFiles.indexOf(file), loop, gain >= 0 ? gain : gains.get(file), file, sourcePos);
        }
    }

    public static void playLeft(String file, boolean loop) {

        if (!initok) {
            return;
        }

        Main.debug("[AL] >) LEFT playing (" + loop + ") " + file);

        if (sourcemap.containsKey(file)) {
            AL10.alSource(sourcemap.get(file), AL10.AL_POSITION, sourcePosLeft);
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
        } else {
            addSource(accessFiles.indexOf(file), loop, gains.get(file), file, sourcePosLeft);
        }
    }

    public static void playRight(String file, boolean loop) {

        if (!initok) {
            return;
        }

        Main.debug("[AL] (< RIGHT playing (" + loop + ") " + file);

        if (sourcemap.containsKey(file)) {
            AL10.alSource(sourcemap.get(file), AL10.AL_POSITION, sourcePosRight);
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
        } else {
            addSource(accessFiles.indexOf(file), loop, gains.get(file), file, sourcePosRight);
        }
    }

    public static void stop(String file) {

        if (!initok) {
            return;
        }

        Main.debug("[AL] Stop " + file);

        AL10.alSourceStop(sourcemap.get(file));
        playing.remove(file);
    }

    public static void stopLooping(String file) {

        if (!initok) {
            return;
        }

        Main.debug("[AL] Stop " + file);

        if (playing.contains(file)) {
            AL10.alSourceStop(sourcemap.get(file));
            playing.remove(file);
        }
    }

    public static void setGain(String file, float gain) {

        if (!initok) {
            return;
        }

        Main.debug("Setting gain of " + file + " to " + gain);

        AL10.alSourcef(sourcemap.get(file), AL10.AL_GAIN, gain);
    }

    public static void stopAllButTheMusic() {

        if (!initok) {
            return;
        }

        for (String f : sourcemap.keySet()) {
            if (!f.contains("music")) {
                stop(f);
            }
        }
    }

    public synchronized static void smashTV() {

        if (!initok) {
            return;
        }

        if (!playing.contains(A_STATIC)) {
            play(A_STATIC, true, gains.get(A_STATIC));
        }

        String[] stops = new String[]{
            A_MUSIC,
            A_HEART_FAST,
            A_HEART_MED,
            A_HEART_SLOW,
            A_FOOTSTEPS
        };
        for (String s : stops) {
            if (playing.contains(s)) {
                stop(s);
            }
        }
    }
}
