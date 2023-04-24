package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.Finding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ResponseBuilder {
    private final JDA jda;
    private final RPGPlayer player;
    private final long channel;
    
    private final List<Operation> operations = new ArrayList<>();

    private ResponseBuilder(JDA jda, RPGPlayer player, long channel) {
        this.jda = jda;
        this.player = player;
        this.channel = channel;
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
    
    public static ResponseBuilder start(JDA jda, RPGPlayer player, long channel) {
        return new ResponseBuilder(jda, player, channel);
    }
    
    public class ActionOperation implements Operation {
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

        public class EmbeddedResponseOperation implements Operation {
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
                then(finding.getResponse(ResponseBuilder.this.jda, ResponseBuilder.this.player,
                    ResponseBuilder.this.channel));

                return this;
            }

            public Inner startFinding(Runnable first, Finding finding) {
                then(ResponseBuilder
                    .start(ResponseBuilder.this.jda, ResponseBuilder.this.player, ResponseBuilder.this.channel)
                    .first(first));
                then(finding.getResponse(ResponseBuilder.this.jda, ResponseBuilder.this.player,
                    ResponseBuilder.this.channel));

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
                return;
            }

            if (ResponseBuilder.this.operations.get(index + 2) instanceof final ElseOperation operation) {
                operation.run(event, index + 2);
            }
        }
    }
    
    public class ScheduleOperation implements Operation {
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
