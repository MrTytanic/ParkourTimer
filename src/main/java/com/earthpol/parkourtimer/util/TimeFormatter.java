package com.earthpol.parkourtimer.util;

public class TimeFormatter {

    // action bar timer display (MM:SS.CS)
    public static String format(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long centiseconds = (ms % 1000) / 10;

        //  String.format for padding
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
    }

    // completion message timer (long form)
    public static String formatLong(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        sb.append(seconds).append(" second").append(seconds != 1 ? "s" : "");

        return sb.toString().trim();
    }
}