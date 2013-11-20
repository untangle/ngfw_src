/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Signals a problem parsing the stream.
 *
 */
@SuppressWarnings("serial")
public class ParseException extends Exception
{
    public ParseException()
    {
        super();
    }

    public ParseException(String message)
    {
        super(message);
    }

    public ParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ParseException(Throwable cause)
    {
        super(cause);
    }
}
