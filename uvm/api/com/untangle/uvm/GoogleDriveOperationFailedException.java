/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * Generic exception class representing failed google drive operations
 */
@SuppressWarnings("serial")
public class GoogleDriveOperationFailedException extends Exception implements Serializable {
    private int exitCode;

    /**
     * Parameterized constructor with message. Exit code is set to 99 as default
     * @param message
     */
    public GoogleDriveOperationFailedException(String message) {
        super(message);
        this.exitCode = 99;
    }

    /**
     * Parameterized constructor with message and the cause. Exit code is set to 99 as default
     * @param message
     * @param cause
     */
    public GoogleDriveOperationFailedException(String message, Throwable cause) {
        super(message, cause);
        this.exitCode = 99;
    }

    /**
     * Parameterized constructor with exit code, message and the cause
     * @param exitCode
     * @param message
     * @param cause
     */
    public GoogleDriveOperationFailedException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    /**
     * Returns the exit code
     * @return
     */
    public int getExitCode() {
        return exitCode;
    }

}
