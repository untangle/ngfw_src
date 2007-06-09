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

package com.untangle.tran.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.IPMaddr;
import com.untangle.mvvm.tran.PipelineEndpoints;
import org.hibernate.annotations.Type;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_spyware_evt_access", schema="events")
    public class SpywareAccessEvent extends SpywareEvent
    {
        private PipelineEndpoints pipelineEndpoints;
        private String identification;
        private IPMaddr ipMaddr; // location
        private boolean blocked;

        // constructors -----------------------------------------------------------

        public SpywareAccessEvent() { }

        public SpywareAccessEvent(PipelineEndpoints pe,
                                  String identification,
                                  IPMaddr ipMaddr,
                                  boolean blocked)
        {
            this.pipelineEndpoints = pe;
            this.identification = identification;
            this.ipMaddr = ipMaddr;
            this.blocked = blocked;
        }

        // SpywareEvent methods ---------------------------------------------------

        @Transient
        public String getType()
        {
            return "Access";
        }

        @Transient
        public String getReason()
        {
            return "in Subnet List";
        }

        @Transient
        public String getLocation()
        {
            return ipMaddr.toString();
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
         * An address or subnet.
         *
         * @return the IPMaddr.
         */
        @Type(type="com.untangle.mvvm.type.IPMaddrUserType")
        public IPMaddr getIpMaddr()
        {
            return ipMaddr;
        }

        public void setIpMaddr(IPMaddr ipMaddr)
        {
            this.ipMaddr = ipMaddr;
        }

        /**
         * The identification (domain matched)
         *
         * @return the protocl name.
         */
        @Column(name="ident")
        public String getIdentification()
        {
            return identification;
        }

        public void setIdentification(String identification)
        {
            this.identification = identification;
        }

        /**
         * Whether or not we blocked it.
         *
         * @return whether or not the session was blocked (closed)
         */
        @Column(nullable=false)
        public boolean isBlocked()
        {
            return blocked;
        }

        public void setBlocked(boolean blocked)
        {
            this.blocked = blocked;
        }

        // Syslog methods ---------------------------------------------------------

        // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
    }
