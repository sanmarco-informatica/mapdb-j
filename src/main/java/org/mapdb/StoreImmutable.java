package org.mapdb;

import kotlin.Metadata;
import kotlin.collections.LongIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StoreImmutable {
    @Nullable
    <T> T get(long var1, @NotNull Serializer<T> var3);

    @NotNull
    LongIterator getAllRecids();

    @NotNull
    Iterable getAllFiles();
}
