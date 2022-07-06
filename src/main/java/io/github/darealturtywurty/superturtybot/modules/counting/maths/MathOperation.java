package io.github.darealturtywurty.superturtybot.modules.counting.maths;

import static io.github.darealturtywurty.superturtybot.modules.counting.maths.OperationType.FLOAT;
import static io.github.darealturtywurty.superturtybot.modules.counting.maths.OperationType.INT;

import java.util.Arrays;
import java.util.List;

public enum MathOperation {
    ADD("%s + %s", INT), SUBTRACT("%s - %s", INT), MULTIPLY("%s * %s", INT), DIVIDE("%s / %s", INT),
    MODULO("%s %% %s", INT), SQUARE("%s ^ 2", INT), SQRT("√%s", INT), FLOOR("⌊%s⌋", FLOAT), ROUND("[%s]", FLOAT),
    CEIL("⌈%s⌉", FLOAT), SINE("sin(%s)", INT), COSINE("cos(%s)", INT), TANGENT("tan(%s)", INT), SECANT("sec(%s)", INT),
    COSECANT("csc(%s)", INT), COTANGENT("cot(%s)", INT);
    
    private final String format;
    private final OperationType operationType;
    
    MathOperation(String format, OperationType opType) {
        this.format = format;
        this.operationType = opType;
    }
    
    public String getFormat() {
        return this.format;
    }
    
    public OperationType getOperationType() {
        return this.operationType;
    }
    
    public static List<MathOperation> getFloats() {
        return getOfType(OperationType.FLOAT);
    }
    
    public static List<MathOperation> getInts() {
        return getOfType(OperationType.INT);
    }
    
    private static List<MathOperation> getOfType(OperationType type) {
        return Arrays.stream(MathOperation.values()).filter(op -> op.getOperationType() == type).toList();
    }
}
