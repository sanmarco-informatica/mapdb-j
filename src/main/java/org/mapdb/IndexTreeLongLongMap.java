// IndexTreeLongLongMap.java
package org.mapdb;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;

import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Ref;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.map.primitive.ImmutableLongLongMap;
import org.eclipse.collections.api.map.primitive.LongLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.factory.primitive.LongLongMaps;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.primitive.AbstractLongIterable;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableLongSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class IndexTreeLongLongMap extends AbstractLongIterable implements MutableLongLongMap {
    @NotNull
    private final Store store;
    private final long rootRecid;
    private final int dirShift;
    private final int levels;
    private final boolean collapseOnRemove;
    @NotNull
    private final MutableLongSet keySet;
    @NotNull
    private final LazyLongIterableAdapter keysView;
    @NotNull
    private final RichIterable keysValuesView;
    @NotNull
    private final MutableLongCollection values;


    private abstract static class MutableCollectionSet extends AbstractMutableLongCollection implements MutableLongSet {
    }

    public IndexTreeLongLongMap(@NotNull Store store, long rootRecid, int dirShift, int levels, boolean collapseOnRemove) {
        this.store = store;
        this.rootRecid = rootRecid;
        this.dirShift = dirShift;
        this.levels = levels;
        this.collapseOnRemove = collapseOnRemove;
        this.keySet = new MutableCollectionSet() {
            public boolean contains(long key) {
                return IndexTreeLongLongMap.this.containsKey(key);
            }

            public long max() {
                long[] var10000 = IndexTreeListJava.treeLast(IndexTreeLongLongMap.this.getRootRecid(), IndexTreeLongLongMap.this.getStore(), IndexTreeLongLongMap.this.getLevels());
                if (var10000 == null) {
                    throw new NoSuchElementException();
                } else {
                    long[] ret = var10000;
                    return ret[0];
                }
            }

            public long min() {
                long[] var10000 = IndexTreeListJava.treeIter(IndexTreeLongLongMap.this.getDirShift(), IndexTreeLongLongMap.this.getRootRecid(), IndexTreeLongLongMap.this.getStore(), IndexTreeLongLongMap.this.getLevels(), 0L);
                if (var10000 == null) {
                    throw new NoSuchElementException();
                } else {
                    long[] ret = var10000;
                    return ret[0];
                }
            }

            public void clear() {
                IndexTreeLongLongMap.this.clear();
            }

            public LongSet freeze() {
                LongHashSet var10000 = LongHashSet.newSet(this);
                return var10000;
            }

            public void forEach(LongProcedure procedure) {
                IndexTreeLongLongMap.this.forEachKey(procedure);
            }

            public MutableLongIterator longIterator() {
                return new Iterator(IndexTreeLongLongMap.this, 0);
            }

            public boolean remove(long value) {
                boolean ret = IndexTreeLongLongMap.this.containsKey(value);
                if (ret) {
                    IndexTreeLongLongMap.this.removeKey(value);
                }

                return ret;
            }

            public boolean removeAll(LongIterable source) {
                Ref.BooleanRef changed = new Ref.BooleanRef();
                source.forEach(k -> removeAll$lambda$0(changed, k));
                return changed.element;
            }

            public boolean removeAll(long... source) {
                boolean changed = false;
                long[] $this$forEach$iv = source;
                int $i$f$forEach = 0;
                int var5 = 0;

                for (int var6 = source.length; var5 < var6; ++var5) {
                    long element$iv = $this$forEach$iv[var5];
                    int var11 = 0;
                    if (this.remove(element$iv)) {
                        changed = true;
                    }
                }

                return changed;
            }

            public boolean retainAll(LongIterable elements) {
                Ref.BooleanRef changed = new Ref.BooleanRef();
                this.forEach(k -> retainAll$lambda$2(elements, changed, k));
                return changed.element;
            }

            public boolean retainAll(long... source) {
                LongHashSet var10001 = LongHashSet.newSetWith(Arrays.copyOf(source, source.length));
                return this.retainAll(var10001);
            }

            public ImmutableLongSet toImmutable() {
                return LongSets.immutable.withAll(this);
            }

            public MutableLongSet asUnmodifiable() {
                return UnmodifiableLongSet.of(this);
            }

            public MutableLongSet asSynchronized() {
                return SynchronizedLongSet.of(this);
            }

            public int size() {
                return IndexTreeLongLongMap.this.size();
            }

            private void removeAll$lambda$0(Ref.BooleanRef $changed, long k) {
                if (remove(k)) {
                    $changed.element = true;
                }
            }

            private void retainAll$lambda$2(LongIterable $elements, Ref.BooleanRef $changed, long k) {
                if (!$elements.contains(k)) {
                    remove(k);
                    $changed.element = true;
                }
            }
        };
        this.keysView = new LazyLongIterableAdapter(this.keySet);
        this.keysValuesView = (new AbstractLazyIterable<LongLongPair>() {
            public void each(Procedure procedure) {
                IndexTreeLongLongMap.this.forEachKeyValue((k, v) -> each$lambda$0(procedure, k, v));
            }

            public java.util.Iterator<LongLongPair> iterator() {
                return new java.util.Iterator<LongLongPair>() {
                    private Long nextKey = -1L;
                    private LongLongPair nextRet;
                    private Long lastKey;

                    public Long getNextKey() {
                        return this.nextKey;
                    }

                    public void setNextKey(Long var1) {
                        this.nextKey = var1;
                    }

                    public LongLongPair getNextRet() {
                        return this.nextRet;
                    }

                    public void setNextRet(LongLongPair var1) {
                        this.nextRet = var1;
                    }

                    public Long getLastKey() {
                        return this.lastKey;
                    }

                    public void setLastKey(Long var1) {
                        this.lastKey = var1;
                    }

                    public boolean hasNext() {
                        if (this.nextRet != null) {
                            return true;
                        } else {
                            Long var10000 = this.nextKey;
                            if (var10000 != null) {
                                long prev = var10000;
                                long[] ret = IndexTreeListJava.treeIter(IndexTreeLongLongMap.this.getDirShift(), IndexTreeLongLongMap.this.getRootRecid(), IndexTreeLongLongMap.this.getStore(), IndexTreeLongLongMap.this.getLevels(), prev + 1L);
                                if (ret == null) {
                                    this.nextRet = null;
                                    this.nextKey = null;
                                } else {
                                    this.nextRet = PrimitiveTuples.pair(ret[0], ret[1]);
                                    this.nextKey = ret[0];
                                }

                                return this.nextRet != null;
                            } else {
                                return false;
                            }
                        }
                    }

                    public LongLongPair next() {
                        LongLongPair ret = this.nextRet;
                        this.nextRet = null;
                        if (ret == null) {
                            if (this.nextKey != null && this.hasNext()) {
                                return this.next();
                            } else {
                                this.lastKey = null;
                                throw new NoSuchElementException();
                            }
                        } else {
                            this.lastKey = ret.getOne();
                            return ret;
                        }
                    }

                    public void remove() {
                        IndexTreeLongLongMap var10000 = IndexTreeLongLongMap.this;
                        Long var10001 = this.lastKey;
                        if (var10001 != null) {
                            var10000.removeKey(var10001);
                            this.lastKey = null;
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }
                };
            }

            private void each$lambda$0(Procedure<LongLongPair> $procedure, long k, long v) {
                $procedure.value(PrimitiveTuples.pair(k, v));
            }
        });
        this.values = new AbstractMutableLongCollection() {
            public boolean contains(long value) {
                return IndexTreeLongLongMap.this.containsValue(value);
            }

            public int size() {
                return IndexTreeLongLongMap.this.size();
            }

            public void forEach(LongProcedure procedure) {
                IndexTreeLongLongMap.this.forEach(procedure);
            }

            public long max() {
                return IndexTreeLongLongMap.this.max();
            }

            public long min() {
                return IndexTreeLongLongMap.this.min();
            }

            public MutableLongCollection asSynchronized() {
                throw new UnsupportedOperationException();
            }

            public MutableLongCollection asUnmodifiable() {
                throw new UnsupportedOperationException();
            }

            public void clear() {
                IndexTreeLongLongMap.this.clear();
            }

            public MutableLongIterator longIterator() {
                return new Iterator(IndexTreeLongLongMap.this, 1);
            }

            public boolean remove(long value) {
                Ref.BooleanRef removed = new Ref.BooleanRef();
                IndexTreeLongLongMap.this.forEachKeyValue((k, v) -> remove$lambda$0(value, removed, k, v));
                return removed.element;
            }

            public boolean removeAll(LongIterable source) {
                MutableLongSet values = source.toSet();
                Ref.BooleanRef removed = new Ref.BooleanRef();
                IndexTreeLongLongMap.this.forEachKeyValue((k, v) -> removeAll$lambda$1(values, removed, k, v));
                return removed.element;
            }

            public boolean removeAll(long... source) {
                LongHashSet var10001 = LongHashSet.newSetWith(Arrays.copyOf(source, source.length));
                return this.removeAll(var10001);
            }

            public boolean retainAll(LongIterable elements) {
                MutableLongSet values = elements.toSet();
                Ref.BooleanRef removed = new Ref.BooleanRef();
                IndexTreeLongLongMap.this.forEachKeyValue((k, v) -> retainAll$lambda$2(values, removed, k, v));
                return removed.element;
            }

            public boolean retainAll(long... source) {
                LongHashSet var10001 = LongHashSet.newSetWith(Arrays.copyOf(source, source.length));
                return this.retainAll(var10001);
            }

            public ImmutableLongCollection toImmutable() {
                throw new UnsupportedOperationException();
            }

            private void remove$lambda$0(long $value, Ref.BooleanRef $removed, long k, long v) {
                if ($value == v) {
                    removeKey(k);
                    $removed.element = true;
                }

            }

            private void removeAll$lambda$1(MutableLongSet $values, Ref.BooleanRef $removed, long k, long v) {
                if ($values.contains(v)) {
                    removeKey(k);
                    $removed.element = true;
                }

            }

            private void retainAll$lambda$2(MutableLongSet $values, Ref.BooleanRef $removed, long k, long v) {
                if (!$values.contains(v)) {
                    removeKey(k);
                    $removed.element = true;
                }

            }
        };
    }

    @NotNull
    public Store getStore() {
        return this.store;
    }

    public long getRootRecid() {
        return this.rootRecid;
    }

    public int getDirShift() {
        return this.dirShift;
    }

    public int getLevels() {
        return this.levels;
    }

    public boolean getCollapseOnRemove() {
        return this.collapseOnRemove;
    }

    public void put(long key, long value) {
        int $i$f$assertKey = 0;
        if (key < 0L) {
            throw new IllegalArgumentException("negative key");
        } else {
            IndexTreeListJava.treePut(this.dirShift, this.rootRecid, this.store, this.levels, key, value);
        }
    }

    public long get(long key) {
        int $i$f$assertKey = 0;
        if (key < 0L) {
            throw new IllegalArgumentException("negative key");
        } else {
            return IndexTreeListJava.treeGet(this.dirShift, this.rootRecid, this.store, this.levels, key);
        }
    }

    public void remove(long key) {
        int $i$f$assertKey = 0;
        if (key < 0L) {
            throw new IllegalArgumentException("negative key");
        } else {
            if (this.collapseOnRemove) {
                Serializable var10000 = IndexTreeListJava.treeRemoveCollapsing(this.dirShift, this.rootRecid, this.store, this.levels, true, key, null);
            } else {
                Serializable var7 = IndexTreeListJava.treeRemove(this.dirShift, this.rootRecid, this.store, this.levels, key, null);
            }

        }
    }

    private void assertKey(long key) {
        int $i$f$assertKey = 0;
        if (key < 0L) {
            throw new IllegalArgumentException("negative key");
        }
    }

    public boolean contains(long value) {
        return this.containsValue(value);
    }

    public boolean containsKey(long key) {
        if (key < 0L) {
            return false;
        } else {
            return IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key) != null;
        }
    }

    public boolean containsValue(long value) {
        return IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, false,
                (k, v, b) -> containsValue$lambda$0(value, k, v, b));
    }

    public void clear() {
        IndexTreeListJava.treeClear(this.rootRecid, this.store, this.levels);
    }

    @NotNull
    public <T> MutableBag<T> collect(@NotNull LongToObjectFunction<? extends T> function) {
        HashBag<T> ret = new HashBag<>();
        this.forEachKeyValue((k, v) -> collect$lambda$0(function, ret, k, v));
        return ret;
    }

    @NotNull
    public MutableLongIterator longIterator() {
        return new Iterator(this, 1);
    }

    @NotNull
    public MutableLongBag reject(@NotNull LongPredicate predicate) {
        LongHashBag ret = new LongHashBag();
        this.forEachKeyValue((k, v) -> reject$lambda$0(predicate, ret, k, v));
        return ret;
    }

    @NotNull
    public MutableLongBag select(@NotNull LongPredicate predicate) {
        LongHashBag ret = new LongHashBag();
        this.forEachKeyValue((k, v) -> select$lambda$0(predicate, ret, k, v));
        return ret;
    }

    public void appendString(@NotNull Appendable appendable, @NotNull String start, @NotNull String separator, @NotNull String end) {
        try {
            appendable.append((CharSequence) start);
            Ref.BooleanRef first = new Ref.BooleanRef();
            first.element = true;
            this.forEachKeyValue((k, v) -> {
                try {
                    appendString$lambda$0(first, appendable, separator, k, v);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            appendable.append((CharSequence) end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int size() {
        Object var10000 = IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, 0L, IndexTreeLongLongMap::size$lambda$0);
        return Utils.roundDownToIntMAXVAL(((Number) var10000).longValue());
    }

    public boolean allSatisfy(@NotNull LongPredicate predicate) {
        return IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, true, (k, v, b) -> allSatisfy$lambda$0(predicate, k, v, b));
    }

    public boolean anySatisfy(@NotNull LongPredicate predicate) {
        return IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, false, (k, v, b) -> anySatisfy$lambda$0(predicate, k, v, b));
    }

    public int count(@NotNull LongPredicate predicate) {
        Long var10000 = IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, 0L, (k, v, l) -> count$lambda$0(predicate, k, v, l));
        return Utils.roundDownToIntMAXVAL(var10000);
    }

    public long detectIfNone(@NotNull LongPredicate predicate, long ifNone) {
        Ref.LongRef ret = new Ref.LongRef();
        ret.element = ifNone;
        this.forEachValue(v -> detectIfNone$lambda$0(predicate, ret, v));
        return ret.element;
    }

    public void each(@NotNull LongProcedure procedure) {
        this.forEach(procedure);
    }

    public void forEach(@NotNull LongProcedure procedure) {
        this.forEachValue(procedure);
    }

    public Object injectInto(Object injectedValue, @Nullable ObjectLongToObjectFunction function) {
        throw new UnsupportedOperationException();
    }

    public long max() {
        Long ret = IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, null, IndexTreeLongLongMap::max$lambda$0);
        if (ret != null) {
            return ret;
        } else {
            throw new NoSuchElementException();
        }
    }

    public long min() {
        Long ret = IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, null, IndexTreeLongLongMap::min$lambda$0);
        if (ret != null) {
            return ret;
        } else {
            throw new NoSuchElementException();
        }
    }

    public boolean noneSatisfy(@NotNull LongPredicate predicate) {
        return IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, true, (k, v, b) -> noneSatisfy$lambda$0(predicate, k, v, b));
    }

    public long sum() {
        return IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, 0L, IndexTreeLongLongMap::sum$lambda$0);
    }

    @NotNull
    public long[] toArray() {
        long[] var10000 = this.values().toArray();
        return var10000;
    }

    public long addToValue(long key, long toBeAdded) {
        long old = this.get(key);
        long newVal = old + toBeAdded;
        this.put(key, newVal);
        return newVal;
    }

    @Nullable
    public MutableLongLongMap asSynchronized() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public MutableLongLongMap asUnmodifiable() {
        throw new UnsupportedOperationException();
    }

    public long getIfAbsentPut(long key, @NotNull LongFunction0 function) {
        Long oldval = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (oldval == null) {
            long value = function.value();
            this.put(key, value);
            return value;
        } else {
            return oldval;
        }
    }

    public long getIfAbsentPut(long key, long value) {
        Long oldval = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (oldval == null) {
            this.put(key, value);
            return value;
        } else {
            return oldval;
        }
    }

    public long getIfAbsentPutWith(long key, @NotNull LongFunction function, Object parameter) {
        Long oldval = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (oldval != null) {
            return oldval;
        } else {
            long value = function.longValueOf(parameter);
            this.put(key, value);
            return value;
        }
    }

    public long getIfAbsentPutWithKey(long key, @NotNull LongToLongFunction function) {
        Long oldval = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (oldval != null) {
            return oldval;
        } else {
            long value = function.valueOf(key);
            this.put(key, value);
            return value;
        }
    }

    public void putAll(@NotNull LongLongMap map) {
        map.forEachKeyValue(this::putAll$lambda$0);
    }

    @NotNull
    public MutableLongLongMap reject(@NotNull LongLongPredicate predicate) {
        LongLongHashMap ret = new LongLongHashMap();
        this.forEachKeyValue((k, v) -> reject$lambda$1(predicate, ret, k, v));
        return ret;
    }

    public void removeKey(long key) {
        this.remove(key);
    }

    public long removeKeyIfAbsent(long key, long value) {
        Long var10000 = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (var10000 != null) {
            long oldval = var10000;
            if (oldval != value) {
                this.removeKey(key);
                return oldval;
            } else {
                return value;
            }
        } else {
            return value;
        }
    }

    public @NonNull MutableLongLongMap select(@NotNull LongLongPredicate predicate) {
        LongLongHashMap ret = new LongLongHashMap();
        this.forEachKeyValue((k, v) -> select$lambda$1(predicate, ret, k, v));
        return ret;
    }

    public long updateValue(long key, long initialValueIfAbsent, @NotNull LongToLongFunction function) {
        Long var10000 = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        long oldval = var10000 != null ? var10000 : initialValueIfAbsent;
        long newVal = function.valueOf(oldval);
        this.put(key, newVal);
        return newVal;
    }

    public @NonNull MutableLongLongMap withKeyValue(long key, long value) {
        this.put(key, value);
        return this;
    }

    public @NonNull MutableLongLongMap withoutAllKeys(@NotNull LongIterable keys) {
        keys.forEach(this::withoutAllKeys$lambda$0);
        return this;
    }

    @Nullable
    public MutableLongLongMap withoutKey(long key) {
        this.remove(key);
        return this;
    }

    public void forEachKey(@NotNull LongProcedure procedure) {
        IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, null, (k, v, x) -> forEachKey$lambda$0(procedure, k, v, x));
    }

    public void forEachKeyValue(@NotNull LongLongProcedure procedure) {
        IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, null, (k, v, x) -> forEachKeyValue$lambda$0(procedure, k, v, x));
    }

    public long getIfAbsent(long key, long ifAbsent) {
        Long var10000 = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        return var10000 != null ? var10000 : ifAbsent;
    }

    public long getOrThrow(long key) {
        Long ret = IndexTreeListJava.treeGetNullable(this.dirShift, this.rootRecid, this.store, this.levels, key);
        if (ret != null) {
            return ret;
        } else {
            throw new IllegalStateException("Key " + key + " not present.");
        }
    }

    public void forEachValue(@NotNull LongProcedure procedure) {
        IndexTreeListJava.treeFold(this.rootRecid, this.store, this.levels, null, (k, v, x) -> forEachValue$lambda$0(procedure, k, v, null));
    }

    public boolean equals(@Nullable Object other) {
        if (other instanceof LongLongMap) {
            Ref.IntRef c = new Ref.IntRef();
            Ref.BooleanRef ret = new Ref.BooleanRef();
            ret.element = true;
            this.forEachKeyValue((k, v) -> equals$lambda$0(c, other, ret, k, v));
            return ret.element && c.element == ((LongLongMap) other).size();
        } else {
            return false;
        }
    }

    public int hashCode() {
        Ref.IntRef result = new Ref.IntRef();
        this.forEachKeyValue((k, v) -> hashCode$lambda$0(result, k, v));
        return result.element;
    }

    @NotNull
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('{');
        Ref.BooleanRef first = new Ref.BooleanRef();
        first.element = true;
        this.forEachKeyValue((k, v) -> toString$lambda$0(first, s, k, v));
        s.append('}');
        return s.toString();
    }

    @NotNull
    public MutableLongSet keySet() {
        return this.keySet;
    }

    @NotNull
    public LazyLongIterable keysView() {
        return this.keysView;
    }

    @NotNull
    public RichIterable keyValuesView() {
        return this.keysValuesView;
    }

    @NotNull
    public ImmutableLongLongMap toImmutable() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public MutableLongCollection values() {
        return this.values;
    }

    @NotNull
    public MutableLongLongMap flipUniqueValues() {
        MutableLongLongMap result = LongLongMaps.mutable.empty();
        this.forEachKeyValue((k, v) -> flipUniqueValues$lambda$0(result, k, v));
        return result;
    }

    public void updateValues(@Nullable LongLongToLongFunction p0) {
        throw new UnsupportedOperationException("UpdateValues() hasn't been implemented yet");
    }

    private static boolean containsValue$lambda$0(long $value, long k, long v, boolean b) {
        return b || v == $value;
    }

    private static void collect$lambda$0(LongToObjectFunction $function, HashBag $ret, long k, long v) {
        $ret.add($function.valueOf(v));
    }

    private static void reject$lambda$0(LongPredicate $predicate, LongHashBag $ret, long k, long v) {
        if (!$predicate.accept(v)) {
            $ret.add(v);
        }

    }

    private static void select$lambda$0(LongPredicate $predicate, LongHashBag $ret, long k, long v) {
        if ($predicate.accept(v)) {
            $ret.add(v);
        }

    }

    private static void appendString$lambda$0(Ref.BooleanRef $first, Appendable $appendable, String $separator, long k, long l) throws IOException {
        if (!$first.element) {
            $appendable.append((CharSequence) $separator);
        }

        $first.element = false;
        $appendable.append((CharSequence) String.valueOf(l));
    }

    private static Long size$lambda$0(long k, long v, long b) {
        return b + 1L;
    }

    private static Boolean allSatisfy$lambda$0(LongPredicate $predicate, long k, long v, boolean b) {
        return b && $predicate.accept(v);
    }

    private static Boolean anySatisfy$lambda$0(LongPredicate $predicate, long k, long v, boolean b) {
        return b || $predicate.accept(v);
    }

    private static Long count$lambda$0(LongPredicate $predicate, long k, long v, long b) {
        return $predicate.accept(v) ? b + 1L : b;
    }

    private static void detectIfNone$lambda$0(LongPredicate $predicate, Ref.LongRef $ret, long v) {
        if ($predicate.accept(v)) {
            $ret.element = v;
        }

    }

    private static Long max$lambda$0(long k, long v, Long b) {
        return b == null ? v : Math.max(b, v);
    }

    private static Long min$lambda$0(long k, long v, Long b) {
        return b == null ? v : Math.min(b, v);
    }

    private static Boolean noneSatisfy$lambda$0(LongPredicate $predicate, long k, long v, boolean b) {
        return b && !$predicate.accept(v);
    }

    private static Long sum$lambda$0(long k, long v, long b) {
        return b + v;
    }

    private void putAll$lambda$0(long k, long v) {
        put(k, v);
    }

    private static void reject$lambda$1(LongLongPredicate $predicate, LongLongHashMap $ret, long k, long v) {
        if (!$predicate.accept(k, v)) {
            $ret.put(k, v);
        }

    }

    private static void select$lambda$1(LongLongPredicate $predicate, LongLongHashMap $ret, long k, long v) {
        if ($predicate.accept(k, v)) {
            $ret.put(k, v);
        }

    }

    private void withoutAllKeys$lambda$0(long key) {
        removeKey(key);
    }

    private static Void forEachKey$lambda$0(LongProcedure $procedure, long k, long v, Object x) {
        $procedure.value(k);
        return null;
    }

    private static Void forEachKeyValue$lambda$0(LongLongProcedure $procedure, long k, long v, Object Unit) {
        $procedure.value(k, v);
        return null;
    }

    private static Void forEachValue$lambda$0(LongProcedure $procedure, long k, long v, Object Unit) {
        $procedure.value(v);
        return null;
    }

    private static void equals$lambda$0(Ref.IntRef $c, Object $other, Ref.BooleanRef $ret, long k, long v) {
        int var7 = $c.element++;
        if (!((LongLongMap) $other).containsKey(k) || ((LongLongMap) $other).get(k) != v) {
            $ret.element = false;
        }

    }

    private static void hashCode$lambda$0(Ref.IntRef $result, long k, long v) {
        $result.element += DataIO.longHash(k + v + (long) 10);
    }

    private static void toString$lambda$0(Ref.BooleanRef $first, StringBuilder $s, long k, long v) {
        if (!$first.element) {
            $s.append(',');
            $s.append(' ');
        }

        $first.element = false;
        $s.append(k);
        $s.append('=');
        $s.append(v);
    }

    private static void flipUniqueValues$lambda$0(MutableLongLongMap $result, long key, long value) {
        if ($result.containsKey(value)) {
            throw new IllegalStateException("duplicate value");
        } else {
            $result.put(value, key);
        }
    }

    @NotNull
    public static IndexTreeLongLongMap make(Store store, Long rootRecid, Integer dirShift, Integer levels, Boolean collapseOnRemove) {
        if (store == null) {
            store = new StoreTrivial();
        }
        if (rootRecid == null) {
            rootRecid = store.put(IndexTreeListJava.dirEmpty(), IndexTreeListJava.dirSer);
        }
        if (dirShift == null) {
            dirShift = CC.INDEX_TREE_LONGLONGMAP_DIR_SHIFT;
        }
        if (levels == null) {
            levels = CC.INDEX_TREE_LONGLONGMAP_LEVELS;
        }
        if (collapseOnRemove == null) {
            collapseOnRemove = true;
        }
        return new IndexTreeLongLongMap(store, rootRecid, dirShift, levels, collapseOnRemove);
    }

    private static final class Iterator implements MutableLongIterator {
        @NotNull
        private final IndexTreeLongLongMap m;
        private final int index;
        @Nullable
        private Long nextKey;
        @Nullable
        private Long nextRet;
        @Nullable
        private Long lastKey;

        public Iterator(@NotNull IndexTreeLongLongMap m, int index) {
            this.m = m;
            this.index = index;
            this.nextKey = -1L;
        }

        @NotNull
        public IndexTreeLongLongMap getM() {
            return this.m;
        }

        public int getIndex() {
            return this.index;
        }

        @Nullable
        public Long getNextKey() {
            return this.nextKey;
        }

        public void setNextKey(@Nullable Long var1) {
            this.nextKey = var1;
        }

        @Nullable
        public Long getNextRet() {
            return this.nextRet;
        }

        public void setNextRet(@Nullable Long var1) {
            this.nextRet = var1;
        }

        @Nullable
        public Long getLastKey() {
            return this.lastKey;
        }

        public void setLastKey(@Nullable Long var1) {
            this.lastKey = var1;
        }

        public boolean hasNext() {
            if (this.nextRet != null) {
                return true;
            } else {
                Long var10000 = this.nextKey;
                if (var10000 != null) {
                    long prev = var10000;
                    long[] ret = IndexTreeListJava.treeIter(this.m.getDirShift(), this.m.getRootRecid(), this.m.getStore(), this.m.getLevels(), prev + 1L);
                    if (ret == null) {
                        this.nextRet = null;
                        this.nextKey = null;
                    } else {
                        this.nextRet = ret[this.index];
                        this.nextKey = ret[0];
                    }

                    return this.nextRet != null;
                } else {
                    return false;
                }
            }
        }

        public long next() {
            Long ret = this.nextRet;
            this.nextRet = null;
            if (ret == null) {
                if (this.nextKey != null && this.hasNext()) {
                    return this.next();
                } else {
                    this.lastKey = null;
                    throw new NoSuchElementException();
                }
            } else {
                this.lastKey = this.nextKey;
                return ret;
            }
        }

        public void remove() {
            IndexTreeLongLongMap var10000 = this.m;
            Long var10001 = this.lastKey;
            if (var10001 != null) {
                var10000.removeKey(var10001);
                this.lastKey = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
