/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Exception for no such inbox.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class NoSuchInboxException extends Exception implements Serializable
{

    private final String m_accountName;

    /**
     * Initialize instance of NoSuchInboxException.
     * @param  accountName Account name for inbox.
     * @return             String of exception.
     */
    public NoSuchInboxException(String accountName) {
        super("No such account \"" + accountName + "\"");
        m_accountName = accountName;
    }

    /**
     * Return account name.
     * @return String of account name.
     */
    public String getAccountName()
    {
        return m_accountName;
    }

}
