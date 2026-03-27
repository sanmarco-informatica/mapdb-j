package org.mapdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Ref;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.serializer.GroupSerializer;

public final class Pump {
    @NotNull
    public static final Pump INSTANCE = new Pump();

    private Pump() {
    }

    @NotNull
    public static <K, V> Sink<Pair<K, V>, Void> treeMap(@NotNull final Store store,
                                                        @NotNull final GroupSerializer<K> keySerializer,
                                                        @NotNull final GroupSerializer<V> valueSerializer,
                                                        final Comparator<K> cmp,
                                                        final int leafNodeSize,
                                                        final int dirNodeSize,
                                                        final Boolean values,
                                                        final Boolean inline) {
        final Ref.ObjectRef<K> prevKey = new Ref.ObjectRef<>();
        boolean valueInline = inline == null || inline;
        boolean hasValues = values == null || values;
        Comparator<K> comparator = cmp == null ? keySerializer : cmp;
        return new Sink<Pair<K, V>, Void>() {
            private final LinkedList<DirData<K>> dirStack = new LinkedList<>();
            private final ArrayList<K> keys = new ArrayList<>();
            private final ArrayList<V> values = new ArrayList<>();
            private int leftEdgeLeaf = 4;
            private long nextLeafLink;
            private final BTreeMapJava.NodeSerializer<K, V> nodeSer;

            {
                this.nodeSer = new BTreeMapJava.NodeSerializer(keySerializer, comparator, valueInline  ? valueSerializer : Serializer.RECID);
            }

            public final LinkedList<DirData<K>> getDirStack() {
                return this.dirStack;
            }

            public final ArrayList<K> getKeys() {
                return this.keys;
            }

            public final ArrayList<V> getValues() {
                return this.values;
            }

            public final int getLeftEdgeLeaf() {
                return this.leftEdgeLeaf;
            }

            public final void setLeftEdgeLeaf(int var1) {
                this.leftEdgeLeaf = var1;
            }

            public final long getNextLeafLink() {
                return this.nextLeafLink;
            }

            public final void setNextLeafLink(long var1) {
                this.nextLeafLink = var1;
            }

            public final BTreeMapJava.NodeSerializer<K, V> getNodeSer() {
                return this.nodeSer;
            }

            public final Object nodeValues() {
                Object var10000;
                if (!hasValues) {
                    var10000 = this.keys.size();
                } else if (valueInline) {
                    GroupSerializer var14 = valueSerializer;
                    ArrayList var10001 = this.values;
                    var10000 = var14.valueArrayFromArray(var10001.toArray());
                } else {
                    ArrayList var15 = this.values;
                    Iterable $this$map$iv = (Iterable) var15;
                    Store var3 = store;
                    GroupSerializer var4 = valueSerializer;
                    int $i$f$map = 0;
                    Collection destination$iv$iv = (Collection) (new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10)));
                    int $i$f$mapTo = 0;

                    for (Object item$iv$iv : $this$map$iv) {
                        int var12 = 0;
                        destination$iv$iv.add(var3.put(item$iv$iv, (Serializer) var4));
                    }

                    var10000 = CollectionsKt.toLongArray((Collection) ((List) destination$iv$iv));
                }

                Object var1 = var10000;
                return var1;
            }

            @Override
            public void put(Pair<K, V> e) {
                if (prevKey.element != null && comparator.compare(prevKey.element, e.getFirst()) >= 0) {
                    throw new DBException.NotSorted();
                } else {
                    prevKey.element = e.getFirst();
                    long link = this.getCounter$Sources_of_mapdb_main();
                    this.setCounter$Sources_of_mapdb_main(link + 1L);
                    this.keys.add(e.getFirst());
                    this.values.add(e.getSecond());
                    if (this.keys.size() >= leafNodeSize) {
                        link = store.preallocate();
                        BTreeMapJava.Node node = new BTreeMapJava.Node(this.leftEdgeLeaf + 1, link, keySerializer.valueArrayFromArray(this.keys.toArray()), this.nodeValues());
                        if (this.nextLeafLink == 0L) {
                            this.nextLeafLink = store.put(node, (Serializer) this.nodeSer);
                        } else {
                            store.update(this.nextLeafLink, node, (Serializer) this.nodeSer);
                        }

                        K lastKey = CollectionsKt.last(this.keys);
                        K keyFromLowerLevel = lastKey;
                        long childFromLowerLevel = this.nextLeafLink;
                        this.nextLeafLink = link;
                        this.keys.clear();
                        this.keys.add(lastKey);
                        this.leftEdgeLeaf = 0;
                        this.values.clear();
                        Iterator var10000 = this.dirStack.iterator();
                        Iterator var9 = var10000;

                        while (var9.hasNext()) {
                            Object var17 = var9.next();
                            DirData<K> dir = (DirData) var17;
                            dir.getKeys().add(keyFromLowerLevel);
                            dir.getChild().add(childFromLowerLevel);
                            if (dir.getKeys().size() < dirNodeSize) {
                                return;
                            }

                            link = store.preallocate();
                            BTreeMapJava.Node dirNode = new BTreeMapJava.Node(dir.getLeftEdge() + 8, link, keySerializer.valueArrayFromArray(dir.getKeys().toArray()), dir.getChild().toArray());
                            if (dir.getNextDirLink() == 0L) {
                                dir.setNextDirLink(store.put(dirNode, (Serializer) this.nodeSer));
                            } else {
                                store.update(dir.getNextDirLink(), dirNode, (Serializer) this.nodeSer);
                            }

                            lastKey = CollectionsKt.last(dir.getKeys());
                            keyFromLowerLevel = lastKey;
                            childFromLowerLevel = dir.getNextDirLink();
                            dir.getKeys().clear();
                            dir.getKeys().add(lastKey);
                            dir.getChild().clear();
                            dir.setLeftEdge(0);
                            dir.setNextDirLink(link);
                        }

                        if (keyFromLowerLevel != null) {
                            DirData dir = new DirData();
                            dir.getKeys().add(keyFromLowerLevel);
                            dir.getChild().add(childFromLowerLevel);
                            this.dirStack.add(dir);
                        }

                    }
                }
            }

