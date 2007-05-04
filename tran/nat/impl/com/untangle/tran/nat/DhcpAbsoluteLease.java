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

package com.untangle.tran.nat;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.firewall.MACAddress;
import org.hibernate.annotations.Type;

/**
 * Log event for a DHCP lease event.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="dhcp_abs_lease", schema="events")
    public class DhcpAbsoluteLease
    {
        static final int REGISTERED = 0;
        static final int EXPIRED    = 1;

        private Long id;
        private MACAddress mac;
        private HostName   hostname;
        private IPaddr     ip;
        private Date       endOfLease;
        private int        eventType;

        // Constructors
        public DhcpAbsoluteLease() { }

        /**
         * XXX Event type should be an enumeration or something */
        public DhcpAbsoluteLease( DhcpLease lease, Date now  )
        {
            this.endOfLease = lease.getEndOfLease();
            this.mac        = lease.getMac();
            this.ip         = lease.getIP();
            this.hostname   = lease.getHostname();
            this.eventType  = now.after( endOfLease ) ? EXPIRED : REGISTERED;
        }

        @Id
        @Column(name="event_id")
        @GeneratedValue
        protected Long getId()
        {
            return id;
        }

        protected void setId(Long id)
        {
            this.id = id;
        }

        /**
         * MAC address
         *
         * @return the mac address.
         */
        @Type(type="com.untangle.mvvm.type.firewall.MACAddressUserType")
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
        @Type(type="com.untangle.mvvm.type.HostNameUserType")
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
        @Type(type="com.untangle.mvvm.type.IPaddrUserType")
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
    }
