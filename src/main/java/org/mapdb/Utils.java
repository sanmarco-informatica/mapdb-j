package org.mapdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import kotlin.jvm.internal.InlineMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Utils {
    public static final Logger LOG = Logger.getLogger("org.mapdb");

    private Utils() {
    }

    public static Logger getLOG() {
        return LOG;
    }

    @NotNull
    public static Path pathChangeSuffix(@NotNull Path path, @NotNull String suffix) {
        return (new File(path.toFile().getPath() + suffix)).toPath();
    }

    public static <T, E extends Throwable> T sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static void logDebug(@NotNull Supplier<String> msg) {
        if (getLOG().isLoggable(Level.FINE)) {
            getLOG().log(Level.FINE, msg.get());
        }
    }

    public static void logInfo(@NotNull Supplier<String> msg) {
        if (getLOG().isLoggable(Level.INFO)) {
            getLOG().log(Level.INFO, msg.get());
        }
    }

    public static <T> T lockWrite(@Nullable ReadWriteLock lock, @NotNull Supplier<T> f) {
        if (lock != null) {
            lock.writeLock().lock();
        }

        T var4;
        try {
            var4 = f.get();
        } finally {
            InlineMarker.finallyStart(1);
            if (lock != null) {
                lock.writeLock().unlock();
            }

            InlineMarker.finallyEnd(1);
        }

        return var4;
    }

    public static <T> T lockRead(@Nullable ReadWriteLock lock, @NotNull Supplier<T> f) {
        if (lock != null) {
            lock.readLock().lock();
        }

        T var4;
        try {
            var4 = f.get();
        } finally {
            InlineMarker.finallyStart(1);
            if (lock != null) {
                lock.readLock().unlock();
            }

            InlineMarker.finallyEnd(1);
        }

        return var4;
    }

    public static void assertReadLock(@Nullable ReadWriteLock lock) {
        if (lock instanceof ReentrantReadWriteLock && ((ReentrantReadWriteLock)lock).getReadLockCount() == 0 && !((ReentrantReadWriteLock)lock).isWriteLockedByCurrentThread()) {
            throw new AssertionError("not read locked");
        } else if (lock instanceof SingleEntryReadWriteLock && ((SingleEntryReadWriteLock)lock).getLock().getReadLockCount() == 0 && !((SingleEntryReadWriteLock)lock).getLock().isWriteLockedByCurrentThread()) {
            throw new AssertionError("not read locked");
        }
    }

    public static void assertWriteLock(@Nullable ReadWriteLock lock) {
        if (lock instanceof ReentrantReadWriteLock && !((ReentrantReadWriteLock)lock).isWriteLockedByCurrentThread()) {
            throw new AssertionError("not write locked");
        } else if (lock instanceof SingleEntryReadWriteLock && !((SingleEntryReadWriteLock)lock).getLock().isWriteLockedByCurrentThread()) {
            throw new AssertionError("not write locked");
        }
    }

    public static <T> T lock(@Nullable Lock lock, @NotNull Supplier<T> body) {
        Intrinsics.checkNotNullParameter(body, "body");
        if (lock != null) {
            lock.lock();
        }

        T var4;
        try {
            var4 = body.get();
        } finally {
            InlineMarker.finallyStart(1);
            if (lock != null) {
                lock.unlock();
            }

            InlineMarker.finallyEnd(1);
        }

        return var4;
    }

    public static int roundDownToIntMAXVAL(long size) {
        return size > 2147483647L ? Integer.MAX_VALUE : (int) size;
    }

    @NotNull
    public static Lock singleEntryLock() {
        final ReentrantLock lock = new ReentrantLock();
        return new Lock() {
            // $FF: synthetic field
            private final ReentrantLock $$delegate_0 = lock;

            private void ensureNotLocked() {
                if (lock.isHeldByCurrentThread()) {
                    throw new IllegalMonitorStateException("already locked by current thread");
                }
            }

            public void lock() {
                this.ensureNotLocked();
                lock.lock();
            }

            public void lockInterruptibly() throws InterruptedException {
                this.ensureNotLocked();
                lock.lockInterruptibly();
            }

            public boolean tryLock() {
                return this.$$delegate_0.tryLock();
            }

            public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
                return this.$$delegate_0.tryLock(time, unit);
            }

            public void unlock() {
                this.$$delegate_0.unlock();
            }

            @NotNull
            public Condition newCondition() {
                return this.$$delegate_0.newCondition();
            }
        };
    }

    @Nullable
    public static Lock newLock(boolean threadSafe) {
        return threadSafe ? new ReentrantLock() : null;
    }

    @Nullable
    public static ReadWriteLock newReadWriteLock(boolean threadSafe) {
        return threadSafe ? new ReentrantReadWriteLock() : null;
    }

    public static void assertLocked(@Nullable Lock lock) {
        if (lock instanceof ReentrantLock && !((ReentrantLock)lock).isHeldByCurrentThread() || lock instanceof SingleEntryLock && !((SingleEntryLock)lock).getLock().isHeldByCurrentThread()) {
            throw new AssertionError("Not locked");
        }
    }

    public static <T> T clone(T value, @NotNull Serializer<T> serializer, @NotNull DataOutput2 out) throws IOException {
        out.pos = 0;
        serializer.serialize(out, value);
        DataInput2.ByteArray in2 = new DataInput2.ByteArray(out.copyBytes());
        return serializer.deserialize(in2, out.pos);
    }

    public static <T> T clone(T value, @NotNull Serializer<T> serializer) throws IOException {
        return clone(value, serializer, new DataOutput2());
    }

    public static void lockReadAll(@NotNull ReadWriteLock[] locks) throws InterruptedException {
        int i = 0;

        while(i < locks.length) {
            int var10001 = i++;
            ReadWriteLock var10000 = locks[var10001];
            if (locks[var10001] != null) {
                ReadWriteLock lock = var10000;
                if (!lock.readLock().tryLock()) {
                    --i;

                    while(i > 0) {
                        --i;
                        var10000 = locks[i];
                        if (locks[i] != null) {
                            var10000.readLock().unlock();
                        }
                    }

                    Thread.sleep(0L, 100000);
                    i = 0;
                }
            }
        }

    }

    public static void unlockReadAll(@NotNull ReadWriteLock[] locks) {
        for(int i = locks.length - 1; -1 < i; --i) {
            ReadWriteLock var10000 = locks[i];
            Intrinsics.checkNotNull(locks[i]);
            var10000.readLock().unlock();
        }

    }

    public static void lockWriteAll(@NotNull ReadWriteLock[] locks) throws InterruptedException {
        Intrinsics.checkNotNullParameter(locks, "locks");
        int i = 0;

        while(i < locks.length) {
            int var10001 = i++;
            ReadWriteLock var10000 = locks[var10001];
            if (locks[var10001] != null) {
                ReadWriteLock lock = var10000;
                if (!lock.writeLock().tryLock()) {
                    --i;

                    while(i > 0) {
                        --i;
                        var10000 = locks[i];
                        if (locks[i] != null) {
                            var10000.writeLock().unlock();
                        }
                    }

                    Thread.sleep(0L, 100000);
                    i = 0;
                }
            }
        }

    }

    public static void unlockWriteAll(@NotNull ReadWriteLock[] locks) {
        for(int i = locks.length - 1; -1 < i; --i) {
            ReadWriteLock lock = locks[i];
            if (lock != null) {
                lock.writeLock().unlock();
            }
        }

    }

    public static int identityCount(@NotNull Object[] vals) {
        IdentityHashMap<Object, String> a = new IdentityHashMap<>();

        for (Object v : vals) {
            a.put(v, "");
        }

        return a.size();
    }

    public static final class SingleProtectionLock implements Lock {
        @NotNull
        private final String name;
        private volatile boolean locked;

        public SingleProtectionLock(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        public boolean getLocked() {
            return this.locked;
        }

        public void setLocked(boolean var1) {
            this.locked = var1;
        }

        public void lockInterruptibly() {
            this.lock();
        }

        @NotNull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public boolean tryLock() {
            this.lock();
            return true;
        }

        public boolean tryLock(long time, @NotNull TimeUnit unit) {
            Intrinsics.checkNotNullParameter(unit, "unit");
            this.lock();
            return true;
        }

        public void unlock() {
            if (!this.locked) {
                throw new IllegalAccessError(this.name + ": Not locked");
            } else {
                this.locked = false;
            }
        }

        public void lock() {
            if (!this.locked) {
                throw new IllegalAccessError(this.name + ": Already locked");
            } else {
                this.locked = true;
            }
        }
    }

    public static final class SingleEntryReadWriteLock implements ReadWriteLock {
        @NotNull
        private final ReentrantReadWriteLock lock;
        private final ReentrantReadWriteLock.WriteLock origWriteLock;
        @NotNull
        private final Lock newWriteLock;

        public SingleEntryReadWriteLock(@NotNull ReentrantReadWriteLock lock) {
            this.lock = lock;
            this.origWriteLock = this.lock.writeLock();
            this.newWriteLock = new Lock() {
                // $FF: synthetic field
                private final ReentrantReadWriteLock.WriteLock $$delegate_0 = SingleEntryReadWriteLock.this.getOrigWriteLock();

                private void ensureNotLocked() {
                    if (SingleEntryReadWriteLock.this.getLock().isWriteLockedByCurrentThread()) {
                        throw new IllegalMonitorStateException("already locked by current thread");
                    }
                }

                public void lock() {
                    this.ensureNotLocked();
                    SingleEntryReadWriteLock.this.getOrigWriteLock().lock();
                }

                public void lockInterruptibly() throws InterruptedException {
                    this.ensureNotLocked();
                    SingleEntryReadWriteLock.this.getOrigWriteLock().lockInterruptibly();
                }

                public boolean tryLock() {
                    return this.$$delegate_0.tryLock();
                }

                public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
                    return this.$$delegate_0.tryLock(time, unit);
                }

                public void unlock() {
                    this.$$delegate_0.unlock();
                }

                @NotNull
                public Condition newCondition() {
                    return this.$$delegate_0.newCondition();
                }
            };
        }

        // $FF: synthetic method
        public SingleEntryReadWriteLock() {
            this(new ReentrantReadWriteLock());
        }

        @NotNull
        public ReentrantReadWriteLock getLock() {
            return this.lock;
        }

        public ReentrantReadWriteLock.WriteLock getOrigWriteLock() {
            return this.origWriteLock;
        }

        @NotNull
        public Lock getNewWriteLock() {
            return this.newWriteLock;
        }

        @NotNull
        public Lock writeLock() {
            return this.newWriteLock;
        }

        @NotNull
        public Lock readLock() {
            return this.lock.readLock();
        }
    }

    public static final class SingleEntryLock implements Lock {
        @NotNull
        private final ReentrantLock lock;

        public SingleEntryLock(@NotNull ReentrantLock lock) {
            this.lock = lock;
        }

        // $FF: synthetic method
        public SingleEntryLock() {
            this(new ReentrantLock());
        }

        @NotNull
        public ReentrantLock getLock() {
            return this.lock;
        }

        public void lock() {
            if (this.lock.isHeldByCurrentThread()) {
                throw new IllegalMonitorStateException("already locked by current thread");
            } else {
                this.lock.lock();
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            if (this.lock.isHeldByCurrentThread()) {
                throw new IllegalMonitorStateException("already locked by current thread");
            } else {
                this.lock.lockInterruptibly();
            }
        }

        public boolean tryLock() {
            return this.lock.tryLock();
        }

        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            return this.lock.tryLock(time, unit);
        }

        public void unlock() {
            this.lock.unlock();
        }

        @NotNull
        public Condition newCondition() {
            return this.lock.newCondition();
        }
    }
}
