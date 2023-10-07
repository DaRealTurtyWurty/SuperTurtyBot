package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Loan {
    private String id;
    private int amount;
    private double interestRate;
    private long timeTaken;
    private long timeToPay;
    private final List<Payment> payments = new ArrayList<>();

    private int amountPaid = 0;
    private boolean paidOff = false;

    public Loan(String id, int amount, double interestRate, long timeTaken, long timeToPay) {
        this.id = id;
        this.amount = amount;
        this.interestRate = interestRate;
        this.timeTaken = timeTaken;
        this.timeToPay = timeToPay;
    }

    /**
     * Pays back the loan by the given amount
     *
     * @param amount The amount of money to request as a loan
     * @return The amount to give back to the user if it exceeds the amount they need to pay
     */
    public int pay(int amount) {
        if(this.paidOff) return amount;

        int remaining = calculateAmountLeftToPay();
        int toPay = Math.min(amount, remaining);
        this.amountPaid += toPay;

        this.payments.add(new Payment(toPay, System.currentTimeMillis()));
        if (this.amountPaid >= calculateTotalAmountToPay()) {
            this.paidOff = true;
        }

        return amount - toPay;
    }

    /**
     * Calculates the total amount to pay back
     *
     * @return The total amount to pay back
     */
    public int calculateTotalAmountToPay() {
        int totalAmountToPay = this.amount;
        if (System.currentTimeMillis() > this.timeToPay) {
            totalAmountToPay += (int) (this.amount * this.interestRate);
        }

        return totalAmountToPay;
    }

    /**
     * Calculates the amount left to pay
     *
     * @return The amount left to pay
     */
    public int calculateAmountLeftToPay() {
        int totalAmountToPay = calculateTotalAmountToPay();
        return totalAmountToPay - this.amountPaid;
    }

    /**
     * Gets the payments made
     *
     * @return The payments made as an immutable list
     */
    public List<Payment> getPayments() {
        return List.copyOf(this.payments);
    }
}
