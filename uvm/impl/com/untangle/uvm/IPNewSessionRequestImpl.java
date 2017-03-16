/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.Endpoint;
import com.untangle.jnetcap.Endpoints;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;

public abstract class IPNewSessionRequestImpl implements IPNewSessionRequest
{
    public static final byte REQUESTED = 2;
    public static final byte REJECTED = 99;
    public static final byte RELEASED = 98;
    public static final byte ENDPOINTED = 100;
    public static final byte REJECTED_SILENT = 101;

    // Codes for rejectReturnUnreachable() and for reset
    public static final byte NET_UNREACHABLE = 0;
    public static final byte HOST_UNREACHABLE = 1;
    public static final byte PROTOCOL_UNREACHABLE = 2;
    public static final byte PORT_UNREACHABLE = 3;
    //public  static final byte DEST_NETWORK_UNKNOWN = 6;  // By RFC1812, should use NET_UNREACHABLE instead
    public static final byte DEST_HOST_UNKNOWN = 7;
    //public  static final byte PROHIBITED_NETWORK = 9;    // By RFC1812, should use PROHIBITED instead
    //public  static final byte PROHIBITED_HOST = 10;      // By RFC1812, should use PROHIBITED instead
    public static final byte PROHIBITED = 13;
    public static final byte TCP_REJECT_RESET = 64; // Only valid for TCP connections

    protected final PipelineConnectorImpl pipelineConnector;
    protected final SessionGlobalState sessionGlobalState;
    protected final SessionEvent sessionEvent;
    
    protected final int clientIntf;
    protected final int serverIntf;

    protected final InetAddress origClientAddr;
    protected final int origClientPort;
    protected final InetAddress origServerAddr;
    protected final int origServerPort;

    protected InetAddress newClientAddr;
    protected int newClientPort;
    protected InetAddress newServerAddr;
    protected int newServerPort;

    protected byte state = REQUESTED; /* REQUESTED, REJECTED, RELEASED */
    protected byte rejectCode  = REJECTED;

    protected HashMap<String,Object> stringAttachments = new HashMap<String,Object>();
    private static final String NO_KEY_VALUE = "NOKEY";
    
    /**
     * First way to create an IPNewSessionRequest:
     * Pass in the netcap session and get the parameters from there.
     */
    public IPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent sessionEvent )
    {
        this.sessionGlobalState = sessionGlobalState;
        this.pipelineConnector  = connector;
        this.sessionEvent = sessionEvent;
        
        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();

        clientIntf = clientSide.interfaceId();
        serverIntf = serverSide.interfaceId();
        
        origClientAddr = clientSide.client().host();
        origClientPort = clientSide.client().port();
        origServerAddr = clientSide.server().host();
        origServerPort = clientSide.server().port();

        newClientAddr = serverSide.client().host();
        newClientPort = serverSide.client().port();
        newServerAddr = serverSide.server().host();
        newServerPort = serverSide.server().port();
    }

    /**
     * Second way to create an IPNewSessionRequest:
     * Pass in the previous request and get the parameters from there
     */
    public IPNewSessionRequestImpl( IPNewSessionRequestImpl prevRequest, PipelineConnectorImpl connector, SessionEvent sessionEvent, SessionGlobalState sessionGlobalState)
    {
        this.sessionGlobalState = sessionGlobalState;
        this.pipelineConnector  = connector;
        this.sessionEvent = sessionEvent;
        
        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();
        
        clientIntf = clientSide.interfaceId();
        serverIntf = serverSide.interfaceId();
        
        origClientAddr = clientSide.client().host();
        origClientPort = clientSide.client().port();
        origServerAddr = clientSide.server().host();
        origServerPort = clientSide.server().port();

        /**
         * get the new tuple attributes from the previous session in case it was changed
         */
        newClientAddr = prevRequest.getNewClientAddr();
        newClientPort = prevRequest.getNewClientPort();
        newServerAddr = prevRequest.getNewServerAddr();
        newServerPort = prevRequest.getNewServerPort();
    }

    public PipelineConnectorImpl pipelineConnector()
    {
        return pipelineConnector;
    }
    
    public NetcapSession netcapSession()
    {
        return sessionGlobalState.netcapSession();
    }

    public SessionGlobalState sessionGlobalState()
    {
        return sessionGlobalState;
    }

    public long id()
    {
        return sessionGlobalState.id();
    }

    public long getSessionId()
    {
        return sessionGlobalState.id();
    }
    
    public String user()
    {
        return sessionGlobalState.user();
    }

    public short getProtocol() { return sessionGlobalState.getProtocol(); }
    public int getClientIntf() { return clientIntf; }
    public int getServerIntf() { return serverIntf; }
    
    public InetAddress getOrigClientAddr() { return origClientAddr; }
    public int getOrigClientPort() { return origClientPort; }
    public InetAddress getNewClientAddr() { return newClientAddr; }
    public int getNewClientPort() { return newClientPort; }

    public InetAddress getOrigServerAddr() { return origServerAddr; }
    public int getOrigServerPort() { return origServerPort; }
    public InetAddress getNewServerAddr() { return newServerAddr; }
    public int getNewServerPort() { return newServerPort; }

    public void setNewClientAddr( InetAddress newValue ) { this.newClientAddr = newValue; }
    public void setNewClientPort( int newValue ) { this.newClientPort = newValue; }
    public void setNewServerAddr( InetAddress newValue ) { this.newServerAddr = newValue; }
    public void setNewServerPort( int newValue ) { this.newServerPort = newValue; }
    
    
    public SessionEvent sessionEvent()
    {
        return sessionEvent;
    }

    public byte state()
    {
        return state;
    }

    public byte rejectCode()
    {
        return rejectCode;
    }

    public void rejectSilently()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = REJECTED_SILENT;
    }

    public void endpoint()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = ENDPOINTED;
    }

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
            this.rejectCode = code;
            break;

        default:
            throw new IllegalArgumentException( "Invalid code: " + code );
        }
    }

    public void release()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Can't release a session that is in the state: " + state );
        }

        state = RELEASED;
    }

    public Object attach(Object ob)
    {
        return attach( NO_KEY_VALUE, ob );
    }

    public Object attachment()
    {
        return attachment( NO_KEY_VALUE );
    }

    public Object attach( String key, Object ob )
    {
        return this.stringAttachments.put( key, ob );
    }

    public Object attachment( String key )
    {
        return this.stringAttachments.get(key);
    }

    public void copyAttachments( AppSessionImpl session )
    {
        for( Map.Entry<String,Object> entry : stringAttachments.entrySet() ) {
            session.attach( entry.getKey(), entry.getValue() );
        }
    }

    public Object globalAttach(String key, Object ob)
    {
        return this.sessionGlobalState().attach(key,ob);
    }

    public Object globalAttachment(String key)
    {
        return this.sessionGlobalState().attachment(key);
    }

    public String toString()
    {
        return "NewSessionRequest: " + getProtocol() + "|" +
            getOrigClientAddr().getHostAddress() + ":" + getOrigClientPort() + " -> " +
            getNewServerAddr().getHostAddress() + ":" + getNewServerPort();
    }
}
