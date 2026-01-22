package com.pa81.mod.client;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

public class SpicetifyServer extends WebSocketServer {
    private static final int PORT = 8974;
    private static final MusicState currentState = new MusicState();
    private static SpicetifyServer instance;

    public SpicetifyServer() {
        super(new InetSocketAddress(PORT));
    }

    public static void init() {
        if (instance == null) {
            instance = new SpicetifyServer();
            instance.start();
            System.out.println("[Webnowcraft] WebSocket Server listening on port " + PORT);
        }
    }

    public static void stopServer() {
        if (instance != null) {
            try {
                instance.stop();
                System.out.println("[Webnowcraft] Server stopped.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static MusicState getState() {
        return currentState;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Webnowcraft] CONNECTED: " + conn.getRemoteSocketAddress());
        conn.send("ADAPTER_VERSION 2.0.0;WNPRLIB_REVISION 1");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[Webnowcraft] DISCONNECTED: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message == null || message.trim().isEmpty()) return;

        // デバッグログ
        System.out.println("[Webnowcraft] RECV: " + message);

        String key = "";
        String value = "";

        // スペースまたはコロンで区切る処理（前回の修正維持）
        int idxSpace = message.indexOf(' ');
        int idxColon = message.indexOf(':');

        if (idxSpace != -1 && (idxColon == -1 || idxSpace < idxColon)) {
            key = message.substring(0, idxSpace).toUpperCase().trim();
            value = message.substring(idxSpace + 1).trim();
        } else if (idxColon != -1) {
            key = message.substring(0, idxColon).toUpperCase().trim();
            value = message.substring(idxColon + 1).trim();
        } else {
            return;
        }

        synchronized (currentState) {
            try {
                switch (key) {
                    case "TITLE":
                        currentState.title = value;
                        break;
                    case "ARTIST":
                        currentState.artist = value;
                        break;

                    // 【ここを修正しました】
                    case "COVER":
                        if (value == null || value.isEmpty() || value.equals("Default")) break;
                        if (value.contains("localfile")) break;

                        if (value.startsWith("http")) {
                            currentState.coverUrl = value;
                        } else {
                            // "spotify:image:ID" から ID を取り出して URL 化
                            String id = value;
                            if (value.lastIndexOf(':') != -1) {
                                id = value.substring(value.lastIndexOf(':') + 1);
                            }
                            if (!id.isEmpty()) {
                                currentState.coverUrl = "https://i.scdn.co/image/" + id;
                                System.out.println("[Webnowcraft] Generated Cover URL: " + currentState.coverUrl);
                            }
                        }
                        break;

                    case "DURATION":
                        currentState.durationStr = value;
                        currentState.durationSec = currentState.parseTimeStr(value);
                        break;
                    case "POSITION":
                        currentState.positionStr = value;
                        currentState.positionSec = currentState.parseTimeStr(value);
                        break;
                    case "STATE":
                        currentState.isPlaying = value.equals("1") || value.equalsIgnoreCase("PLAYING");
                        break;
                }
            } catch (Exception e) {
                System.err.println("[Webnowcraft] Error processing key " + key + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[Webnowcraft] Server started successfully!");
    }
}