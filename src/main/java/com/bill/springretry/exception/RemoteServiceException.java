package com.bill.springretry.exception;

/**
 * @author Bill.Lin 2025/4/13
 */
public class RemoteServiceException extends RuntimeException {
    
    private final int statusCode;
    
    public RemoteServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
