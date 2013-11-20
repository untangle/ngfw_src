/**
 * $Id$
 */
package com.untangle.node.token.header;

@SuppressWarnings("serial")
public class IllegalFieldException extends Exception
{
    public IllegalFieldException(String message)
    {
        super(message);
    }

    public IllegalFieldException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
