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

package com.metavize.tran.virus;

import java.io.Serializable;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the VirusTransform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_SETTINGS"
 */
public class VirusSettings implements Serializable
{
    private static final long serialVersionUID = -7246008133224046834L;
/*
    public static final String OUT_MOD_SUB_TEMPLATE =
      "[VIRUS] $MIMEMessage:SUBJECT$";
    public static final String OUT_MOD_BODY_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ was found to contain\r\n" +
      "the virus \"$VirusReport:VIRUS_NAME$\".  The infected portion of the attached email was removed\r\n" +
      "by Metavize EdgeGuard.\r\n";
    public static final String OUT_MOD_BODY_SMTP_TEMPLATE =
      "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was found to contain\r\n" +
      "the virus \"$VirusReport:VIRUS_NAME$\".  The infected portion of the attached email was removed\r\n" +
      "by Metavize EdgeGuard.\r\n";

    public static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    public static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
    public static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;
*/
    private Long id;
    private Tid tid;
    private boolean ftpDisableResume = true;
    private boolean httpDisableResume = true;
    private int tricklePercent = 90;
    private String ftpDisableResumeDetails = "no description";
    private String httpDisableResumeDetails = "no description";
    private String tricklePercentDetails = "no description";
    private VirusConfig httpInbound;
    private VirusConfig httpOutbound;
    private VirusConfig ftpInbound;
    private VirusConfig ftpOutbound;

    private VirusSMTPConfig SMTPInbound;
    private VirusSMTPConfig SMTPOutbound;
    private VirusPOPConfig POPInbound;
    private VirusPOPConfig POPOutbound;
    private VirusIMAPConfig IMAPInbound;
    private VirusIMAPConfig IMAPOutbound;

    private List httpMimeTypes;
    private List extensions;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusSettings() { }

    public VirusSettings(Tid tid)
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
     * unique="true"
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
     * Disable resume of FTP download.
     *
     * @return true if FTP resume is disabled.
     * @hibernate.property
     * column="DISABLE_FTP_RESUME"
     */
    public boolean getFtpDisableResume()
    {
        return ftpDisableResume;
    }

    public void setFtpDisableResume(boolean ftpDisableResume)
    {
        this.ftpDisableResume = ftpDisableResume;
    }


    /**
     * Disable resume of HTTP download.
     *
     * @return true if HTTP resume is disabled.
     * @hibernate.property
     * column="DISABLE_HTTP_RESUME"
     */
    public boolean getHttpDisableResume()
    {
        return httpDisableResume;
    }

    public void setHttpDisableResume(boolean httpDisableResume)
    {
        this.httpDisableResume = httpDisableResume;
    }

    /**
     * The trickle rate.
     *
     * @return the trickle rate, between 0 and 100.
     * @hibernate.property
     * column="TRICKLE_PERCENT"
     */
    public int getTricklePercent()
    {
        return tricklePercent;
    }

    public void setTricklePercent(int tricklePercent)
    {
        if (0 > tricklePercent || 100 < tricklePercent) {
            throw new IllegalArgumentException("bad trickle rate: "
                                               + tricklePercent);
        }

        this.tricklePercent = tricklePercent;
    }

    /**
     * XXX what is this for?
     *
     * @return XXX
     * @hibernate.property
     * column="FTP_DISABLE_RESUME_DETAILS"
     */
    public String getFtpDisableResumeDetails()
    {
        return ftpDisableResumeDetails;
    }

    public void setFtpDisableResumeDetails(String ftpDisableResumeDetails)
    {
        this.ftpDisableResumeDetails = ftpDisableResumeDetails;
    }

    /**
     * XXX what is this for?
     *
     * @return XXX
     * @hibernate.property
     * column="HTTP_DISABLE_RESUME_DETAILS"
     */
    public String getHttpDisableResumeDetails()
    {
        return httpDisableResumeDetails;
    }

    public void setHttpDisableResumeDetails(String httpDisableResumeDetails)
    {
        this.httpDisableResumeDetails = httpDisableResumeDetails;
    }

    /**
     * XXX what is this for?
     *
     * @return XXX
     * @hibernate.property
     * column="TRICKLE_PERCENT_DETAILS"
     */
    public String getTricklePercentDetails()
    {
        return tricklePercentDetails;
    }

    public void setTricklePercentDetails(String tricklePercentDetails)
    {
        this.tricklePercentDetails = tricklePercentDetails;
    }

