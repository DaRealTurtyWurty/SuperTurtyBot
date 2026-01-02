package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rent {
    private BigInteger baseRent;
    private List<BigInteger> previousRents;
    private BigInteger currentRent;
    private boolean isPaused;

    public Rent(BigInteger baseRent) {
        this.baseRent = baseRent;
        this.previousRents = new ArrayList<>();
        this.currentRent = baseRent;
    }

    public void updateRent(BigInteger amount) {
        this.previousRents.add(this.currentRent);
        this.currentRent = amount;
    }

    public void resetRent() {
        this.currentRent = this.baseRent;
    }

    public void pauseRent() {
        this.isPaused = true;

        if (this.currentRent.signum() != 0) {
            this.previousRents.add(this.currentRent);
            this.currentRent = BigInteger.ZERO;
        }
    }

    public void resumeRent() {
        this.isPaused = false;

        if (!this.previousRents.isEmpty()) {
            this.currentRent = this.previousRents.getLast();
            this.previousRents.removeLast();
        } else {
            this.currentRent = this.baseRent;
        }
    }
}
