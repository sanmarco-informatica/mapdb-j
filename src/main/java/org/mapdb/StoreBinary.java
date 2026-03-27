package org.mapdb;

import org.jetbrains.annotations.NotNull;

public interface StoreBinary extends Store {
    long getBinaryLong(long var1, @NotNull StoreBinaryGetLong var3);
}