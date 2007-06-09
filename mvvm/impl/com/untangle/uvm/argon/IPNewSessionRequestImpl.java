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

package com.untangle.mvvm.argon;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Endpoint;
import com.untangle.jnetcap.Endpoints;

import com.untangle.mvvm.localapi.LocalIntfManager;
import com.untangle.mvvm.tran.PipelineEndpoints;

public abstract class IPNewSessionRequestImpl extends NewSessionRequestImpl implements IPNewSessionRequest
{
    protected InetAddress clientAddr;
    protected int clientPort;
    protected byte clientIntf;

    protected InetAddress serverAddr;
    protected int serverPort;
    final byte originalServerIntf;
    protected byte serverIntf;

    protected PipelineEndpoints pipelineEndpoints;

    protected byte state = REQUESTED;

    /* This is used to distinguish between REJECTED and REJECTED with code */
    protected byte code  = REJECTED;

    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());

    /* Two ways to create an IPNewSessionRequest:
     * A. Pass in the netcap session and get the parameters from there.
     */
    public IPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent,
                                    byte originalServerIntf, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent );

        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();

        Endpoint client = clientSide.client();
        Endpoint server = clientSide.server();

        /* Get the server and client from the client end of the endpoint from the netcap session */
        clientAddr = client.host();
        clientPort = client.port();

        LocalIntfManager lim = Argon.getInstance().getIntfManager();
        clientIntf = lim.toArgon( clientSide.interfaceId());
        serverIntf = lim.toArgon( serverSide.interfaceId());
        this.originalServerIntf = originalServerIntf;
        this.pipelineEndpoints = pe;

        serverAddr = server.host();
        serverPort = server.port();
    }

    /* Two ways to create an IPNewSessionRequest:
     * B. Pass in the previous request and get the parameters from there
     */
    public IPNewSessionRequestImpl( IPSession session, ArgonAgent agent, byte originalServerIntf, PipelineEndpoints pe )
    {
        super( session.sessionGlobalState(), agent);

        /* Get the server and client from the previous request */
        clientAddr = session.clientAddr();
        clientPort = session.clientPort();
        clientIntf = session.clientIntf();

        serverAddr = session.serverAddr();
        serverPort = session.serverPort();
        serverIntf = session.serverIntf();

        this.originalServerIntf = originalServerIntf;
        this.pipelineEndpoints = pe;
    }


    public short protocol()
    {
        return sessionGlobalState.protocol();
    }

    public InetAddress clientAddr()
    {
        return clientAddr;
    }

    public void clientAddr( InetAddress addr )
    {
        clientAddr = addr;
    }

    public int clientPort()
    {
        return clientPort;
    }

    public void clientPort( int port )
    {
        clientPort = port;
    }

    public byte clientIntf()
    {
        return clientIntf;
    }

    public InetAddress serverAddr()
    {
        return serverAddr;
    }

    public void serverAddr( InetAddress addr )
    {
        serverAddr = addr;
    }

    public int serverPort()
    {
        return serverPort;
    }

    public void serverPort( int port )
    {
        serverPort = port;
    }

    public byte serverIntf()
    {
        return serverIntf;
    }

    public void serverIntf( byte intf )
    {
        serverIntf = intf;
    }

    public byte originalServerIntf()
    {
        return this.originalServerIntf;
    }

    public PipelineEndpoints pipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    // One of REQUESTED, REJECTED, RELEASED
    public byte state()
    {
        return state;
    }

    public byte rejectCode()
    {
        return code;
    }

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    public void rejectSilently()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = REJECTED_SILENT;
    }

    // May only call if the session is in the requested state
    public void endpoint()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = ENDPOINTED;
    }

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    public void rejectReturnUnreachable( byte code )
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is in the state: " + state );
        }

        switch ( code ) {
        case NET_UNREACHABLE:
        case HOST_UNREACHABLE:
        case PROTOCOL_UNREACHABLE:
        case PORT_UNREACHABLE:
        case DEST_HOST_UNKNOWN:
        case PROHIBITED:
            state     = REJECTED;
            this.code = code;
            break;

        default:
            throw new IllegalArgumentException( "Invalid code: " + code );
        }
    }

    // May only be called before session is established (from TCPNewSessionRequestEvent handler)
    public void release()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Can't release a session that is in the state: " + state );
        }

        state = RELEASED;
    }
}
