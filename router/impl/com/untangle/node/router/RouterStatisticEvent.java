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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a Router statistics.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_router_statistic_evt", schema="events")
@SuppressWarnings("serial")
    public class RouterStatisticEvent extends StatisticEvent
    {

        /* Number of outbound nat sessions */
        private int natSessions           = 0;
        private int tcpIncomingRedirects  = 0;
        private int tcpOutgoingRedirects  = 0;
        private int udpIncomingRedirects  = 0;
        private int udpOutgoingRedirects  = 0;
        private int icmpIncomingRedirects = 0;
        private int icmpOutgoingRedirects = 0;
        private int dmzSessions           = 0;

        // Constructors
        public RouterStatisticEvent() {}

        public RouterStatisticEvent( int natSessions, int tcpIncomingRedirects, int tcpOutgoingRedirects,
                                  int udpIncomingRedirects,  int udpOutgoingRedirects,
                                  int icmpIncomingRedirects, int icmpOutgoingRedirects,
                                  int dmzSessions )
        {
            this.natSessions           = natSessions;
            this.tcpIncomingRedirects  = tcpIncomingRedirects;
            this.tcpOutgoingRedirects  = tcpOutgoingRedirects;
            this.udpIncomingRedirects  = udpIncomingRedirects;
            this.udpOutgoingRedirects  = udpOutgoingRedirects;
            this.icmpIncomingRedirects = icmpIncomingRedirects;
            this.icmpOutgoingRedirects = icmpOutgoingRedirects;
            this.dmzSessions           = dmzSessions;
        }

        /**
         * Number of natted connections since the last log event
         *
         * @return Number of natted connections since the last log event
         */
        @Column(name="nat_sessions", nullable=false)
        public int getRouterSessions()
        {
            return natSessions;
        }

        public void setRouterSessions( int natSessions )
        {
            this.natSessions = natSessions;
        }

        public void incrRouterSessions()
        {
            this.natSessions++;
        }

        /**
         * Number of tcp incoming redirects since the last log event
         *
         * @return Number of tcp incoming redirects since the last log event
         */
        @Column(name="tcp_incoming", nullable=false)
        public int getTcpIncomingRedirects()
        {
            return tcpIncomingRedirects;
        }

        public void setTcpIncomingRedirects( int tcpIncomingRedirects )
        {
            this.tcpIncomingRedirects = tcpIncomingRedirects;
        }

        public void incrTcpIncomingRedirects()
        {
            this.tcpIncomingRedirects++;
        }

        /**
         * Number of tcp outgoing redirects since the last log event
         *
         * @return Number of tcp outgoing redirects since the last log event
         */
        @Column(name="tcp_outgoing", nullable=false)
        public int getTcpOutgoingRedirects()
        {
            return tcpOutgoingRedirects;
        }

        public void setTcpOutgoingRedirects( int tcpOutgoingRedirects )
        {
            this.tcpOutgoingRedirects = tcpOutgoingRedirects;
        }

        public void incrTcpOutgoingRedirects()
        {
            this.tcpOutgoingRedirects++;
        }


        /**
         * Number of udp incoming redirects since the last log event
         *
         * @return Number of udp incoming redirects since the last log event
         */
        @Column(name="udp_incoming", nullable=false)
        public int getUdpIncomingRedirects()
        {
            return udpIncomingRedirects;
        }

        public void setUdpIncomingRedirects( int udpIncomingRedirects )
        {
            this.udpIncomingRedirects = udpIncomingRedirects;
        }

        public void incrUdpIncomingRedirects()
        {
            this.udpIncomingRedirects++;
        }

        /**
         * Number of udp outgoing redirects since the last log event
         *
         * @return Number of udp outgoing redirects since the last log event
         */
        @Column(name="udp_outgoing", nullable=false)
        public int getUdpOutgoingRedirects()
        {
            return udpOutgoingRedirects;
        }

        public void setUdpOutgoingRedirects( int udpOutgoingRedirects )
        {
            this.udpOutgoingRedirects = udpOutgoingRedirects;
        }

        public void incrUdpOutgoingRedirects()
        {
            this.udpOutgoingRedirects++;
        }

        /**
         * Number of icmp incoming redirects since the last log event
         *
         * @return Number of icmp incoming redirects since the last log event
         */
        @Column(name="icmp_incoming", nullable=false)
        public int getIcmpIncomingRedirects()
        {
            return icmpIncomingRedirects;
        }

        public void setIcmpIncomingRedirects( int icmpIncomingRedirects )
        {
            this.icmpIncomingRedirects = icmpIncomingRedirects;
        }

        public void incrIcmpIncomingRedirects()
        {
            this.icmpIncomingRedirects++;
        }

        /**
         * Number of icmp outgoing redirects since the last log event
         *
         * @return Number of icmp outgoing redirects since the last log event
         */
        @Column(name="icmp_outgoing", nullable=false)
        public int getIcmpOutgoingRedirects()
        {
            return icmpOutgoingRedirects;
        }

        public void setIcmpOutgoingRedirects( int icmpOutgoingRedirects )
        {
            this.icmpOutgoingRedirects = icmpOutgoingRedirects;
        }

        public void incrIcmpOutgoingRedirects()
        {
            this.icmpOutgoingRedirects++;
        }

        /**
         * Number of DMZd sessions since the last log event
         *
         * @return Number of DMZd sessions since the last log event
         */
        @Column(name="dmz_sessions", nullable=false)
        public int getDmzSessions()
        {
            return dmzSessions;
        }

        public void setDmzSessions( int dmzSessions )
        {
            this.dmzSessions = dmzSessions;
        }

        public void incrDmzSessions()
        {
            this.dmzSessions++;
        }

        /**
         * Returns true if any of the stats are non-zero, whenever all the
         * stats are zero, a new log event is not created.
         */
        public boolean hasStatistics()
        {
            return (( natSessions + dmzSessions +
                      tcpIncomingRedirects  + tcpOutgoingRedirects +
                      udpIncomingRedirects  + udpOutgoingRedirects +
                      icmpIncomingRedirects + icmpOutgoingRedirects ) > 0 );
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");
            sb.addField("nat-sessions", natSessions);
            sb.addField("tcp-incoming-redirects", tcpIncomingRedirects);
            sb.addField("tcp-outgoing-redirects", tcpOutgoingRedirects);
            sb.addField("udp-incoming-redirects", udpIncomingRedirects);
            sb.addField("udp-outgoing-redirects", udpOutgoingRedirects);
            sb.addField("icmp-incoming-redirects", icmpIncomingRedirects);
            sb.addField("icmp-outgoing-redirects", icmpOutgoingRedirects);
            sb.addField("dmz-sessions", dmzSessions);
        }

        @Transient
        public String getSyslogId()
        {
            return "Statistic";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.INFORMATIONAL; // statistics or normal operation
        }
    }
