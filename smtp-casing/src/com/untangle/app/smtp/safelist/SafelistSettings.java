/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Setting for safelist (recipient and sender pair).
 * 
 */
@SuppressWarnings("serial")
public class SafelistSettings implements Serializable, JSONString
{
    private String recipient;
    private String sender;

    public SafelistSettings() {}

    /**
     * @return the recipient of this safelist
     */
    public String getRecipient() { return recipient; }
    public void setRecipient(String newValue) { this.recipient = newValue; }

    /**
     * @return the sender of this safelist
     */
    public String getSender() { return sender; }
    public void setSender(String newValue) { this.sender = newValue; }

    public boolean equals(Object o)
    {
        if (!(o instanceof SafelistSettings)) {
            return false;
        }
        SafelistSettings sls = (SafelistSettings) o;
        return (true == recipient.equals(sls.recipient) && true == sender.equals(sls.sender));
    }

    public int hashCode()
    {
        if ( recipient != null )
            return recipient.hashCode();
        else
            return 0;
    }
    
    /**
     * Convert settings to JSON string.
     *
     * @return
     *      JSON string.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
