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

package com.untangle.mvvm.shield;

import java.io.Serializable;
import java.net.InetAddress;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import org.hibernate.annotations.Type;

/**
 * Log event for the shield rejection.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="shield_rejection_evt", schema="events")
    public class ShieldRejectionEvent extends LogEvent implements Serializable
    {
        private InetAddress clientAddr;
        private byte        clientIntf;
        private double      reputation;
        private int         mode;
        private int         limited;
        private int         rejected;
        private int         dropped;

        // Constructors
        public ShieldRejectionEvent() { }

        public ShieldRejectionEvent( InetAddress clientAddr, byte clientIntf, double reputation, int mode,
                                     int limited, int dropped, int rejected )
        {
            this.clientAddr = clientAddr;
            this.clientIntf = clientIntf;
            this.reputation = reputation;
            this.mode       = mode;
            this.limited    = limited;
            this.rejected   = rejected;
            this.dropped    = dropped;
        }


        /**
         * IP of the user that generated the event
         *
         * @return the identity of the user that generated the event
         */
        @Column(name="client_addr")
        @Type(type="com.untangle.mvvm.type.InetAddressUserType")
        public InetAddress getClientAddr()
        {
            return this.clientAddr;
        }

        public void setClientAddr( InetAddress clientAddr )
        {
            this.clientAddr = clientAddr;
        }

        /**
         * Interface where all of the events were received.
         *
         * @return the identity of the user that generated the event
         */
        @Column(name="client_intf", nullable=false)
        public byte getClientIntf()
        {
            return this.clientIntf;
        }

        public void setClientIntf( byte clientIntf )
        {
            this.clientIntf = clientIntf;
        }

        /**
         * Reputation of the user at the time of the event.
         *
         * @return reputation at the time of the event.
         */
        @Column(nullable=false)
        public double getReputation()
        {
            return reputation;
        }

        public void setReputation( double reputation )
        {
            this.reputation = reputation;
        }

        /**
         * Mode of the system when this event occured.
         *
         * @return Mode of the system at the time of the event.
         */
        @Column(nullable=false)
        public int getMode()
        {
            return mode;
        }

        public void setMode( int mode )
        {
            this.mode = mode;
        }

        /**
         * Number of limited sessions since the last time the user generated an event.
         *
         * @return reputation at the time of the event.
         */
        @Column(nullable=false)
        public int getLimited()
        {
            return limited;
        }

        public void setLimited( int limited )
        {
            this.limited = limited;
        }

        /**
         * Number of rejected sessions since the last time the user generated an event.
         *
         * @return reputation at the time of the event.
         */
        @Column(nullable=false)
        public int getRejected()
        {
            return rejected;
        }

        public void setRejected( int rejected )
        {
            this.rejected = rejected;
        }

        /**
         * Number of dropped sessions since the last time the user generated an event.
         *
         * @return reputation at the time of the event.
         */
        @Column(nullable=false)
        public int getDropped()
        {
            return dropped;
        }

        public void setDropped( int dropped )
        {
            this.dropped = dropped;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");
            sb.addField("client-addr", clientAddr);
            sb.addField("client-iface", clientIntf);
            sb.addField("reputation", reputation);
            sb.addField("mode", mode);
            sb.addField("limited", limited);
            sb.addField("rejected", rejected);
            sb.addField("dropped", dropped);
        }

        @Transient
        public String getSyslogId()
        {
            return "Shield_Rejection";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.WARNING; // traffic altered
        }
    }
