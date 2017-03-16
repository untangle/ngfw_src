/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.io.Serializable;

/**
 * Generic "something went wrong" exception. <b>Not</b> the fault of the user or the data - the back-end is simply
 * hosed.
 */
@SuppressWarnings("serial")
public class SafelistActionFailedException extends Exception implements Serializable
{

    public SafelistActionFailedException() {
    }

    public SafelistActionFailedException(String msg) {
        super(msg);
    }

    public SafelistActionFailedException(Throwable cause) {
        super(cause);
    }

    public SafelistActionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
