/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusLogEvent.java,v 1.5 2005/03/15 02:11:52 amread Exp $
 */

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log for non-HTTP Virus events.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT"
 * mutable="false"
 */
public class VirusLogEvent extends LogEvent
{
    private int sessionId;
    private VirusScannerResult result;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusLogEvent() { }

    public VirusLogEvent(int sessionId, VirusScannerResult result)
    {
        this.sessionId = sessionId;
        this.result = result;
    }

    // accessors --------------------------------------------------------------

    /**
     * Session id.
     *
     * @return the session id.
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     * @hibernate.property
     * cascade="all"
     * type="com.metavize.tran.virus.VirusScannerResultUserType"
     * @hibernate.column
     * name="CLEAN"
     * @hibernate.column
     * name="VIRUS_NAME"
     * @hibernate.column
     * name="VIRUS_CLEANED"
     */
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }
}
