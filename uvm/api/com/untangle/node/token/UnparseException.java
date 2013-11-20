/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Signals a problem unparsing a token-stream.
 *
 */
@SuppressWarnings("serial")
public class UnparseException extends Exception
{
    public UnparseException()
    {
        super();
    }

    public UnparseException(String message)
    {
        super(message);
    }

    public UnparseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnparseException(Throwable cause)
    {
        super(cause);
    }
}
