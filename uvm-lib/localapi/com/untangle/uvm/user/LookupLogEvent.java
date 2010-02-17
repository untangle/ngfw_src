/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.HostName;

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
