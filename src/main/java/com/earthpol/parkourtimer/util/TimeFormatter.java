package com.earthpol.parkourtimer.util;

public class TimeFormatter {

    public static String format(long ms) {
        long seconds = ms / 1000;
        long centiseconds = (ms % 1000) / 10;
        return seconds + "." + (centiseconds < 10 ? "0" : "") + centiseconds + "s";
    }
}
