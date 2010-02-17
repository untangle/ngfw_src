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

package com.untangle.uvm.node;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;

/**
 * Used to record the Session stats at session end time.
 * PipelineStats and PipelineEndpoints used to be the PiplineInfo
 * object.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="pl_stats", schema="events")
    public class PipelineStats extends PipelineEvent
    {
        private static final long serialVersionUID = 2479594766473917892L;

        private long c2pBytes = 0;
        private long p2sBytes = 0;
        private long s2pBytes = 0;
        private long p2cBytes = 0;

        private long c2pChunks = 0;
        private long p2sChunks = 0;
        private long s2pChunks = 0;
        private long p2cChunks = 0;

        private String uid;

        // constructors -----------------------------------------------------------

        public PipelineStats() { }

        public PipelineStats(IPSessionDesc begin, IPSessionDesc end,
                             PipelineEndpoints pe, String uid)
        {
            super(pe);

            c2pBytes = begin.c2tBytes();
            p2cBytes = begin.t2cBytes();
            c2pChunks = begin.c2tChunks();
            p2cChunks = begin.t2cChunks();

            p2sBytes = end.t2sBytes();
            s2pBytes = end.s2tBytes();
            p2sChunks = end.t2sChunks();
            s2pChunks = end.s2tChunks();

            this.uid = uid;
        }

        // accessors --------------------------------------------------------------

        /**
         * Total bytes send from client to pipeline
         *
         * @return the number of bytes sent from the client into the pipeline
         */
        @Column(name="c2p_bytes", nullable=false)
        public long getC2pBytes()
        {
            return c2pBytes;
        }

        public void setC2pBytes(long c2pBytes)
        {
            this.c2pBytes = c2pBytes;
        }

        /**
         * Total bytes send from server to pipeline
         *
         * @return the number of bytes sent from the server into the pipeline
         */
        @Column(name="s2p_bytes", nullable=false)
        public long getS2pBytes()
        {
            return s2pBytes;
        }

        public void setS2pBytes(long s2pBytes)
        {
            this.s2pBytes = s2pBytes;
        }

        /**
         * Total bytes send from pipeline to client
         *
         * @return the number of bytes sent from the pipeline to the client
         */
        @Column(name="p2c_bytes", nullable=false)
        public long getP2cBytes()
        {
            return p2cBytes;
        }

        public void setP2cBytes(long p2cBytes)
        {
            this.p2cBytes = p2cBytes;
        }

        /**
         * Total bytes send from pipeline to server
         *
         * @return the number of bytes sent from the pipeline to the server
         */
        @Column(name="p2s_bytes", nullable=false)
        public long getP2sBytes()
        {
            return p2sBytes;
        }

        public void setP2sBytes(long p2sBytes)
        {
            this.p2sBytes = p2sBytes;
        }

        /**
         * Total chunks send from client to pipeline
         *
         * @return the number of chunks sent from the client into the pipeline
         */
        @Column(name="c2p_chunks", nullable=false)
        public long getC2pChunks()
        {
            return c2pChunks;
        }

        public void setC2pChunks(long c2pChunks)
        {
            this.c2pChunks = c2pChunks;
        }

        /**
         * Total chunks send from server to pipeline
         *
         * @return the number of chunks sent from the server into the pipeline
         */
        @Column(name="s2p_chunks", nullable=false)
        public long getS2pChunks()
        {
            return s2pChunks;
        }

        public void setS2pChunks(long s2pChunks)
        {
            this.s2pChunks = s2pChunks;
        }

        /**
         * Total chunks send from pipeline to client
         *
         * @return the number of chunks sent from the pipeline to the client
         */
        @Column(name="p2c_chunks", nullable=false)
        public long getP2cChunks()
        {
            return p2cChunks;
        }

        public void setP2cChunks(long p2cChunks)
        {
            this.p2cChunks = p2cChunks;
        }

        /**
         * Total chunks send from pipeline to server
         *
         * @return the number of chunks sent from the pipeline to the server
         */
        @Column(name="p2s_chunks", nullable=false)
        public long getP2sChunks()
        {
            return p2sChunks;
        }

        public void setP2sChunks(long p2sChunks)
        {
            this.p2sChunks = p2sChunks;
        }

        /**
         * Login used for session
         *
         * @return a <code>String</code> giving the uid for the user (null if unknown)
         */
        public String getUid()
        {
            return uid;
        }

        public void setUid(String uid)
        {
            this.uid = uid;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            getPipelineEndpoints().appendSyslog(sb);

            sb.startSection("stats");
            sb.addField("raze-date", getTimeStamp());
            sb.addField("c2pBytes", c2pBytes);
            sb.addField("p2sBytes", p2sBytes);
            sb.addField("s2pBytes", s2pBytes);
            sb.addField("p2cBytes", p2cBytes);
            sb.addField("c2pChunks", c2pChunks);
            sb.addField("p2sChunks", p2sChunks);
            sb.addField("s2pChunks", s2pChunks);
            sb.addField("p2cChunks", p2cChunks);
            sb.addField("uid", uid);
        }

        // reuse default getSyslogId
        // reuse default getSyslogPriority
    }
