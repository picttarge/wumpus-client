/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package htwge;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.swing.JFrame;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

/**
 *
 * @author picttarge
 */
public class Main extends JFrame {

    private static boolean DEBUG = false;
    static final String DEFAULT_SERVERIP = "wumpus.uk.to";
    static final int DEFAULT_SERVERPORT = 5000;
    static String S_TITLE = "Hunt The Wumpus : Global Edition v1.4.2";
    static String S_RECONNECT = "reconnect";
    static String SERVERIP;
    static int SERVERPORT;
    static boolean[] keys = new boolean[1024];
    /** Graphics variables */
    static int[] c; // colour palette
    static char[] PX; // stored graphics data
    static int[] px; // screen pixel data (_wx_h in size)
    static int[] pxblank; // used for blanking the screen (arraycopy)
    static int _w = 1280;
    static int _h = 760;
    static Graphics r;
    static BufferedImage rbi;
    static final int margin = 32;
    static final int linespace = 16;
    /** Switching window size */
    static boolean windowactivated = false;
    static boolean resize = false;
    /** Font colorings */
    static final int[] FONT_WHITE = new int[]{15, 14}; // highlight, basic
    static final int[] FONT_YELLOW = new int[]{12, 11}; // highlight, basic
    static final int[] FONT_RED = new int[]{2, 9}; // highlight, basic
    static final int[] FONT_GREEN = new int[]{8, 7}; // highlight, basic
    static final int[] FONT_BLUE = new int[]{4, 3}; // highlight, basic
    static final int[] FONT_PURPLE = new int[]{10, 1}; // highlight, basic
    static final char C_WHITE = 'W';
    static final char C_YELLOW = 'Y';
    static final char C_RED = 'R';
    static final char C_GREEN = 'G';
    static final char C_BLUE = 'B';
    static final char C_PURPLE = 'P';
    /** Everything else */
    private static final StringBuilder console = new StringBuilder();
    private static Network network;
    private static final String CMD_PLAYERS = "PLAYERS";
    static final String CMD_MOVE = "MOVE";
    private static final String CMD_SHOOT = "SHOOT";
    private static Properties properties;
    /** smashTV */
    private static char[] wumpus;
    private static int[] signals = new int[]{0, 3, 0, 2, 0, 6, 0, 25, 0, 8};
    private static int tuner = 1;
    private static long tuned = System.currentTimeMillis();
    /** overlay */
    static boolean overlaytoggle = false;
    static StringBuilder overlaytext = new StringBuilder();
    private static boolean running = true;
    /** controllers/jinput */
    private static ArrayList<String> alControllers = new ArrayList<String>();
    private static boolean controllerselected = false;
    private static String selectedController;
    private static StringBuilder controllers = new StringBuilder();
    private static int selection = -1;
    private static Controller actualController;
    private static ArrayList<String> buttons = new ArrayList<String>();
    private static double[] buttonvalues;
    private static boolean anybutton = false;
    private static long anybuttontimer = 0;
    private static final String _A = "A";
    private static final String _B = "B";
    private static final String _X = "X";
    private static final String _Y = "Y";
    private static final String _LEFTTHUMB = "Left Thumb";
    private static final String _RIGHTTHUMB = "Right Thumb";
    private static final String _SELECT = "Select";
    private static final String _MODE = "Mode";
    private static final String _LEFTTHUMB3 = "Left Thumb 3";
    private static final String _RIGHTTHUMB3 = "Right Thumb 3";

