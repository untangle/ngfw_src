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

package com.untangle.node.nat;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Log event for a DHCP absolute event .
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_nat_evt_dhcp_abs", schema="events")
    public class DhcpAbsoluteEvent extends LogEvent implements Serializable
    {
        private List<DhcpAbsoluteLease> absoluteLeaseList = null;

        public DhcpAbsoluteEvent() {}

        public DhcpAbsoluteEvent( List s )
        {
            absoluteLeaseList = s;
        }

        /**
         * List of the absolute leases associated with the event.
         *
         * @return the list of the redirect rules.
         */
        @OneToMany(fetch=FetchType.EAGER)
        @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                       org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
        @JoinTable(name="tr_nat_evt_dhcp_abs_leases",
                   joinColumns=@JoinColumn(name="event_id"),
                   inverseJoinColumns=@JoinColumn(name="lease_id"))
        @IndexColumn(name="position")
        public List<DhcpAbsoluteLease> getAbsoluteLeaseList()
        {
            return absoluteLeaseList;
        }

        public void setAbsoluteLeaseList( List<DhcpAbsoluteLease> s )
        {
            absoluteLeaseList = s;
        }

        void addAbsoluteLease( DhcpAbsoluteLease lease )
        {
            if ( absoluteLeaseList == null ) {
                absoluteLeaseList = new LinkedList();
            }

            absoluteLeaseList.add( lease );
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            /* Don't log anything if this is null, this can be null at startup */
            if ( this.absoluteLeaseList == null ) return;

            sb.startSection("info");
            sb.addField("num-leases", absoluteLeaseList.size());

            // there is no reason to log each absolute lease to sys log
            // - an absolute lease can be used for auditing and reporting later
            //   but is not useful to add to sys log (see rbscott for more info)
        }

        @Transient
        public String getSyslogId()
        {
            return "DHCP_AbsoluteLeases";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.INFORMATIONAL; // statistics or normal operation
        }
    }
