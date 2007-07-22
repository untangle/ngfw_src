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

package com.untangle.node.mail.papi;

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

import com.untangle.node.mail.papi.quarantine.QuarantineSettings;
import com.untangle.node.mail.papi.safelist.SafelistSettings;
import com.untangle.node.util.UvmUtil;
import org.hibernate.annotations.IndexColumn;

/**
 * Mail casing settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_mail_settings", schema="settings")
public class MailNodeSettings implements Serializable
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
    private List<SafelistSettings> safelistSettings;

    // constructors -----------------------------------------------------------

    public MailNodeSettings() { }

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
     * Enabled status of SMTP casing.
     *
     * @return true if SMTP casing is enabled, false otherwise.
     */
    @Column(name="smtp_enabled", nullable=false)
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
     */
    @Column(name="pop_enabled", nullable=false)
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
     */
    @Column(name="imap_enabled", nullable=false)
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
     */
    @Column(name="smtp_inbound_timeout", nullable=false)
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
     */
    @Column(name="Smtp_outbound_timeout", nullable=false)
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
     */
    @Column(name="pop_inbound_timeout", nullable=false)
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
     */
    @Column(name="pop_outbound_timeout", nullable=false)
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
     */
    @Column(name="imap_inbound_timeout", nullable=false)
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
     */
    @Column(name="imap_outbound_timeout", nullable=false)
    public long getImapOutboundTimeout()
    {
        return imapOutboundTimeout;
    }

    public void setImapOutboundTimeout(long imapOutboundTimeout)
    {
        this.imapOutboundTimeout = imapOutboundTimeout;
    }

    /**
     * Quarantine properties associated with this casing.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="quarantine_settings", nullable=false)
    public QuarantineSettings getQuarantineSettings() {
        return quarantineSettings;
    }

    public void setQuarantineSettings(QuarantineSettings s) {
        this.quarantineSettings = s;
    }

    /**
     * (actions cascade from parent to child/children and
     *  orphan children are deleted)
     *
     * @return the list of Safelist settings
     */
    @OneToMany(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinTable(name="n_mail_safelists",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="safels_id"))
    @IndexColumn(name="position")
    public List<SafelistSettings> getSafelistSettings()
    {
        return UvmUtil.eliminateNulls(safelistSettings);
    }

    public void setSafelistSettings(List<SafelistSettings> safelistSettings)
    {
        this.safelistSettings = safelistSettings;

        return;
    }
}
