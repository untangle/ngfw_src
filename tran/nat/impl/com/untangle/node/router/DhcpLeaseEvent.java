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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.firewall.MACAddress;
import org.hibernate.annotations.Type;

/**
 * Log event for a DHCP lease event.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_nat_evt_dhcp", schema="events")
    public class DhcpLeaseEvent extends LogEvent
    {
        private static final long serialVersionUID = -6582660598334287365L;

        static final int REGISTER = 0;
        static final int RENEW    = 1;
        static final int EXPIRE   = 2;
        static final int RELEASE  = 3;

        private MACAddress mac;
        private HostName   hostname;
        private IPaddr     ip;
        private Date       endOfLease;
        private int        eventType;

        // Constructors
        public DhcpLeaseEvent() { }

        /**
         * XXX Event type should be an enumeration or something */
        public DhcpLeaseEvent( DhcpLease lease, int eventType  )
        {
            this.endOfLease = lease.getEndOfLease();
            this.mac        = lease.getMac();
            this.ip         = lease.getIP();
            this.hostname   = lease.getHostname();
            this.eventType  = eventType;
        }

        /**
         * MAC address
         *
         * @return the mac address.
         */
        @Type(type="com.untangle.uvm.type.firewall.MACAddressUserType")
        public MACAddress getMac()
        {
            return mac;
        }

        public void setMac( MACAddress mac )
        {
            this.mac = mac;
        }

        /**
         * Host name
         *
         * @return the host name.
         */
        @Type(type="com.untangle.uvm.type.HostNameUserType")
        public HostName getHostname()
        {
            return hostname;
        }

        public void setHostname( HostName hostname )
        {
            this.hostname = hostname;
        }

        /**
         * Get IP address for this lease
         *
         * @return desired static address.
         */
        @Type(type="com.untangle.uvm.type.IPaddrUserType")
        public IPaddr getIP()
        {
            return this.ip;
        }

        public void setIP( IPaddr ip )
        {
            this.ip = ip;
        }

        /**
         * Expiration date of the lease.
         *
         * @return expiration date.
         */
        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="end_of_lease")
        public Date getEndOfLease()
        {
            return endOfLease;
        }

        public void setEndOfLease( Date endOfLease )
        {
            this.endOfLease = endOfLease;
        }

        /**
         * State of the lease.
         *
         * @return expiration date.
         */
        @Column(name="event_type", nullable=false)
        public int getEventType()
        {
            return eventType;
        }

        public void setEventType( int eventType )
        {
            this.eventType = eventType;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");

            sb.addField("mac", mac.toString());
            sb.addField("hostname", hostname.toString());
            sb.addField("ip", ip.toString());
            sb.addField("lease-end", endOfLease);
            String type;
            if (REGISTER == eventType) {
                type = "register";
            } else if (RENEW == eventType) {
                type = "renew";
            } else if (EXPIRE == eventType) {
                type = "expire";
            } else if (RELEASE == eventType) {
                type = "release";
            } else {
                type = "unknown";
            }

            sb.addField("type", type);
        }

        @Transient
        public String getSyslogId()
        {
            return "DHCP_Lease";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.INFORMATIONAL; // statistics or normal operation
        }
    }
