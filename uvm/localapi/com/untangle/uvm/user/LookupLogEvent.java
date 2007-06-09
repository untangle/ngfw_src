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

package com.untangle.uvm.user;

import java.net.InetAddress;
import java.sql.Timestamp;
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
import org.hibernate.annotations.Type;

@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="u_lookup_evt", schema="events")
    public class LookupLogEvent extends LogEvent
    {
        private long lookupKey;
        private InetAddress address;
        private String username;
        private String hostname;
        private Date lookupTime;

        public LookupLogEvent()
        {
        }

        LookupLogEvent( UserInfo info )
        {
            this.lookupKey = info.getLookupKey();
            this.address   = info.getAddress();
            Username u = info.getUsername();
            HostName h = info.getHostname();
            this.username = ( u == null ) ? "" : u.toString();
            this.hostname = ( h == null ) ? "" : h.toString();
            this.lookupTime = info.getLookupTime();
        }

        @Column(name="lookup_key")
        public long getLookupKey()
        {
            return this.lookupKey;
        }

        public void setLookupKey( long newValue )
        {
            this.lookupKey = newValue;
        }

        @Column(name="address")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getAddress()
        {
            return this.address;
        }

        public void setAddress( InetAddress newValue )
        {
            this.address = newValue;
        }

        @Column(name="username")
        public String getUsername()
        {
            return this.username;
        }

        public void setUsername( String newValue )
        {
            this.username = newValue;
        }

        @Column(name="hostname")
        public String getHostname()
        {
            return this.hostname;
        }

        public void setHostname( String newValue )
        {
            this.hostname = newValue;
        }

        /**
         * Time the lookup was initiated.
         *
         * @return time lookup started.
         */
        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="lookup_time")
        public Date getLookupTime()
        {
            return lookupTime;
        }

        public void setLookupTime(Date newValue)
        {
            if (newValue instanceof Timestamp) newValue = new Date(newValue.getTime());

            this.lookupTime = newValue;
        }


        public void appendSyslog(SyslogBuilder sb)
        {
        }


        @Transient
        public String getSyslogId()
        {
            return "Phonebook";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.DEBUG; // traffic altered
        }
    }
