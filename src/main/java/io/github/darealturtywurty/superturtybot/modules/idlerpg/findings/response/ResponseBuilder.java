package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
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
        final Consumer<MessageReceivedEvent> action;
        
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
        public IfElseBuilder elseThen(Consumer<MessageReceivedEvent> action) {
            addOperation(new ElseOperation());
            addOperation(new ActionOperation(action));
            return this;
        }
        
        public ResponseBuilder end() {
            return ResponseBuilder.this;
        }

        public IfElseBuilder then(Consumer<MessageReceivedEvent> action) {
            addOperation(new ActionOperation(action));
            return this;
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
}
