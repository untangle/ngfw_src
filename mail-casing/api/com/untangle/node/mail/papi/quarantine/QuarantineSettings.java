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

package com.untangle.node.mail.papi.quarantine;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.node.mail.papi.EmailAddressPairRule;
import com.untangle.node.mail.papi.EmailAddressRule;
import com.untangle.node.util.UvmUtil;

/**
 * Settings for the quarantine stuff
 */
@Entity
@Table(name="n_mail_quarantine_settings", schema="settings")
public class QuarantineSettings implements Serializable {

    public static final long HOUR = 1000L*60L*60L; // millisecs per hour
    public static final long DAY = HOUR*24L; // millisecs per day
    public static final long WEEK = DAY*7L; // millisecs per week

    private Long m_id;
    private long m_maxMailIntern = 2L*WEEK;
    private long m_maxIdleInbox = 4L*WEEK;
    private byte[] m_secretKey;
    private int m_digestHOD;//Hour Of Day
    private int m_digestMOD;//Minute Of Day
    private long m_maxQuarantineSz;
    private List<EmailAddressPairRule> m_addressRemaps;
    private List<EmailAddressRule> m_allowedAddressPatterns;
    private boolean sendDailyDigests = true;
    
    private boolean quarantineExternalMail = false;

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    public Long getId() {
        return m_id;
    }

    public void setId(Long id) {
        m_id = id;
    }

    /**
     * Get the list of {@link EmailAddressRule} objects, defining the
     * address patterns for-which this server will quarantine emails.
     * The patterns are based on email address syntax
     * ("local@domain").  However, a limited glob syntax is also
     * supported ("*@domain").  The glob matches any characters (0 or
     * more).  This should not be confused with "real" regex which is
     * not supported.  Only glob.  <br>
     * @return a List of EmailAddressRule objects.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id", nullable=false)
    @IndexColumn(name="position")
    public List<EmailAddressRule> getAllowedAddressPatterns() {
        if (m_allowedAddressPatterns == null) {
            setAllowedAddressPatterns(null);
        }
        return UvmUtil.eliminateNulls(m_allowedAddressPatterns);
    }

    public void setAllowedAddressPatterns(List<EmailAddressRule> patterns) {
        if (patterns == null) {
            patterns = new ArrayList<EmailAddressRule>();
        }
        m_allowedAddressPatterns = patterns;
    }

    /**
     * Set a List of {@link EmailAddressPairRule}
     * objects, defining the "remappings" supported by this server.
     * Remappings associate a pattern with an address.  For example,
     * to cause all emails for "sales@foo.com" to be quarantined in
     * the inbox of "joe.salesguy@foo.com" "sales@foo.com" is the
     * pattern and "joe.salesguy@foo.com" is the mapped address.
     * Since the "EmailAddressPairRule" class is generic (doesn't have
     * "pattern" and "address" members, the "address1" member is the
     * pattern and "address2" is the remap.  <br> The pattern also
     * suports limited wildcards, based on glob ("*") syntax.  The
     * glob matches any characters.  For example, to cause all mails
     * for "foo.com" to be quarantined within "fred@moo.com"'s inbox,
     * the pattern would be "*@foo.com" and the remapping would be
     * "fred@moo.com".
     *
     *
     * @return a List of EmailAddressPairRule objects.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id", nullable=false)
    @IndexColumn(name="position")
    public List<EmailAddressPairRule> getAddressRemaps() {
        if(m_addressRemaps == null) {
            setAddressRemaps(null);
        }
        return UvmUtil.eliminateNulls(m_addressRemaps);
    }

    /**
     * Set the list of addresses to be remapped.  The argument is a
     * list of {@link EmailAddressPairRule}.  These represent the
     * <b>ordered</b> collection of pairs to be remapped.  Note also
     * that the <code>addr1</code> property of the pair is the "map
     * from" (i.e. <code>addr1</code> of "*@foo.com" and
     * <code>addr2</code> of "trash@foo.com").
     *
     * @param remaps the list of remapped addresses.
     */
    public void setAddressRemaps(List<EmailAddressPairRule> remaps) {
        if(remaps == null) {
            remaps = new ArrayList<EmailAddressPairRule>();
        }
        m_addressRemaps = remaps;
    }

