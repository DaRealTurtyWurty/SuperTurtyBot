package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.registry.Registry;

public class FindingRegistry {
    public static final Registry<Finding> FINDINGS = new Registry<>();
    
    public static final Finding NOTHING = FINDINGS.register("nothing", new BlankFinding());
    public static final Finding CAVE = FINDINGS.register("cave", new CaveFinding());
    public static final Finding DUNGEON = FINDINGS.register("dungeon", new DungeonFinding());
    public static final Finding CHEST = FINDINGS.register("chest", new ChestFinding());
    public static final Finding VILLAGE = FINDINGS.register("village", new VillageFinding());
    public static final Finding POND = FINDINGS.register("pond", new PondFinding());
    public static final Finding TRAP = FINDINGS.register("trap", new TrapFinding());
    public static final Finding ENEMY = FINDINGS.register("enemy", new EnemyFinding());
    public static final Finding FOOD = FINDINGS.register("food", new FoodFinding());

    public static final Finding CAVE_OUTCOME = FINDINGS.register("cave_outcome", new CaveFinding.CaveOutcomeFinding());
}
