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

import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.tran.http.HttpRequestEvent;
import com.metavize.tran.http.RequestLine;

/**
 * Log for Virus events.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT_HTTP"
 * mutable="false"
 */
public class VirusHttpEvent extends VirusEvent
{
    private RequestLine requestLine;
    private VirusScannerResult result;
    private String vendorName;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusHttpEvent()
    {
        System.out.println("HI");
    }

    public VirusHttpEvent(RequestLine requestLine, VirusScannerResult result,
                          String vendorName)
    {
        this.requestLine = requestLine;
        this.result = result;
        this.vendorName = vendorName;
    }

    // VirusEvent methods -----------------------------------------------------

    public String getType()
    {
        return "HTTP";
    }

    public String getLocation()
    {
        return null == requestLine ? "" : requestLine.getUrl().toString();
    }

    public boolean isInfected()
    {
        return !result.isClean();
    }

    public String getActionName()
    {
        if (result.isClean()) {
            return "clean";
        } else if (result.isVirusCleaned()) {
            return "cleaned";
        } else {
            return "blocked";
        }
    }

    public String getVirusName()
    {
        String n = result.getVirusName();

        return null == n ? "" : n;
    }

    public PipelineEndpoints getPipelineEndpoints()
    {
        if (null == requestLine) {
            return null;
        } else {
            HttpRequestEvent req = requestLine.getHttpRequestEvent();
            return null == req ? null : req.getPipelineEndpoints();
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * Corresponding request line.
     *
     * @return the request line.
     * @hibernate.many-to-one
     * column="REQUEST_LINE"
     * cascade="save-update"
     */
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     * @hibernate.property
     * cascade="save-update"
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

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     * @hibernate.property
     * column="VENDOR_NAME"
     */
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }
}
