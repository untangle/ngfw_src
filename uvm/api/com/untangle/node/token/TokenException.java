/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Signals a problem in a <code>TokenHandler</code>.
 *
 */
@SuppressWarnings("serial")
public class TokenException extends Exception
{
    public TokenException()
    {
        super();
    }

    public TokenException(String message)
    {
        super(message);
    }

    public TokenException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TokenException(Throwable cause)
    {
        super(cause);
    }
}
