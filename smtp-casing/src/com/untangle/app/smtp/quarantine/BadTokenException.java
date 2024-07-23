/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Exception thrown when a Token (a sequence of characters used to have obscured information passed back in a URL) is of
 * a bad format (garbage).
 */
@SuppressWarnings("serial")
@ValidSerializable
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
