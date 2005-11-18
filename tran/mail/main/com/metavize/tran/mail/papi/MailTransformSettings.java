/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;

import java.io.Serializable;
import java.util.List;

import com.metavize.tran.mail.papi.quarantine.QuarantineSettings;

/**
 * Mail casing settings.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_MAIL_SETTINGS"
 */
public class MailTransformSettings implements Serializable
{
    private static final long serialVersionUID = -6466793822226799781L;

    private Long id;

    private boolean smtpEnabled = true;
    private boolean popEnabled = true;
    private boolean imapEnabled = true;

    public static final long TIMEOUT_MAX = 86400000l;
    public static final long TIMEOUT_MIN = 0l;

    private long smtpInboundTimeout;
    private long smtpOutboundTimeout;
    private long popInboundTimeout;
    private long popOutboundTimeout;
    private long imapInboundTimeout;
    private long imapOutboundTimeout;
    private QuarantineSettings quarantineSettings;
    private List safelistSettings; // xdoclet doesn't support java 1.5 syntax

    // constructors -----------------------------------------------------------

    public MailTransformSettings() { }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Enabled status of SMTP casing.
     *
     * @return true if SMTP casing is enabled, false otherwise.
     * @hibernate.property
     * column="SMTP_ENABLED"
     * not-null="true"
     */
    public boolean isSmtpEnabled()
    {
        return smtpEnabled;
    }

    public void setSmtpEnabled(boolean smtpEnabled)
    {
        this.smtpEnabled = smtpEnabled;
    }

    /**
     * Enabled status of POP casing.
     *
     * @return true of POP casing is enabled, false otherwise.
     * @hibernate.property
     * column="POP_ENABLED"
     * not-null="true"
     */
    public boolean isPopEnabled()
    {
        return popEnabled;
    }

    public void setPopEnabled(boolean popEnabled)
    {
        this.popEnabled = popEnabled;
    }

    /**
     * Enabled status of IMAP casing.
     *
     * @return true of IMAP casing is enabled, false otherwise.
     * @hibernate.property
     * column="IMAP_ENABLED"
     * not-null="true"
     */
    public boolean isImapEnabled()
    {
        return imapEnabled;
    }

    public void setImapEnabled(boolean imapEnabled)
    {
        this.imapEnabled = imapEnabled;
    }

    /**
     * Timeout for SMTP inbound traffic.
     *
     * @return timeout for SMTP in millis.
     * @hibernate.property
     * column="SMTP_INBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getSmtpInboundTimeout()
    {
        return smtpInboundTimeout;
    }

    public void setSmtpInboundTimeout(long smtpInboundTimeout)
    {
        this.smtpInboundTimeout = smtpInboundTimeout;
    }

    /**
     * Timeout for SMTP outbound traffic.
     *
     * @return timeout for SMTP in millis.
     * @hibernate.property
     * column="SMTP_OUTBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getSmtpOutboundTimeout()
    {
        return smtpOutboundTimeout;
    }

    public void setSmtpOutboundTimeout(long smtpOutboundTimeout)
    {
        this.smtpOutboundTimeout = smtpOutboundTimeout;
    }

    /**
     * Timeout for POP inbound traffic.
     *
     * @return timeout for POP in millis.
     * @hibernate.property
     * column="POP_INBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getPopInboundTimeout()
    {
        return popInboundTimeout;
    }

    public void setPopInboundTimeout(long popInboundTimeout)
    {
        this.popInboundTimeout = popInboundTimeout;
    }

    /**
     * Timeout for POP outbound traffic.
     *
     * @return timeout for POP in millis.
     * @hibernate.property
     * column="POP_OUTBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getPopOutboundTimeout()
    {
        return popOutboundTimeout;
    }

    public void setPopOutboundTimeout(long popOutboundTimeout)
    {
        this.popOutboundTimeout = popOutboundTimeout;
    }

    /**
     * Timeout for IMAP inbound traffic.
     *
     * @return timeout for IMAP in millis.
     * @hibernate.property
     * column="IMAP_INBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getImapInboundTimeout()
    {
        return imapInboundTimeout;
    }

    public void setImapInboundTimeout(long imapInboundTimeout)
    {
        this.imapInboundTimeout = imapInboundTimeout;
    }

    /**
     * Timeout for IMAP outbound traffic.
     *
     * @return timeout for IMAP in millis.
     * @hibernate.property
     * column="IMAP_OUTBOUND_TIMEOUT"
     * not-null="true"
     */
    public long getImapOutboundTimeout()
    {
        return imapOutboundTimeout;
    }

    public void setImapOutboundTimeout(long imapOutboundTimeout)
    {
        this.imapOutboundTimeout = imapOutboundTimeout;
    }

    /**
     * Set the Quarantine properties associated with this
     * casing
     */
    public void setQuarantineSettings(QuarantineSettings s) {
       this.quarantineSettings = s;
    }
    /**
     *
     * @hibernate.many-to-one
     * column="QUARANTINE_SETTINGS"
     * not-null="false"
     * cascade="all"
     */
    public QuarantineSettings getQuarantineSettings() {
      return quarantineSettings;
    }

    /**
     * (actions cascade from parent to child/children and
     *  orphan children are deleted)
     *
     * @return the list of Safelist settings
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_MAIL_SAFELISTS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.mail.papi.safelist.SafelistSettings"
     * column="SAFELS_ID"
     */
    public List getSafelistSettings()
    {
        return safelistSettings;
    }

    public void setSafelistSettings(List safelistSettings)
    {
        this.safelistSettings = safelistSettings;

        return;
    }
}
