/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package htwge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author picttarge
 */
public class Network implements Runnable {

    private String SERVERIP;
    private final int SERVERPORT;
    private final String KEY = "@#"; // i am the fancy client
    private final String DEATH = ";S4";
    private final String SHOTGUN = ";S5";
    private final String WUMPUSDEATH = ";S6";
    private final String CMD_READYTOMOVE = ";M";
    private final String CMD_SOUND = ";S";
    private final String CMD_SCOREBOARDTAG = ";T";
    private final String CMD_CLEAR = ";U";
    private final String CMD_RESET = ";Z";
    private final String S_FIRESHOTGUN = "You fire your shotgun to the";
    private final String S_WAITING_FOR_PLAYERS = "Waiting for other players";
    /** Threads */
    private Thread threadRead;
    private Thread threadWrite;
    private final static String WumpusNetworkRead = "R";
    private final static String WumpusNetworkWrite = "W";
    /** Sockets */
    private Socket sock;
    private PrintWriter out;
    private BufferedReader in;
    private boolean firstcommand = true;
    private final int MAXHISTORYLINES;
    /* Public members */
    public LinkedList<String> history;
    public CopyOnWriteArrayList<String> commands;
    public boolean connect = false;
    public int RESPAWNDEATHTIME = 2000;
    public long respawntimer = 0;
    public boolean dead = false;
    public boolean waitingforplayers = false;
    public boolean firstdisconnect = true;
    public boolean firstconnect = true;
    private boolean wumpusdead = false;
    private boolean importantsounds = false;
    private boolean pauseforshotgun = false;
    private int spas12ammo = 8 + 1; // 8 in the mag, one in the chamber
    public boolean firstresetroom = false;

    public Network(final String SERVERIP, final int SERVERPORT, final int MAXHISTORYLINES) {
        this.SERVERIP = SERVERIP;
        this.SERVERPORT = SERVERPORT;
        this.MAXHISTORYLINES = MAXHISTORYLINES;
        history = new LinkedList<String>();
        commands = new CopyOnWriteArrayList<String>();
        threadRead = new Thread(this, WumpusNetworkRead);
        threadWrite = new Thread(this, WumpusNetworkWrite);
    }

    public void start() {
        threadRead.start();
        threadWrite.start();
    }

    public synchronized void openSocket() {
        if (connect) {
            return;
        }

        firstcommand = true;

        while (!connect) {
            try {
                sock = new Socket();
                sock.connect(new InetSocketAddress(SERVERIP, SERVERPORT), 1000);

                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                out = new PrintWriter(sock.getOutputStream(), true);
                connect = true;
                firstconnect = true;
                SoundFactory.stopLooping(SoundFactory.A_STATIC);
                SoundFactory.play(SoundFactory.A_MUSIC, true, -1);
            } catch (IOException e) {
                connect = false;
                firstdisconnect = true;
                SoundFactory.smashTV();
                String rc = ";RReconnecting to " + SERVERIP + ":" + SERVERPORT;

                if (history != null) {
                    if (history.size() == 0 || (history.size() > 0 && !rc.equals(history.getLast()))) {
                        history.add(rc);
                    }
                }
                Main.debug("[NETWORK] Reconnecting: " + e.getMessage());

                if (sock != null) {
                    if (!sock.isClosed()) {
                        try {
                            sock.close();
                        } catch (IOException ioe) {
                            sock = null;
                        }
                    }
                }
                pause(500);
            }
        }
    }

    public void run() {

        Main.debug("[NET] thread: " + Thread.currentThread().getName());

        if (!connect) {
            openSocket();
        }

        if (WumpusNetworkRead.equals(Thread.currentThread().getName())) {
            while (!connect) {
                pause(500);
            }
            reading();
        }

        if (WumpusNetworkWrite.equals(Thread.currentThread().getName())) {
            while (!connect) {
                pause(500);
            }
            writing();
        }
    }

