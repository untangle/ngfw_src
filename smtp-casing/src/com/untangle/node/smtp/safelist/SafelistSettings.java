/**
 * $Id$
 */
package com.untangle.node.smtp.safelist;

import java.io.Serializable;
import com.untangle.node.smtp.SmtpMessageEvent;

/**
 * Setting for safelist (recipient and sender pair).
 * 
 */
@SuppressWarnings("serial")
public class SafelistSettings implements Serializable
{
    private String recipient;
    private String sender;

    // constructors -----------------------------------------------------------

    public SafelistSettings() {
    }

    // accessors --------------------------------------------------------------

    /**
     * @return the recipient of this safelist
     */
    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String addr)
    {
        if (addr.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.recipient = addr;

        return;
    }

    /**
     * @return the sender of this safelist
     */
    public String getSender()
    {
        return sender;
    }

    public void setSender(String addr)
    {
        if (addr.length() > SmtpMessageEvent.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, SmtpMessageEvent.DEFAULT_STRING_SIZE);
        }
        this.sender = addr;

        return;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof SafelistSettings)) {
            return false;
        }

        SafelistSettings sls = (SafelistSettings) o;
        return (true == recipient.equals(sls.recipient) && true == sender.equals(sls.sender));
    }
}
