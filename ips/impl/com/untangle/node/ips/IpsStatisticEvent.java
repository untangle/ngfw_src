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

package com.untangle.node.ips;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a Ips statistics.
 *
 * @author <a href="mailto:nchilders@untangle.com">nchilders</a>
 * @stolen from rbscott yo
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_ips_statistic_evt", schema="events")
    public class IpsStatisticEvent extends StatisticEvent {
        
    
        private int dnc = 0; // did-not-care
        private int logged = 0; // logged or alerted
        private int blocked = 0;

        // Constructors
        public IpsStatisticEvent() { }

        public IpsStatisticEvent( int dnc, int logged, int blocked )
        {
            this.dnc = dnc;
            this.logged  = logged;
            this.blocked = blocked;
        }

        /**
         * Number of dnc chunks (did-not-cares are not logged or blocked)
         *
         * @return Number of dnc chunks
         */
        @Column(nullable=false)
        public int getDNC() { return dnc; }
        public void setDNC( int dnc ) { this.dnc = dnc; }
        public void incrDNC() { dnc++; }

        /**
         * Number of logged chunks
         *
         * @return Number of logged chunks
         */
        @Column(nullable=false)
        public int getLogged() { return logged; }
        public void setLogged(int logged) { this.logged = logged; }
        public void incrLogged() { logged++; }

        /**
         * Number of blocked chunks
         *
         * @return Number of blocked chunks
         */
        @Column(nullable=false)
        public int getBlocked() { return blocked; }
        public void setBlocked(int blocked) { this.blocked = blocked; }
        public void incrBlocked() { blocked++; }

        /**
         * Returns true if any of the stats are non-zero, whenever all the stats are zero,
         * a new log event is not created.
         */
        public boolean hasStatistics() { return ((dnc + logged + blocked) > 0 ); }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");
            sb.addField("did-not-care", dnc);
            sb.addField("logged", logged);
            sb.addField("blocked", blocked);
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
