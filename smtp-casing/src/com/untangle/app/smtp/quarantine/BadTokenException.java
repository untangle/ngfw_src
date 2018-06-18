/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;

/**
 * Exception thrown when a Token (a sequence of characters used to have obscured information passed back in a URL) is of
 * a bad format (garbage).
 */
@SuppressWarnings("serial")
public class BadTokenException extends Exception implements Serializable
{
    /**
     * Initialize instnace of BadTokenException.
     * @param  token String to display.
     * @return       Instance of BadTokenException.
     */
    public BadTokenException(String token) {
        super("Bad Token \"" + token + "\"");
    }
}
