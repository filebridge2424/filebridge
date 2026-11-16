package com.filebridge.exception;

public class ShareAccessDeniedException extends RuntimeException {
    public ShareAccessDeniedException(String message) {
        super(message);
    }
}
