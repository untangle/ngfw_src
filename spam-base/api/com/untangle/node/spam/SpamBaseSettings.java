/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * Base Settings for the Spam node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Embeddable
@SuppressWarnings("serial")
public class SpamBaseSettings implements Serializable
{
    private SpamSmtpConfig smtpConfig;
    private SpamPopConfig popConfig;
    private SpamImapConfig imapConfig;

    private int rblListLength;

    /* This is the date when the system last got updates */
    private Date lastUpdate;

    /* This is the date when the system last checked for updates */
    private Date lastUpdateCheck;
    
    /* This is the version string for the signatures, it may or may not include a date */
    private String signatureVersion;
    
    /**
     * SMTP settings.
     *
     * @return SMTP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="smtp_config", nullable=false)
    public SpamSmtpConfig getSmtpConfig()
    {
        return smtpConfig;
    }

    public void setSmtpConfig(SpamSmtpConfig smtpConfig)
    {
        this.smtpConfig = smtpConfig;
    }

    /**
     * POP spam settings.
     *
     * @return POP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="pop_config", nullable=false)
    public SpamPopConfig getPopConfig()
    {
        return popConfig;
    }

    public void setPopConfig(SpamPopConfig popConfig)
    {
        this.popConfig = popConfig;
    }

    /**
     * IMAP spam settings.
     *
     * @return IMAP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="imap_config", nullable=false)
    public SpamImapConfig getImapConfig()
    {
        return imapConfig;
    }

    public void setImapConfig(SpamImapConfig imapConfig)
    {
        this.imapConfig = imapConfig;
    }

    @Transient
    public int getSpamRBLListLength()
    {
        return this.rblListLength;
    }

    public void setSpamRBLListLength(int newValue)
    {
        this.rblListLength = newValue;
    }


    @Transient
    public Date getLastUpdate()
    {
        return this.lastUpdate;
    }

    public void setLastUpdate(Date newValue)
    {
        this.lastUpdate = newValue;
    }

    @Transient
    public Date getLastUpdateCheck()
    {
        return this.lastUpdateCheck;
    }

    public void setLastUpdateCheck(Date newValue)
    {
        this.lastUpdateCheck = newValue;
    }

    @Transient
    public String getSignatureVersion()
    {
        return this.signatureVersion;
    }

    public void setSignatureVersion(String newValue)
    {
        this.signatureVersion = newValue;
    }

    public void copy(SpamBaseSettings baseSettings)
    {
		baseSettings.setSmtpConfig(this.smtpConfig);
		baseSettings.setPopConfig(this.popConfig);
		baseSettings.setImapConfig(this.imapConfig);
		baseSettings.setSpamRBLListLength(this.rblListLength);
		baseSettings.setLastUpdate(this.lastUpdate);
		baseSettings.setSignatureVersion(this.signatureVersion);
    }    
}
