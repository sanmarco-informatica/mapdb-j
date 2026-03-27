package org.mapdb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DBException extends RuntimeException {
    public DBException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public DBException(@NotNull String message) {
        this(message, null);
    }

    public static final class NotSorted extends DBException {
        public NotSorted() {
            super("Keys are not sorted");
        }
    }

    public static final class WrongConfiguration extends DBException {
        public WrongConfiguration(@NotNull String message) {
            super(message);
        }
    }

    public static final class OutOfMemory extends VolumeIOError {
        public OutOfMemory(@NotNull Throwable e) {
            super(Objects.equals("Direct buffer memory", e.getMessage()) ? "Out of Direct buffer memory. Increase it with JVM option '-XX:MaxDirectMemorySize=10G'" : e.getMessage(), e);
        }
    }

    public static final class GetVoid extends DBException {
        public GetVoid(long recId) {
            super("Record does not exist, recid=" + recId);
        }
    }

    public static final class WrongFormat extends DBException {
        public WrongFormat(@NotNull String msg) {
            super(msg);
        }
    }

    public static final class Interrupted extends DBException {
        public Interrupted(@NotNull InterruptedException e) {
            super("One of threads was interrupted while accessing store", e);
        }
    }

    public static class DataCorruption extends DBException {
        public DataCorruption(@NotNull String msg) {
            super(msg);
        }
    }

    public static final class NewMapDBFormat extends DBException {

        public NewMapDBFormat(String message) {
            super(message);
        }
    }

    public static final class PointerChecksumBroken extends DataCorruption {
        public PointerChecksumBroken() {
            super("Broken bit parity");
        }
    }

    public static final class FileLocked extends DBException {
        public FileLocked(@NotNull Path path, @Nullable Exception exception) {
            super("File is already opened and is locked: " + path, exception);
        }
    }

    public static class VolumeClosed extends DBException {
        public VolumeClosed(@Nullable String msg, @Nullable Throwable e) {
            super(msg, e);
        }

        public VolumeClosed(@NotNull Throwable e) {
            this(null, e);
        }
    }

    public static class VolumeClosedByInterrupt extends DBException {
        public VolumeClosedByInterrupt(@Nullable Throwable e) {
            super("Thread was interrupted during IO, FileChannel closed in result", e);
        }
    }

    public static class VolumeIOError extends DBException {
        public VolumeIOError(@Nullable String msg, @Nullable Throwable e) {
            super(msg, e);
        }

        public VolumeIOError(@NotNull IOException e) {
            this(null, e);
        }
    }

    public static class VolumeEOF extends VolumeIOError {
        public VolumeEOF(@Nullable String msg, @Nullable IOException e) {
            super(msg, e);
        }

        public VolumeEOF(@NotNull String msg) {
            this(msg, null);
        }
    }

    public static final class VolumeMaxSizeExceeded extends DBException {
        public VolumeMaxSizeExceeded(long length, long requestedLength) {
            super("Could not expand store. Maximal store size: " + length + ", new requested size: " + requestedLength);
        }
    }

    public static class SerializationError extends DBException {
        public SerializationError(@Nullable String msg, @Nullable Throwable e) {
            super(msg, e);
        }

        public SerializationError(@NotNull Throwable e) {
            this(null, e);
        }

        public SerializationError(@NotNull String msg) {
            this(msg, null);
        }
    }
}
