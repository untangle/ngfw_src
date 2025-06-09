/**
 * $Id: $
 */

package com.untangle.app.http;

/**
 * Custom exception for TLS_HANDSHAKE issue.
 * 
 */
@SuppressWarnings("serial")
public class TlsHandshakeException extends Exception {
    /**
     * Constructor
     */
    public TlsHandshakeException()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param message
     *        The exception message
     */
    public TlsHandshakeException(String message)
    {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message
     *        The exception message
     * @param cause
     *        The exception cause
     */
    public TlsHandshakeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructor
     * 
     * @param cause
     *        The exception cause
     */
    public TlsHandshakeException(Throwable cause)
    {
        super(cause);
    }
}