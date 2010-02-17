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

package com.untangle.node.spam;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log for Spam SMTP RBL events.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_spam_smtp_rbl_evt", schema="events")
    public class SpamSmtpRblEvent extends PipelineEvent
    {
        private HostName hostname;
        private IPaddr ipAddr;
        private boolean skipped;

        // constructors -----------------------------------------------------------

        public SpamSmtpRblEvent() {}

        public SpamSmtpRblEvent(PipelineEndpoints plEndp, HostName hostname, IPaddr ipAddr, boolean skipped) {
            super(plEndp);
            this.hostname = hostname;
            this.ipAddr = ipAddr;
            this.skipped = skipped;
        }

        public SpamSmtpRblEvent(PipelineEndpoints plEndp, String hostnameS, InetAddress ipAddrIN, boolean skipped) {
            super(plEndp);
            try {
                this.hostname = HostName.parse(hostnameS);
            } catch (ParseException e) {
                this.hostname = HostName.getEmptyHostName();
            }
            this.ipAddr = new IPaddr(ipAddrIN);
            this.skipped = skipped;
        }

        // accessors --------------------------------------------------------------

        /**
         * Hostname of RBL service.
         *
         * @return hostname of RBL service.
         */
        @Column(nullable=false)
        @Type(type="com.untangle.uvm.type.HostNameUserType")
        public HostName getHostname() {
            return hostname;
        }

        public void setHostname(HostName hostname) {
            this.hostname = hostname;
            return;
        }

        /**
         * IP address of mail server listed on RBL service.
         *
         * @return IP address of mail server listed on RBL service.
         */
        @Column(nullable=false)
        @Type(type="com.untangle.uvm.type.IPaddrUserType")
        public IPaddr getIPAddr() {
            return ipAddr;
        }

        public void setIPAddr(IPaddr ipAddr) {
            this.ipAddr = ipAddr;
            return;
        }

        /**
         * Confirmed RBL hit but skipping rejection indicator.
         *
         * @return confirmed RBL hit but skipping rejection indicator.
         */
        @Column(nullable=false)
        public boolean getSkipped() {
            return skipped;
        }

        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
            return;
        }

        // Syslog methods ---------------------------------------------------------

        @Transient
        public void appendSyslog(SyslogBuilder sb)
        {
            // No longer log pipeline endpoints, they are not necessary anyway.
            // PipelineEndpoints pe = getPipelineEndpoints();
            /* unable to log this event */
            // pe.appendSyslog(sb);

            sb.startSection("info");
            sb.addField("hostname", getHostname().toString());
            sb.addField("ipaddr", getIPAddr().toString());
            sb.addField("skipped", getSkipped());
        }

        @Transient
        public String getSyslogId()
        {
            return "SMTP_RBL";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            // INFORMATIONAL = statistics or normal operation
            // WARNING = traffic altered
            return false == getSkipped() ? SyslogPriority.INFORMATIONAL : SyslogPriority.WARNING; // traffic altered
        }
    }
