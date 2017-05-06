package com.mobiquityinc.exception;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class APIException extends Exception {
    private String line;
    private long lineNumber;

    public APIException() {
    }

    public APIException(Throwable cause, String line, long lineNumber) {
        super(cause);
        this.line = line;
        this.lineNumber = lineNumber;
    }

    public APIException(String message, String line, long lineNumber) {
        super(message);
        this.line = line;
        this.lineNumber = lineNumber;
    }

    public APIException(String message, Throwable cause, String line, long lineNumber) {
        super(message, cause);
        this.line = line;
        this.lineNumber = lineNumber;
    }

    public APIException(String message) {
        super(message);
    }

    public APIException(String message, Throwable cause) {
        super(message, cause);
    }

    public APIException(Throwable cause) {
        super(cause);
    }

    public APIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
