package org.mapdb.volume;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class ReadOnlyVolumeFactory extends VolumeFactory {
    private final VolumeFactory volfab;

    public ReadOnlyVolumeFactory(@NotNull VolumeFactory volfab) {
        this.volfab = volfab;
    }

    public boolean exists(@Nullable String file) {
        return this.volfab.exists(file);
    }

    public @NonNull Volume makeVolume(@Nullable String file, boolean readOnly, long fileLockWait, int sliceShift, long initSize, boolean fixedSize) {
        Volume volume = this.volfab.makeVolume(file, readOnly, fileLockWait, sliceShift, initSize, fixedSize);
        return (Volume)(new ReadOnlyVolume(volume));
    }

    public boolean handlesReadonly() {
        return true;
    }
}
