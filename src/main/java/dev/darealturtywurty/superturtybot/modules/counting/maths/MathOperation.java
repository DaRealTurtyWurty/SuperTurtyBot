package dev.darealturtywurty.superturtybot.modules.counting.maths;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

import static dev.darealturtywurty.superturtybot.modules.counting.maths.OperationType.FLOAT;
import static dev.darealturtywurty.superturtybot.modules.counting.maths.OperationType.INT;

@Getter
public enum MathOperation {
    ADD("%.1f + %.1f", INT), SUBTRACT("%.1f - %.1f", INT), MULTIPLY("%.1f * %.1f", INT), DIVIDE("%.1f / %.1f", INT),
    MODULO("%.1f %% %.1f", INT), SQUARE("%.1f ^ 2", INT), SQRT("√%.1f", INT), FLOOR("floor(%.1f)", FLOAT), ROUND("round(%.1f)", FLOAT),
    CEIL("ceil(%.1f)", FLOAT);
    
    private final String format;
    private final OperationType operationType;
    
    MathOperation(String format, OperationType opType) {
        this.format = format;
        this.operationType = opType;
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
