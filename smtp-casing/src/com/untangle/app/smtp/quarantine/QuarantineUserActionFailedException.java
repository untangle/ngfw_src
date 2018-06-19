/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;

/**
 * Generic "something went wrong" exception. <b>Not</b> the fault of the user or the data - the back-end is simply
 * hosed.
 */
@SuppressWarnings("serial")
public class QuarantineUserActionFailedException extends Exception implements Serializable
{
    /**
     * Initialize instance of QuarantineUserActionFailedException.
     * @return Instance of QuarantineUserActionFailedException.
     */
    public QuarantineUserActionFailedException() {
    }

    /**
     * Initialize instance of QuarantineUserActionFailedException.
     * @param msg String of message.
     * @return Instance of QuarantineUserActionFailedException.
     */
    public QuarantineUserActionFailedException(String msg) {
        super(msg);
    }

    /**
     * Initialize instance of QuarantineUserActionFailedException.
     * @param cause Throwable for cause.
     * @return Instance of QuarantineUserActionFailedException.
     */
    public QuarantineUserActionFailedException(Throwable cause) {
        super(cause);
    }

    /**
     * Initialize instance of QuarantineUserActionFailedException.
     * @param msg String of message.
     * @param cause Throwable for cause.
     * @return Instance of QuarantineUserActionFailedException.
     */
    public QuarantineUserActionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
