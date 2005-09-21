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

import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import java.io.Serializable;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_IMAP_CONFIG"
 */
public class VirusIMAPConfig implements Serializable
{
    private static final long serialVersionUID = 7520156745253589027L;

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private VirusMessageAction zMsgAction = VirusMessageAction.REMOVE;
    private boolean bScan = false;
    private String zNotes = NO_NOTES;
    private transient WrappedMessageGenerator m_msgGenerator;
    private String m_subjectWrapperTemplate;
    private String m_bodyWrapperTemplate;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusIMAPConfig() {}

    public VirusIMAPConfig(boolean bScan,
      VirusMessageAction zMsgAction,
      String zNotes,
      String subjectTemplate,
      String bodyTemplate)
    {
        this.bScan = bScan;
        this.zMsgAction = zMsgAction;
        this.zNotes = zNotes;
        m_subjectWrapperTemplate = subjectTemplate;
        m_bodyWrapperTemplate = bodyTemplate;
    }

    // business methods ------------------------------------------------------

    /*
    public String render(String site, String category)
    {
        String message = BLOCK_TEMPLATE.replace("@HEADER@", header);
        message = message.replace("@SITE@", site);
        message = message.replace("@CATEGORY@", category);
        message = message.replace("@CONTACT@", contact);

        return message;
    }
    */

    // accessors --------------------------------------------------------------


    public String getSubjectWrapperTemplate() {
      return m_subjectWrapperTemplate;
    }
    
    public void setSubjectWrapperTemplate(String template) {
      m_subjectWrapperTemplate = template;
      ensureMessageGenerator();
      m_msgGenerator.setSubject(template);
    }
    
    public String getBodyWrapperTemplate() {
      return m_bodyWrapperTemplate;
    }
    
    public void setBodyWrapperTemplate(String template) {
      m_bodyWrapperTemplate = template;
      ensureMessageGenerator();
      m_msgGenerator.setBody(template);
    }

    /**
     * Access the helper object, for wrapping messages
     * based on the values of the
     * {@link #getSubjectWrapperTemplate subject} and
     * {@link #getBodyWrapperTemplate body} templates.
     *
     *
     */
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
     * @hibernate.id
     * column="CONFIG_ID"
     * generator-class="native"
     * not-null="true"
     */
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
     * scan: a boolean specifying whether or not to scan a message for virus (defaults to true)
     *
     * @return whether or not to scan message for virus
     * @hibernate.property
     * column="SCAN"
     * not-null="true"
     */
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
     * messageAction: a string specifying a response if a message contains virus (defaults to CLEAN)
     * one of CLEAN or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="ACTION"
     * type="com.metavize.tran.virus.VirusMessageActionUserType"
     * not-null="true"
     */
    public VirusMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(VirusMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        VirusMessageAction[] azMsgAction = VirusMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

    /**
     * notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this virus config
     * @hibernate.property
     * column="NOTES"
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
}
