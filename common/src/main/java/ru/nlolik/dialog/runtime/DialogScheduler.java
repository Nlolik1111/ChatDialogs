package ru.nlolik.dialog.runtime;

import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Supplier;

public class DialogScheduler {
    private static final class ScheduledTask {
        final long runAt;
        final Runnable runnable;

        ScheduledTask(long runAt, Runnable runnable) {
            this.runAt = runAt;
            this.runnable = runnable;
        }
    }

    private static final class RepeatingTask {
        long nextRun;
        final int interval;
        final long endTick;
        final Supplier<Boolean> condition;
        final Runnable onComplete;
        final Runnable onTimeout;

        RepeatingTask(long nextRun, int interval, long endTick, Supplier<Boolean> condition, Runnable onComplete, Runnable onTimeout) {
            this.nextRun = nextRun;
            this.interval = interval;
            this.endTick = endTick;
            this.condition = condition;
            this.onComplete = onComplete;
            this.onTimeout = onTimeout;
        }
    }

    private MinecraftServer server;
    private final Queue<ScheduledTask> tasks = new PriorityQueue<>(Comparator.comparingLong(t -> t.runAt));
    private final Queue<RepeatingTask> repeating = new PriorityQueue<>(Comparator.comparingLong(t -> t.nextRun));

    public void attach(MinecraftServer server) {
        this.server = server;
        tasks.clear();
        repeating.clear();
    }

    public void detach() {
        tasks.clear();
        repeating.clear();
        server = null;
    }

    public void schedule(int delayTicks, Runnable runnable) {
        if (server == null) {
            return;
        }
        tasks.add(new ScheduledTask(server.getTickCount() + Math.max(0, delayTicks), runnable));
    }

    public void scheduleRepeating(int intervalTicks, int timeoutTicks, Supplier<Boolean> condition, Runnable onComplete, Runnable onTimeout) {
        if (server == null) {
            return;
        }
        long now = server.getTickCount();
        long end = timeoutTicks <= 0 ? Long.MAX_VALUE : now + timeoutTicks;
        repeating.add(new RepeatingTask(now + Math.max(1, intervalTicks), Math.max(1, intervalTicks), end, condition, onComplete, onTimeout));
    }

    public void tick() {
        if (server == null) {
            return;
        }
        long currentTick = server.getTickCount();
        while (!tasks.isEmpty() && tasks.peek().runAt <= currentTick) {
            Runnable runnable = tasks.poll().runnable;
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        while (!repeating.isEmpty() && repeating.peek().nextRun <= currentTick) {
            RepeatingTask task = repeating.poll();
            boolean conditionMet = false;
            try {
                conditionMet = task.condition.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (conditionMet) {
                if (task.onComplete != null) {
                    task.onComplete.run();
                }
                continue;
            }
            if (currentTick >= task.endTick) {
                if (task.onTimeout != null) {
                    task.onTimeout.run();
                }
                continue;
            }
            task.nextRun = currentTick + task.interval;
            repeating.add(task);
        }
    }
}
