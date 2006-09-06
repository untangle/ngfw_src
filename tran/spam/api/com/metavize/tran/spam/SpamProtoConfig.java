/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import java.io.Serializable;
import java.util.*;

/**
 * 
 *
 * Hibernate mappings for this class are in the MVVM resource
 * directory.
 */
public abstract class SpamProtoConfig implements Serializable
{
    public static final int DEFAULT_MESSAGE_SIZE_LIMIT = 1 << 18; 
    public static final String NO_NOTES = "no description";

    public static final int DEFAULT_STRENGTH = 50; 
    private Long id;

    /* settings */
    private boolean bScan = false;
    private int strength = DEFAULT_STRENGTH;
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
        String zNotes,
        String subjectTemplate,
        String bodyTemplate,
        String headerName,
        String isSpamHeaderValue,
        String isHamHeaderValue) {
        this.bScan = bScan;
        this.strength = strength;
        this.zNotes = zNotes;
        m_subjectWrapperTemplate = subjectTemplate;
        m_bodyWrapperTemplate = bodyTemplate;
        m_headerName = headerName;
        m_isSpamHeaderValue = isSpamHeaderValue;
        m_isHamHeaderValue = isHamHeaderValue;
        
    }

    // accessors --------------------------------------------------------------

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
     * Get the name of the header (e.g. "X-SPAM")
     * used to indicate the SPAM/HAM
     * value of this email
     */
    public String getHeaderName() {
      return m_headerName;
    }
    public void setHeaderName(String headerName) {
      m_headerName = headerName;
    }
    /**
     * Get the name of the header value (e.g. "YES")
     * used to indicate the SPAM/HAM
     * value of this email
     */
    public String getHeaderValue(boolean isSpam) {
      return isSpam?m_isSpamHeaderValue:m_isHamHeaderValue;
    }

    /**
     * Set the value of the {@link #getHeaderName header}
     * used when the mail is HAM/SPAM
     *
     *
     * param headerValue the value of the header
     * param isSpam true if this is the value for spam,
     *        false for ham
     */
    public void setHeaderValue(String headerValue, boolean isSpam) {
      if(isSpam) {
        m_isSpamHeaderValue = headerValue;
      }
      else {
        m_isHamHeaderValue = headerValue;
      }
    }
    

    /**
    * Get the tempalte used to create the subject
    * for a wrapped message.
    */
    public String getSubjectWrapperTemplate() {
      return m_subjectWrapperTemplate;
    }
  
    public void setSubjectWrapperTemplate(String template) {
      m_subjectWrapperTemplate = template;
      ensureMessageGenerator();
      m_msgGenerator.setSubject(template);
    }
  
    /**
    * Get the tempalte used to create the body
    * for a wrapped message.
    */
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
     * scan: a boolean specifying whether or not to scan a message for spam (defaults to true)
     *
     * @return whether or not to scan message for spam
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
     * strength: an integer giving scan strength.  Divide by 10 to get SpamAssassin strength.
     * Thus range should be something like: 30 to 100
     *
     * @return an <code>int</code> giving the spam strength * 10 
     * @hibernate.property
     * column="STRENGTH"
     * not-null="true"
     */
    public int getStrength()
    {
        return strength;
    }

    public void setStrength(int strength)
    {
        this.strength = strength;
    }

    /**
     * msgSizeLimit: an integer giving scan message size limit.  Files over this size are
     * presumed not to be spam, and not scanned for performance reasons.
     *
     * @return an <code>int</code> giving the spam message size limit (cutoff) in bytes.
     * @hibernate.property
     * column="MSG_SIZE_LIMIT"
     * not-null="true"
     */
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


    // Help for the UI follows.
    public static final int VERY_LOW_STRENGTH = 80;
    public static final int LOW_STRENGTH = 65;
    public static final int MEDIUM_STRENGTH = 50;
    public static final int HIGH_STRENGTH = 43;
    public static final int VERY_HIGH_STRENGTH = 35;
    public static final Map scanStrengthEnumeration = new LinkedHashMap();
    static {
        scanStrengthEnumeration.put("very low",  VERY_LOW_STRENGTH);
        scanStrengthEnumeration.put("low",       LOW_STRENGTH);
        scanStrengthEnumeration.put("medium",    MEDIUM_STRENGTH);
        scanStrengthEnumeration.put("high",      HIGH_STRENGTH);
        scanStrengthEnumeration.put("very high", VERY_HIGH_STRENGTH);
    }

    public void setStrengthByName(String strengthEnum)
    {
        Integer str = (Integer) scanStrengthEnumeration.get(strengthEnum);
        if (str != null)
            strength = str.intValue();
    }

    public String getStrengthByName()
    {
        for (Iterator iter = scanStrengthEnumeration.keySet().iterator(); iter.hasNext();) {
            String strname = (String) iter.next();
            int strval = ((Integer) scanStrengthEnumeration.get(strname)).intValue();
            if (strength >= strval)
                return (String) strname;
        }
        // Failsafe, shouldn't happen.
        return "very low";
    }

    private static final String[] SSE_PROTO = new String[0];


    public static String[] getScanStrengthEnumeration()
    {
        
        return (String[])scanStrengthEnumeration.keySet().toArray(SSE_PROTO);
    }
}
