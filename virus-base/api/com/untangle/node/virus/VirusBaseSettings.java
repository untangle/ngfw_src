package com.untangle.node.virus;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * Base Settings for the Virus node.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@Embeddable
public class VirusBaseSettings {
    private boolean ftpDisableResume = true;
    private boolean httpDisableResume = true;
    private int tricklePercent = 90;
    private String ftpDisableResumeDetails = "no description";
    private String httpDisableResumeDetails = "no description";
    private String tricklePercentDetails = "no description";
    private VirusConfig httpConfig;
    private VirusConfig ftpConfig;

    private VirusSMTPConfig smtpConfig;
    private VirusPOPConfig popConfig;
    private VirusIMAPConfig imapConfig;
    
    private int httpMimeTypesLength;
    private int extensionsLength;
    
    public VirusBaseSettings() {
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
     * HTTP virus settings.
     *
     * @return HTTP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="http_config", nullable=false)
    public VirusConfig getHttpConfig()
    {
        return httpConfig;
    }

    public void setHttpConfig(VirusConfig httpConfig)
    {
        this.httpConfig = httpConfig;
    }

    /**
     * FTP virus settings.
     *
     * @return FTP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="ftp_config", nullable=false)
    public VirusConfig getFtpConfig()
    {
        return ftpConfig;
    }

    public void setFtpConfig(VirusConfig ftpConfig)
    {
        this.ftpConfig = ftpConfig;
    }

    /**
     * SMTP virus settings.
     *
     * @return SMTP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="smtp_config", nullable=false)
    public VirusSMTPConfig getSmtpConfig()
    {
        return smtpConfig;
    }

    public void setSmtpConfig(VirusSMTPConfig smtpConfig)
    {
        this.smtpConfig = smtpConfig;
    }

    /**
     * POP virus settings.
     *
     * @return POP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="pop_config", nullable=false)
    public VirusPOPConfig getPopConfig()
    {
        return popConfig;
    }

    public void setPopConfig(VirusPOPConfig popConfig)
    {
        this.popConfig = popConfig;
    }

    /**
     * IMAP virus settings.
     *
     * @return IMAP settings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="imap_config", nullable=false)
    public VirusIMAPConfig getImapConfig()
    {
        return imapConfig;
    }

    public void setImapConfig(VirusIMAPConfig imapConfig)
    {
        this.imapConfig = imapConfig;
    }

    @Transient
	public int getHttpMimeTypesLength() {
		return httpMimeTypesLength;
	}

	public void setHttpMimeTypesLength(int httpMimeTypesLength) {
		this.httpMimeTypesLength = httpMimeTypesLength;
	}

    @Transient
	public int getExtensionsLength() {
		return extensionsLength;
	}

	public void setExtensionsLength(int extensionsLength) {
		this.extensionsLength = extensionsLength;
	}

}
