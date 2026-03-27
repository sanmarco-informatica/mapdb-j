package org.mapdb;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.BiConsumer;

import kotlin.Metadata;
import kotlin.Pair;
import kotlin.internal.ProgressionUtilKt;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.TypeIntrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.serializer.GroupSerializer;
import org.mapdb.volume.Volume;

public final class SortedTableMap<K, V> implements ConcurrentMap<K, V>, ConcurrentNavigableMap<K, V>, ConcurrentNavigableMapExtra<K, V> {
    @NotNull
    private final GroupSerializer<K> keySerializer;
    @NotNull
    private final GroupSerializer<V> valueSerializer;
    private final long pageSize;
    @NotNull
    private final Volume volume;
    private final boolean hasValues;
    @NotNull
    private final GroupSerializer comparator;
    private final long sizeLong;
    private final long pageCount;
    private final Object pageKeys;
    @NotNull
    private final Set<Entry<K, V>> entries;
    @NotNull
    private final NavigableSet<K> keys;
    @NotNull
    private final Collection<V> values;
    @NotNull
    private final BTreeMapJava.DescendingMap<K, V> descendingMap;
    private static final long SIZE_OFFSET = 16L;
    private static final long PAGE_COUNT_OFFSET = 24L;
    private static final long PAGE_SIZE_OFFSET = 32L;
    private static final int start = 64;

    public SortedTableMap(@NotNull GroupSerializer<K> keySerializer, @NotNull GroupSerializer<V> valueSerializer, long pageSize, @NotNull Volume volume, boolean hasValues) throws IOException {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.pageSize = pageSize;
        this.volume = volume;
        this.hasValues = hasValues;
        this.comparator = this.getKeySerializer();
        this.sizeLong = this.volume.getLong(SIZE_OFFSET);
        this.pageCount = this.volume.getLong(PAGE_COUNT_OFFSET);
        if ((long) this.volume.getUnsignedByte(0L) != 74L) {
            throw new DBException.WrongFormat("Wrong file header, not MapDB file");
        } else if ((long) this.volume.getUnsignedByte(1L) != 10L) {
            throw new DBException.WrongFormat("Wrong file header, not StoreDirect file");
        } else if (this.volume.getUnsignedShort(2L) != 0) {
            throw new DBException.NewMapDBFormat("SortedTableMap file was created with newer MapDB version");
        } else if (this.volume.getInt(4L) != 0) {
            throw new DBException.NewMapDBFormat("SortedTableMap has some extra features, not supported in this version");
        }
        SortedTableMap var10000 = this;
        ArrayList keys = new ArrayList();
        long var8 = this.pageCount * this.pageSize;
        long var10 = this.pageSize;
        if (var10 <= 0L) {
            throw new IllegalArgumentException("Step must be positive, was: " + var10 + '.');
        } else {
            long i = 0L;
            long var14 = ProgressionUtilKt.getProgressionLastElement(0L, var8, var10);
            long var16 = var10;
            if (i <= var14) {
                while (true) {
                    long ii = i == 0L ? (long) start : i;
                    long offset = i + (long) this.volume.getInt(ii + (long) 4);
                    int size = (int) (i + (long) this.volume.getInt(ii + (long) 8) - offset);
                    DataInput2 input = this.volume.getDataInput(offset, size);
                    int keysSize = input.unpackInt();
                    Object key = this.getKeySerializer().valueArrayBinaryGet(input, keysSize, 0);
                    keys.add(key);
                    if (i == var14) {
                        var10000 = this;
                        break;
                    }

                    i += var16;
                }
            }

            pageKeys = this.getKeySerializer().valueArrayFromArray(keys.toArray());
            this.entries = (Set) (new AbstractSet() {
                public boolean contains(Map.Entry<K, V> element) {
                    V value = SortedTableMap.this.get(element.getKey());
                    return value != null && SortedTableMap.this.getValueSerializer().equals(value, element.getValue());
                }

                public boolean isEmpty() {
                    return SortedTableMap.this.isEmpty();
                }

                public int getSize() {
                    return SortedTableMap.this.size();
                }

                public boolean add(Map.Entry element) {
                    Intrinsics.checkNotNullParameter(element, "element");
                    throw new UnsupportedOperationException("read-only");
                }

                public void clear() {
                    throw new UnsupportedOperationException("read-only");
                }

                public Iterator iterator() {
                    return SortedTableMap.this.entryIterator();
                }

                public boolean remove(Map.Entry element) {
                    Intrinsics.checkNotNullParameter(element, "element");
                    throw new UnsupportedOperationException("read-only");
                }

                // $FF: bridge method
                public final boolean contains(Object element) {
                    return !TypeIntrinsics.isMutableMapEntry(element) ? false : this.contains((Map.Entry) element);
                }

                // $FF: bridge method
                public final int size() {
                    return this.getSize();
                }

                // $FF: synthetic method
                // $FF: bridge method
                public boolean add(Object e) {
                    return this.add((Map.Entry) e);
                }

                // $FF: bridge method
                public final boolean remove(Object element) {
                    return !TypeIntrinsics.isMutableMapEntry(element) ? false : this.remove((Map.Entry) element);
                }
            });
            Intrinsics.checkNotNull(this, "null cannot be cast to non-null type org.mapdb.ConcurrentNavigableMapExtra<K of org.mapdb.SortedTableMap, kotlin.Any>");
            this.keys = (NavigableSet) (new BTreeMapJava.KeySet((BTreeMapJava.ConcurrentNavigableMap2) (this), true));
            this.values = (Collection) (new AbstractSet() {
                public boolean contains(Object element) {
                    return SortedTableMap.this.containsValue(element);
                }

                public boolean isEmpty() {
                    return SortedTableMap.this.isEmpty();
                }

                public int getSize() {
                    return SortedTableMap.this.size();
                }

                public boolean add(Object element) {
                    throw new UnsupportedOperationException("read-only");
                }

                public void clear() {
                    throw new UnsupportedOperationException("read-only");
                }

                public Iterator iterator() {
                    return SortedTableMap.this.valueIterator();
                }

                public boolean remove(Object element) {
                    throw new UnsupportedOperationException("read-only");
                }

                // $FF: bridge method
                public final int size() {
                    return this.getSize();
                }
            });
            this.descendingMap = new BTreeMapJava.DescendingMap(this, (Object) null, true, (Object) null, false);
        }
    }

    @NotNull
    public GroupSerializer<K> getKeySerializer() {
        return this.keySerializer;
    }

    @NotNull
    public GroupSerializer<V> getValueSerializer() {
        return this.valueSerializer;
    }

    public final long getPageSize() {
        return this.pageSize;
    }

    @NotNull
    protected final Volume getVolume() {
        return this.volume;
    }

    public boolean getHasValues() {
        return this.hasValues;
    }

    @NotNull
    public final GroupSerializer getComparator() {
        return this.comparator;
    }

    public final long getSizeLong() {
        return this.sizeLong;
    }

    public final long getPageCount() {
        return this.pageCount;
    }

    protected final Object getPageKeys() {
        return this.pageKeys;
    }

    public boolean containsKey(@Nullable Object key) {
        return this.get(key) != null;
    }

