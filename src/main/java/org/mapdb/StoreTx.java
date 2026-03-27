package org.mapdb;

import org.mapdb.Store;

public interface StoreTx extends Store {
    void rollback();
}