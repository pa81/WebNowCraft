package com.pa81.mod.client;

public class MusicState {
    public String title = "Waiting...";
    public String artist = "";
    public String coverUrl = "";
    public String durationStr = "0:00";
    public String positionStr = "0:00";

    public long durationSec = 1;
    public long positionSec = 0;

    public boolean isPlaying = false;

    // 時間文字列 (MM:SS, H:MM:SS) または秒数文字列をパース
    public long parseTimeStr(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            // "0:00" のような形式
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                long seconds = 0;
                if (parts.length == 3) { // H:MM:SS
                    seconds += Long.parseLong(parts[0]) * 3600;
                    seconds += Long.parseLong(parts[1]) * 60;
                    seconds += Long.parseLong(parts[2]);
                } else if (parts.length == 2) { // MM:SS
                    seconds += Long.parseLong(parts[0]) * 60;
                    seconds += Long.parseLong(parts[1]);
                }
                return seconds;
            } else {
                // すでに秒数だけの文字列の場合 (Spicetifyの設定による)
                double d = Double.parseDouble(timeStr);
                return (long) d;
            }
        } catch (NumberFormatException e) {
            // パース失敗時は0を返す（ログには出さない）
            return 0;
        }
    }
}