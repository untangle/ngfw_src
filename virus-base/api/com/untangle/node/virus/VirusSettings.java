/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.virus;

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

import com.untangle.node.util.UvmUtil;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.security.Tid;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings for the VirusNode.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_virus_settings", schema="settings")
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
    private VirusConfig httpConfig;
    private VirusConfig ftpConfig;

    private VirusSMTPConfig smtpConfig;
    private VirusPOPConfig popConfig;
    private VirusIMAPConfig imapConfig;

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
     * Node id for these settings.
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

    /**
     * Set of scanned mime types
     *
     * @return the list of scanned mime types.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_virus_vs_mt",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<MimeTypeRule> getHttpMimeTypes()
    {
        return UvmUtil.eliminateNulls(httpMimeTypes);
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
    @JoinTable(name="n_virus_vs_ext",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getExtensions()
    {
        return UvmUtil.eliminateNulls(extensions);
    }

    public void setExtensions(List<StringRule> extensions)
    {
        this.extensions = extensions;
    }
}
