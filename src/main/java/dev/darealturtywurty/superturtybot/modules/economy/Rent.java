package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rent {
    private int baseRent;
    private List<Integer> previousRents;
    private int currentRent;
    private boolean isPaused;

    public Rent(int baseRent) {
        this.baseRent = baseRent;
        this.previousRents = new ArrayList<>();
        this.currentRent = baseRent;
    }

    public void setRent(int amount) {
        this.previousRents.add(this.currentRent);
        this.currentRent = amount;
    }

    public void resetRent() {
        this.currentRent = this.baseRent;
    }

    public void pauseRent() {
        this.isPaused = true;

        if (this.currentRent != 0) {
            this.previousRents.add(this.currentRent);
            this.currentRent = 0;
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
