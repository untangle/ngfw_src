/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PipelineInfo.java,v 1.10 2005/03/15 02:11:53 amread Exp $
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

import com.metavize.mvvm.argon.IPSessionDesc;

/**
 * Each session has one row in this table.  It records, for each
 * endpoint (client and server of the *overall* pipeline session, not
 * an individual transform), the endpoint information and statistics.
 * Rows are written at overall session razing time.
 *
 * This object is not filled out until the sesion is ended. It exists
 * primarily for logging purposes.
 *
 * XXX should this be tamper proof?
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="PIPELINE_INFO"
 * mutable="false"
 */
public class PipelineInfo implements Serializable
{
    private static final long serialVersionUID = -52807336282341566L;

    private Long id;
    private int sessionId;

    private short protocol;

    private Date createDate;
    private Date razeDate;

    private long c2pBytes = 0;
    private long p2sBytes = 0;
    private long s2pBytes = 0;
    private long p2cBytes = 0;

    private long c2pChunks = 0;
    private long p2sChunks = 0;
    private long s2pChunks = 0;
    private long p2cChunks = 0;

    private byte clientIntf;
    private byte serverIntf;

    private InetAddress cClientAddr;
    private InetAddress sClientAddr;

    private InetAddress cServerAddr;
    private InetAddress sServerAddr;

    private int cClientPort;
    private int sClientPort;

    private int cServerPort;
    private int sServerPort;

    // constructors -----------------------------------------------------------

    public PipelineInfo() { }

    public PipelineInfo(IPSessionDesc sessionDesc)
    {
        createDate = new Date();
        sessionId = sessionDesc.id();
        protocol = (short)sessionDesc.protocol();
    }

    // business methods -------------------------------------------------------

    public void update(IPSessionDesc begin, IPSessionDesc end)
    {
        razeDate = new Date();

        cClientAddr = begin.clientAddr();
        cClientPort = begin.clientPort();
        cServerAddr = begin.serverAddr();
        cServerPort = begin.serverPort();

        sClientAddr = end.clientAddr();
        sClientPort = end.clientPort();
        sServerAddr = end.serverAddr();
        sServerPort = end.serverPort();

        clientIntf = begin.clientIntf();
        serverIntf = end.serverIntf(); /* XXX never filled out */

        c2pBytes = begin.c2tBytes();
        p2cBytes = begin.t2cBytes();
        c2pChunks = begin.c2tChunks();
        p2cChunks = begin.t2cChunks();

        p2sBytes = end.t2sBytes();
        s2pBytes = end.s2tBytes();
        p2sChunks = end.t2sChunks();
        s2pChunks = end.s2tChunks();
    }

    // accessors --------------------------------------------------------------

    /**
     * Hibernate synthetic key.
     *
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Session id.
     *
     * @return the id of the session
     * @hibernate.property
     * column="SESSION_ID"
     */
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
     * @hibernate.property
     * column="PROTO"
     */
    public short getProtocol()
    {
        return protocol;
    }

    public void setProtocol(short protocol)
    {
        this.protocol = protocol;
    }


    /**
     * Time the session began
     *
     * @return the time the session began
     * @hibernate.property
     * column="CREATE_DATE"
     */
    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    /**
     * Time the session ended
     *
     * @return the time the session ended
     * @hibernate.property
     * column="RAZE_DATE"
     */
    public Date getRazeDate()
    {
        return razeDate;
    }

    public void setRazeDate(Date razeDate)
    {
        this.razeDate = razeDate;
    }

    /**
     * Total bytes send from client to pipeline
     *
     * @return the number of bytes sent from the client into the pipeline
     * @hibernate.property
     * column="C2P_BYTES"
     */
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
     * @hibernate.property
     * column="S2P_BYTES"
     */
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
     * @hibernate.property
     * column="P2C_BYTES"
     */
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
     * @hibernate.property
     * column="P2S_BYTES"
     */
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
     * @hibernate.property
     * column="C2P_CHUNKS"
     */
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
     * @hibernate.property
     * column="S2P_CHUNKS"
     */
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
     * @hibernate.property
     * column="P2C_CHUNKS"
     */
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
     * @hibernate.property
     * column="P2S_CHUNKS"
     */
    public long getP2sChunks()
    {
        return p2sChunks;
    }

    public void setP2sChunks(long p2sChunks)
    {
        this.p2sChunks = p2sChunks;
    }

    /**
     * Client interface number (at client).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the client
     * @hibernate.property
     * column="CLIENT_INTF"
     */
    public byte getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(byte clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Server interface number (at server).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the server
     * @hibernate.property
     * column="SERVER_INTF"
     */
    public byte getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(byte serverIntf)
    {
        this.serverIntf = serverIntf;
    }


    /**
     * Client address, at the client side.
     *
     * @return the address of the client (as seen at client side of pipeline)
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="C_CLIENT_ADDR"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="S_CLIENT_ADDR"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="C_SERVER_ADDR"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="S_SERVER_ADDR"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * column="C_CLIENT_PORT"
     */
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
     * @hibernate.property
     * column="S_CLIENT_PORT"
     */
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
     * @hibernate.property
     * column="C_SERVER_PORT"
     */
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
     * @hibernate.property
     * column="S_SERVER_PORT"
     */
    public int getSServerPort()
    {
        return sServerPort;
    }

    public void setSServerPort(int sServerPort)
    {
        this.sServerPort = sServerPort;
    }
}
