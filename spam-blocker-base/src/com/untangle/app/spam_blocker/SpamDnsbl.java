/**
 * $Id$
 */
package com.untangle.app.spam_blocker;

import java.io.Serializable;
import org.json.JSONString;

/**
 * Spam DNS blacklist implementation
 */
@SuppressWarnings("serial")
public class SpamDnsbl implements Serializable, JSONString
{
    private Long id;

    private String hostname;
    private String description = null;
    private boolean active = false;

    public SpamDnsbl() {}

    public SpamDnsbl(String hostname, String description, boolean active)
    {
        this.hostname = hostname;
        this.description = description;
        this.active = active;
    }

    public Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * hostname of spam DNSBL
     *
     * @return hostname of spam DNSBL
     */
    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
        return;
    }

    /**
     * description of hostname (for display)
     *
     * @return description of hostname
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
        return;
    }

    /**
     * active spam DNSBL flag
     *
     * @return active spam DNSBL flag
     */
    public boolean getActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
        return;
    }

    public void update( SpamDnsbl newRule )
    {
        this.hostname = newRule.hostname;
        this.description = newRule.description;
        this.active = newRule.active;
    }
    
    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
