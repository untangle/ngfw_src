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

package com.metavize.tran.spam;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the SpamTransform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_SETTINGS"
 */
public class SpamSettings implements Serializable
{
    private static final long serialVersionUID = -7246008133224040004L;

    public static final String OUT_MOD_SUB_TEMPLATE =
      "[SPAM] $MIMEMessage:SUBJECT$";
    public static final String OUT_MOD_BODY_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ was determined\r\n " +
      "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$\r\n" +
      "is SPAM.  The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";
    public static final String OUT_MOD_BODY_SMTP_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was determined\r\n " +
      "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$\r\n" +
      "is SPAM.  The details of the report are as follows:\r\n\r\n" +
      "$SPAMReport:FULL$";

    public static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    public static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
    public static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private Long id;
    private Tid tid;

    private SpamSMTPConfig SMTPInbound;
    private SpamSMTPConfig SMTPOutbound;
    private SpamPOPConfig POPInbound;
    private SpamPOPConfig POPOutbound;
    private SpamIMAPConfig IMAPInbound;
    private SpamIMAPConfig IMAPOutbound;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamSettings() { }

    public SpamSettings(Tid tid)
    {
        this.tid = tid;
    }

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
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Inbound SMTP spam settings.
     *
     * @return inbound SMTP settings.
     * @hibernate.many-to-one
     * column="SMTP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamSMTPConfig getSMTPInbound()
    {
        return SMTPInbound;
    }

    public void setSMTPInbound(SpamSMTPConfig SMTPInbound)
    {
        this.SMTPInbound = SMTPInbound;
        return;
    }

    /**
     * Outbound SMTP spam settings.
     *
     * @return outbound SMTP settings.
     * @hibernate.many-to-one
     * column="SMTP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamSMTPConfig getSMTPOutbound()
    {
        return SMTPOutbound;
    }

    public void setSMTPOutbound(SpamSMTPConfig SMTPOutbound)
    {
        this.SMTPOutbound = SMTPOutbound;
        return;
    }

    /**
     * Inbound POP spam settings.
     *
     * @return inbound POP settings.
     * @hibernate.many-to-one
     * column="POP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamPOPConfig getPOPInbound()
    {
        return POPInbound;
    }

    public void setPOPInbound(SpamPOPConfig POPInbound)
    {
        this.POPInbound = POPInbound;
        return;
    }

    /**
     * Outbound POP spam settings.
     *
     * @return outbound POP settings.
     * @hibernate.many-to-one
     * column="POP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamPOPConfig getPOPOutbound()
    {
        return POPOutbound;
    }

    public void setPOPOutbound(SpamPOPConfig POPOutbound)
    {
        this.POPOutbound = POPOutbound;
        return;
    }

    /**
     * Inbound IMAP spam settings.
     *
     * @return inbound IMAP settings.
     * @hibernate.many-to-one
     * column="IMAP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamIMAPConfig getIMAPInbound()
    {
        return IMAPInbound;
    }

    public void setIMAPInbound(SpamIMAPConfig IMAPInbound)
    {
        this.IMAPInbound = IMAPInbound;
        return;
    }

    /**
     * Outbound IMAP spam settings.
     *
     * @return outbound IMAP settings.
     * @hibernate.many-to-one
     * column="IMAP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SpamIMAPConfig getIMAPOutbound()
    {
        return IMAPOutbound;
    }

    public void setIMAPOutbound(SpamIMAPConfig IMAPOutbound)
    {
        this.IMAPOutbound = IMAPOutbound;
        return;
    }
}