    @Column(name="max_quarantine_sz", nullable=false)
    public long getMaxQuarantineTotalSz() {
        return m_maxQuarantineSz;
    }

    /**
     * Set the total size (in bytes) that the quarantine
     * is permitted to consume on disk.
     *
     * @param max the max size
     */
    public void setMaxQuarantineTotalSz(long max) {
        m_maxQuarantineSz = max;
    }


    /**
     * @return the Hour of the day when digest emails should be sent.
     */
    @Column(name="hour_in_day")
    public int getDigestHourOfDay() {
        return m_digestHOD;
    }

    /**
     * Set the Hour of the day when digest emails should
     * be sent.  This should be a value between 0 and 23 (inclusive
     * of both ends).
     *
     * @param hod the hour of the day
     */
    public void setDigestHourOfDay(int hod) {
        m_digestHOD = hod;
    }

    /**
     *
     * @return the Minute of the day when digest emails should be sent.
     */
    @Column(name="minute_in_day")
    public int getDigestMinuteOfDay() {
        return m_digestMOD;
    }

    /**
     * Set the Minute of the day when digest emails should
     * be sent.  This should be a value between 0 and 59 (inclusive
     * of both ends).
     *
     * @param mod the minute of the day
     */
    public void setDigestMinuteOfDay(int mod) {
        m_digestMOD = mod;
    }

    /**
     * Password, encrypted with password utils.
     *
     * @return encrypted password bytes.
     */
    @Column(name="secret_key", length=32, nullable=false)
    public byte[] getSecretKey() {
        return m_secretKey;
    }

    /**
     * Set the key used to create authentication "tokens".  This should
     * really only ever be set once for a given deployment (or else
     * folks with older emails won't be able to use the links).
     */
    public void setSecretKey(byte[] key) {
        m_secretKey = key;
    }

    @Transient
    public String getSecretKeyString()
    {
        try {
            return URLEncoder.encode(new String(this.m_secretKey),"UTF-8");
        } catch ( UnsupportedEncodingException e ) {
            return null;
        }
    }

    public void setSecretKeyString(String newValue)
    {
        if (newValue==null) {
            return;
        }

        try {
            this.m_secretKey = URLDecoder.decode(new String(newValue),"UTF-8").getBytes();
        } catch ( UnsupportedEncodingException e ) {
            /* nothing to do */
        }
    }

    @Column(name="max_intern_time", nullable=false)
    public long getMaxMailIntern() {
        return m_maxMailIntern;
    }

    /**
     * Get the longest period of time (in ms) that a mail may be interned
     * before it is automagically purged.
     */
    public void setMaxMailIntern(long max) {
        m_maxMailIntern = max;
    }

    @Column(name="max_idle_inbox_time", nullable=false)
    public long getMaxIdleInbox() {
        return m_maxIdleInbox;
    }

    /**
     * Set the maximum relative time (in milliseconds)
     * that inboxes can be idle before they are implicitly
     * cleaned-up.  This is a relative unit (ie "2 weeks")
     */
    public void setMaxIdleInbox(long max) {
        m_maxIdleInbox = max;
    }
    
    /**
     *
     * @return a boolean to determine whether to send daily digests
     */
    @Column(name="send_daily_digests")
    public boolean getSendDailyDigests() {
        return this.sendDailyDigests;
    }

    /**
     * Set whether to send daily digests
     */
    public void setSendDailyDigests(boolean sendDailyDigests) {
        this.sendDailyDigests = sendDailyDigests;
    }

}
