package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RenterOffer {
    private String name;
    private BigInteger offer;
}
