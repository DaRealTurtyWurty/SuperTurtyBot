package dev.darealturtywurty.superturtybot.modules.rpg.explore.response;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ResponseBuilder extends ListenerAdapter {
    private final RPGPlayer player;
    private final JDA jda;
    private final long guild, channel;

    private final List<Operation> operations = new ArrayList<>();
    private int index = 0;

    private ResponseBuilder(RPGPlayer player, JDA jda, long guild, long channel) {
        this.player = player;
        this.jda = jda;
        this.guild = guild;
        this.channel = channel;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage() || event.getGuild().getIdLong() != this.guild || event.getChannel().getIdLong() != this.channel || event.getAuthor().getIdLong() != this.player.getUser())
            return;

        runOperation(event, this.index++);

        if(this.index >= this.operations.size() || this.operations.get(this.index - 1) instanceof ActionOperation) {
            this.jda.removeEventListener(this);
        }
    }

    public IfElseBuilder condition(Predicate<MessageReceivedEvent> condition) {
        addOperation(new IfOperation(condition));
        return new IfElseBuilder();
    }

    public ResponseBuilder first(Runnable action) {
        addOperation(new FirstOperation(action));
        return this;
    }

    public void run() {
        runOperation(null, 0);
    }

    public ResponseBuilder schedule(TimeUnit timeUnit, long delay, Runnable action) {
        addOperation(new ScheduleOperation(timeUnit, delay, action));
        return this;
    }

    public ResponseBuilder then(Consumer<MessageReceivedEvent> handler) {
        addOperation(new ActionOperation(handler));
        return this;
    }

    protected void addOperation(int index, Operation operation) {
        this.operations.add(index, operation);
    }

    protected void addOperation(Operation operation) {
        this.operations.add(operation);
    }

    protected void runOperation(MessageReceivedEvent event, int index) {
        this.operations.get(index).run(event, index);
    }

    public static ResponseBuilder start(RPGPlayer player, JDA jda, long guild, long channel) {
        return new ResponseBuilder(player, jda, guild, channel);
    }

    public static class ActionOperation implements Operation {
        protected final Consumer<MessageReceivedEvent> action;

        protected ActionOperation(Consumer<MessageReceivedEvent> action) {
            this.action = action;
        }

        @Override
        public void run(MessageReceivedEvent event, int index) {
            this.action.accept(event);
        }
    }

    public class ElseOperation implements Operation {
        @Override
        public void run(MessageReceivedEvent event, int index) {
            runOperation(event, index + 1);
            ResponseBuilder.this.index = index + 1;
        }
    }

    public class FirstOperation implements Operation {
        private final Runnable action;

        public FirstOperation(Runnable action) {
            this.action = action;
        }

        @Override
        public void run(MessageReceivedEvent event, int index) {
            if (index != 0)
                throw new IllegalStateException("First operation did not have index 0");
            this.action.run();
        }
    }

    public class IfElseBuilder {
        private int elseOffset = 0;

        public Inner ifTrue(Consumer<MessageReceivedEvent> action) {
            addOperation(new ActionOperation(action));
            return new Inner();
        }

        public static class EmbeddedResponseOperation implements Operation {
            private final ResponseBuilder responseBuilder;

            public EmbeddedResponseOperation(ResponseBuilder responseBuilder) {
                this.responseBuilder = responseBuilder;
            }

            @Override
            public void run(MessageReceivedEvent event, int index) {
                this.responseBuilder.run();
            }
        }

        public class Inner {
            public ResponseBuilder end() {
                return ResponseBuilder.this;
            }

            public ResponseBuilder ifFalse(Consumer<MessageReceivedEvent> action) {
                addOperation(new ElseOperation());
                addOperation(new ActionOperation(action));
                return end();
            }

            public Inner startFinding(Finding finding) {
                then(finding.getResponse(ResponseBuilder.this.player, ResponseBuilder.this.jda,
                        ResponseBuilder.this.guild, ResponseBuilder.this.channel));

                return this;
            }

            public Inner startFinding(Runnable first, Finding finding) {
                then(ResponseBuilder
                        .start(ResponseBuilder.this.player, ResponseBuilder.this.jda, ResponseBuilder.this.guild,
                                ResponseBuilder.this.channel)
                        .first(first));
                then(finding.getResponse(ResponseBuilder.this.player, ResponseBuilder.this.jda,
                        ResponseBuilder.this.guild, ResponseBuilder.this.channel));

                return this;
            }

            public Inner then(ResponseBuilder responseBuilder) {
                addOperation(new EmbeddedResponseOperation(responseBuilder));
                IfElseBuilder.this.elseOffset++;
                return this;
            }
        }
    }

    public class IfOperation implements Operation {
        private final Predicate<MessageReceivedEvent> condition;

        public IfOperation(Predicate<MessageReceivedEvent> conditional) {
            this.condition = conditional;
        }

        @Override
        public void run(MessageReceivedEvent event, int index) {
            if (this.condition.test(event)) {
                runOperation(event, index + 1);
                ResponseBuilder.this.index = index + 1;
                return;
            }

            if (ResponseBuilder.this.operations.get(index + 2) instanceof final ElseOperation operation) {
                operation.run(event, index + 2);
                ResponseBuilder.this.index = index + 2;
            }
        }
    }

    public static class ScheduleOperation implements Operation {
        private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(50);
        private final TimeUnit timeUnit;
        private final long delay;
        private final Runnable action;

        public ScheduleOperation(TimeUnit timeUnit, long delay, Runnable action) {
            this.timeUnit = timeUnit;
            this.delay = delay;
            this.action = action;
        }

        @Override
        public void run(MessageReceivedEvent event, int index) {
            EXECUTOR.schedule(this.action, this.delay, this.timeUnit);
        }
    }
}