    /**
     * Inbound HTTP virus settings.
     *
     * @return inbound HTTP settings.
     * @hibernate.many-to-one
     * column="HTTP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusConfig getHttpInbound()
    {
        return httpInbound;
    }

    public void setHttpInbound(VirusConfig httpInbound)
    {
        this.httpInbound = httpInbound;
    }

    /**
     * Outbound HTTP virus settings.
     *
     * @return outbound HTTP settings.
     * @hibernate.many-to-one
     * column="HTTP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusConfig getHttpOutbound()
    {
        return httpOutbound;
    }

    public void setHttpOutbound(VirusConfig httpOutbound)
    {
        this.httpOutbound = httpOutbound;
    }

    /**
     * Inbound FTP virus settings.
     *
     * @return inbound FTP settings.
     * @hibernate.many-to-one
     * column="FTP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusConfig getFtpInbound()
    {
        return ftpInbound;
    }

    public void setFtpInbound(VirusConfig ftpInbound)
    {
        this.ftpInbound = ftpInbound;
    }

    /**
     * Outbound FTP virus settings.
     *
     * @return outbound FTP settings.
     * @hibernate.many-to-one
     * column="FTP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusConfig getFtpOutbound()
    {
        return ftpOutbound;
    }

    public void setFtpOutbound(VirusConfig ftpOutbound)
    {
        this.ftpOutbound = ftpOutbound;
    }

    /**
     * Inbound SMTP virus settings.
     *
     * @return inbound SMTP settings.
     * @hibernate.many-to-one
     * column="SMTP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusSMTPConfig getSMTPInbound()
    {
        return SMTPInbound;
    }

    public void setSMTPInbound(VirusSMTPConfig SMTPInbound)
    {
        this.SMTPInbound = SMTPInbound;
        return;
    }

    /**
     * Outbound SMTP virus settings.
     *
     * @return outbound SMTP settings.
     * @hibernate.many-to-one
     * column="SMTP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusSMTPConfig getSMTPOutbound()
    {
        return SMTPOutbound;
    }

    public void setSMTPOutbound(VirusSMTPConfig SMTPOutbound)
    {
        this.SMTPOutbound = SMTPOutbound;
        return;
    }

    /**
     * Inbound POP virus settings.
     *
     * @return inbound POP settings.
     * @hibernate.many-to-one
     * column="POP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusPOPConfig getPOPInbound()
    {
        return POPInbound;
    }

    public void setPOPInbound(VirusPOPConfig POPInbound)
    {
        this.POPInbound = POPInbound;
        return;
    }

    /**
     * Outbound POP virus settings.
     *
     * @return outbound POP settings.
     * @hibernate.many-to-one
     * column="POP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusPOPConfig getPOPOutbound()
    {
        return POPOutbound;
    }

    public void setPOPOutbound(VirusPOPConfig POPOutbound)
    {
        this.POPOutbound = POPOutbound;
        return;
    }

    /**
     * Inbound IMAP virus settings.
     *
     * @return inbound IMAP settings.
     * @hibernate.many-to-one
     * column="IMAP_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusIMAPConfig getIMAPInbound()
    {
        return IMAPInbound;
    }

    public void setIMAPInbound(VirusIMAPConfig IMAPInbound)
    {
        this.IMAPInbound = IMAPInbound;
        return;
    }

    /**
     * Outbound IMAP virus settings.
     *
     * @return outbound IMAP settings.
     * @hibernate.many-to-one
     * column="IMAP_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VirusIMAPConfig getIMAPOutbound()
    {
        return IMAPOutbound;
    }

    public void setIMAPOutbound(VirusIMAPConfig IMAPOutbound)
    {
        this.IMAPOutbound = IMAPOutbound;
        return;
    }

    /**
     * Set of scanned mime types
     *
     * @return the list of scanned mime types.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_VIRUS_VS_MT"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.MimeTypeRule"
     * column="RULE_ID"
     */
    public List getHttpMimeTypes()
    {
        return httpMimeTypes;
    }

    public void setHttpMimeTypes(List httpMimeTypes)
    {
        this.httpMimeTypes = httpMimeTypes;
    }

    /**
     * Extensions to be scanned.
     *
     * @return the set of scanned extensions.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_VIRUS_VS_EXT"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getExtensions()
    {
        return extensions;
    }

    public void setExtensions(List extensions)
    {
        this.extensions = extensions;
    }
}
