package htwge;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

public class SoundFactory {

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
    public static final HashMap<String, Integer> sourcemap = new HashMap<String, Integer>();
    public static final HashSet<String> playing = new HashSet<String>();
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
        }
    };
    public static final int MAX_BUFFERS = 128;
    public static final ArrayList<String> soundsToProcess = new ArrayList<String>();
    public static final ArrayList<String> serverSoundList = new ArrayList<String>();
    private static final FilenameFilter filter = new FileListFilter("sound file extension", "wav");
    private static final String[] soundFiles = new File("./sounds").list(filter);
    private static ArrayList<String> accessFiles = new ArrayList<String>();
    /** Buffers hold sound data. */
    private static IntBuffer buffer = BufferUtils.createIntBuffer(soundFiles.length);
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

    static void init() {

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
            if (Main.DEBUG) {
                System.err.println("[AL] Error loading wave data.");
            }
        }

        setListenerValues();
    }

    static int loadALData() {

        // Load wav data into a buffer.
        AL10.alGenBuffers(buffer);

        int errcode = AL10.alGetError();
        if (errcode != AL10.AL_NO_ERROR) {
            if (Main.DEBUG) {
                System.err.println("[AL] A Error code: " + errcode);
            }
            return AL10.AL_FALSE;
        }

        WaveData waveFile = null;

        for (int i = 0; i < soundFiles.length; i++) {
            if (Main.DEBUG) {
                System.out.println("[AL] Adding sound file: " + soundFiles[i]);
            }
            accessFiles.add(i, soundFiles[i]);

            try {
                waveFile = WaveData.create(new BufferedInputStream(new FileInputStream("./sounds/" + soundFiles[i])));
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                e.printStackTrace(System.err);
            }
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
            if (Main.DEBUG) {
                System.err.println("[AL] B ERROR CODE: " + errcode);
            }
            return AL10.AL_FALSE;
        }
    }

    static private void addSource(int type, boolean repeat, float definedgain, String file, FloatBuffer sp) {
        int position = source.position();
        if (Main.DEBUG) {
            System.out.println("[AL] source position for " + file + " " + position);
        }
        source.limit(position + 1);
        AL10.alGenSources(source);

        int alerror = AL10.alGetError();
        if (alerror != AL10.AL_NO_ERROR) {
            System.out.println("[AL] Error generating audio source. " + alerror + " " + file+" "+source.get(position));
            Main.fakeTrace();
            System.exit(-1);
        }

        if (Main.DEBUG) {
            System.out.println("[AL] adding to sourcemap " + file + " " + source.get(position));
        }
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
        if (Main.DEBUG) {
            System.out.println("[AL] Cleaning up all OpenAL data");
        }
        for (int i : sourcemap.values()) {
            AL10.alSourceStop(i);
        }

        if (source != null) {
            int position = source.position();
            source.position(0).limit(position);
            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(buffer);
        }

        buffer.clear();
        source.clear();

        AL.destroy();

        if (Main.DEBUG) {
            System.out.println("[AL] Cleanup complete");
        }
    }

    public static void play(String file, boolean loop, float gain) {
        if (Main.DEBUG) {
            System.out.println("[AL] >) (< CENTER playing (" + loop + ") " + file);
        }

        if (sourcemap.containsKey(file)) {
            AL10.alSourcef(sourcemap.get(file), AL10.AL_GAIN, gain >= 0 ? gain : gains.get(file));
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
            if (Main.DEBUG) {
                System.out.println("playing already added source " + file + " " + sourcemap.get(file));
            }
        } else {
            if (Main.DEBUG) {
                System.out.println("adding new source " + file + " " + sourcemap.get(file));
            }
            addSource(accessFiles.indexOf(file), loop, gain >= 0 ? gain : gains.get(file), file, sourcePos);
        }
    }

    public static void playLeft(String file, boolean loop) {
        if (Main.DEBUG) {
            System.out.println("[AL] >) LEFT playing (" + loop + ") " + file);
        }
        if (sourcemap.containsKey(file)) {
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
        } else {
            addSource(accessFiles.indexOf(file), loop, gains.get(file), file, sourcePosLeft);
        }
    }

    public static void playRight(String file, boolean loop) {
        if (Main.DEBUG) {
            System.out.println("[AL] (< RIGHT playing (" + loop + ") " + file);
        }
        if (sourcemap.containsKey(file)) {
            AL10.alSourcePlay(sourcemap.get(file));
            playing.add(file);
        } else {
            addSource(accessFiles.indexOf(file), loop, gains.get(file), file, sourcePosRight);
        }
    }

    public static void stop(String file) {
        if (Main.DEBUG) {
            System.out.println("[AL] Stop " + file);
        }
        AL10.alSourceStop(sourcemap.get(file));
        playing.remove(file);
    }

    public static void stopLooping(String file) {
        if (Main.DEBUG) {
            System.out.println("[AL] Stop " + file);
        }
        if (playing.contains(file)) {
            AL10.alSourceStop(sourcemap.get(file));
            playing.remove(file);
        }
    }

    public static void setGain(String file, float gain) {
        if (Main.DEBUG) {
            System.out.println("Setting gain of " + file + " to " + gain);
        }
        AL10.alSourcef(sourcemap.get(file), AL10.AL_GAIN, gain);
    }

    public static void stopAllButTheMusic() {

        for (String f : sourcemap.keySet()) {
            if (!f.contains("music")) {
                stop(f);
            }
        }
    }

    public synchronized static void smashTV() {

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

class FileListFilter implements FilenameFilter {

    private String extension;

    public FileListFilter(String name, String extension) {
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
