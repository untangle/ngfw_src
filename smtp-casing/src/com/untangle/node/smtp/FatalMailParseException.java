/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.io.Serializable;

/**
 * Exception thrown when a Token (a sequence of characters used to have obscured information passed back in a URL) is of
 * a bad format (garbage).
 */
@SuppressWarnings("serial")
public class FatalMailParseException extends Exception implements Serializable
{
    public FatalMailParseException(String message, Exception cause) {
        super(message, cause);
    }
    
    public FatalMailParseException(String message) {
        super(message);
    }
}
