/**
 * $Id: FatalMailParseException.java,v 1.00 2013/03/16 21:59:12 dmorris Exp $
 */
package com.untangle.node.smtp;

import java.io.Serializable;

/**
 * Exception thrown when a Token (a sequence of characters
 * used to have obscured information passed back
 * in a URL) is of a bad format (garbage).
 */
@SuppressWarnings("serial")
public class FatalMailParseException extends Exception implements Serializable
{
    public FatalMailParseException(String message, Exception cause)
    {
        super(message, cause);
    }
}
