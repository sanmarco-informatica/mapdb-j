package org.mapdb;

import kotlin.Metadata;
import org.jetbrains.annotations.NotNull;

public interface ConcurrencyAware {
    boolean isThreadSafe();

    default void checkThreadSafe() {
        if (!this.isThreadSafe()) {
            throw new AssertionError();
        }
    }
}
