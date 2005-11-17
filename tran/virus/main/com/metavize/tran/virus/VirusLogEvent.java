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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log for non-mail, non-HTTP Virus events.  Currently just FTP.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT"
 * mutable="false"
 */
public class VirusLogEvent extends VirusEvent
{
    private PipelineEndpoints pipelineEndpoints;
    private VirusScannerResult result;
    private String vendorName;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusLogEvent() { }

    public VirusLogEvent(PipelineEndpoints pe, VirusScannerResult result,
                         String vendorName)
    {
        this.pipelineEndpoints = pe;
        this.result = result;
        this.vendorName = vendorName;
    }

    // VirusEvent methods -----------------------------------------------------

    public String getType()
    {
        return "FTP";
    }

    public String getLocation()
    {
        return pipelineEndpoints.getSServerAddr().getHostAddress();
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

    // accessors --------------------------------------------------------------

    /**
     * Get the PipelineEndpoints.
     *
     * @return the PipelineEndpoints.
     * @hibernate.many-to-one
     * column="PL_ENDP_ID"
     * not-null="true"
     * cascade="all"
     */
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
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
