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
