package dev.darealturtywurty.superturtybot.modules.privacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class PrivacyCoordinator {
    private static final ConcurrentHashMap<Long, AccountLock> ACCOUNTS = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock BROAD_OPERATIONS = new ReentrantReadWriteLock(true);
    private static final LongAdder WAIT_NANOS = new LongAdder();
    private static final LongAdder OPERATIONS = new LongAdder();
    private static final ThreadLocal<Integer> SHARED_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> BROAD = ThreadLocal.withInitial(() -> false);

    private PrivacyCoordinator() {
    }

    public static <T> T shared(long user, Supplier<T> operation) {
        requireAccountAccess();
        AccountLock account = retain(user);
        Lock lock = account.lock.readLock();
        acquireShared(lock);
        try {
            return operation.get();
        } finally {
            lock.unlock();
            leaveShared();
            release(user);
        }
    }

    public static <T> T shared(Collection<Long> users, Supplier<T> operation) {
        requireAccountAccess();
        if (users.size() == 1)
            return shared(users.iterator().next(), operation);
        var acquired = new ArrayList<Long>();
        try {
            for (long user : users.stream().distinct().sorted().toList()) {
                AccountLock account = retain(user);
                acquireShared(account.lock.readLock());
                acquired.add(user);
            }
            return operation.get();
        } finally {
            for (int index = acquired.size() - 1; index >= 0; index--) {
                long user = acquired.get(index);
                ACCOUNTS.get(user).lock.readLock().unlock();
                leaveShared();
                release(user);
            }
        }
    }

    public static <T> T broad(Supplier<T> operation) {
        Lock lock = BROAD_OPERATIONS.readLock();
        acquire(lock);
        boolean previous = BROAD.get();
        BROAD.set(true);
        try {
            return operation.get();
        } finally {
            lock.unlock();
            if (previous) {
                BROAD.set(true);
            } else {
                BROAD.remove();
            }
        }
    }

    public static <T> T exclusive(long user, Supplier<T> operation) {
        requireAccountAccess();
        if (SHARED_DEPTH.get() > 0)
            throw new IllegalStateException("Cannot upgrade an optional operation to account deletion");
        AccountLock account = retain(user);
        Lock lock = account.lock.writeLock();
        acquire(lock);
        try {
            Lock broad = BROAD_OPERATIONS.writeLock();
            acquire(broad);
            try {
                SHARED_DEPTH.set(-1);
                return operation.get();
            } finally {
                SHARED_DEPTH.remove();
                broad.unlock();
            }
        } finally {
            lock.unlock();
            release(user);
        }
    }

    private static void requireAccountAccess() {
        if (SHARED_DEPTH.get() < 0)
            throw new IllegalStateException("Deletion operations must not acquire nested account locks");
        if (BROAD.get())
            throw new IllegalStateException(
                "Broad optional operations must use the raw delegate without account locks");
    }

    private static void acquireShared(Lock lock) {
        if (SHARED_DEPTH.get() > 0 && lock.tryLock()) {
            OPERATIONS.increment();
        } else {
            acquire(lock);
        }
        SHARED_DEPTH.set(SHARED_DEPTH.get() + 1);
    }

    private static void leaveShared() {
        int depth = SHARED_DEPTH.get() - 1;
        if (depth == 0) {
            SHARED_DEPTH.remove();
        } else {
            SHARED_DEPTH.set(depth);
        }
    }

    public static Timing drainTiming() {
        return new Timing(OPERATIONS.sumThenReset(), WAIT_NANOS.sumThenReset());
    }

    private static void acquire(Lock lock) {
        long start = System.nanoTime();
        lock.lock();
        WAIT_NANOS.add(System.nanoTime() - start);
        OPERATIONS.increment();
    }

    private static AccountLock retain(long user) {
        return ACCOUNTS.compute(user, (_, current) -> {
            AccountLock account = current == null ? new AccountLock() : current;
            account.references++;
            return account;
        });
    }

    private static void release(long user) {
        ACCOUNTS.computeIfPresent(user, (_, account) -> --account.references == 0 ? null : account);
    }

    public record Timing(long acquisitions, long waitNanos) {
    }

    private static final class AccountLock {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        private int references;
    }
}
