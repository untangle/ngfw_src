/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router;


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
@Table(name="n_router_evt_dhcp_abs", schema="events")
@SuppressWarnings("serial")
    public class DhcpAbsoluteEvent extends LogEvent implements Serializable
    {
        private List<DhcpAbsoluteLease> absoluteLeaseList = null;

        public DhcpAbsoluteEvent() {}

        public DhcpAbsoluteEvent( List<DhcpAbsoluteLease> s )
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
        @JoinTable(name="n_router_evt_dhcp_abs_leases",
                   joinColumns=@JoinColumn(name="event_id"),
                   inverseJoinColumns=@JoinColumn(name="lease_id"))
        @IndexColumn(name="position")
        public List<DhcpAbsoluteLease> getAbsoluteLeaseList()
        {
            absoluteLeaseList.removeAll(java.util.Collections.singleton(null));
            return absoluteLeaseList;
        }

        public void setAbsoluteLeaseList( List<DhcpAbsoluteLease> s )
        {
            absoluteLeaseList = s;
        }

        void addAbsoluteLease( DhcpAbsoluteLease lease )
        {
            if ( absoluteLeaseList == null ) {
                absoluteLeaseList = new LinkedList<DhcpAbsoluteLease>();
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
