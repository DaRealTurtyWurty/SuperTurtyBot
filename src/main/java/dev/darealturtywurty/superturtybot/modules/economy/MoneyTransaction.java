package dev.darealturtywurty.superturtybot.modules.economy;

import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public record MoneyTransaction(long timestamp, BigInteger amount, byte type, @Nullable Long targetId) {
    public static final byte DEPOSIT = 0;
    public static final byte WITHDRAW = 1;
    public static final byte DONATE = 2;
    public static final byte REWARD = 3;
    public static final byte TAX = 4;
    public static final byte WORK = 5;
    public static final byte JOB = 6;
    public static final byte SLOTS = 7;
    public static final byte CRASH = 8;
    public static final byte CRIME = 9;
    public static final byte ROB = 10;
    public static final byte HEIST = 11;
    public static final byte LOAN = 12;
    public static final byte PAY_LOAN = 13;
    public static final byte WARNING = 14;
    public static final byte SET_MONEY = 15;
    public static final byte CREATE_ACCOUNT = 16;
    public static final byte HEIST_SETUP = 17;

    public static String getTypeName(byte typeId) {
        return switch (typeId) {
            case DEPOSIT -> "Deposit";
            case WITHDRAW -> "Withdraw";
            case DONATE -> "Donate";
            case REWARD -> "Reward";
            case TAX -> "Tax";
            case WORK -> "Work";
            case JOB -> "Job";
            case SLOTS -> "Slots";
            case CRASH -> "Crash";
            case CRIME -> "Crime";
            case ROB -> "Rob";
            case HEIST -> "Heist";
            case LOAN -> "Loan";
            case PAY_LOAN -> "Pay Loan";
            case WARNING -> "Warning";
            case SET_MONEY -> "Set Money";
            case CREATE_ACCOUNT -> "Create Account";
            case HEIST_SETUP -> "Heist Setup";
            default -> "Unknown";
        };
    }
}