    public void reading() {
        while (true) {

            if (!connect) {
                openSocket();
            }
            try {
                addHistory(";GConnected to server " + SERVERIP + ":" + SERVERPORT);
                String buffer = null;
                while ((buffer = in.readLine()) != null) {
                    Main.debug("RX:" + buffer);

                    if (buffer.indexOf(CMD_SOUND) > -1) {
                        String[] sa = buffer.replaceAll(CMD_SOUND, "").split("\\|");
                        String id = sa[0];
                        String meta = (sa.length > 1 ? sa[1] : null);
                        Main.debug("[NET] Metadata for " + buffer + " " + meta);

                        if (SoundFactory.important.contains(SoundFactory.map.get(id))) {
                            importantsounds = true;
                        }
                        String sf = SoundFactory.map.get(id);

                        boolean loop = sf.indexOf("loop_") == 0;

                        if (meta != null) {
                            String[] md = meta.split(",");
                            int dx = Integer.parseInt(md[0]);
                            int dy = Integer.parseInt(md[1]);
                            if (dx < 0) {
                                // X = left
                                SoundFactory.playLeft(sf, loop);
                            } else if (dx > 0) {
                                // X = right
                                SoundFactory.playRight(sf, loop);
                            } else if (dy < 0) {
                                // Y = behind
                                // and how do you propose to simulate this?
                                SoundFactory.play(sf, loop, -1);
                            } else if (dy > 0) {
                                // Y = front
                                // and how do you propose to simulate this?
                                SoundFactory.play(sf, loop, -1);
                            } else {
                                SoundFactory.play(sf, loop, -1);
                            }
                        } else {
                            SoundFactory.play(sf, loop, -1);
                        }

                        // special hooks
                        if (buffer.indexOf(DEATH) > -1) {
                            respawntimer = System.currentTimeMillis();
                            spas12ammo = 8 + 1;
                        }
                        if (buffer.indexOf(SHOTGUN) > -1) {
                            pauseforshotgun = true;
                        }
                        if (buffer.indexOf(WUMPUSDEATH) > -1) {
                            wumpusdead = true;
                            Main.overlaytext.setLength(0); // prepare for scoreboard
                        }
                    } else if (buffer.indexOf(CMD_RESET) > -1) {
                        firstresetroom = true;
                        if (wumpusdead) {
                            pause(5000);// allow death, scoreboard, etc
                        } else if (pauseforshotgun) {
                            pause(1750);
                        }
                        pauseforshotgun = false;
                        importantsounds = false;
                        SoundFactory.stopAllButTheMusic();
                        pause(100);
                        wumpusdead = false;
                        Main.overlaytoggle = false;
                    } else if (buffer.indexOf(CMD_CLEAR) > -1) {
                        Main.overlaytoggle = false;
                        history.clear();
                        firstresetroom = false; // allow gamepad to fire for respawn
                    } else if (buffer.indexOf(CMD_READYTOMOVE) == 0) {
                        Main.debug("setting music to gain level, important?=" + importantsounds);
                        SoundFactory.setGain(SoundFactory.A_MUSIC, importantsounds ? 0.25f : 1.0f);
                    } else if (buffer.indexOf(CMD_SCOREBOARDTAG) > -1) {
                        Main.overlaytoggle = true;
                        Main.overlaytext.append(buffer).append("\n");
                        if (buffer.indexOf("Wumpus") > -1) {
                            history.add(buffer);
                        }
                    } else {
                        if (buffer.indexOf(S_WAITING_FOR_PLAYERS) > -1) {
                            waitingforplayers = true;
                            Main.debug("[NET] Waiting for players");
                        } else {
                            waitingforplayers = false;
                        }

                        if (buffer.indexOf(S_FIRESHOTGUN) > -1) {
                            if (spas12ammo > 1) {
                                spas12ammo--;
                            } else {
                                addHistory(";BRELOADING...");
                                pause(2000);
                                SoundFactory.play(SoundFactory.A_AMMO, false, -1);
                                spas12ammo = 8 + 1;
                            }
                        }
                        addHistory(buffer);
                    }
                }

            } catch (UnknownHostException e) {
                addHistory(";Runable to connect to server");
                Main.debug("UnknownHostException: " + e.getMessage());
            } catch (IOException e) {
                addHistory(";RCould not get connection from server");
                Main.debug("B) IOException: " + e.getMessage());
            } finally {
                connect = false;
                firstdisconnect = true;
                SoundFactory.smashTV();
                try {
                    if (in != null) {
                        in.close();
                    }

                    if (sock != null) {
                        sock.close();
                    }
                } catch (IOException ioe) {
                    System.err.println("IOException closing network connections " + ioe.getMessage());
                    System.exit(1);
                }
            }
        }
    }

    public void writing() {

        try {
            if (!connect) {
                openSocket();
            }

            while (true) {
                if (commands != null) {

                    for (String s : commands) {
                        if (firstcommand) {
                            firstcommand = false;
                            out.write(KEY + s);
                        } else {
                            out.write(s);
                        }

                        Main.debug("TX:" + s);
                        out.flush();
                        addHistory(";B" + s);
                        commands.remove(s);
                    }
                }

                pause(500);
            }

        } finally {
            connect = false;
            firstdisconnect = true;
            SoundFactory.smashTV();
            if (out != null) {
                out.close();
            }
            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (IOException ioe) {
                System.err.println("IOException closing network connections " + ioe.getMessage());
                System.exit(1);
            }
        }
    }

    void addHistory(String s) {
        if (history.size() >= MAXHISTORYLINES-1) {
            history.remove();
        }

        history.add(s);
    }

    void reconnect(String ip) {
        SERVERIP = ip;
        connect = false;
        firstdisconnect = true;
        SoundFactory.smashTV();
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
        }
    }
}