    public static void debug(String s) {
        if (DEBUG) {
            System.out.println("[" + new Date() + "] " + s);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        Main o = new Main();
        o.setTitle(S_TITLE);
        o.setSize(_w, _h);
        o.setLocationByPlatform(true);
        o.setResizable(true);
        o.setUndecorated(false);
        o.enableEvents(KeyEvent.KEY_PRESSED | KeyEvent.KEY_RELEASED);
        o.setVisible(true);

        initControllers();
        formControllersString();

        initGraphics(o);

        pause(500);

        try {
            SoundFactory.init();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println(e.getMessage());
            System.exit(1);
        }

        pause(500);

        loadProperties();

        network = new Network(SERVERIP, SERVERPORT, (_h / linespace) - (linespace >> 1));
        //network.reconnect(SERVERIP); // only uncomment if forcing menu (no conn to server)
        network.start();

        while (running) {

            if (resize) {
                resize = false;
                Thread.sleep(500);
                initGraphics(o);
                Thread.sleep(500);
                continue;
            }

            if (!network.connect) {
                tvBorderScreen();
                if (network.firstdisconnect) {
                    if (console.indexOf(S_RECONNECT) == -1) {
                        console.setLength(0);
                        console.append(S_RECONNECT).append(" ").append(SERVERIP);
                        network.firstdisconnect = false;
                    }
                }
            } else {
                if (network.connect && network.firstconnect) {
                    console.setLength(0);
                    console.append(System.getProperty("user.name"));
                    network.firstconnect = false;
                }

                if (network.respawntimer > 0 && !network.dead) {
                    //if (System.currentTimeMillis() - network.respawntimer >= network.RESPAWNDEATHTIME) {
                    network.dead = true;
                    network.respawntimer = System.currentTimeMillis();
                    //}
                    clearScreen();
                } else if (network.dead && network.respawntimer > 0) {

                    if (System.currentTimeMillis() - network.respawntimer >= network.RESPAWNDEATHTIME) {
                        network.dead = false;
                        network.respawntimer = 0;
                        clearScreen();
                    }
                } else {
                    clearScreen();
                }
            }

            if (alControllers.size() > 0) {
                Controllers.poll();

                updateControllerDetails();
            }

            if (!network.dead) {

                drawString(getIntArray(";Whunt the ;Rwumpus;W: ;GGlobal ;WEdition"), margin, (linespace * 3), 2);
                drawString(getIntArray(";B==============================="), margin, (linespace * 4), 2);

                if (console.length() > 0) {
                    drawString(getIntArray(";B" + console.toString() + "_"), margin, _h - (linespace * 3), 2);
                }

                if (network.history != null) {
                    for (int i = 0; i < network.history.size(); i++) {
                        if (network.history.get((network.history.size() - 1) - i) != null) {
                            drawString(getIntArray(network.history.get(((network.history.size() - 1) - i))), margin, _h - 64 - (linespace * i), 2);
                        }
                    }
                }
            }

            if (!network.connect) {
                if (System.currentTimeMillis() - tuned >= 500 + ((int) (Math.random() * 2000))) {
                    tuner = signals[(int) (Math.random() * signals.length)];
                    tuned = System.currentTimeMillis();
                }
                tvStaticScreen(false);
                drawString(getIntArray(";Whunt the ;Rwumpus"), (_w >> 1) + (8 * 14), _h - (linespace * 3), 2);
                drawString(getIntArray(";GGlobal ;WEdition"), (_w >> 1) + (8 * 14), _h - (linespace * 2), 2);

            } else if (network.dead && network.respawntimer > 0) {
                bloodStaticScreen(true);
            }

            if (overlaytoggle || !controllerselected) {
                if (!controllerselected) {
                    overlayScreen(controllers.toString());
                } else {
                    overlayScreen(overlaytext.toString());
                }
            }

            r.drawImage(rbi, 0, 0, null);
            Thread.sleep(32); // limit to approx 30 fps
        }

        cleanup();
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
        }
    }

