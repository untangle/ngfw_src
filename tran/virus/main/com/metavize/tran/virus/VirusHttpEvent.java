/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusHttpEvent.java,v 1.1 2005/01/29 06:24:50 amread Exp $
 */

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.http.RequestLine;

/**
 * Log for Virus events.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT_HTTP"
 */
public class VirusHttpEvent extends LogEvent
{
    private RequestLine requestLine;
    private VirusScannerResult result;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusHttpEvent() { }

    public VirusHttpEvent(RequestLine requestLine, VirusScannerResult result)
    {
        this.requestLine = requestLine;
        this.result = result;
    }

    // accessors --------------------------------------------------------------

    /**
     * Corresponding request line.
     *
     * @return the request line.
     * @hibernate.many-to-one
     * column="REQUEST_LINE"
     * cascade="all"
     */
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestline)
    {
        this.requestLine = requestLine;
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
