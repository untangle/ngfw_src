/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;

/**
 * ...name says it all...
 */
@SuppressWarnings("serial")
public class NoSuchInboxException extends Exception implements Serializable
{

    private final String m_accountName;

    public NoSuchInboxException(String accountName) {
        super("No such account \"" + accountName + "\"");
        m_accountName = accountName;
    }

    public String getAccountName()
    {
        return m_accountName;
    }

}
