/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.virus;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.MimeTypeRule;
import com.untangle.mvvm.tran.StringRule;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings for the VirusTransform.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_virus_settings", schema="settings")
public class VirusSettings implements Serializable
{
    private static final long serialVersionUID = -7246008133224046834L;

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

    private List<MimeTypeRule> httpMimeTypes;
    private List<StringRule> extensions;

    // constructors -----------------------------------------------------------

    public VirusSettings() { }

    public VirusSettings(Tid tid)
    {
        this.tid = tid;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
    @GeneratedValue
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
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
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
     */
    @Column(name="disable_ftp_resume", nullable=false)
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
     */
    @Column(name="disable_http_resume", nullable=false)
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
     */
    @Column(name="trickle_percent", nullable=false)
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

    @Column(name="ftp_disable_resume_details")
    public String getFtpDisableResumeDetails()
    {
        return ftpDisableResumeDetails;
    }

    public void setFtpDisableResumeDetails(String ftpDisableResumeDetails)
    {
        this.ftpDisableResumeDetails = ftpDisableResumeDetails;
    }

    @Column(name="http_disable_resume_details")
    public String getHttpDisableResumeDetails()
    {
        return httpDisableResumeDetails;
    }

    public void setHttpDisableResumeDetails(String httpDisableResumeDetails)
    {
        this.httpDisableResumeDetails = httpDisableResumeDetails;
    }

    @Column(name="trickle_percent_details")
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="http_inbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="http_outbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="ftp_inbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="ftp_outbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="smtp_inbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="smtp_outbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="pop_inbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="pop_outbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="imap_inbound", nullable=false)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="imap_outbound", nullable=false)
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_virus_vs_mt",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<MimeTypeRule> getHttpMimeTypes()
    {
        return httpMimeTypes;
    }

    public void setHttpMimeTypes(List<MimeTypeRule> httpMimeTypes)
    {
        this.httpMimeTypes = httpMimeTypes;
    }

    /**
     * Extensions to be scanned.
     *
     * @return the set of scanned extensions.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_virus_vs_ext",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getExtensions()
    {
        return extensions;
    }

    public void setExtensions(List<StringRule> extensions)
    {
        this.extensions = extensions;
    }
}
