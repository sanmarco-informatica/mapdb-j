package org.mapdb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Store extends StoreImmutable, Verifiable, ConcurrencyAware {
    long preallocate();

    <T> long put(@Nullable T var1, @NotNull Serializer<T> var2);

    <T> void update(long var1, @Nullable T var3, @NotNull Serializer<T> var4);

    <T> boolean compareAndSwap(long recid, @Nullable T expectedOldRecord, @Nullable T newRecord, @NotNull Serializer<T> serializer);

    <T> void delete(long var1, @NotNull Serializer<T> var3);

    void commit();

    void compact();

    void close();

    boolean isClosed();

    void verify();

    boolean isReadOnly();

    boolean fileLoad();
}
