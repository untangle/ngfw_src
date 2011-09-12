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

    private List<SpamRBL> spamRBLList;
    private SpamSmtpConfig smtpConfig;
    private SpamPopConfig popConfig;
    private SpamImapConfig imapConfig;

    // constructors -----------------------------------------------------------

    public SpamSettings()
    {
        spamRBLList = new LinkedList<SpamRBL>();
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

    public List<SpamRBL> getSpamRBLList()
    {
        return spamRBLList;
    }

    public void setSpamRBLList(List<SpamRBL> spamRBLList)
    {
        this.spamRBLList = spamRBLList;
    }

    public SpamSmtpConfig getSmtpConfig()
    {
        return smtpConfig;
    }

    public void setSmtpConfig(SpamSmtpConfig smtpConfig)
    {
        this.smtpConfig = smtpConfig;
    }

    public SpamPopConfig getPopConfig()
    {
        return popConfig;
    }

    public void setPopConfig(SpamPopConfig popConfig)
    {
        this.popConfig = popConfig;
    }

    public SpamImapConfig getImapConfig()
    {
        return imapConfig;
    }

    public void setImapConfig(SpamImapConfig imapConfig)
    {
        this.imapConfig = imapConfig;
    }

    public void copy(SpamSettings argSettings)
    {
        argSettings.setSpamRBLList(this.spamRBLList);
        argSettings.setSmtpConfig(this.smtpConfig);
        argSettings.setPopConfig(this.popConfig);
        argSettings.setImapConfig(this.imapConfig);
    }    
}
