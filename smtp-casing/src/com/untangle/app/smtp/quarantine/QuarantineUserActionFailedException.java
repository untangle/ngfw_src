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
    public QuarantineUserActionFailedException() {
    }

    public QuarantineUserActionFailedException(String msg) {
        super(msg);
    }

    public QuarantineUserActionFailedException(Throwable cause) {
        super(cause);
    }

    public QuarantineUserActionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
