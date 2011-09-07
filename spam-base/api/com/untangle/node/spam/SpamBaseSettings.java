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

/*
 * Since the jabsorb serializer doesn't seem to pay attention to the transient
 * keyword, I have prefixed all of the getXxx and setXxx functions that deal
 * with transient objects with T_ which prevents them from being included
 * in the JSON stream.
 */

package com.untangle.node.spam;

import java.io.Serializable;
import java.util.Date;

/**
 * Base Settings for the Spam node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class SpamBaseSettings implements Serializable
{
    private SpamSmtpConfig smtpConfig;
    private SpamPopConfig popConfig;
    private SpamImapConfig imapConfig;

    transient private int rblListLength;

    /* This is the date when the system last got updates */
    transient private Date lastUpdate;

    /* This is the date when the system last checked for updates */
    transient private Date lastUpdateCheck;
    
    /* This is the version string for the signatures, it may or may not include a date */
    transient private String signatureVersion;
    
    /**
     * SMTP settings.
     *
     * @return SMTP settings.
     */
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
    public SpamImapConfig getImapConfig()
    {
        return imapConfig;
    }

    public void setImapConfig(SpamImapConfig imapConfig)
    {
        this.imapConfig = imapConfig;
    }

    public int T_getSpamRBLListLength()
    {
        return this.rblListLength;
    }

    public void T_setSpamRBLListLength(int newValue)
    {
        this.rblListLength = newValue;
    }

    public Date T_getLastUpdate()
    {
        return this.lastUpdate;
    }

    public void T_setLastUpdate(Date newValue)
    {
        this.lastUpdate = newValue;
    }

    public Date T_getLastUpdateCheck()
    {
        return this.lastUpdateCheck;
    }

    public void T_setLastUpdateCheck(Date newValue)
    {
        this.lastUpdateCheck = newValue;
    }

    public String T_getSignatureVersion()
    {
        return this.signatureVersion;
    }

    public void T_setSignatureVersion(String newValue)
    {
        this.signatureVersion = newValue;
    }

    public void copy(SpamBaseSettings baseSettings)
    {
		baseSettings.setSmtpConfig(this.smtpConfig);
		baseSettings.setPopConfig(this.popConfig);
		baseSettings.setImapConfig(this.imapConfig);
		baseSettings.T_setSpamRBLListLength(this.rblListLength);
		baseSettings.T_setLastUpdate(this.lastUpdate);
		baseSettings.T_setSignatureVersion(this.signatureVersion);
    }    
}
