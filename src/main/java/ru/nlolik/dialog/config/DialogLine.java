package ru.nlolik.dialog.config;

public class DialogLine {
    private final String text;
    private final DialogTextStyle style;
    private final int delayTicks;
    private final LoopSettings loop;

    public DialogLine(String text, DialogTextStyle style, int delayTicks, LoopSettings loop) {
        this.text = text;
        this.style = style;
        this.delayTicks = Math.max(0, delayTicks);
        this.loop = loop;
    }

    public String text() {
        return text;
    }

    public DialogTextStyle style() {
        return style;
    }

    public int delayTicks() {
        return delayTicks;
    }

    public LoopSettings loop() {
        return loop;
    }
}