            public Void create() {
                BTreeMapJava.Node endLeaf = new BTreeMapJava.Node(this.leftEdgeLeaf + 2, 0L, keySerializer.valueArrayFromArray(this.keys.toArray()), this.nodeValues());
                if (this.nextLeafLink == 0L) {
                    this.nextLeafLink = store.put(endLeaf, (Serializer) this.nodeSer);
                } else {
                    store.update(this.nextLeafLink, endLeaf, (Serializer) this.nodeSer);
                }

                if (this.leftEdgeLeaf != 0) {
                    this.setRootRecidRecid$Sources_of_mapdb_main(store.put(this.nextLeafLink, (Serializer) Serializer.RECID));
                } else if (((DirData) this.dirStack.getLast()).getLeftEdge() == 0) {
                    throw new AssertionError();
                } else {
                    long childFromLowerLevel = this.nextLeafLink;
                    Iterator var10000 = this.dirStack.iterator();

                    DirData dir;
                    for (Iterator var4 = var10000; var4.hasNext(); childFromLowerLevel = dir.getNextDirLink()) {
                        Object var7 = var4.next();
                        dir = (DirData) var7;
                        dir.getChild().add(childFromLowerLevel);
                        BTreeMapJava.Node dirNode = new BTreeMapJava.Node(dir.getLeftEdge() + 2 + 8, 0L, keySerializer.valueArrayFromArray(dir.getKeys().toArray()), dir.getChild().toArray());
                        if (dir.getNextDirLink() == 0L) {
                            dir.setNextDirLink(store.put(dirNode, (Serializer) this.nodeSer));
                        } else {
                            store.update(dir.getNextDirLink(), dirNode, (Serializer) this.nodeSer);
                        }
                    }

                    this.setRootRecidRecid$Sources_of_mapdb_main(store.put(childFromLowerLevel, (Serializer) Serializer.RECID));
                }
                return null;
            }
        };
    }

    private static final class DirData<K> {
        private int leftEdge = 4;
        private ArrayList<K> keys = new ArrayList<>();
        private LongArrayList child = new LongArrayList();
        private long nextDirLink;

        public DirData() {
        }

        public final int getLeftEdge() {
            return this.leftEdge;
        }

        public final void setLeftEdge(int var1) {
            this.leftEdge = var1;
        }

        public final ArrayList<K> getKeys() {
            return this.keys;
        }

        public final void setKeys(ArrayList<K> var1) {
            this.keys = var1;
        }

        public final LongArrayList getChild() {
            return this.child;
        }

        public final void setChild(LongArrayList var1) {
            this.child = var1;
        }

        public final long getNextDirLink() {
            return this.nextDirLink;
        }

        public final void setNextDirLink(long var1) {
            this.nextDirLink = var1;
        }
    }

    public abstract static class Sink<E, R> {
        @Nullable
        protected Long rootRecidRecid;
        protected long counter;

        @Nullable
        public final Long getRootRecidRecid$Sources_of_mapdb_main() {
            return this.rootRecidRecid;
        }

        public final void setRootRecidRecid$Sources_of_mapdb_main(@Nullable Long var1) {
            this.rootRecidRecid = var1;
        }

        public final long getCounter$Sources_of_mapdb_main() {
            return this.counter;
        }

        public final void setCounter$Sources_of_mapdb_main(long var1) {
            this.counter = var1;
        }

        public abstract void put(E obj);

        public abstract R create();

        public final void putAll(@NotNull Iterable<E> i) {
            this.putAll(i.iterator());
        }

        public final void putAll(@NotNull Iterator<E> i) {
            while (i.hasNext()) {
                this.put(i.next());
            }
        }
    }
}
