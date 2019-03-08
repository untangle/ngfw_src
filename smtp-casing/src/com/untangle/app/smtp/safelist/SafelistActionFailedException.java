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

    /**
     * Action failed exception with no parameter.
     * 
     * @return Exception with default message.
     */
    public SafelistActionFailedException() {
    }

    /**
     * Action failed exception with message.
     * 
     * @param  msg Message to incldue with exception.
     * @return     Exception with message.
     */
    public SafelistActionFailedException(String msg) {
        super(msg);
    }

    /**
     * Action failed exception with throwable cause.
     * 
     * @param  cause Throwablw cause.
     * @return       Exception with default message.
     */
    public SafelistActionFailedException(Throwable cause) {
        super(cause);
    }

    /**
     * Action failed exception with messge abd throwable cause.
     *
     * @param msg Message to include with exception.
     * @param  cause Throwablw cause.
     * @return       Exception with included message.
     */
    public SafelistActionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
