/*
 * $Id$
 */
package com.untangle.node.spam;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Settings for the SpamNode.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class SpamSettings implements Serializable
{
    private Long id;

    private List<SpamDnsbl> spamDnsblList;
    private SpamSmtpConfig smtpConfig;

    // constructors -----------------------------------------------------------

    public SpamSettings()
    {
        spamDnsblList = new LinkedList<SpamDnsbl>();
    }

    // accessors --------------------------------------------------------------

    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    public List<SpamDnsbl> getSpamDnsblList()
    {
        return spamDnsblList;
    }

    public void setSpamDnsblList(List<SpamDnsbl> spamDnsblList)
    {
        this.spamDnsblList = spamDnsblList;
    }

    public SpamSmtpConfig getSmtpConfig()
    {
        return smtpConfig;
    }

    public void setSmtpConfig(SpamSmtpConfig smtpConfig)
    {
        this.smtpConfig = smtpConfig;
    }

    public void copy(SpamSettings argSettings)
    {
        argSettings.setSpamDnsblList(this.spamDnsblList);
        argSettings.setSmtpConfig(this.smtpConfig);
    }    
}
