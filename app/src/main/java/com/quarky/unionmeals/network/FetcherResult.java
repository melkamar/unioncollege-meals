package com.quarky.unionmeals.network;

/**
 * Created by Martin on 10. 1. 2015.
 */
public class FetcherResult {
    public static final int WRONG_LOGIN = -2;
    public static final int NO_CONNECTION = -3;
    public static final int IOEXCEPTION = -4;
    public static final int LOGIN_NOT_OK = -5;


    int swipes;
    double balance;
    String txt;

    public int getSwipes() {
        return swipes;
    }

    public double getBalance() {
        return balance;
    }

    public String getTxt() {
        return txt;
    }

    public FetcherResult(int swipes, double balance, String txt) {
        this.swipes = swipes;
        this.balance = balance;
        this.txt = txt;
    }

    public FetcherResult(int swipes, double balance) {
        this.swipes = swipes;
        this.balance = balance;
        this.txt = "";
    }
}
