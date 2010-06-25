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

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.untangle.node.mail.papi.WrappedMessageGenerator;

/**
 * Base class for Virus config information for the differernt email
 * protocols.
 */
@SuppressWarnings("serial")
@MappedSuperclass
public abstract class VirusMailConfig implements Serializable {

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private boolean bScan = false;
    private String zNotes = NO_NOTES;
    private transient WrappedMessageGenerator m_msgGenerator;
    private String m_subjectWrapperTemplate;
    private String m_bodyWrapperTemplate;

    // constructor ------------------------------------------------------------

    public VirusMailConfig() { }

    public VirusMailConfig(boolean bScan,
                           String notes,
                           String subjectWrapperTemplate,
                           String bodyWrapperTemplate)
    {
        this.bScan = bScan;
        this.zNotes = notes;
        m_subjectWrapperTemplate = subjectWrapperTemplate;
        m_bodyWrapperTemplate = bodyWrapperTemplate;
    }

    /**
     * Get the tempalte used to create the subject for a wrapped
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
     * Get the tempalte used to create the body for a wrapped message.
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
     * scan: a boolean specifying whether or not to scan a message for
     * virus (defaults to true)
     *
     * @return whether or not to scan message for virus
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

    public String getNotes()
    {
        return zNotes;
    }

    public void setNotes(String zNotes)
    {
        this.zNotes = zNotes;
        return;
    }
}
