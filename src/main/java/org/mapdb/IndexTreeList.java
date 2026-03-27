package org.mapdb;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IndexTreeList<T> extends AbstractList<T> {
    private final Store store;
    private final Serializer<T> serializer;
    private final MutableLongLongMap map;
    private final long counterRecid;
    private final boolean isThreadSafe;
    @Nullable
    private final ReentrantReadWriteLock lock;

    public IndexTreeList(@NotNull Store store, @NotNull MutableLongLongMap map, @NotNull Serializer<T> serializer, boolean isThreadSafe, long counterRecid) {
        this.store = store;
        this.serializer = serializer;
        this.map = map;
        this.counterRecid = counterRecid;
        this.isThreadSafe = isThreadSafe;
        this.lock = this.isThreadSafe ? new ReentrantReadWriteLock() : null;
    }

    @NotNull
    public Store getStore() {
        return this.store;
    }

    @NotNull
    public Serializer<T> getSerializer() {
        return this.serializer;
    }

    @NotNull
    public MutableLongLongMap getMap() {
        return this.map;
    }

    public long getCounterRecid() {
        return this.counterRecid;
    }

    public boolean isThreadSafe() {
        return this.isThreadSafe;
    }

    @Nullable
    public ReentrantReadWriteLock getLock() {
        return this.lock;
    }

    public boolean add(@Nullable T element) {
        return Utils.lockWrite(this.lock, () -> add$lambda$0(element));
    }

    public void add(int index, @Nullable T element) {
        Utils.lockWrite(this.lock, () -> add$lambda$1(index, element));
    }

    public void clear() {
        Utils.lockWrite(this.lock, this::clear$lambda$0);
    }

    @Nullable
    public T removeAt(int index) {
        return Utils.lockWrite(this.lock, () -> removeAt$lambda$0(index));
    }

    @Nullable
    public T set(int index, @Nullable T element) {
        return Utils.lockWrite(this.lock, () -> set$lambda$0(index, element));
    }

    public void checkIndex(int index) {
        if (index < 0 || index >= this.size()) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Nullable
    public T get(int index) {
        return Utils.lockRead(this.lock, () -> get$lambda$0(index));
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    @NotNull
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private volatile int index;
            private volatile Integer indexToRemove;

            public int getIndex() {
                return this.index;
            }

            public void setIndex(int var1) {
                this.index = var1;
            }

            public Integer getIndexToRemove() {
                return this.indexToRemove;
            }

            public void setIndexToRemove(Integer var1) {
                this.indexToRemove = var1;
            }

            public boolean hasNext() {
                return Utils.lockRead(IndexTreeList.this.getLock(), this::hasNext$lambda$0);
            }

            public T next() {
                return Utils.lockRead(IndexTreeList.this.getLock(), this::next$lambda$1);
            }

            public void remove() {
                Utils.lockWrite(IndexTreeList.this.getLock(), this::remove$lambda$2);
            }

            private boolean hasNext$lambda$0() {
                return index < size();
            }

            private T next$lambda$1() {
                if (index >= size()) {
                    throw new NoSuchElementException();
                } else {
                    indexToRemove = index;
                    T ret = get(index);
                    index++;
                    return ret;
                }
            }

            private Void remove$lambda$2() {
                Integer var10001 = indexToRemove;
                if (var10001 != null) {
                    IndexTreeList.this.removeAt(var10001);
                    int var2 = index;
                    index = var2 + -1;
                    indexToRemove = null;

                    return null;
                } else {
                    throw new IllegalStateException();
                }
            }
        };
    }

    public int getSize() {
        long var10000 = this.store.get(this.counterRecid, (Serializer<Long>) Serializer.LONG_PACKED);
        return (int) (var10000);
    }

    private void setSize(int size) {
        this.store.update(this.counterRecid, (long) size, Serializer.LONG_PACKED);
    }

    private boolean add$lambda$0(T $element) {
        int var3 = size();
        setSize(var3 + 1);
        int index = var3;
        long recid = store.put($element, serializer);
        map.put(index, recid);
        return true;
    }

    private Void add$lambda$1(int $index, T $element) {
        checkIndex($index);
        int i = size() - 1;
        if ($index <= i) {
            while (true) {
                long recid = map.get(i);
                if (recid != 0L) {
                    map.remove(i);
                    map.put(i + 1, recid);
                }

                if (i == $index) {
                    break;
                }

                --i;
            }
        }

        i = size();
        setSize(i + 1);
        long recid = map.get($index);
        if (recid == 0L) {
            map.put($index, store.put($element, serializer));
        } else {
            store.update(recid, $element, serializer);
        }

        return null;
    }

    private void clear$lambda$0$0(long recid) {
        store.delete(recid, serializer);
    }

    private Void clear$lambda$0() {
        setSize(0);
        map.forEachValue(this::clear$lambda$0$0);
        map.clear();

        return null;
    }

    private T removeAt$lambda$0(int $index) {
        checkIndex($index);
        long recid = map.get($index);
        T var10000;
        if (recid == 0L) {
            var10000 = null;
        } else {
            T ret = store.get(recid, serializer);
            store.delete(recid, serializer);
            map.remove($index);
            var10000 = ret;
        }

        T ret = var10000;
        int i = $index + 1;

        for (int var6 = size(); i < var6; ++i) {
            long r = map.get(i);
            if (r != 0L) {
                map.remove(i);
                map.put(i - 1, r);
            }
        }

        i = size();
        setSize(i + -1);
        return ret;
    }

    private T set$lambda$0(int $index, T $element) {
        checkIndex($index);
        long recid = map.get($index);
        if (recid == 0L) {
            map.put($index, store.put($element, serializer));
            return null;
        } else {
            T ret = store.get(recid, serializer);
            store.update(recid, $element, serializer);
            return ret;
        }
    }

    private T get$lambda$0(int $index) {
        checkIndex($index);
        long recid = map.get($index);
        return recid == 0L ? null : store.get(recid, serializer);
    }

    public T remove(int index) {
        return this.removeAt(index);
    }

    // $FF: bridge method
    public int size() {
        return this.getSize();
    }
}
