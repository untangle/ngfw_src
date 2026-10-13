/**
 * Upload handling APIs and related servlet types.
 */
package com.untangle.uvm.servlet;

/**
 * Signals that an upload handler is temporarily unavailable.
 */
public class UploadUnavailableException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public UploadUnavailableException(String message)
    {
        super(message);
    }
}
