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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.node.http.RequestLine;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

/**
 * Log for Virus events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_virus_evt_http", schema="events")
    public class VirusHttpEvent extends VirusEvent
    {
        private RequestLine requestLine;
        private VirusScannerResult result;
        private String vendorName;

        // constructors -----------------------------------------------------------

        public VirusHttpEvent() { }

        public VirusHttpEvent(RequestLine requestLine, VirusScannerResult result,
                              String vendorName)
        {
            this.requestLine = requestLine;
            this.result = result;
            this.vendorName = vendorName;
        }

        // VirusEvent methods -----------------------------------------------------

        @Transient
        public String getType()
        {
            return "HTTP";
        }

        @Transient
        public String getLocation()
        {
            return null == requestLine ? "" : requestLine.getUrl().toString();
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

        @Transient
        public PipelineEndpoints getPipelineEndpoints()
        {
            return null == requestLine ? null : requestLine.getPipelineEndpoints();
        }

        // accessors --------------------------------------------------------------

        /**
         * Corresponding request line.
         *
         * @return the request line.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="request_line")
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
         */
        @Columns(columns = {
            @Column(name="clean"),
            @Column(name="virus_name"),
            @Column(name="virus_cleaned")
        })
        @Type(type="com.untangle.node.virus.VirusScannerResultUserType")
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