    static void tvStaticScreen(boolean fullscreen) {

        int[] statics = new int[]{0, 13, 14, 15};
        for (int x = 0; x < _w; x++) {
            for (int y = 0; y < _h; y++) {
                if (y < (_h - (fullscreen ? 0 : 70))) {

                    try {
                        if (tuner > 0 && wumpusLookup(x, y) == 49) {
                            int idx = (int) (Math.random() * (statics.length - 1));
                            px[x + (y * _w)] = c[(tuner == 25 || y % 2 == 0 ? statics[idx] : 0)];
                        } else {
                            if ((y) % 2 == 0) {
                                px[x + (y * _w)] = c[0];
                                continue;
                            }
                            if ((x) % 2 == 0) {
                                px[x + (y * _w)] = c[0];
                                continue;
                            }
                            int idx = (int) (Math.random() * statics.length);
                            px[x + (y * _w)] = c[statics[idx]];
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // only tv static
                    }
                }
            }
        }
    }

    static int wumpusLookup(int x, int y) {
        x %= 26;
        y %= tuner;
        // 26 x 25
        if (x >= 0 && x < 26) {
            if (y >= 0 && y < 25) {
                return wumpus[x + (y * 26)];
            }
        }
        return -1;
    }

    static void bloodStaticScreen(boolean fullscreen) {
        int[] statics = new int[]{0, 2, 9};

        for (int i = 0; i < px.length >> 1; i++) {
            int x = (int) (Math.random() * _w);
            int y = (int) (Math.random() * _h);

            if (y < (_h - (fullscreen ? 0 : 70))) {
                int idx = (int) (Math.random() * statics.length);
                px[x + (y * _w)] = c[statics[idx]];
            }
        }
    }

    static void overlayScreen(String s) {

        try {
            int scoremargin = margin << 2;
            for (int x = 0; x < _w; x++) {
                for (int y = 0; y < _h; y++) {

                    if ((x >= scoremargin && x <= _w - scoremargin)
                            && (y >= scoremargin && y <= _h - scoremargin)) {
                        if (x == scoremargin || x == _w - scoremargin
                                || y == scoremargin || y == _h - scoremargin) {
                            px[x + (y * _w)] = c[12];
                        } else {
                            px[x + (y * _w)] = c[0];
                        }

                    } else {
                        if (x % 2 == 0 || y % 2 == 0) {
                            px[x + (y * _w)] = c[0];
                        }
                    }
                }
            }

            String[] lines = s.split("\n");

            int cnt = 0;
            int ycentered = ((lines.length * linespace) >> 1);
            for (String x : lines) {
                String trimmed = x.trim();
                int xcentered = ((trimmed.length() * 13) >> 1);

                drawString(getIntArray(trimmed), (_w >> 1) - xcentered, (_h >> 1) - ycentered + (linespace * cnt), 2);
                cnt++;
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            // ignore
        }


    }

    static void tvBorderScreen() {
        for (int x = 0; x < _w; x++) {
            for (int y = 0; y < _h; y++) {
                if (y >= (_h - 70)) {
                    px[x + (y * _w)] = c[0];
                }
            }
        }
    }

    static void clearScreen() {
        System.arraycopy(pxblank, 0, px, 0, px.length);
    }

    static int[] getIntArray(String s) {
        char[] ca = s.toUpperCase().toCharArray();
        String[] split = s.split(";");
        int[] ia = new int[ca.length - (split.length - 1)];
        boolean changecol = false;
        int z = 0;
        for (int i = 0; i < ca.length; i++) {

            if (ca[i] == ';') {
                // special magic colouring bananas
                changecol = true;
                continue;
            }

            if (ca[i] == 32) {
                ia[z] = -32;
                z++;
            } else if (ca[i] >= 33 && ca[i] < 97) {

                if (changecol) {
                    // big ;Bstring;W of stuff
                    switch (ca[i]) {
                        case C_WHITE:
                            ia[z] = -128;
                            break;
                        case C_YELLOW:
                            ia[z] = -129;
                            break;
                        case C_RED:
                            ia[z] = -130;
                            break;
                        case C_GREEN:
                            ia[z] = -131;
                            break;
                        case C_BLUE:
                            ia[z] = -132;
                            break;
                        case C_PURPLE:
                            ia[z] = -133;
                            break;
                        default:
                            ia[z] = -128;
                    }
                    z++;
                    changecol = false;
                    continue;
                } else {
                    ia[z] = ca[i] - 33; // -22 here because ASCII 48 = 0, and the font has 0 at pos 26, so 48-22 = 26 = "0"
                    z++;
                }
            } else {
                ia[z] = -32;
                z++;
            }
        }
        return ia;
    }

    static void drawString(int[] chars, int posx, int posy, int m) {

        int[] colours = FONT_WHITE;

        int offset = 0;
        for (int w = 0; w < chars.length; w++) {

            if (chars[w] >= -133 && chars[w] <= -128) {
                switch (chars[w]) {
                    case -128:
                        colours = FONT_WHITE;
                        break;
                    case -129:
                        colours = FONT_YELLOW;
                        break;
                    case -130:
                        colours = FONT_RED;
                        break;
                    case -131:
                        colours = FONT_GREEN;
                        break;
                    case -132:
                        colours = FONT_BLUE;
                        break;
                    case -133:
                        colours = FONT_PURPLE;
                        break;
                    default:
                        colours = FONT_WHITE;
                }
                offset++;
                continue;
            }

            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 6; x++) {
                    char cc = 1;
                    if (chars[w] == -32) {
                        continue;
                    } else if (chars[w] >= -133 && chars[w] <= -128) {
                        continue;// do nothing 
                    } else {
                        int idx = (chars[w] * 6) + x + (y * 384);
                        if (idx < PX.length) {
                            cc = PX[idx];
                        } else {
                            // cc = ' ';
                        }
                    }

                    int cidx = -1;
                    if (cc == '2') {
                        cidx = colours[0];
                    }
                    if (cc == ' ') {
                        cidx = colours[1];
                    }

                    for (int yy = 0; yy < 2; yy++) {
                        for (int xx = 0; xx < 2; xx++) {
                            if (cidx > -1) {
                                int val = posx + ((w - offset) * 14) + xx + (x << 1) + ((posy + yy + (y << 1)) * _w);
                                if (val < px.length && val >= 0) {
                                    px[val] = c[cidx];
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean isRunningJavaWebStart() {
        return System.getProperty("javawebstart.version", null) != null;
    }

    private static void loadProperties() {

        if (isRunningJavaWebStart()) {
            SERVERIP = System.getProperty("serverip", DEFAULT_SERVERIP);
            SERVERPORT = Integer.parseInt(System.getProperty("serverport", "" + DEFAULT_SERVERPORT));
        } else {
            properties = new Properties();
            try {
                properties.load(new FileReader("server.properties"));
                SERVERIP = properties.getProperty("serverip", DEFAULT_SERVERIP);
                SERVERPORT = Integer.parseInt(properties.getProperty("serverport", "" + DEFAULT_SERVERPORT));
            } catch (IOException e) {
                System.err.println("unable to load properties : need server.properties with serverip= and serverport=");
                System.exit(1);
            }

            saveProperties();
        }
    }

    private static void saveProperties() {
        try {
            properties.setProperty("serverip", SERVERIP);
            properties.setProperty("serverport", "" + SERVERPORT);
            properties.store(new FileWriter("server.properties"), "Hunt The Wumpus : Global Edition. Default IP=electune.dyndns.org Default Port=5000");
        } catch (IOException e) {
            System.err.println("unable to load properties : need server.properties with serverip= and serverport=");
            System.exit(1);
        }
    }

    private static void initGraphics(Main o) {
        /** Init stored graphics data */
        PX = ( // 1920
                "112 1112 2 1121211111111111111111111112 11111211112111112 1111121111111111111111111111112112   1112 1122   122   12211  22    12   122    12   112   11122111122111112 111111112 11112  1111221112   122   112   122   122    22    12    2211  22    1111222211  22111122111 2211  12   122   112   122   112    22    2211  2211  22111 2211  2211  22    1122 1 11111122 11111111111111111111"
                + "112 111 1 112    1111111111111111111112 1111211111121112   1111 111111111111111111111112 12 11  12  111111  1111  2 11  2 11112 1111111  12 11  2 11  112 11112 11112 1112   1112 111111 1121  12 11  2 11  2 11  2 11  2 11112 11112 11112 11  11  1111112 2 1  12 11112  1  2  1  2 11  2 11  2 11  2 11  2 111111  112 11  2 11  2 1 1 1    12 11  111  111 1111 1111111 11111111111111111111"
                + "112 111111111 1 11111111111111      111 1111 111111 1111  11122   111111122  1111111112 11  11  11  111    11    12     2    12    111  111    11     11111111111112 111111111111  1112 1121 1 12     2    12 11112 11  2   112   112 1   2     11  1111112 2   112 11112     2     2 11  2    12 11  2    11    111  112 11  2 112 2     11  111    111  1111 11111 111111 11111111111111      "
                + "1111111111112    1111111      11111111111111 111111 111211 1111 11112 11111111112 1112 111  112 112 112 111111112 11112 11112   112 12 111  112 11112 112211112 1111  1112   111  111111111  1 1  112   112   112   112   1111  1111  112   112 11  11 211    12 1  1111  1 12  12    112   1111  1 21  112 11112 112 11  112 1    1   1 21  2 1112 1112 11111 111111 11111 11111111      111111"
                + "112 111111111 1 11      111111111111111111111 1111 111111111111 1112 11111111111  111 11111    1                 11111       11    11  1111    11    1112 111  111111  111111112 111112 1111  11  11       11    1     1        11111       11        1    1  11          111   11  1    1  11111       11       111  111    111  11  111   11  11  11      11   11111 112  11      111111111111").toCharArray();

        /** Init colour palette */
//        c = new int[]{
//                    0x040404, // 0 black
//                    0x206020, // 1 vd green
//                    0x602000, // 2 vd red
//                    0x404040, // 3 vd grey5
//                    0x606060, // 4 vd grey4
//                    0x204080, // 5 dark blue
//                    0x2060C0, // 6 light blue
//                    0x208000, // 7 dark green
//                    0x40A000, // 8 light green
//                    0xA00000, // 9 red
//                    0x804000, // 10 dark brown
//                    0xA06000, // 11 brown
//                    0xA0A000, // 12 yellow
//                    0x808080, // 13 dark grey3
//                    0xA0A0A0, // 14 dark grey2
//                    0xC0C0C0}; // 15 grey1
        c = new int[]{
                    0x000000, // 0 black
                    0x800080, // 1 purple
                    0x800000, // 2 vd red
                    0x008080, // 3 teal
                    0x00FFFF, // 4 aqua
                    0x000080, // 5 dark blue
                    0x0000FF, // 6 light blue
                    0x008000, // 7 dark green
                    0x00FF00, // 8 light green
                    0xFF0000, // 9 red
                    0xFF00FF, // 10 magenta/fus.
                    0x808000, // 11 brown
                    0xFFFF00, // 12 yellow
                    0xFFFFFF, // 13 white
                    0x808080, // 14 dark grey
                    0xC0C0C0}; // 15 light grey1

        r = o.getGraphics();
        rbi = new BufferedImage(_w, _h, BufferedImage.TYPE_INT_RGB);
        px = ((DataBufferInt) (rbi.getRaster().getDataBuffer())).getData();
        pxblank = new int[px.length];
        for (int i = 0; i < pxblank.length; i++) {
            pxblank[i] = c[0];
        }

        wumpus = ("11111111122222222111111111"
                + "11111111222222222211111111"
                + "11111112222222222221111111"
                + "11111122222222222222111111"
                + "11111222222222222222211111"
                + "11112222222222222222221111"
                + "11122222211222212222222111"
                + "11222222211222112222222211"
                + "12222222211222121222222221"
                + "22222222222222222222222222"
                + "22222122222222222221222222"
                + "22221122222222222221122222"
                + "22221122222222222221112222"
                + "22221122121212212121112222"
                + "22221121111111111111112222"
                + "22221121111111111111112222"
                + "22221122121212212121112222"
                + "22221122222222222221112222"
                + "22221122222222222221112222"
                + "12222112222222222211122221"
                + "11222211222222222111222211"
                + "11122221111111111112222111"
                + "11222222111111111122222211"
                + "12222222211111111222222221"
                + "22221122221111112222112222").toCharArray();
    }

    private static void cleanup() {
        SoundFactory.killALData();
        System.exit(0);
    }

    private static void initControllers() {

        try {
            Controllers.create();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        int count = Controllers.getControllerCount();

        debug("[jinput] " + count + " Controllers Found");

        if (count == 0) {
            controllerselected = true;
            return;
        }

        for (int i = 0; i < count; i++) {
            actualController = Controllers.getController(i);
            alControllers.add(actualController.getName()
                    + " (" + actualController.getButtonCount() + " button, "
                    + (actualController.getButtonCount() + actualController.getAxisCount() + 2) + " item)");
            debug("[jinput] " + actualController.getName());

            buttonvalues = new double[actualController.getButtonCount() + actualController.getAxisCount() + 2];
            buttons.clear();
            for (int b = 0; b < actualController.getButtonCount(); b++) {
                debug("[jinput] button: " + actualController.getButtonName(b));
                buttons.add(actualController.getButtonName(b));
            }

            for (int j = actualController.getButtonCount();
                    j < actualController.getButtonCount() + actualController.getAxisCount();
                    j++) {
                debug("[jinput] axis: " + actualController.getAxisName(j - actualController.getButtonCount()));
                buttons.add(actualController.getAxisName(j - actualController.getButtonCount()));
            }

            buttons.add("povx");
            buttons.add("povy");
        }
    }

    private static void formControllersString() {
        controllers.setLength(0);
        controllers.append(selection == -1 ? ";GKeyboard\n" : ";WKeyboard\n");
        for (int i = 0; i < alControllers.size(); i++) {
            if (selection == i) {
                controllers.append(";G");
            } else {
                controllers.append(";W");
            }
            controllers.append(alControllers.get(i) + "\n");
        }
    }

    private static void updateControllerDetails() {

        if (actualController != null) {
            if (buttonvalues == null) {
                buttonvalues = new double[actualController.getButtonCount() + actualController.getAxisCount() + 2];
            }
            anybutton = false;
            for (int i = 0; i < actualController.getButtonCount(); i++) {
//                debug(""+i+": "+buttons.get(i)+" => "+ actualController.isButtonPressed(i));
                boolean newvalue = actualController.isButtonPressed(i);
                if (buttonvalues[i] == 0 && newvalue) {
                    if (!controllerselected) {
                        SoundFactory.play(SoundFactory.A_SHOTGUN, false, -1);
                    }

                    if (buttons.get(i).equals(_MODE)) {
                        network.commands.add(CMD_PLAYERS);
                    } else if (buttons.get(i).equals(_LEFTTHUMB)
                     || buttons.get(i).equals(_RIGHTTHUMB)) {
                        overlaytoggle = !overlaytoggle;
                    } else {
                        enter(true);
                    }
                }
                buttonvalues[i] = newvalue ? 1 : 0;
                if (newvalue) {
                    anybutton = true;
                }
            }

//            if (System.currentTimeMillis() - anybuttontimer >= 2000) {
//                anybuttontimer = System.currentTimeMillis();
//            }

            int buttonCount = actualController.getButtonCount();
            float threshold = 0.5f;
            for (int i = buttonCount; i < buttonCount + actualController.getAxisCount(); i++) {
                
                float newvalue = actualController.getAxisValue(i - buttonCount);
                if (buttonvalues[i] > -threshold 
                        && buttonvalues[i] < threshold
                        && (newvalue <= -threshold || newvalue >= threshold)) {


                    //debug(""+i+". "+actualController.getAxisName(i- buttonCount)+" "+newvalue);
                    if (i == 10) { // START on xbox pad
                        enter(true);
                    } else if (i == 12 || i == 15) {
                        
                        if (!controllerselected) {
                            SoundFactory.play(SoundFactory.A_BUMP, false, -1);
                        }
                        // up and down
                        if (newvalue < -threshold) {
                            up();
                        } else if (newvalue > threshold) {
                            down();
                        }
                    } else if (i == 11 || i == 14) {

                        // left and right
                        if (newvalue < -threshold) {
                            if (!controllerselected) {
                                SoundFactory.playLeft(SoundFactory.A_BUMP, false);
                            }
                            left();
                        } else if (newvalue > threshold) {
                            if (!controllerselected) {
                                SoundFactory.playRight(SoundFactory.A_BUMP, false);
                            }
                            right();
                        }
                    }
                }
                buttonvalues[i] = newvalue;
            }

            int i = buttonCount + actualController.getAxisCount();
            float povx_new = actualController.getPovX();
            if (buttonvalues[i] == 0 && povx_new != 0) {

                if (povx_new < 0) {
                    if (!controllerselected) {
                        SoundFactory.playLeft(SoundFactory.A_BUMP, false);
                    }
                    left();
                } else {
                    if (!controllerselected) {
                        SoundFactory.playRight(SoundFactory.A_BUMP, false);
                    }
                    right();
                }
            }
            buttonvalues[i] = povx_new;
            //debug(""+i+": "+buttons.get(i)+" => "+ buttonvalues[i]);
            i++;
            float povy_new = actualController.getPovY();
            if (buttonvalues[i] == 0 && povy_new != 0) {
                if (povy_new < 0) {
                    up();
                } else {
                    down();
                }
            }
            buttonvalues[i] = povy_new;
            //debug(""+i+": "+buttons.get(i)+" => "+ buttonvalues[i]);

            if (DEBUG) {
                for (int n = 0; n < buttonvalues.length; n++) {
                    String s = "" + n + ". " + buttons.get(n) + ":" + buttonvalues[n];
                    drawString(getIntArray(s), _w - (14 * s.length()), _h - linespace - (linespace * n), 2);
                }
            }
        }
    }

    @Override
    public void processEvent(AWTEvent e) {

        switch (e.getID()) {
            case WindowEvent.WINDOW_ACTIVATED:
                windowactivated = true;
                break;
            case ComponentEvent.COMPONENT_RESIZED:
                debug(windowactivated + "? component resized: " + ((ComponentEvent) e).paramString());

                if (windowactivated) {
                    int newwidth = ((ComponentEvent) e).getComponent().getWidth();
                    int newheight = ((ComponentEvent) e).getComponent().getHeight();
                    debug("w: " + newwidth);
                    debug("h: " + newheight);
                    if (_w != newwidth || _h != newheight) {
                        // resize required
                        _w = newwidth;
                        _h = newheight;
                        resize = true;
                    }
                }
                break;
            case WindowEvent.WINDOW_CLOSING:
                System.out.println("Exiting cleanly (window closed)...");
                running = false;
                break;
            case KeyEvent.KEY_PRESSED:
                keys[((KeyEvent) e).getKeyCode()] = true;
                debug("KEY PRESSED: " + ((KeyEvent) e).getKeyCode());
                switch (((KeyEvent) e).getKeyCode()) {
                    // immediate
                    case 8:
                        if (console.length() >= 1) {
                            console.deleteCharAt(console.length() - 1);
                        }
                        break;
                    case 10: // enter
                        enter(false);
                        break;
                    case 38: // up
                        up();
                        break;
                    case 40: // down
                        down();
                        break;
                    case 37: // left
                        left();
                        break;
                    case 39: // right
                        right();
                        break;
                    case 27:
                        System.out.println("Exiting cleanly (escape pressed)...");
                        running = false;
                        break;
                    case 112: // F1
                        overlaytoggle = !overlaytoggle;
                        break;
                    case 113: // F2
                        network.commands.add(CMD_PLAYERS);
                        break;
                    case 114: // F3 - debug mode
                        DEBUG = !DEBUG;
                        break;
                }

                break;
            case KeyEvent.KEY_RELEASED:
                int code = ((KeyEvent) e).getKeyCode();
                if (code >= 32) { // do not append control chars
                    if (code >= 37 && code <= 40) {
                        // arrow keys probably
                        return;
                    }
                    if (code >= 112 && code <= 123) {
                        // function keys
                        return;
                    }

                    console.append((char) ((KeyEvent) e).getKeyCode());
                }

                if (((KeyEvent) e).getKeyCode() != 27) {
                    keys[((KeyEvent) e).getKeyCode()] = false;
                }
                break;
        }
    }

    private static void up() {
        if (!controllerselected) {
            SoundFactory.play(SoundFactory.A_BUMP, false, -1);
            if (selection - 1 < -1) {
                selection = alControllers.size() - 1;
            } else {
                selection--;
            }

            formControllersString();
        } else if (!network.waitingforplayers) {
            network.commands.add((anybutton || keys[16] || keys[17] ? CMD_SHOOT : CMD_MOVE) + " NORTH");
        }
    }

    private static void down() {
        if (!controllerselected) {
            SoundFactory.play(SoundFactory.A_BUMP, false, -1);
            if (selection + 1 > alControllers.size() - 1) {
                selection = -1;
            } else {
                selection++;
            }
            formControllersString();
        } else if (!network.waitingforplayers) {
            network.commands.add((anybutton || keys[16] || keys[17] ? CMD_SHOOT : CMD_MOVE) + " SOUTH");
        }
    }

    private static void left() {
        if (!controllerselected) {
        } else if (!network.waitingforplayers) {
            network.commands.add((anybutton || keys[16] || keys[17] ? CMD_SHOOT : CMD_MOVE) + " WEST");
        }
    }

    private static void right() {
        if (!controllerselected) {
        } else if (!network.waitingforplayers) {
            network.commands.add((anybutton || keys[16] || keys[17] ? CMD_SHOOT : CMD_MOVE) + " EAST");
        }
    }

    private static void enter(boolean gamepad) {
        if (!controllerselected) {
            SoundFactory.play(SoundFactory.A_WUMPUS_DEATH, false, -1);
            controllerselected = true;
            if (selection == -1) {
                // keyboard
                debug("Using keyboard");
            } else {
                actualController = Controllers.getController(selection);
                debug("Using " + actualController.getName());
            }
        } else {
            // submit console
            if (gamepad) {
                if (console.length() == 0 && network.firstresetroom) {
                    // nada
                } else {
                    submitConsole();
                }
            } else {
                submitConsole();
            }
        }
    }

    private static void submitConsole() {
        if (network != null) {
            if (console.length() == 0) {
                console.append('\n');
            }

            debug(console.toString());

            if (console.indexOf(S_RECONNECT) == 0) {
                String[] sa = console.toString().split("\\s");
                SERVERIP = sa[1];
                network.reconnect(SERVERIP);
                console.setLength(0);
                saveProperties();
                return;
            }

            if (!network.dead) {
                if (console.indexOf(CMD_MOVE) == 0
                        && network.waitingforplayers) {
                    // nope
                } else {
                    network.commands.add(console.toString());
                    console.setLength(0);
                }
            }
        }
    }

    /**
     * Does what it says on the tin - a fake stackTrace
     * when you can't be bothered running a proper debugger
     */
    public static void fakeTrace() {
        try {
            throw new RuntimeException("fake as hell");
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
