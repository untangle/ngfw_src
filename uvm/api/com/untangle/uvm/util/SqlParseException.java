/**
 * $Id$
 */
package com.untangle.uvm.util;

/**
 * Thrown when user-supplied SQL condition values fail parsing or validation.
 *
 * Used by {@link SqlUtil} to reject malformed or unsafe input
 * (invalid IN-list syntax, disallowed IS keywords, unrecognized tokens, etc.).
 */
@SuppressWarnings("serial")
public class SqlParseException extends RuntimeException
{
    public SqlParseException( String message )
    {
        super(message);
    }

    public SqlParseException( String message, Throwable cause )
    {
        super(message, cause);
    }
}
