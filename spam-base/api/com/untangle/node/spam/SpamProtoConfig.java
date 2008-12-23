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

package com.untangle.node.spam;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.untangle.node.mail.papi.WrappedMessageGenerator;

/**
 * Hibernate mappings for this class are in the UVM resource
 * directory.
 */
@MappedSuperclass
public abstract class SpamProtoConfig implements Serializable
{
    public static final int DEFAULT_MESSAGE_SIZE_LIMIT = 1 << 18;
    public static final String NO_NOTES = "no description";
    public static final int DEFAULT_STRENGTH = 43;
    public static final int DEFAULT_SUPER_STRENGTH = 43;
    private Long id;

    /* settings */
    private boolean bScan = false;
    private int strength = DEFAULT_STRENGTH;
    private int superSpamStrength = DEFAULT_SUPER_STRENGTH;
    private boolean blockSuperSpam = false;
    private int msgSizeLimit = DEFAULT_MESSAGE_SIZE_LIMIT;
    private String zNotes = NO_NOTES;
    private transient WrappedMessageGenerator m_msgGenerator;
    private String m_subjectWrapperTemplate;
    private String m_bodyWrapperTemplate;
    private String m_headerName = "X-Spam-Flag";//To prevent assertion by LCString in initialized-only state
    private String m_isSpamHeaderValue;
    private String m_isHamHeaderValue;

    // constructors -----------------------------------------------------------

    protected SpamProtoConfig() { }

    protected SpamProtoConfig(boolean bScan,
                              int strength,
                              boolean blockSuperSpam,
                              int superSpamStrength,
                              String zNotes,
                              String subjectTemplate,
                              String bodyTemplate,
                              String headerName,
                              String isSpamHeaderValue,
                              String isHamHeaderValue) {
        this.bScan = bScan;
        this.strength = strength;
        this.blockSuperSpam = blockSuperSpam;
        this.superSpamStrength = superSpamStrength;
        this.zNotes = zNotes;
        m_subjectWrapperTemplate = subjectTemplate;
        m_bodyWrapperTemplate = bodyTemplate;
        m_headerName = headerName;
        m_isSpamHeaderValue = isSpamHeaderValue;
        m_isHamHeaderValue = isHamHeaderValue;

    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="config_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
        return;
    }

    /**
     * Get the name of the header (e.g. "X-SPAM") used to indicate the
     * SPAM/HAM value of this email
     */
    @Transient
    public String getHeaderName() {
        return m_headerName;
    }

    public void setHeaderName(String headerName) {
        m_headerName = headerName;
    }

    /**
     * Get the name of the header value (e.g. "YES") used to indicate
     * the SPAM/HAM value of this email
     */
    @Transient
    public String getHeaderValue(boolean isSpam) {
        return isSpam?m_isSpamHeaderValue:m_isHamHeaderValue;
    }

    /**
     * Set the value of the {@link #getHeaderName header} used when
     * the mail is HAM/SPAM
     *
     * @param headerValue the value of the header
     * @param isSpam true if this is the value for spam,
     *        false for ham
     */
    @Transient
    public void setHeaderValue(String headerValue, boolean isSpam) {
        if(isSpam) {
            m_isSpamHeaderValue = headerValue;
        }
        else {
            m_isHamHeaderValue = headerValue;
        }
    }


    /**
     * Get the template used to create the subject for a wrapped
     * message.
     */
    @Transient
    public String getSubjectWrapperTemplate() {
        return m_subjectWrapperTemplate;
    }

    public void setSubjectWrapperTemplate(String template) {
        m_subjectWrapperTemplate = template;
        ensureMessageGenerator();
        m_msgGenerator.setSubject(template);
    }

    /**
     * Get the template used to create the body for a wrapped message.
     */
    @Transient
    public String getBodyWrapperTemplate() {
        return m_bodyWrapperTemplate;
    }

    public void setBodyWrapperTemplate(String template) {
        m_bodyWrapperTemplate = template;
        ensureMessageGenerator();
        m_msgGenerator.setBody(template);
    }

    /**
     * Access the helper object, for wrapping messages based on the
     * values of the {@link #getSubjectWrapperTemplate subject} and
     * {@link #getBodyWrapperTemplate body} templates.
     */
    @Transient
    public WrappedMessageGenerator getMessageGenerator() {
        ensureMessageGenerator();
        return m_msgGenerator;
    }

    private void ensureMessageGenerator() {
        if(m_msgGenerator == null) {
            m_msgGenerator = new WrappedMessageGenerator(
                                                         getSubjectWrapperTemplate(),
                                                         getBodyWrapperTemplate());
        }
    }


    /**
     * scan: a boolean specifying whether or not to scan a message for
     * spam (defaults to true)
     *
     * @return whether or not to scan message for spam
     */
    @Column(nullable=false)
    public boolean getScan()
    {
        return bScan;
    }

    public void setScan(boolean bScan)
    {
        this.bScan = bScan;
        return;
    }

    /**
     * strength: an integer giving scan strength.  Divide by 10 to get
     * SpamAssassin strength.  Thus range should be something like: 30
     * to 100
     *
     * @return an <code>int</code> giving the spam strength * 10
     */
    @Column(nullable=false)
    public int getStrength()
    {
        return strength;
    }

    public void setStrength(int strength)
    {
        this.strength = strength;
    }

    @Column(name="block_superspam", nullable=false)
    public boolean getBlockSuperSpam()
    {
        return blockSuperSpam;
    }

    public void setBlockSuperSpam(boolean blockSuperSpam)
    {
        this.blockSuperSpam = blockSuperSpam;
    }

    @Column(name="superspam_strength", nullable=false)
    public int getSuperSpamStrength()
    {
        return superSpamStrength;
    }

    public void setSuperSpamStrength(int superSpamStrength)
    {
        this.superSpamStrength = superSpamStrength;
    }


    /**
     * msgSizeLimit: an integer giving scan message size limit.  Files
     * over this size are presumed not to be spam, and not scanned for
     * performance reasons.
     *
     * @return an <code>int</code> giving the spam message size limit
     * (cutoff) in bytes.
     */
    @Column(name="msg_size_limit", nullable=false)
    public int getMsgSizeLimit()
    {
        return msgSizeLimit;
    }

    public void setMsgSizeLimit(int msgSizeLimit)
    {
        this.msgSizeLimit = msgSizeLimit;
    }

    /**
     * notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this spam config
     */
    public String getNotes()
    {
        return zNotes;
    }

    public void setNotes(String zNotes)
    {
        this.zNotes = zNotes;
        return;
    }

    // Help for the UI follows.
    public static final int LOW_STRENGTH = 50;
    public static final int MEDIUM_STRENGTH = 43;
    public static final int HIGH_STRENGTH = 35;
    public static final int VERY_HIGH_STRENGTH = 33;
    public static final int EXTREME_STRENGTH = 30;
}
