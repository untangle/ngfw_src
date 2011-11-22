package com.untangle.uvm.node;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.policy.Policy;

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
@SuppressWarnings("serial")
    public class PipelineStats extends PipelineEvent
    {

        private int sessionId;

        private short protocol;

        private int clientIntf;
        private int serverIntf;

        private InetAddress cClientAddr;
        private InetAddress sClientAddr;

        private InetAddress cServerAddr;
        private InetAddress sServerAddr;

        private int cClientPort;
        private int sClientPort;

        private int cServerPort;
        private int sServerPort;

        private Policy policy;

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

        public PipelineStats(SessionStats begin, SessionStats end, PipelineEndpoints pe, String uid)
        {
            super(pe);

            sessionId = pe.getSessionId();

            protocol = pe.getProtocol();

            cClientAddr = pe.getCClientAddr();
            cClientPort = pe.getCClientPort();
            cServerAddr = pe.getCServerAddr();
            cServerPort = pe.getCServerPort();

            sClientAddr = pe.getSClientAddr();
            sClientPort = pe.getSClientPort();
            sServerAddr = pe.getSServerAddr();
            sServerPort = pe.getSServerPort();

            clientIntf = pe.getClientIntf();
            serverIntf = pe.getServerIntf();

            policy = pe.getPolicy();

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
         * Session id.
         *
         * @return the id of the session
         */
        @Column(name="session_id", nullable=false)
            public int getSessionId()
        {
            return sessionId;
        }

        public void setSessionId(int sessionId)
        {
            this.sessionId = sessionId;
        }

        /**
         * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
         *
         * @return the id of the session
         */
        @Column(name="proto", nullable=false)
            public short getProtocol()
        {
            return protocol;
        }

        public void setProtocol(short protocol)
        {
            this.protocol = protocol;
        }

        /**
         * Client interface number (at client).  (0 outside, 1 inside)
         *
         * @return the number of the interface of the client
         */
        @Column(name="client_intf", nullable=false)
            public int getClientIntf()
        {
            return clientIntf;
        }

        public void setClientIntf(int clientIntf)
        {
            this.clientIntf = clientIntf;
        }

        @Transient
            public String getClientIntf(int clientInf)
        {
            return 0 == clientIntf ? "outside" : "inside";
        }

        /**
         * Server interface number (at server).  (0 outside, 1 inside)
         *
         * @return the number of the interface of the server
         */
        @Column(name="server_intf", nullable=false)
            public int getServerIntf()
        {
            return serverIntf;
        }

        public void setServerIntf(int serverIntf)
        {
            this.serverIntf = serverIntf;
        }

        @Transient
            public String getServerIntf(int serverIntf)
        {
            return 0 == serverIntf ? "outside" : "inside";
        }

        /**
         * Client address, at the client side.
         *
         * @return the address of the client (as seen at client side of pipeline)
         */
        @Column(name="c_client_addr")
            @Type(type="com.untangle.uvm.type.InetAddressUserType")
            public InetAddress getCClientAddr()
        {
            return cClientAddr;
        }

        public void setCClientAddr(InetAddress cClientAddr)
        {
            this.cClientAddr = cClientAddr;
        }

        /**
         * Client address, at the server side.
         *
         * @return the address of the client (as seen at server side of pipeline)
         */
        @Column(name="s_client_addr")
            @Type(type="com.untangle.uvm.type.InetAddressUserType")
            public InetAddress getSClientAddr()
        {
            return sClientAddr;
        }

        public void setSClientAddr(InetAddress sClientAddr)
        {
            this.sClientAddr = sClientAddr;
        }

        /**
         * Server address, at the client side.
         *
         * @return the address of the server (as seen at client side of pipeline)
         */
        @Column(name="c_server_addr")
            @Type(type="com.untangle.uvm.type.InetAddressUserType")
            public InetAddress getCServerAddr()
        {
            return cServerAddr;
        }

        public void setCServerAddr(InetAddress cServerAddr)
        {
            this.cServerAddr = cServerAddr;
        }

        /**
         * Server address, at the server side.
         *
         * @return the address of the server (as seen at server side of pipeline)
         */
        @Column(name="s_server_addr")
            @Type(type="com.untangle.uvm.type.InetAddressUserType")
            public InetAddress getSServerAddr()
        {
            return sServerAddr;
        }

        public void setSServerAddr(InetAddress sServerAddr)
        {
            this.sServerAddr = sServerAddr;
        }

        /**
         * Client port, at the client side.
         *
         * @return the port of the client (as seen at client side of pipeline)
         */
        @Column(name="c_client_port", nullable=false)
            public int getCClientPort()
        {
            return cClientPort;
        }

        public void setCClientPort(int cClientPort)
        {
            this.cClientPort = cClientPort;
        }

        /**
         * Client port, at the server side.
         *
         * @return the port of the client (as seen at server side of pipeline)
         */
        @Column(name="s_client_port", nullable=false)
            public int getSClientPort()
        {
            return sClientPort;
        }

        public void setSClientPort(int sClientPort)
        {
            this.sClientPort = sClientPort;
        }

        /**
         * Server port, at the client side.
         *
         * @return the port of the server (as seen at client side of pipeline)
         */
        @Column(name="c_server_port", nullable=false)
            public int getCServerPort()
        {
            return cServerPort;
        }

        public void setCServerPort(int cServerPort)
        {
            this.cServerPort = cServerPort;
        }

        /**
         * Server port, at the server side.
         *
         * @return the port of the server (as seen at server side of pipeline)
         */
        @Column(name="s_server_port", nullable=false)
            public int getSServerPort()
        {
            return sServerPort;
        }

        public void setSServerPort(int sServerPort)
        {
            this.sServerPort = sServerPort;
        }

        /**
         * Policy that was applied for this pipeline.
         *
         * @return Policy for this pipeline
         */
        @ManyToOne(fetch=FetchType.EAGER)
            @JoinColumn(name="policy_id")
            public Policy getPolicy()
        {
            return policy;
        }

        public void setPolicy(Policy policy)
        {
            this.policy = policy;
        }


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
