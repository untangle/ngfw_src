/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.io.Serializable;
import org.json.JSONString;
import java.util.LinkedList;
import java.util.List;

/**
 * Settings for the SpamApp.
 */
@SuppressWarnings("serial")
public class SpamSettings implements Serializable, JSONString
{
    private List<SpamDnsbl> spamDnsblList;
    private SpamSmtpConfig smtpConfig;

    public SpamSettings()
    {
        spamDnsblList = new LinkedList<SpamDnsbl>();
    }

    public List<SpamDnsbl> getSpamDnsblList() { return spamDnsblList; }
    public void setSpamDnsblList( List<SpamDnsbl> newValue ) { this.spamDnsblList = newValue; }

    public SpamSmtpConfig getSmtpConfig() { return smtpConfig; }
    public void setSmtpConfig( SpamSmtpConfig newValue ) { this.smtpConfig = newValue; }

    // public void copy(SpamSettings argSettings)
    // {
    //     argSettings.setSpamDnsblList(this.spamDnsblList);
    //     argSettings.setSmtpConfig(this.smtpConfig);
    // }
    
    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
