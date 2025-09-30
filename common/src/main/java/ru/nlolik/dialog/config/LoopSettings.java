package ru.nlolik.dialog.config;

public record LoopSettings(int times, int intervalTicks) {
    public boolean infinite() {
        return times < 0;
    }
}
