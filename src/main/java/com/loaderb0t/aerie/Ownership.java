package com.loaderb0t.aerie;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/** Who owns a Core and whether it is locked to them. Pure rules, no Minecraft types. */
public final class Ownership {
    public enum Result { CLAIMED, STATUS, LOCKED, UNLOCKED, DENIED }

    private @Nullable UUID owner;
    private boolean locked;

    public @Nullable UUID owner() {
        return owner;
    }

    public boolean locked() {
        return locked;
    }

    /** CLAIMED means the owner changed and the caller must reset the charge. */
    public Result use(UUID clicker, boolean sneaking, boolean clickerIsOp) {
        boolean isOwner = Objects.equals(clicker, owner);
        if (!sneaking) {
            if (isOwner) return Result.STATUS;
            if (owner != null && locked) return Result.DENIED;
            owner = clicker;
            locked = false;
            return Result.CLAIMED;
        }
        if (owner == null) return Result.STATUS;
        if (!isOwner && !clickerIsOp) return Result.DENIED;
        locked = !locked;
        return locked ? Result.LOCKED : Result.UNLOCKED;
    }

    public void load(@Nullable UUID owner, boolean locked) {
        this.owner = owner;
        this.locked = locked;
    }
}
