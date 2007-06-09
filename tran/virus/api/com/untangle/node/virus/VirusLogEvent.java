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

package com.untangle.tran.virus;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.PipelineEndpoints;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

/**
 * Log for non-mail, non-HTTP Virus events.  Currently just FTP.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_virus_evt", schema="events")
    public class VirusLogEvent extends VirusEvent
    {
        private PipelineEndpoints pipelineEndpoints;
        private VirusScannerResult result;
        private String vendorName;

        // constructors -----------------------------------------------------------

        public VirusLogEvent() { }

        public VirusLogEvent(PipelineEndpoints pe, VirusScannerResult result,
                             String vendorName)
        {
            this.pipelineEndpoints = pe;
            this.result = result;
            this.vendorName = vendorName;
        }

        // VirusEvent methods -----------------------------------------------------

        @Transient
        public String getType()
        {
            return "FTP";
        }

        @Transient
        public String getLocation()
        {
            return null == pipelineEndpoints ? "" : pipelineEndpoints.getSServerAddr().getHostAddress();
        }

        @Transient
        public boolean isInfected()
        {
            return !result.isClean();
        }

        @Transient
        public int getActionType()
        {
            if (true == result.isClean()) {
                return PASSED;
            } else if (true == result.isVirusCleaned()) {
                return CLEANED;
            } else {
                return BLOCKED;
            }
        }

        @Transient
        public String getActionName()
        {
            switch(getActionType())
                {
                case PASSED:
                    return "clean";
                case CLEANED:
                    return "cleaned";
                default:
                case BLOCKED:
                    return "blocked";
                }
        }

        @Transient
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
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="pl_endp_id", nullable=false)
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
         */
        @Columns(columns = {
            @Column(name="clean"),
            @Column(name="virus_name"),
            @Column(name="virus_cleaned")
        })
        @Type(type="com.untangle.tran.virus.VirusScannerResultUserType")
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
         */
        @Column(name="vendor_name")
        public String getVendorName()
        {
            return vendorName;
        }

        public void setVendorName(String vendorName)
        {
            this.vendorName = vendorName;
        }
    }