    public boolean containsValue(@Nullable Object value) {
        if (value == null) {
            throw new NullPointerException();
        } else {
            Iterator<V> iter = this.valueIterator();

            while (iter.hasNext()) {
                if (this.getValueSerializer().equals((V) value, iter.next())) {
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    public V get(@Nullable Object key) {
        try {
            if (key == null) {
                throw new NullPointerException();
            } else {
                int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, (K) key);
                if (keyPos == -1) {
                    return null;
                } else {
                    if (keyPos < 0) {
                        keyPos = -keyPos - 2;
                    }

                    int headSize = keyPos == 0 ? start : 0;
                    long offset = (long) keyPos * this.pageSize;
                    long offsetWithHead = offset + (long) headSize;
                    int nodeCount = this.volume.getInt(offsetWithHead);
                    int pos = this.nodeSearch(key, offset, offsetWithHead, nodeCount);
                    if (pos < 0) {
                        pos = -pos - 2;
                    }

                    long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (pos * 4));
                    long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (pos * 4) + (long) 4) - keysOffset;
                    DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
                    int keysSize = di.unpackInt();
                    int valuePos = this.getKeySerializer().valueArrayBinarySearch((K) key, di, keysSize, (Comparator) this.comparator);
                    if (valuePos < 0) {
                        return null;
                    } else {
                        long valOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((pos + nodeCount) * 4));
                        long valsBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((pos + nodeCount + 1) * 4)) - valOffset;
                        DataInput2 di2 = this.volume.getDataInput(valOffset, (int) valsBinarySize);
                        return this.getValueSerializer().valueArrayBinaryGet(di2, keysSize, valuePos);
                    }
                }
            }
        } catch (Exception x) {
            return Utils.sneakyThrow(x);
        }
    }

    protected final int nodeSearch(Object key, long offset, long offsetWithHead, int nodeCount) throws IOException {
        int lo = 0;
        int hi = nodeCount - 1;

        while (lo <= hi) {
            int mid = lo + hi >>> 1;
            long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (mid * 4));
            long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (mid * 4) + (long) 4) - keysOffset;
            DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
            int keysSize = di.unpackInt();
            int compare = this.comparator.compare(key, this.getKeySerializer().valueArrayBinaryGet(di, keysSize, 0));
            if (compare == 0) {
                return mid;
            }

            if (compare < 0) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }

        return -(lo + 1);
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public int getSize() {
        return (int) Math.min(2147483647L, this.sizeLong());
    }

    public long sizeLong() {
        return this.sizeLong;
    }

    @NotNull
    protected final NodeIterator nodeIterator() {
        return new NodeIterator(this, 0L, (long) start, (long) this.volume.getInt((long) start), -1L);
    }

    @NotNull
    protected final NodeIterator nodeIterator(K lo) {
        try {
            int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, lo);
            if (keyPos == -1) {
                return this.nodeIterator();
            } else {
                if (keyPos < 0) {
                    keyPos = -keyPos - 2;
                }

                int headSize = keyPos == 0 ? start : 0;
                long offset = (long) keyPos * this.pageSize;
                long offsetWithHead = offset + (long) headSize;
                int nodeCount = this.volume.getInt(offsetWithHead);
                int pos = this.nodeSearch(lo, offset, offsetWithHead, nodeCount);
                if (pos < 0) {
                    pos = -pos - 2;
                }

                long pageOffset = (long) keyPos * this.pageSize;
                return new NodeIterator(this, pageOffset, pageOffset == 0L ? (long) start : pageOffset + (long) headSize, (long) nodeCount, (long) pos - 1L);
            }
        } catch (Exception x) {
            throw new RuntimeException(x); // TODO
        }
    }

    @NotNull
    protected final NodeIterator descendingNodeIterator() {
        long page = this.pageCount * this.pageSize;
        long pageWithHead = page == 0L ? (long) start : page;
        long nodeCount = (long) this.volume.getInt(pageWithHead);
        return new NodeIterator(this, page, pageWithHead, nodeCount, nodeCount);
    }

    @NotNull
    public Iterator keyIterator() {
        if (this.isEmpty()) {
            Iterator var10000 = Collections.emptyIterator();
            Intrinsics.checkNotNullExpressionValue(var10000, "emptyIterator(...)");
            return var10000;
        } else {
            return new Iterator() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeKeys;

                {
                    this.nodeIter = nodeIterator();
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final void loadNextNode() {
                    this.nodeKeys = this.nodeIter.moveToNext() ? this.nodeIter.loadKeys() : null;
                    this.nodePos = 0;
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Object next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        int var3 = this.nodePos++;
                        Object ret = nodeKeys[var3];
                        if (nodeKeys.length == this.nodePos) {
                            this.loadNextNode();
                        }

                        return ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public final Iterator<Entry<K, V>> entryIterator() {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;

                {
                    this.nodeIter = nodeIterator();
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToNext()) {
                        this.nodeKeys = this.nodeIter.loadKeys();
                        NodeIterator var10001 = this.nodeIter;
                        Object[] var10002 = this.nodeKeys;
                        Intrinsics.checkNotNull(var10002);
                        this.nodeVals = var10001.loadVals(var10002.length);
                    } else {
                        this.nodeKeys = null;
                        this.nodeVals = null;
                    }

                    this.nodePos = 0;
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Map.Entry next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        Object var10002 = nodeKeys[this.nodePos];
                        Object[] var10003 = this.nodeVals;
                        Intrinsics.checkNotNull(var10003);
                        AbstractMap.SimpleImmutableEntry ret = new AbstractMap.SimpleImmutableEntry(var10002, var10003[this.nodePos]);
                        int var3 = this.nodePos++;
                        if (nodeKeys.length == this.nodePos) {
                            this.loadNextNode();
                        }

                        return (Map.Entry) ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public final Iterator<V> valueIterator() {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator<V>() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeVals;

                {
                    this.nodeIter = nodeIterator();
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToNext()) {
                        this.nodeVals = this.nodeIter.loadVals(this.nodeIter.keysSize());
                    } else {
                        this.nodeVals = null;
                    }

                    this.nodePos = 0;
                }

                public boolean hasNext() {
                    return this.nodeVals != null;
                }

                public V next() {
                    Object[] nodeVals = this.nodeVals;
                    if (nodeVals == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object ret = nodeVals[this.nodePos];
                        int var3 = this.nodePos++;
                        if (nodeVals.length == this.nodePos) {
                            this.loadNextNode();
                        }

                        return (V) ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Set<Entry<K, V>> getEntries() {
        return this.entries;
    }

    @NotNull
    public NavigableSet getKeys() {
        return this.keys;
    }

    @Nullable
    public NavigableSet navigableKeySet() {
        return (NavigableSet) this.keySet();
    }

    @NotNull
    public Collection getValues() {
        return this.values;
    }

    public void clear() {
        throw new UnsupportedOperationException("read-only");
    }

    @Nullable
    public Object put(@Nullable Object key, @Nullable Object value) {
        throw new UnsupportedOperationException("read-only");
    }

    public void putAll(@NotNull Map from) {
        Intrinsics.checkNotNullParameter(from, "from");
        throw new UnsupportedOperationException("read-only");
    }

    @Nullable
    public V remove(@Nullable Object key) {
        throw new UnsupportedOperationException("read-only");
    }

    @Nullable
    public Object putIfAbsent(@Nullable Object key, @Nullable Object value) {
        throw new UnsupportedOperationException("read-only");
    }

    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("read-only");
    }

    public boolean replace(@Nullable Object key, @Nullable Object oldValue, @Nullable Object newValue) {
        throw new UnsupportedOperationException("read-only");
    }

    @Nullable
    public Object replace(@Nullable Object key, @Nullable Object value) {
        throw new UnsupportedOperationException("read-only");
    }

    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof Map)) {
            return false;
        } else if (((Map) other).size() != this.size()) {
            return false;
        } else {
            try {
                for (Map.Entry e : this.entrySet()) {
                    Object key = e.getKey();
                    Object value = e.getValue();
                    if (value == null) {
                        if (((Map) other).get(key) != null || !((Map) other).containsKey(key)) {
                            return false;
                        }
                    } else if (!Intrinsics.areEqual(value, ((Map) other).get(key))) {
                        return false;
                    }
                }

                return true;
            } catch (ClassCastException var6) {
                return false;
            } catch (NullPointerException var7) {
                return false;
            }
        }
    }

    @Nullable
    public Comparator comparator() {
        return (Comparator) this.getKeySerializer();
    }

    @Nullable
    public K firstKey2() {
        Map.Entry<K, V> var10000 = this.firstEntry();
        return var10000 != null ? var10000.getKey() : null;
    }

    @Nullable
    public K lastKey2() {
        Map.Entry<K, V> var10000 = this.lastEntry();
        return var10000 != null ? var10000.getKey() : null;
    }

    public K firstKey() {
        K var10000 = this.firstKey2();
        if (var10000 == null) {
            throw new NoSuchElementException();
        } else {
            return var10000;
        }
    }

    public K lastKey() {
        K var10000 = this.lastKey2();
        if (var10000 == null) {
            throw new NoSuchElementException();
        } else {
            return var10000;
        }
    }

    @Nullable
    public Map.Entry<K, V> ceilingEntry(@Nullable K key) {
        if (key == null) {
            throw new NullPointerException();
        } else {
            return this.findHigher(key, true);
        }
    }

    @Nullable
    public K ceilingKey(@Nullable K key) {
        Map.Entry<K, V> var10000 = this.ceilingEntry(key);
        return var10000 != null ? var10000.getKey() : null;
    }

    @Nullable
    public Map.Entry<K, V> firstEntry() {
        return this.isEmpty() ? null : this.entryIterator().next();
    }

    @Nullable
    public Map.Entry<K, V> floorEntry(@Nullable K key) {
        if (key == null) {
            throw new NullPointerException();
        } else {
            return this.findLower(key, true);
        }
    }

    @Nullable
    public K floorKey(@Nullable K key) {
        Map.Entry<K, V> var10000 = this.floorEntry(key);
        return var10000 != null ? var10000.getKey() : null;
    }

    @Nullable
    public Map.Entry<K, V> higherEntry(@Nullable K key) {
        if (key == null) {
            throw new NullPointerException();
        } else {
            return this.findHigher(key, false);
        }
    }

    @Nullable
    public K higherKey(@Nullable K key) {
        Map.Entry<K, V> var10000 = this.higherEntry(key);
        return var10000 != null ? var10000.getKey() : null;
    }

    @Nullable
    public Map.Entry<K, V> lastEntry() {
        if (this.isEmpty()) {
            return null;
        } else {
            Object var10000 = this.descendingEntryIterator().next();
            Intrinsics.checkNotNull(var10000, "null cannot be cast to non-null type kotlin.collections.MutableMap.MutableEntry<K of org.mapdb.SortedTableMap, V of org.mapdb.SortedTableMap>");
            return TypeIntrinsics.asMutableMapEntry(var10000);
        }
    }

    @Nullable
    public Map.Entry<K, V> lowerEntry(@Nullable K key) {
        if (key == null) {
            throw new NullPointerException();
        } else {
            return this.findLower(key, false);
        }
    }

    @Nullable
    public K lowerKey(@Nullable K key) {
        Map.Entry<K, V> var10000 = this.lowerEntry(key);
        return var10000 != null ? var10000.getKey() : null;
    }

    @Nullable
    public Map.Entry pollFirstEntry() {
        throw new UnsupportedOperationException("read-only");
    }

    @Nullable
    public Map.Entry pollLastEntry() {
        throw new UnsupportedOperationException("read-only");
    }

    @NotNull
    public ConcurrentNavigableMap subMap(@Nullable Object fromKey, boolean fromInclusive, @Nullable Object toKey, boolean toInclusive) {
        if (fromKey != null && toKey != null) {
            return (ConcurrentNavigableMap) (new BTreeMapJava.SubMap(this, fromKey, fromInclusive, toKey, toInclusive));
        } else {
            throw new NullPointerException();
        }
    }

    @NotNull
    public ConcurrentNavigableMap headMap(@Nullable Object toKey, boolean inclusive) {
        if (toKey == null) {
            throw new NullPointerException();
        } else {
            return (ConcurrentNavigableMap) (new BTreeMapJava.SubMap(this, (Object) null, false, toKey, inclusive));
        }
    }

    @NotNull
    public ConcurrentNavigableMap tailMap(@Nullable Object fromKey, boolean inclusive) {
        if (fromKey == null) {
            throw new NullPointerException();
        } else {
            return (ConcurrentNavigableMap) (new BTreeMapJava.SubMap(this, fromKey, inclusive, (Object) null, false));
        }
    }

    @NotNull
    public ConcurrentNavigableMap<K, V> subMap(Object fromKey, Object toKey) {
        return this.subMap(fromKey, true, toKey, false);
    }

    @NotNull
    public ConcurrentNavigableMap<K, V> headMap(Object toKey) {
        return this.headMap(toKey, false);
    }

    @NotNull
    public ConcurrentNavigableMap<K, V> tailMap(Object fromKey) {
        return this.tailMap(fromKey, true);
    }

    @Nullable
    public NavigableSet<K> descendingKeySet() {
        return this.descendingMap.navigableKeySet();
    }

    @NotNull
    public ConcurrentNavigableMap<K, V> descendingMap() {
        return this.descendingMap;
    }

    @NotNull
    public Iterator<Entry<K, V>> descendingEntryIterator() {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;

                {
                    this.nodeIter = descendingNodeIterator();
                    this.nodePos = -1;
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToPrev()) {
                        Object[] k = this.nodeIter.loadKeys();
                        this.nodeKeys = this.nodeIter.loadKeys();
                        this.nodeVals = this.nodeIter.loadVals(k.length);
                        this.nodePos = k.length - 1;
                    } else {
                        this.nodeKeys = null;
                        this.nodeVals = null;
                        this.nodePos = -1;
                    }

                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Map.Entry next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        Object var10002 = nodeKeys[this.nodePos];
                        Object[] var10003 = this.nodeVals;
                        Intrinsics.checkNotNull(var10003);
                        AbstractMap.SimpleImmutableEntry ret = new AbstractMap.SimpleImmutableEntry(var10002, var10003[this.nodePos]);
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        return (Map.Entry) ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<Entry<K, V>> descendingEntryIterator(@Nullable final K lo, boolean loInclusive, @Nullable final K hi, final boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator() {
                private long page = SortedTableMap.this.getPageSize() * SortedTableMap.this.getPageCount();
                private long pageWithHead;
                private int pageNodeCount;
                private int node;
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;
                private final int loComp;

                {
                    this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                    this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                    this.node = this.pageNodeCount - 1;
                    this.loComp = loInclusive ? 0 : 1;
                    if (hi == null) {
                        this.loadFirstEntry();
                    } else {
                        this.findHi();
                    }

                    this.checkLoBound();
                }

                public final long getPage() {
                    return this.page;
                }

                public final void setPage(long var1) {
                    this.page = var1;
                }

                public final long getPageWithHead() {
                    return this.pageWithHead;
                }

                public final void setPageWithHead(long var1) {
                    this.pageWithHead = var1;
                }

                public final int getPageNodeCount() {
                    return this.pageNodeCount;
                }

                public final void setPageNodeCount(int var1) {
                    this.pageNodeCount = var1;
                }

                public final int getNode() {
                    return this.node;
                }

                public final void setNode(int var1) {
                    this.node = var1;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final int getLoComp() {
                    return this.loComp;
                }

                public final void loadFirstEntry() {
                    try {
                        int keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        int keysBinarySize = nextOffset - keysOffset;
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, keysBinarySize);
                        int keysSize = di.unpackInt();
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize));
                        int valsOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node)));
                        int nextValsOffset = this.pageNodeCount == this.node - 1 ? (int) SortedTableMap.this.getPageSize() : SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node + 1)));
                        int valsBinarySize = nextValsOffset - valsOffset;
                        DataInput2 diVals = SortedTableMap.this.getVolume().getDataInput(this.page + (long) valsOffset, valsBinarySize);
                        this.nodePos = keysSize - 1;
                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(SortedTableMap.this.getValueSerializer().valueArrayDeserialize(diVals, keysSize));
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void findHi() {
                    try {
                        if (hi == null) {
                            throw new NullPointerException();
                        } else {
                            for (int keyPos = SortedTableMap.this.getKeySerializer().valueArraySearch(SortedTableMap.this.getPageKeys(), hi); keyPos != -1; --keyPos) {
                                if ((long) keyPos > SortedTableMap.this.getPageCount()) {
                                    this.loadFirstEntry();
                                    return;
                                }

                                if (keyPos < 0) {
                                    keyPos = -keyPos - 2;
                                }

                                int headSize = keyPos == 0 ? SortedTableMap.start : 0;
                                long offset = (long) keyPos * SortedTableMap.this.getPageSize();
                                long offsetWithHead = offset + (long) headSize;
                                int nodeCount = SortedTableMap.this.getVolume().getInt(offsetWithHead);
                                int nodePos = SortedTableMap.this.nodeSearch(hi, offset, offsetWithHead, nodeCount);
                                if (nodePos < 0) {
                                    nodePos = -nodePos - 2;
                                }

                                while (true) {
                                    long keysOffset = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                                    long keysBinarySize = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                                    DataInput2 di = SortedTableMap.this.getVolume().getDataInput(keysOffset, (int) keysBinarySize);
                                    int keysSize = di.unpackInt();
                                    Object keys = SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                                    int valuePos = SortedTableMap.this.getKeySerializer().valueArraySearch(keys, hi, (Comparator) SortedTableMap.this.getComparator());
                                    if (!hiInclusive && valuePos >= 0) {
                                        --valuePos;
                                    } else if (valuePos < 0) {
                                        valuePos = -valuePos - 2;
                                    }

                                    if (valuePos >= 0) {
                                        if (valuePos >= keysSize) {
                                            --valuePos;
                                        }

                                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(keys);
                                        this.nodePos = valuePos;
                                        this.node = nodePos + 1;
                                        this.pageWithHead = offsetWithHead;
                                        this.pageNodeCount = nodeCount;
                                        this.page = offset;
                                        long valOffset = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount) * 4));
                                        long valsBinarySize = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount + 1) * 4)) - valOffset;
                                        DataInput2 di2 = SortedTableMap.this.getVolume().getDataInput(valOffset, (int) valsBinarySize);
                                        Object vals = SortedTableMap.this.getValueSerializer().valueArrayDeserialize(di2, keysSize);
                                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(vals);
                                        return;
                                    }

                                    --nodePos;
                                    if (nodePos < 0) {
                                        break;
                                    }
                                }
                            }

                            this.nodeKeys = null;
                            this.nodeVals = null;
                        }
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void loadNextNode() {
                    try {
                        if (this.node == 0) {
                            if (this.page == 0L) {
                                this.nodeKeys = null;
                                this.nodeVals = null;
                                return;
                            }

                            this.page -= SortedTableMap.this.getPageSize();
                            this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                            this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                            this.node = this.pageNodeCount;
                        }

                        int keysOffset = this.node;
                        this.node = keysOffset + -1;
                        keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        int keysBinarySize = nextOffset - keysOffset;
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, keysBinarySize);
                        int keysSize = di.unpackInt();
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize));
                        int valsOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node)));
                        int nextValsOffset = this.pageNodeCount == this.node - 1 ? (int) SortedTableMap.this.getPageSize() : SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node + 1)));
                        int valsBinarySize = nextValsOffset - valsOffset;
                        DataInput2 diVals = SortedTableMap.this.getVolume().getDataInput(this.page + (long) valsOffset, valsBinarySize);
                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(SortedTableMap.this.getValueSerializer().valueArrayDeserialize(diVals, keysSize));
                        this.nodePos = keysSize - 1;
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public boolean hasNext() {
                    return this.nodeVals != null;
                }

                public Map.Entry next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        Object var10002 = nodeKeys[this.nodePos];
                        Object[] var10003 = this.nodeVals;
                        Intrinsics.checkNotNull(var10003);
                        AbstractMap.SimpleImmutableEntry ret = new AbstractMap.SimpleImmutableEntry(var10002, var10003[this.nodePos]);
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        this.checkLoBound();
                        return (Map.Entry) ret;
                    }
                }

                public final void checkLoBound() {
                    Object var10000 = lo;
                    if (var10000 != null) {
                        Object lo = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) nextKey, (K) lo) < this.loComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                                this.nodeVals = null;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator descendingKeyIterator() {
        if (this.isEmpty()) {
            Iterator var10000 = Collections.emptyIterator();
            Intrinsics.checkNotNullExpressionValue(var10000, "emptyIterator(...)");
            return var10000;
        } else {
            return new Iterator() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeKeys;

                {
                    this.nodeIter = descendingNodeIterator();
                    this.nodePos = -1;
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToPrev()) {
                        this.nodeKeys = this.nodeIter.loadKeys();
                        Object[] var10001 = this.nodeKeys;
                        Intrinsics.checkNotNull(var10001);
                        this.nodePos = var10001.length - 1;
                    } else {
                        this.nodeKeys = null;
                        this.nodePos = -1;
                    }

                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Object next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        Object ret = nodeKeys[var3];
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        return ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<K> descendingKeyIterator(@Nullable final K lo, boolean loInclusive, @Nullable final K hi, final boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator() {
                private long page = SortedTableMap.this.getPageSize() * SortedTableMap.this.getPageCount();
                private long pageWithHead;
                private int pageNodeCount;
                private int node;
                private int nodePos;
                private Object[] nodeKeys;
                private final int loComp;

                {
                    this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                    this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                    this.node = this.pageNodeCount - 1;
                    this.loComp = loInclusive ? 0 : 1;
                    if (hi == null) {
                        this.loadFirstEntry();
                    } else {
                        this.findHi();
                    }

                    this.checkLoBound();
                }

                public final long getPage() {
                    return this.page;
                }

                public final void setPage(long var1) {
                    this.page = var1;
                }

                public final long getPageWithHead() {
                    return this.pageWithHead;
                }

                public final void setPageWithHead(long var1) {
                    this.pageWithHead = var1;
                }

                public final int getPageNodeCount() {
                    return this.pageNodeCount;
                }

                public final void setPageNodeCount(int var1) {
                    this.pageNodeCount = var1;
                }

                public final int getNode() {
                    return this.node;
                }

                public final void setNode(int var1) {
                    this.node = var1;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final int getLoComp() {
                    return this.loComp;
                }

                public final void loadFirstEntry() {
                    try {
                        int keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, nextOffset - keysOffset);
                        int nodeSize = di.unpackInt();
                        this.nodePos = nodeSize - 1;
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, nodeSize));
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void findHi() {
                    try {
                        if (hi == null) {
                            throw new NullPointerException();
                        } else {
                            for (int keyPos = SortedTableMap.this.getKeySerializer().valueArraySearch(SortedTableMap.this.getPageKeys(), hi); keyPos != -1; --keyPos) {
                                if ((long) keyPos > SortedTableMap.this.getPageCount()) {
                                    this.loadFirstEntry();
                                    return;
                                }

                                if (keyPos < 0) {
                                    keyPos = -keyPos - 2;
                                }

                                int headSize = keyPos == 0 ? SortedTableMap.start : 0;
                                long offset = (long) keyPos * SortedTableMap.this.getPageSize();
                                long offsetWithHead = offset + (long) headSize;
                                int nodeCount = SortedTableMap.this.getVolume().getInt(offsetWithHead);
                                int nodePos = SortedTableMap.this.nodeSearch(hi, offset, offsetWithHead, nodeCount);
                                if (nodePos < 0) {
                                    nodePos = -nodePos - 2;
                                }

                                while (true) {
                                    long keysOffset = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                                    long keysBinarySize = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                                    DataInput2 di = SortedTableMap.this.getVolume().getDataInput(keysOffset, (int) keysBinarySize);
                                    int keysSize = di.unpackInt();
                                    Object keys = SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                                    int valuePos = SortedTableMap.this.getKeySerializer().valueArraySearch(keys, hi, (Comparator) SortedTableMap.this.getComparator());
                                    if (!hiInclusive && valuePos >= 0) {
                                        --valuePos;
                                    } else if (valuePos < 0) {
                                        valuePos = -valuePos - 2;
                                    }

                                    if (valuePos >= 0) {
                                        if (valuePos >= keysSize) {
                                            --valuePos;
                                        }

                                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(keys);
                                        this.nodePos = valuePos;
                                        this.node = nodePos;
                                        this.pageWithHead = offsetWithHead;
                                        this.pageNodeCount = nodeCount;
                                        this.page = offset;
                                        return;
                                    }

                                    --nodePos;
                                    if (nodePos < 0) {
                                        break;
                                    }
                                }
                            }

                            this.nodeKeys = null;
                        }
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void loadNextNode() {
                    try {
                        if (this.node == 0) {
                            if (this.page == 0L) {
                                this.nodeKeys = null;
                                return;
                            }

                            this.page -= SortedTableMap.this.getPageSize();
                            this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                            this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                            this.node = this.pageNodeCount;
                        }

                        int keysOffset = this.node;
                        this.node = keysOffset + -1;
                        keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        int keysBinarySize = nextOffset - keysOffset;
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, keysBinarySize);
                        int keysSize = di.unpackInt();
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize));
                        this.nodePos = keysSize - 1;
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Object next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        Object ret = nodeKeys[this.nodePos];
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        this.checkLoBound();
                        return ret;
                    }
                }

                public final void checkLoBound() {
                    Object var10000 = lo;
                    if (var10000 != null) {
                        Object lo = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) nextKey, (K) lo) < this.loComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<V> descendingValueIterator() {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator() {
                private final NodeIterator nodeIter;
                private int nodePos;
                private Object[] nodeVals;

                {
                    this.nodeIter = descendingNodeIterator();
                    this.nodePos = -1;
                    this.loadNextNode();
                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToPrev()) {
                        this.nodeVals = this.nodeIter.loadVals(this.nodeIter.keysSize());
                        Object[] var10001 = this.nodeVals;
                        Intrinsics.checkNotNull(var10001);
                        this.nodePos = var10001.length - 1;
                    } else {
                        this.nodeVals = null;
                        this.nodePos = -1;
                    }

                }

                public boolean hasNext() {
                    return this.nodeVals != null;
                }

                public Object next() {
                    Object[] var10000 = this.nodeVals;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeVals = var10000;
                        Object ret = nodeVals[this.nodePos];
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        return ret;
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<V> descendingValueIterator(@Nullable final K lo, boolean loInclusive, @Nullable final K hi, final boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator<V>() {
                private long page = SortedTableMap.this.getPageSize() * SortedTableMap.this.getPageCount();
                private long pageWithHead;
                private int pageNodeCount;
                private int node;
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;
                private final int loComp;

                {
                    this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                    this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                    this.node = this.pageNodeCount - 1;
                    this.loComp = loInclusive ? 0 : 1;
                    if (hi == null) {
                        this.loadFirstEntry();
                    } else {
                        this.findHi();
                    }

                    this.checkLoBound();
                }

                public final long getPage() {
                    return this.page;
                }

                public final void setPage(long var1) {
                    this.page = var1;
                }

                public final long getPageWithHead() {
                    return this.pageWithHead;
                }

                public final void setPageWithHead(long var1) {
                    this.pageWithHead = var1;
                }

                public final int getPageNodeCount() {
                    return this.pageNodeCount;
                }

                public final void setPageNodeCount(int var1) {
                    this.pageNodeCount = var1;
                }

                public final int getNode() {
                    return this.node;
                }

                public final void setNode(int var1) {
                    this.node = var1;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final int getLoComp() {
                    return this.loComp;
                }

                public final void loadFirstEntry() {
                    try {
                        int keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        int keysBinarySize = nextOffset - keysOffset;
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, keysBinarySize);
                        int keysSize = di.unpackInt();
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize));
                        int valsOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node)));
                        int nextValsOffset = this.pageNodeCount == this.node - 1 ? (int) SortedTableMap.this.getPageSize() : SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node + 1)));
                        int valsBinarySize = nextValsOffset - valsOffset;
                        DataInput2 diVals = SortedTableMap.this.getVolume().getDataInput(this.page + (long) valsOffset, valsBinarySize);
                        this.nodePos = keysSize - 1;
                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(SortedTableMap.this.getValueSerializer().valueArrayDeserialize(diVals, keysSize));
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void findHi() {
                    try {
                        if (hi == null) {
                            throw new NullPointerException();
                        } else {
                            for (int keyPos = SortedTableMap.this.getKeySerializer().valueArraySearch(SortedTableMap.this.getPageKeys(), hi); keyPos != -1; --keyPos) {
                                if ((long) keyPos > SortedTableMap.this.getPageCount()) {
                                    this.loadFirstEntry();
                                    return;
                                }

                                if (keyPos < 0) {
                                    keyPos = -keyPos - 2;
                                }

                                int headSize = keyPos == 0 ? SortedTableMap.start : 0;
                                long offset = (long) keyPos * SortedTableMap.this.getPageSize();
                                long offsetWithHead = offset + (long) headSize;
                                int nodeCount = SortedTableMap.this.getVolume().getInt(offsetWithHead);
                                int nodePos = SortedTableMap.this.nodeSearch(hi, offset, offsetWithHead, nodeCount);
                                if (nodePos < 0) {
                                    nodePos = -nodePos - 2;
                                }

                                while (true) {
                                    long keysOffset = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                                    long keysBinarySize = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                                    DataInput2 di = SortedTableMap.this.getVolume().getDataInput(keysOffset, (int) keysBinarySize);
                                    int keysSize = di.unpackInt();
                                    Object keys = SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                                    int valuePos = SortedTableMap.this.getKeySerializer().valueArraySearch(keys, hi, (Comparator) SortedTableMap.this.getComparator());
                                    if (!hiInclusive && valuePos >= 0) {
                                        --valuePos;
                                    } else if (valuePos < 0) {
                                        valuePos = -valuePos - 2;
                                    }

                                    if (valuePos >= 0) {
                                        if (valuePos >= keysSize) {
                                            --valuePos;
                                        }

                                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(keys);
                                        this.nodePos = valuePos;
                                        this.node = nodePos + 1;
                                        this.pageWithHead = offsetWithHead;
                                        this.pageNodeCount = nodeCount;
                                        this.page = offset;
                                        long valOffset = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount) * 4));
                                        long valsBinarySize = offset + (long) SortedTableMap.this.getVolume().getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount + 1) * 4)) - valOffset;
                                        DataInput2 di2 = SortedTableMap.this.getVolume().getDataInput(valOffset, (int) valsBinarySize);
                                        Object vals = SortedTableMap.this.getValueSerializer().valueArrayDeserialize(di2, keysSize);
                                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(vals);
                                        return;
                                    }

                                    --nodePos;
                                    if (nodePos < 0) {
                                        break;
                                    }
                                }
                            }

                            this.nodeKeys = null;
                            this.nodeVals = null;
                        }
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void loadNextNode() {
                    try {
                        if (this.node == 0) {
                            if (this.page == 0L) {
                                this.nodeKeys = null;
                                this.nodeVals = null;
                                return;
                            }

                            this.page -= SortedTableMap.this.getPageSize();
                            this.pageWithHead = this.page == 0L ? (long) SortedTableMap.start : this.page;
                            this.pageNodeCount = SortedTableMap.this.getVolume().getInt(this.pageWithHead);
                            this.node = this.pageNodeCount;
                        }

                        int keysOffset = this.node;
                        this.node = keysOffset + -1;
                        keysOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * this.node));
                        int nextOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.node + 1)));
                        int keysBinarySize = nextOffset - keysOffset;
                        DataInput2 di = SortedTableMap.this.getVolume().getDataInput(this.page + (long) keysOffset, keysBinarySize);
                        int keysSize = di.unpackInt();
                        this.nodeKeys = SortedTableMap.this.getKeySerializer().valueArrayToArray(SortedTableMap.this.getKeySerializer().valueArrayDeserialize(di, keysSize));
                        int valsOffset = SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node)));
                        int nextValsOffset = this.pageNodeCount == this.node - 1 ? (int) SortedTableMap.this.getPageSize() : SortedTableMap.this.getVolume().getInt(this.pageWithHead + (long) 4 + (long) (4 * (this.pageNodeCount + this.node + 1)));
                        int valsBinarySize = nextValsOffset - valsOffset;
                        DataInput2 diVals = SortedTableMap.this.getVolume().getDataInput(this.page + (long) valsOffset, valsBinarySize);
                        this.nodeVals = SortedTableMap.this.getValueSerializer().valueArrayToArray(SortedTableMap.this.getValueSerializer().valueArrayDeserialize(diVals, keysSize));
                        this.nodePos = keysSize - 1;
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public boolean hasNext() {
                    return this.nodeVals != null;
                }

                public V next() {
                    Object[] nodeVals = this.nodeVals;
                    if (nodeVals == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object ret = nodeVals[this.nodePos];
                        int var3 = this.nodePos;
                        this.nodePos = var3 + -1;
                        if (this.nodePos == -1) {
                            this.loadNextNode();
                        }

                        this.checkLoBound();
                        return (V) ret;
                    }
                }

                public final void checkLoBound() {
                    Object var10000 = lo;
                    if (var10000 != null) {
                        Object lo = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) nextKey, (K) lo) < this.loComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                                this.nodeVals = null;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<Entry<K, V>> entryIterator(@Nullable final K lo, final boolean loInclusive, @Nullable final K hi, boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator<Entry<K, V>>() {
                private final NodeIterator nodeIter = lo == null ? SortedTableMap.this.nodeIterator() : SortedTableMap.this.nodeIterator(lo);
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;
                private final int hiComp;

                {
                    this.hiComp = hiInclusive ? 0 : 1;
                    if (lo == null) {
                        this.loadNextNode();
                    } else {
                        this.findStart();
                    }

                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final int getHiComp() {
                    return this.hiComp;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToNext()) {
                        this.nodeKeys = this.nodeIter.loadKeys();
                        NodeIterator var10001 = this.nodeIter;
                        Object[] var10002 = this.nodeKeys;
                        Intrinsics.checkNotNull(var10002);
                        this.nodeVals = var10001.loadVals(var10002.length);
                    } else {
                        this.nodeKeys = null;
                        this.nodeVals = null;
                    }

                    this.nodePos = 0;
                }

                public final void findStart() {
                    int comp = loInclusive ? -1 : 0;

                    while (true) {
                        this.loadNextNode();
                        Object[] var10000 = this.nodeKeys;
                        if (var10000 == null) {
                            return;
                        }

                        Object[] keys = var10000;

                        for (int pos = 0; pos < keys.length; ++pos) {
                            if (SortedTableMap.this.getKeySerializer().compare((K) keys[pos], lo) > comp) {
                                this.nodePos = pos;
                                this.checkHiBound();
                                return;
                            }
                        }
                    }
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public Map.Entry next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        var10000 = this.nodeVals;
                        if (var10000 == null) {
                            throw new NoSuchElementException();
                        } else {
                            Object[] nodeVals = var10000;
                            AbstractMap.SimpleImmutableEntry ret = new AbstractMap.SimpleImmutableEntry(nodeKeys[this.nodePos], nodeVals[this.nodePos]);
                            int var4 = this.nodePos++;
                            if (nodeVals.length == this.nodePos) {
                                this.loadNextNode();
                            }

                            this.checkHiBound();
                            return (Map.Entry) ret;
                        }
                    }
                }

                public final void checkHiBound() {
                    Object var10000 = hi;
                    if (var10000 != null) {
                        Object hi = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) hi, (K) nextKey) < this.hiComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<K> keyIterator(@Nullable final K lo, final boolean loInclusive, @Nullable final K hi, boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator<K>() {
                private final NodeIterator nodeIter = lo == null ? SortedTableMap.this.nodeIterator() : SortedTableMap.this.nodeIterator(lo);
                private int nodePos;
                private Object[] nodeKeys;
                private final int hiComp;

                {
                    this.hiComp = hiInclusive ? 0 : 1;
                    if (lo == null) {
                        this.loadNextNode();
                    } else {
                        this.findStart();
                    }

                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final int getHiComp() {
                    return this.hiComp;
                }

                public final void loadNextNode() {
                    this.nodeKeys = this.nodeIter.moveToNext() ? this.nodeIter.loadKeys() : null;
                    this.nodePos = 0;
                }

                public final void findStart() {
                    int comp = loInclusive ? -1 : 0;

                    while (true) {
                        this.loadNextNode();
                        Object[] var10000 = this.nodeKeys;
                        if (var10000 == null) {
                            return;
                        }

                        Object[] keys = var10000;

                        for (int pos = 0; pos < keys.length; ++pos) {
                            if (SortedTableMap.this.getKeySerializer().compare((K) keys[pos], lo) > comp) {
                                this.nodePos = pos;
                                this.checkHiBound();
                                return;
                            }
                        }
                    }
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public K next() {
                    Object[] var10000 = this.nodeKeys;
                    if (var10000 == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeKeys = var10000;
                        int var3 = this.nodePos++;
                        Object ret = nodeKeys[var3];
                        if (nodeKeys.length == this.nodePos) {
                            this.loadNextNode();
                        }

                        this.checkHiBound();
                        return (K) ret;
                    }
                }

                public final void checkHiBound() {
                    Object var10000 = hi;
                    if (var10000 != null) {
                        Object hi = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) hi, (K) nextKey) < this.hiComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @NotNull
    public Iterator<V> valueIterator(@Nullable final K lo, final boolean loInclusive, @Nullable final K hi, boolean hiInclusive) {
        if (this.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return new Iterator<V>() {
                private final NodeIterator nodeIter = lo == null ? SortedTableMap.this.nodeIterator() : SortedTableMap.this.nodeIterator(lo);
                private int nodePos;
                private Object[] nodeKeys;
                private Object[] nodeVals;
                private final int hiComp;

                {
                    this.hiComp = hiInclusive ? 0 : 1;
                    if (lo == null) {
                        this.loadNextNode();
                    } else {
                        this.findStart();
                    }

                }

                public final NodeIterator getNodeIter() {
                    return this.nodeIter;
                }

                public final int getNodePos() {
                    return this.nodePos;
                }

                public final void setNodePos(int var1) {
                    this.nodePos = var1;
                }

                public final Object[] getNodeKeys() {
                    return this.nodeKeys;
                }

                public final void setNodeKeys(Object[] var1) {
                    this.nodeKeys = var1;
                }

                public final Object[] getNodeVals() {
                    return this.nodeVals;
                }

                public final void setNodeVals(Object[] var1) {
                    this.nodeVals = var1;
                }

                public final int getHiComp() {
                    return this.hiComp;
                }

                public final void loadNextNode() {
                    if (this.nodeIter.moveToNext()) {
                        this.nodeKeys = this.nodeIter.loadKeys();
                        NodeIterator var10001 = this.nodeIter;
                        Object[] var10002 = this.nodeKeys;
                        Intrinsics.checkNotNull(var10002);
                        this.nodeVals = var10001.loadVals(var10002.length);
                    } else {
                        this.nodeKeys = null;
                        this.nodeVals = null;
                    }

                    this.nodePos = 0;
                }

                public final void findStart() {
                    int comp = loInclusive ? -1 : 0;

                    while (true) {
                        this.loadNextNode();
                        Object[] var10000 = this.nodeKeys;
                        if (var10000 == null) {
                            return;
                        }

                        Object[] keys = var10000;

                        for (int pos = 0; pos < keys.length; ++pos) {
                            if (SortedTableMap.this.getKeySerializer().compare((K) keys[pos], lo) > comp) {
                                this.nodePos = pos;
                                this.checkHiBound();
                                return;
                            }
                        }
                    }
                }

                public boolean hasNext() {
                    return this.nodeKeys != null;
                }

                public V next() {
                    if (this.nodeKeys == null) {
                        throw new NoSuchElementException();
                    } else {
                        Object[] nodeVals = this.nodeVals;
                        if (nodeVals == null) {
                            throw new NoSuchElementException();
                        } else {
                            int var3 = this.nodePos++;
                            Object ret = nodeVals[var3];
                            if (nodeVals.length == this.nodePos) {
                                this.loadNextNode();
                            }

                            this.checkHiBound();
                            return (V) ret;
                        }
                    }
                }

                public final void checkHiBound() {
                    Object var10000 = hi;
                    if (var10000 != null) {
                        Object hi = var10000;
                        var10000 = this.nodeKeys;
                        if (var10000 != null) {
                            Object[] nodeKeys = (Object[]) var10000;
                            Object nextKey = nodeKeys[this.nodePos];
                            if (SortedTableMap.this.getKeySerializer().compare((K) hi, (K) nextKey) < this.hiComp) {
                                this.nodeKeys = null;
                                this.nodePos = -1;
                            }

                        }
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException("read-only");
                }
            };
        }
    }

    @Nullable
    public Map.Entry<K, V> findHigher(@Nullable K key, boolean inclusive) {
        try {
            if (key == null) {
                throw new NullPointerException();
            } else {
                for (int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, key); keyPos != -1; ++keyPos) {
                    if ((long) keyPos > this.pageCount) {
                        return null;
                    }

                    if (keyPos < 0) {
                        keyPos = -keyPos - 2;
                    }

                    int headSize = keyPos == 0 ? start : 0;
                    long offset = (long) keyPos * this.pageSize;
                    long offsetWithHead = offset + (long) headSize;
                    int nodeCount = this.volume.getInt(offsetWithHead);
                    int nodePos = this.nodeSearch(key, offset, offsetWithHead, nodeCount);
                    if (nodePos == -1) {
                        nodePos = 0;
                    } else if (nodePos < 0) {
                        nodePos = -nodePos - 2;
                    }

                    while (true) {
                        long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                        long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                        DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
                        int keysSize = di.unpackInt();
                        Object keys = this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                        int valuePos = this.getKeySerializer().valueArraySearch(keys, key, (Comparator) this.comparator);
                        if (!inclusive && valuePos >= 0) {
                            ++valuePos;
                        }

                        if (valuePos < 0) {
                            valuePos = -valuePos - 1;
                        }

                        if (valuePos < keysSize) {
                            Object key2 = this.getKeySerializer().valueArrayGet(keys, valuePos);
                            long valOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount) * 4));
                            long valsBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount + 1) * 4)) - valOffset;
                            DataInput2 di2 = this.volume.getDataInput(valOffset, (int) valsBinarySize);
                            Object value = this.getValueSerializer().valueArrayBinaryGet(di2, keysSize, valuePos);
                            return (Map.Entry) (new AbstractMap.SimpleImmutableEntry(key2, value));
                        }

                        ++nodePos;
                        if (nodePos >= nodeCount) {
                            break;
                        }
                    }
                }

                return this.firstEntry();
            }
        } catch (Exception x) {
            throw new RuntimeException(x); // TODO
        }
    }

    @Nullable
    public Map.Entry<K, V> findLower(@Nullable K key, boolean inclusive) {
        try {
            if (key == null) {
                throw new NullPointerException();
            } else {
                for (int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, key); keyPos != -1; --keyPos) {
                    if ((long) keyPos > this.pageCount) {
                        return this.lastEntry();
                    }

                    if (keyPos < 0) {
                        keyPos = -keyPos - 2;
                    }

                    int headSize = keyPos == 0 ? start : 0;
                    long offset = (long) keyPos * this.pageSize;
                    long offsetWithHead = offset + (long) headSize;
                    int nodeCount = this.volume.getInt(offsetWithHead);
                    int nodePos = this.nodeSearch(key, offset, offsetWithHead, nodeCount);
                    if (nodePos < 0) {
                        nodePos = -nodePos - 2;
                    }

                    while (true) {
                        long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                        long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                        DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
                        int keysSize = di.unpackInt();
                        Object keys = this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                        int valuePos = this.getKeySerializer().valueArraySearch(keys, key, (Comparator) this.comparator);
                        if (!inclusive && valuePos >= 0) {
                            --valuePos;
                        } else if (valuePos < 0) {
                            valuePos = -valuePos - 2;
                        }

                        if (valuePos >= 0) {
                            if (valuePos >= keysSize) {
                                --valuePos;
                            }

                            Object key2 = this.getKeySerializer().valueArrayGet(keys, valuePos);
                            long valOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount) * 4));
                            long valsBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) ((nodePos + nodeCount + 1) * 4)) - valOffset;
                            DataInput2 di2 = this.volume.getDataInput(valOffset, (int) valsBinarySize);
                            Object value = this.getValueSerializer().valueArrayBinaryGet(di2, keysSize, valuePos);
                            return (Map.Entry) (new AbstractMap.SimpleImmutableEntry(key2, value));
                        }

                        --nodePos;
                        if (nodePos < 0) {
                            break;
                        }
                    }
                }

                return null;
            }
        } catch (Exception x) {
            throw new RuntimeException(x); // TODO
        }
    }

    @Nullable
    public K findHigherKey(@Nullable K key, boolean inclusive) {
        try {
            if (key == null) {
                throw new NullPointerException();
            } else {
                for (int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, key); keyPos != -1; ++keyPos) {
                    if ((long) keyPos > this.pageCount) {
                        return null;
                    }

                    if (keyPos < 0) {
                        keyPos = -keyPos - 2;
                    }

                    int headSize = keyPos == 0 ? start : 0;
                    long offset = (long) keyPos * this.pageSize;
                    long offsetWithHead = offset + (long) headSize;
                    int nodeCount = this.volume.getInt(offsetWithHead);
                    int nodePos = this.nodeSearch(key, offset, offsetWithHead, nodeCount);
                    if (nodePos == -1) {
                        nodePos = 0;
                    } else if (nodePos < 0) {
                        nodePos = -nodePos - 2;
                    }

                    while (true) {
                        long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                        long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                        DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
                        int keysSize = di.unpackInt();
                        Object keys = this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                        int valuePos = this.getKeySerializer().valueArraySearch(keys, key, (Comparator) this.comparator);
                        if (!inclusive && valuePos >= 0) {
                            ++valuePos;
                        }

                        if (valuePos < 0) {
                            valuePos = -valuePos - 1;
                        }

                        if (valuePos < keysSize) {
                            return this.getKeySerializer().valueArrayGet(keys, valuePos);
                        }

                        ++nodePos;
                        if (nodePos >= nodeCount) {
                            break;
                        }
                    }
                }

                return this.firstKey();
            }
        } catch (Exception x) {
            throw new RuntimeException(x); // TODO
        }
    }

    @Nullable
    public K findLowerKey(@Nullable K key, boolean inclusive) {
        try {
            if (key == null) {
                throw new NullPointerException();
            } else {
                for (int keyPos = this.getKeySerializer().valueArraySearch(this.pageKeys, key); keyPos != -1; --keyPos) {
                    if ((long) keyPos > this.pageCount) {
                        return this.lastKey();
                    }

                    if (keyPos < 0) {
                        keyPos = -keyPos - 2;
                    }

                    int headSize = keyPos == 0 ? start : 0;
                    long offset = (long) keyPos * this.pageSize;
                    long offsetWithHead = offset + (long) headSize;
                    int nodeCount = this.volume.getInt(offsetWithHead);
                    int nodePos = this.nodeSearch(key, offset, offsetWithHead, nodeCount);
                    if (nodePos < 0) {
                        nodePos = -nodePos - 2;
                    }

                    while (true) {
                        long keysOffset = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4));
                        long keysBinarySize = offset + (long) this.volume.getInt(offsetWithHead + (long) 4 + (long) (nodePos * 4) + (long) 4) - keysOffset;
                        DataInput2 di = this.volume.getDataInput(keysOffset, (int) keysBinarySize);
                        int keysSize = di.unpackInt();
                        Object keys = this.getKeySerializer().valueArrayDeserialize(di, keysSize);
                        int valuePos = this.getKeySerializer().valueArraySearch(keys, key, (Comparator) this.comparator);
                        if (!inclusive && valuePos >= 0) {
                            --valuePos;
                        } else if (valuePos < 0) {
                            valuePos = -valuePos - 2;
                        }

                        if (valuePos >= 0) {
                            if (valuePos >= keysSize) {
                                --valuePos;
                            }

                            return this.getKeySerializer().valueArrayGet(keys, valuePos);
                        }

                        --nodePos;
                        if (nodePos < 0) {
                            break;
                        }
                    }
                }

                return null;
            }
        } catch (Exception x) {
            throw new RuntimeException(x); // TODO
        }
    }

    public void forEachKey(@NotNull Function1 procedure) {
        Intrinsics.checkNotNullParameter(procedure, "procedure");
        Iterator var10000 = ((NavigableSet) this.keySet()).iterator();
        Intrinsics.checkNotNullExpressionValue(var10000, "iterator(...)");
        Iterator var2 = var10000;

        while (var2.hasNext()) {
            Object k = var2.next();
            procedure.invoke(k);
        }

    }

    public void forEachValue(@NotNull Function1 procedure) {
        Intrinsics.checkNotNullParameter(procedure, "procedure");

        for (Object k : this.values()) {
            procedure.invoke(k);
        }

    }

    public void forEach(@NotNull BiConsumer action) {
        Intrinsics.checkNotNullParameter(action, "action");

        for (Map.Entry e : this.entrySet()) {
            action.accept(e.getKey(), e.getValue());
        }

    }

    public boolean isClosed() {
        return this.volume.isClosed();
    }

    public final void close() {
        this.volume.close();
    }

    public boolean putIfAbsentBoolean(@Nullable Object key, @Nullable Object value) {
        throw new UnsupportedOperationException("read-only");
    }

    // $FF: bridge method
    public final int size() {
        return this.getSize();
    }

    // $FF: bridge method
    public final Set<Entry<K, V>> entrySet() {
        return this.getEntries();
    }

    // $FF: bridge method
    public final NavigableSet keySet() {
        return this.getKeys();
    }

    // $FF: bridge method
    public final Collection values() {
        return this.getValues();
    }

    public abstract static class Sink<K, V> extends Pump.Sink<Pair<K, V>, SortedTableMap<K, V>> {
        public final void put(K key, V value) {
            this.put(new Pair<>(key, value));
        }
    }

    @NotNull
    public static Maker create(@NotNull Volume volume, @NotNull GroupSerializer keySerializer, @NotNull GroupSerializer valueSerializer) {
        return new Maker(volume, keySerializer, valueSerializer);
    }

    @JvmStatic
    @NotNull
    public static SortedTableMap open(@NotNull Volume volume, @NotNull GroupSerializer keySerializer, @NotNull GroupSerializer valueSerializer) {
        try {
            long pageSize = volume.getLong(SortedTableMap.PAGE_SIZE_OFFSET);
            if (pageSize <= 0L) {
                throw new DBException.DataCorruption("Wrong page size: " + pageSize);
            } else {
                int volSliceSize = volume.sliceSize();
                if (volSliceSize > 0 && (long) volSliceSize < pageSize) {
                    throw new DBException.WrongConfiguration("Slice Size of underlying Volume is too small.");
                } else {
                    return new SortedTableMap(keySerializer, valueSerializer, pageSize, volume, false);
                }
            }
        } catch (Exception x) {
            Utils.sneakyThrow(x);
            return null;
        }
    }

    @NotNull
    public static <K, V> Sink<K, V> createFromSink(@NotNull GroupSerializer<K> keySerializer,
                                                   @NotNull GroupSerializer<V> valueSerializer,
                                                   @NotNull Volume volume, Long pageSizes, Integer nodeSizes) {
        if (pageSizes == null) {
            pageSizes = CC.PAGE_SIZE;
        }
        if (nodeSizes == null) {
            nodeSizes = CC.BTREEMAP_MAX_NODE_SIZE;
        }
        long pageSize = pageSizes;
        long nodeSize = nodeSizes;

        int volSliceSize = volume.sliceSize();
        if (volSliceSize > 0 && (long) volSliceSize < pageSize) {
            throw new DBException.WrongConfiguration("Slice Size of underlying Volume is too small.");
        } else {
            return new Sink() {
                private final byte[] bytes = new byte[(int) pageSize];
                private final ArrayList<byte[]> nodeKeys = new ArrayList<>();
                private final ArrayList<byte[]> nodeVals = new ArrayList<>();
                private final ArrayList<Pair<K, V>> pairs = new ArrayList<>();
                private int nodesSize;
                private long fileTail;
                private K oldKey;

                {
                    this.nodesSize = SortedTableMap.start + 4 + 4;
                }

                public final byte[] getBytes() {
                    return this.bytes;
                }

                public final ArrayList getNodeKeys() {
                    return this.nodeKeys;
                }

                public final ArrayList getNodeVals() {
                    return this.nodeVals;
                }

                public final ArrayList getPairs() {
                    return this.pairs;
                }

                public final int getNodesSize() {
                    return this.nodesSize;
                }

                public final void setNodesSize(int var1) {
                    this.nodesSize = var1;
                }

                public final long getFileTail() {
                    return this.fileTail;
                }

                public final void setFileTail(long var1) {
                    this.fileTail = var1;
                }

                public final K getOldKey() {
                    return this.oldKey;
                }

                public final void setOldKey(K var1) {
                    this.oldKey = var1;
                }

                public void put(Pair<K, V> e) {
                    if (this.oldKey != null && keySerializer.compare(this.oldKey, e.getFirst()) >= 0) {
                        throw new DBException.NotSorted();
                    } else {
                        this.oldKey = e.getFirst();
                        this.pairs.add(e);
                        long var2 = this.counter;
                        this.counter = (var2 + 1L);
                        if (this.pairs.size() >= nodeSize) {
                            this.pairsToNodes();
                        }
                    }
                }

                public SortedTableMap<K, V> create() {
                    try {
                        this.pairsToNodes();
                        if (!this.nodeKeys.isEmpty()) {
                            this.flushPage();
                        }

                        if (this.counter == 0L) {
                            volume.ensureAvailable((long) SortedTableMap.start);
                        }

                        volume.putLong(0L, 5335076708573773824L);
                        volume.putLong(SortedTableMap.SIZE_OFFSET, this.counter);
                        volume.putLong(SortedTableMap.PAGE_COUNT_OFFSET, (this.fileTail - pageSize) / pageSize);
                        volume.putLong(SortedTableMap.PAGE_SIZE_OFFSET, pageSize);
                        volume.sync();
                        return new SortedTableMap<>(keySerializer, valueSerializer, pageSize, volume, false);
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void pairsToNodes() {
                    try {
                        if (pairs.isEmpty())
                            return;

                        Object[] keys = pairs.stream().map(x -> x.getFirst()).toArray();
                        DataOutput2 out = new DataOutput2();
                        out.packInt(keys.length);
                        keySerializer.valueArraySerialize(out, keySerializer.valueArrayFromArray(keys));
                        byte[] binaryKeys = out.copyBytes();

                        Object[] values = pairs.stream().map(x -> x.getSecond()).toArray();
                        out.pos = 0;
                        valueSerializer.valueArraySerialize(out, valueSerializer.valueArrayFromArray(values));
                        byte[] binaryVals = out.copyBytes();

                        this.pairs.clear();
                        int newNodesSize = this.nodesSize + 8 + binaryKeys.length + binaryVals.length;
                        if (newNodesSize < pageSize) {
                            this.nodesSize = newNodesSize;
                            this.nodeKeys.add(binaryKeys);
                            this.nodeVals.add(binaryVals);
                        } else {
                            this.flushPage();
                            this.nodesSize = 16 + binaryKeys.length + binaryVals.length;
                            this.nodeKeys.add(binaryKeys);
                            this.nodeVals.add(binaryVals);
                        }
                    } catch (Exception x) {
                        throw new RuntimeException(x); // TODO
                    }
                }

                public final void flushPage() {
                    if (!this.nodeKeys.isEmpty()) {
                        byte[] bytes = this.bytes;
                        int headSize = this.fileTail == 0L ? SortedTableMap.start : 0;
                        DataIO.putInt(bytes, headSize, this.nodeKeys.size());
                        int intPos = headSize + 4;
                        int pos = headSize + 4 + 8 * this.nodeKeys.size() + 4;
                        ArrayList[] var6 = new ArrayList[]{this.nodeKeys, this.nodeVals};
                        ArrayList[] var5 = var6;
                        int var12 = 0;

                        for (int var7 = var6.length; var12 < var7; ++var12) {
                            ArrayList array = var5[var12];
                            Iterator var10000 = array.iterator();
                            Intrinsics.checkNotNullExpressionValue(var10000, "iterator(...)");

                            byte[] bb;
                            for (Iterator var9 = var10000; var9.hasNext(); pos += bb.length) {
                                Object var13 = var9.next();
                                Intrinsics.checkNotNullExpressionValue(var13, "next(...)");
                                bb = (byte[]) var13;
                                DataIO.putInt(bytes, intPos, pos);
                                if (pos + bb.length > bytes.length) {
                                    throw new AssertionError();
                                }

                                System.arraycopy(bb, 0, bytes, pos, bb.length);
                                intPos += 4;
                            }
                        }

                        DataIO.putInt(bytes, intPos, pos);

                        for (intPos += 4; (long) pos < pageSize; bytes[pos++] = 0) {
                        }

                        if (intPos != headSize + 4 + 8 * this.nodeKeys.size() + 4) {
                            throw new AssertionError();
                        } else {
                            volume.ensureAvailable(this.fileTail + pageSize);
                            volume.putData(this.fileTail, bytes, 0, bytes.length);
                            this.fileTail += pageSize;
                            this.nodeKeys.clear();
                            this.nodeVals.clear();
                        }
                    }
                }

                public void put(Object e) {
                    this.put((Pair) e);
                }
            };
        }
    }

    public static final class Maker {
        @Nullable
        private final Volume _volume;
        @Nullable
        private final GroupSerializer _keySerializer;
        @Nullable
        private final GroupSerializer _valueSerializer;
        private long _pageSize;
        private int _nodeSize;

        public Maker(@Nullable Volume _volume, @Nullable GroupSerializer _keySerializer, @Nullable GroupSerializer _valueSerializer) {
            this._volume = _volume;
            this._keySerializer = _keySerializer;
            this._valueSerializer = _valueSerializer;
            this._pageSize = 1048576L;
            this._nodeSize = 32;
        }

        @Nullable
        protected final Volume get_volume() {
            return this._volume;
        }

        @Nullable
        protected final GroupSerializer get_keySerializer() {
            return this._keySerializer;
        }

        @Nullable
        protected final GroupSerializer get_valueSerializer() {
            return this._valueSerializer;
        }

        protected final long get_pageSize() {
            return this._pageSize;
        }

        protected final void set_pageSize(long var1) {
            this._pageSize = var1;
        }

        protected final int get_nodeSize() {
            return this._nodeSize;
        }

        protected final void set_nodeSize(int var1) {
            this._nodeSize = var1;
        }

        @NotNull
        public final Maker pageSize(long pageSize) {
            this._pageSize = DataIO.nextPowTwo(pageSize);
            return this;
        }

        @NotNull
        public final Maker nodeSize(int nodeSize) {
            this._nodeSize = nodeSize;
            return this;
        }

        @NotNull
        public final SortedTableMap createFrom(@NotNull Iterable<Pair> pairs) {
            Sink consumer = this.createFromSink();

            for (Pair pair : pairs) {
                consumer.put(pair);
            }

            return (SortedTableMap) consumer.create();
        }

        @NotNull
        public <K, V> SortedTableMap<K, V> createFrom(@NotNull Map<K, V> map) {
            Sink consumer = this.createFromSink();

            for (Map.Entry<K, V> pair : map.entrySet()) {
                consumer.put(new Pair(pair.getKey(), pair.getValue()));
            }

            return (SortedTableMap) consumer.create();
        }

        @NotNull
        public final Sink createFromSink() {
            GroupSerializer var10001 = this._keySerializer;
            Intrinsics.checkNotNull(var10001);
            GroupSerializer var10002 = this._valueSerializer;
            Intrinsics.checkNotNull(var10002);
            Volume var10003 = this._volume;
            Intrinsics.checkNotNull(var10003);
            return SortedTableMap.createFromSink(var10001, var10002, var10003, this._pageSize, this._nodeSize);
        }
    }

    protected static final class NodeIterator {
        @NotNull
        private SortedTableMap map;
        private long pageOffset;
        private long pageWithHeadOffset;
        private long pageNodeCount;
        private long node;

        public NodeIterator(@NotNull SortedTableMap map, long pageOffset, long pageWithHeadOffset, long pageNodeCount, long node) {
            this.map = map;
            this.pageOffset = pageOffset;
            this.pageWithHeadOffset = pageWithHeadOffset;
            this.pageNodeCount = pageNodeCount;
            this.node = node;
        }

        public final boolean moveToNext() {
            ++this.node;
            if (this.node >= this.pageNodeCount) {
                this.pageOffset += this.map.getPageSize();
                this.pageWithHeadOffset = this.pageOffset;
                if (this.pageOffset > this.map.getPageCount() * this.map.getPageSize()) {
                    return false;
                }

                this.pageNodeCount = (long) this.map.getVolume().getInt(this.pageWithHeadOffset);
                this.node = 0L;
            }

            return true;
        }

        public final boolean moveToPrev() {
            this.node += -1L;
            if (this.node <= -1L) {
                this.pageOffset -= this.map.getPageSize();
                this.pageWithHeadOffset = this.pageOffset == 0L ? (long) SortedTableMap.start : this.pageOffset;
                if (this.pageOffset < 0L) {
                    return false;
                }

                this.pageNodeCount = (long) this.map.getVolume().getInt(this.pageWithHeadOffset);
                this.node = this.pageNodeCount - 1L;
            }

            return true;
        }

        public final long keysOffset() {
            return this.pageOffset + (long) this.map.getVolume().getInt(this.pageWithHeadOffset + (1L + this.node) * (long) 4);
        }

        public final long keysOffsetEnd() {
            return this.pageOffset + (long) this.map.getVolume().getInt(this.pageWithHeadOffset + (1L + this.node + 1L) * (long) 4);
        }

        public final long valsOffset() {
            return this.pageOffset + (long) this.map.getVolume().getInt(this.pageWithHeadOffset + (1L + this.pageNodeCount + this.node) * (long) 4);
        }

        public final long valsOffsetEnd() {
            return this.pageOffset + (long) this.map.getVolume().getInt(this.pageWithHeadOffset + (1L + this.pageNodeCount + this.node + 1L) * (long) 4);
        }

        public final int keysSize() {
            return (int) this.map.getVolume().getPackedLong(this.keysOffset());
        }

        @NotNull
        public final Object[] loadKeys() {
            try {
                long keysOffset = this.keysOffset();
                long keysBinarySize = this.keysOffsetEnd() - keysOffset;
                DataInput2 di = this.map.getVolume().getDataInput(keysOffset, (int) keysBinarySize);
                int keysSize = di.unpackInt();
                Object[] var10000 = this.map.getKeySerializer().valueArrayToArray(this.map.getKeySerializer().valueArrayDeserialize(di, keysSize));
                Intrinsics.checkNotNullExpressionValue(var10000, "valueArrayToArray(...)");
                return var10000;
            } catch (Exception x) {
                throw new RuntimeException(x); // TODO
            }
        }

        @NotNull
        public final Object[] loadVals(int keysSize) {
            try {
                long valsOffset = this.valsOffset();
                long valsBinarySize = this.valsOffsetEnd() - valsOffset;
                DataInput2 di = this.map.getVolume().getDataInput(valsOffset, (int) valsBinarySize);
                Object[] var10000 = this.map.getValueSerializer().valueArrayToArray(this.map.getValueSerializer().valueArrayDeserialize(di, keysSize));
                return var10000;
            } catch (Exception x) {
                throw new RuntimeException(x); // TODO
            }
        }
    }

}
