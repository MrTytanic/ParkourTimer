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
        Long start = activeTimers.remove(uuid);
        if (start == null) return 0L;
        return System.currentTimeMillis() - start;
    }

    public boolean isRunning(UUID uuid) {
        return activeTimers.containsKey(uuid);
    }

    public long getElapsed(UUID uuid) {
        Long start = activeTimers.get(uuid);
        if (start == null) return 0L;
        return System.currentTimeMillis() - start;
    }
}