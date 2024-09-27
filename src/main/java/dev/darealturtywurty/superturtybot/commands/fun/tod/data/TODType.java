package dev.darealturtywurty.superturtybot.commands.fun.tod.data;

public enum TODType {
    TRUTH, DARE, RANDOM;

    public TODResult pickRandom(TODPack pack) {
        return new TODResult(
                switch (this) {
                    case TRUTH -> pack.getTruths().get((int) (Math.random() * pack.getTruths().size()));
                    case DARE -> pack.getDares().get((int) (Math.random() * pack.getDares().size()));
                    case RANDOM ->
                            Math.random() < 0.5 ? pack.getTruths().get((int) (Math.random() * pack.getTruths().size()))
                                    : pack.getDares().get((int) (Math.random() * pack.getDares().size()));
                },
                this);
    }
}
