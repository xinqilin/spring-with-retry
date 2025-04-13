package com.bill.springretry.exception;

/**
 * @author Bill.Lin 2025/4/13
 */
public class TransientNetworkException extends RuntimeException {
    public TransientNetworkException(String message) {
        super(message);
    }
}
