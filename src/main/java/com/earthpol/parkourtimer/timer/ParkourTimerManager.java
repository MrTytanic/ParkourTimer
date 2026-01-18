package com.earthpol.parkourtimer.timer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourTimerManager {

    private final Map<UUID, Long> activeTimers = new HashMap<>();

    public void start(UUID uuid) {
        activeTimers.put(uuid, System.currentTimeMillis());
    }

    public long stop(UUID uuid) {
        long start = activeTimers.remove(uuid);
        return System.currentTimeMillis() - start;
    }

    public boolean isRunning(UUID uuid) {
        return activeTimers.containsKey(uuid);
    }

    public long getElapsed(UUID uuid) {
        return System.currentTimeMillis() - activeTimers.get(uuid);
    }
}
