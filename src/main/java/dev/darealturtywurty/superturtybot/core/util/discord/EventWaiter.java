package dev.darealturtywurty.superturtybot.core.util.discord;

import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventWaiter extends ListenerAdapter {
    private final Map<Class<? extends GenericEvent>, Set<Waiter<?>>> waitingEvents = new HashMap<>();
    private final ScheduledExecutorService executorService;

    public EventWaiter(ScheduledExecutorService executorService) {
        Checks.notNull(executorService, "ExecutorService");
        this.executorService = executorService;
        ShutdownHooks.register(() -> {
            this.executorService.shutdown();
            this.waitingEvents.clear();
        });
    }

    public EventWaiter() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    public <T extends GenericEvent> Builder<T> builder(Class<T> eventClass) {
        return new Builder<>(eventClass);
    }

    public class Builder<T extends GenericEvent> {
        private final Class<T> eventClass;
        private long timeout = -1;
        private TimeUnit timeUnit = null;
        private Consumer<T> success = t -> {};
        private Predicate<T> condition = t -> true;
        private Runnable failure = () -> {};
        private Runnable timeoutAction = () -> {};

        public Builder(Class<T> eventClass) {
            Checks.notNull(eventClass, "Event class");
            this.eventClass = eventClass;
        }

        public Builder<T> timeout(long timeout, TimeUnit timeUnit) {
            Checks.positive(timeout, "Timeout");
            Checks.notNull(timeUnit, "Time unit");
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder<T> success(Consumer<T> success) {
            Checks.notNull(success, "Success action");
            this.success = success;
            return this;
        }

        public Builder<T> condition(Predicate<T> condition) {
            Checks.notNull(condition, "Condition");
            this.condition = condition;
            return this;
        }

        public Builder<T> failure(Runnable failure) {
            Checks.notNull(failure, "Failure action");
            this.failure = failure;
            return this;
        }

        public Builder<T> timeoutAction(Runnable timeoutAction) {
            Checks.notNull(timeoutAction, "Timeout action");
            this.timeoutAction = timeoutAction;
            return this;
        }

        public void build() {
            Set<Waiter<?>> waiters = EventWaiter.this.waitingEvents.computeIfAbsent(this.eventClass, k -> new HashSet<>());
            var waiter = new Waiter<>(this.eventClass, this.success, this.condition, this.failure,
                    this.timeoutAction, this.timeout, this.timeUnit);
            waiters.add(waiter);
            EventWaiter.this.waitingEvents.put(this.eventClass, waiters);

            if (waiter.timeout > 0 && waiter.timeUnit != null) {
                EventWaiter.this.executorService.schedule(() -> {
                    if (waiters.remove(waiter)) {
                        waiter.timeoutAction.run();
                    }
                }, waiter.timeout, waiter.timeUnit);
            }
        }
    }

    @ApiStatus.Internal
    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        Class<?> eventClass = event.getClass();
        while (eventClass != null) {
            if(this.waitingEvents.containsKey(eventClass)) {
                Set<Waiter<?>> waiters = this.waitingEvents.get(eventClass);
                Waiter<?>[] toRemove = waiters.toArray(new Waiter[0]);

                waiters.removeAll(Stream.of(toRemove).filter(waiter -> waiter.run(event)).collect(Collectors.toSet()));
            }

            eventClass = eventClass.getSuperclass();
        }
    }

    private record Waiter<T extends GenericEvent>(Class<T> eventClass, Consumer<T> success, Predicate<T> condition,
                                                  Runnable failure, Runnable timeoutAction, long timeout, TimeUnit timeUnit) {
        boolean run(@NotNull GenericEvent event) {
            if(eventClass.isInstance(event)) {
                try {
                    T castedEvent = eventClass.cast(event);
                    if(condition.test(castedEvent)) {
                        this.success.accept(castedEvent);
                        return true;
                    }
                } catch(Exception exception) {
                    this.failure.run();
                    Constants.LOGGER.error("An error occurred while running a waiter!", exception);
                    return false;
                }
            }

            return false;
        }
    }
}
