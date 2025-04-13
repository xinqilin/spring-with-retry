package com.bill.springretry.exception;

/**
 * @author Bill.Lin 2025/4/13
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
