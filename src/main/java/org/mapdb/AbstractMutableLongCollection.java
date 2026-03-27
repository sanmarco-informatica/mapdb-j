// AbstractMutableLongCollection.java
package org.mapdb;

import java.util.Arrays;

import kotlin.jvm.internal.Ref;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.primitive.AbstractLongIterable;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.utility.internal.primitive.LongIterableIterate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMutableLongCollection extends AbstractLongIterable implements MutableLongCollection {
    public boolean allSatisfy(@NotNull LongPredicate predicate) {
        MutableLongIterator iter = (MutableLongIterator) this.longIterator();

        while (iter.hasNext()) {
            if (!predicate.accept(iter.next())) {
                return false;
            }
        }

        return true;
    }

    public void appendString(@Nullable Appendable appendable, @Nullable String start, @Nullable String separator, @Nullable String end) {
        LongIterableIterate.appendString((LongIterable) this, appendable, start, separator, end);
    }

    @Nullable
    public long[] toArray() {
        Ref.ObjectRef<long[]> ret = new Ref.ObjectRef<>();
        ret.element = new long[32];
        Ref.IntRef pos = new Ref.IntRef();
        this.forEach(k -> toArray$lambda$0(ret, pos, k));
        if (pos.element != ((long[]) ret.element).length) {
            ret.element = Arrays.copyOf((long[]) ret.element, pos.element);
        }

        return (long[]) ret.element;
    }

    public long sum() {
        Ref.LongRef ret = new Ref.LongRef();
        this.forEach(k -> sum$lambda$0(ret, k));
        return ret.element;
    }

    public boolean noneSatisfy(@Nullable LongPredicate predicate) {
        return LongIterableIterate.noneSatisfy((LongIterable) this, predicate);
    }

    public <T> T injectInto(T injectedValue, @NotNull ObjectLongToObjectFunction<? super T, ? extends T> function) {
        return LongIterableIterate.injectInto((LongIterable) this, injectedValue, function);
    }

    public void each(@NotNull LongProcedure procedure) {
        this.forEach(procedure);
    }

    public long detectIfNone(@Nullable LongPredicate predicate, long ifNone) {
        return LongIterableIterate.detectIfNone((LongIterable) this, predicate, ifNone);
    }

    public int count(@Nullable LongPredicate predicate) {
        return LongIterableIterate.count((LongIterable) this, predicate);
    }

    public boolean anySatisfy(@Nullable LongPredicate predicate) {
        return LongIterableIterate.anySatisfy((LongIterable) this, predicate);
    }

    @NotNull
    public <T> MutableSet<T> collect(@NotNull LongToObjectFunction<? extends T> function) {
        MutableSet<T> result = Sets.mutable.with();
        this.forEach(k -> collect$lambda$0(result, function, k));
        return result;
    }

    @NotNull
    public MutableLongSet reject(@NotNull LongPredicate predicate) {
        LongHashSet ret = new LongHashSet();
        this.forEach(k -> reject$lambda$0(ret, predicate, k));
        return (MutableLongSet) ret;
    }

    @NotNull
    public MutableLongSet select(@NotNull LongPredicate predicate) {
        LongHashSet ret = new LongHashSet();
        this.forEach(k -> select$lambda$0(ret, predicate, k));
        return (MutableLongSet) ret;
    }

    public boolean add(long element) {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    public boolean addAll(@NotNull long... source) {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    public boolean addAll(@NotNull LongIterable source) {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @NotNull
    public MutableLongSet with(long element) {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @NotNull
    public MutableLongSet without(long element) {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @NotNull
    public MutableLongSet withAll(@NotNull LongIterable elements) {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @NotNull
    public MutableLongSet withoutAll(@NotNull LongIterable elements) {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof LongSet)) {
            return false;
        } else {
            return this.size() == ((LongSet) obj).size() && this.containsAll((LongIterable) obj);
        }
    }

    public int hashCode() {
        Ref.IntRef ret = new Ref.IntRef();
        this.forEach(k -> hashCode$lambda$0(ret, k));
        return ret.element;
    }

    private void toArray$lambda$0(Ref.ObjectRef<long[]> $ret, Ref.IntRef $pos, long k) {
        if (((long[]) $ret.element).length == $pos.element) {
            $ret.element = Arrays.copyOf((long[]) $ret.element, ((long[]) $ret.element).length * 2);
        }

        long[] var10000 = (long[]) $ret.element;
        int var4 = $pos.element++;
        var10000[var4] = k;
    }

    private void sum$lambda$0(Ref.LongRef $ret, long it) {
        $ret.element += it;
    }

    private <T> void collect$lambda$0(MutableSet<T> $result, LongToObjectFunction<? extends T> $function, long e) {
        $result.add($function.valueOf(e));
    }

    private void reject$lambda$0(LongHashSet $ret, LongPredicate $predicate, long r) {
        if (!$predicate.accept(r)) {
            $ret.add(r);
        }

    }

    private void select$lambda$0(LongHashSet $ret, LongPredicate $predicate, long r) {
        if ($predicate.accept(r)) {
            $ret.add(r);
        }

    }

    private static final void hashCode$lambda$0(Ref.IntRef $ret, long k) {
        $ret.element += DataIO.longHash(k);
    }
}
