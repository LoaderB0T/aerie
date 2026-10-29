package com.loaderb0t.aerie;

/** Attunement progress of a Core, in ticks, plus the position it was attuned at. Pure rules, no Minecraft types. */
public final class Charge {
    public enum Event { NONE, RESET, COMPLETED }

    public static final long NO_ANCHOR = Long.MIN_VALUE;

    private int progress;
    private long anchor = NO_ANCHOR;

    public int progress() {
        return progress;
    }

    public long anchor() {
        return anchor;
    }

    public boolean charged(int requiredTicks) {
        return progress >= requiredTicks;
    }

    public float fraction(int requiredTicks) {
        if (requiredTicks <= 0) return 1f;
        return Math.clamp((float) progress / requiredTicks, 0f, 1f);
    }

    public Event tick(int elapsedTicks, boolean complete, boolean powered, int requiredTicks) {
        if (!complete) {
            if (progress == 0) return Event.NONE;
            reset();
            return Event.RESET;
        }
        if (powered) return Event.NONE;
        int before = progress;
        // Never lowers progress when the admin shortens the charge time; never overshoots a raised one.
        progress = Math.min(progress + elapsedTicks, Math.max(progress, requiredTicks));
        return before < requiredTicks && progress >= requiredTicks ? Event.COMPLETED : Event.NONE;
    }

    /** Returns true if the Core moved since it was attuned, which resets the charge. */
    public boolean checkAnchor(long pos) {
        if (anchor == NO_ANCHOR) {
            anchor = pos;
            return false;
        }
        if (anchor == pos) return false;
        anchor = pos;
        reset();
        return true;
    }

    public void reset() {
        progress = 0;
    }

    public void load(int progress, long anchor) {
        this.progress = progress;
        this.anchor = anchor;
    }
}
