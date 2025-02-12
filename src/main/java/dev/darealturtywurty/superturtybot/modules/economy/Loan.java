package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Loan {
    private String id;
    private BigInteger amount;
    private BigDecimal interestRate;
    private long timeTaken;
    private long timeToPay;
    private final List<Payment> payments = new ArrayList<>();

    private BigInteger amountPaid = BigInteger.ZERO;
    private boolean paidOff = false;

    public Loan(String id, BigInteger amount, BigDecimal interestRate, long timeTaken, long timeToPay) {
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
    public BigInteger pay(BigInteger amount) {
        if (this.paidOff) return amount;

        BigInteger remaining = calculateAmountLeftToPay();
        BigInteger toPay = amount.min(remaining);
        this.amountPaid = this.amountPaid.add(toPay);

        this.payments.add(new Payment(System.currentTimeMillis(), toPay));
        if (this.amountPaid.compareTo(calculateTotalAmountToPay()) >= 0) {
            this.paidOff = true;
        }

        return amount.subtract(toPay);
    }

    /**
     * Calculates the total amount to pay back
     *
     * @return The total amount to pay back
     */
    public BigInteger calculateTotalAmountToPay() {
        BigInteger totalAmountToPay = this.amount;
        if (System.currentTimeMillis() > this.timeToPay) {
            totalAmountToPay = totalAmountToPay.add(new BigDecimal(this.amount).multiply(this.interestRate).toBigInteger());
        }

        return totalAmountToPay;
    }

    /**
     * Calculates the amount left to pay
     *
     * @return The amount left to pay
     */
    public BigInteger calculateAmountLeftToPay() {
        BigInteger totalAmountToPay = calculateTotalAmountToPay();
        return totalAmountToPay.subtract(this.amountPaid);
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
