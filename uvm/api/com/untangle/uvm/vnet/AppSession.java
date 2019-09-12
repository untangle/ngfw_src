/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.Tag;
import com.untangle.uvm.app.SessionEvent;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.List;
import java.net.InetAddress;

/**
 * The base Sessoin interface
 */
public interface AppSession extends SessionAttachments
{
    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    static final byte CLOSED = 0;
    static final byte EXPIRED = 0;
    static final byte OPEN = 4;
    static final byte HALF_OPEN_INPUT = 5; /* for TCP */
    static final byte HALF_OPEN_OUTPUT = 6; /* for TCP */
    
    /**
     * <code>pipelineConnector</code> returns the Meta Pipe <code>PipelineConnector</code>
     * that this session lives on.
     *
     * @return the <code>PipelineConnector</code> that this session is for
     */
    PipelineConnector pipelineConnector();

    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Netcap.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    long id();
    long getSessionId();
    
    /**
     * User identified for the session.  May be null, which means
     * that no user could be idenitifed for the session.
     *
     */
    String user();

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
     */
    short getProtocol();

    /**
     * Returns an netcap interface for the client.</p>
     *
     * @return a <code>int</code> giving the client interface of the session.
     */
    int getClientIntf();

    /**
     * Returns an netcap interface for the server.</p>
     *
     * @return a <code>int</code> giving the server interface of the session.
     */
    int getServerIntf();

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getClientAddr();

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getServerAddr();

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    int getClientPort();

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    int getServerPort();

    /**
     * Return the policy Id for this session
     */
    long getPolicyId();
    
    /**
     * Gets the original (pre-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getOrigClientAddr();

    /**
     * Gets the new (post-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getNewClientAddr();
    
    /**
     * Gets the original (pre-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getOrigServerAddr();

    /**
     * Gets the new (post-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getNewServerAddr();
    
    /**
     * Gets the original (pre-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getOrigClientPort();

    /**
     * Gets the new (post-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getNewClientPort();
    
    /**
     * Gets the original (pre-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getOrigServerPort();

    /**
     * Gets the new (post-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getNewServerPort();

    /**
     * Get the attachments to this session
     */
    Map<String,Object> getAttachments();

    /**
     * Kill/Reset this session
     */
    void killSession();

    /**
     * <code>release</code> releases all interest in all non-final events for this session.
     *
     * This call is only valid while in NORMAL_MODE.
     * Note: Just calls release(true);
     *
     */
    void release();

    /**
     * <code>scheduleTimer</code> sets the timer for this session to fire in
     * the given number of milliseconds. If the timer is already scheduled, it
     * the existing delay is discarded and the timer is rescheduled for the new
     * <code>delay</code>.
     *
     * @param delay a <code>long</code> giving milliseconds until the timer is to fire
     * @exception IllegalArgumentException if the delay is negative
     */
    void scheduleTimer(long delay) throws IllegalArgumentException;

    /**
     * <code>cancelTimer</code> cancels any scheduled timer expiration for this session.
     *
     */
    void cancelTimer();

    /**
     * <code>clientMark</code> returns the server-side socket mark for this session
     */
    int  clientMark();

    /**
     * <code>clientMark</code> sets the server-side socket mark for this session
     */
    void clientMark(int newmark);

    /**
     * <code>orClientMark</code> bitwise ORs the provided bitmask with the current client-side conn-mark
     */
    void orClientMark(int bitmask);

    /**
     * <code>setClientQosMark</code> sets the connmark so this session' client-side packets get the provided QoS priority
     */
    void setClientQosMark(int priority);
    
    /**
     * <code>serverMark</code> returns the server-side socket mark for this session
     */
    int  serverMark();

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     */
    void serverMark(int newmark);

    /**
     * <code>orServerMark</code> bitwise ORs the provided bitmask with the current server-side conn-mark
     */
    void orServerMark(int bitmask);

    /**
     * <code>setServerQosMark</code> sets the connmark so this session' server-side packets get the provided QoS priority
     */
    void setServerQosMark(int priority);
    
    /**
     * Get the pipeline endpoints for this session
     */
    SessionEvent sessionEvent();

    byte clientState();
    byte serverState();

    void simulateClientData(ByteBuffer data);
    void simulateServerData(ByteBuffer data);

    void sendObjectToClient( Object obj );
    void sendObjectToServer( Object obj );
    void sendObject( int side, Object obj );

    void sendObjectsToClient( Object[] obj );
    void sendObjectsToServer( Object[] obj );
    void sendObjects( int side, Object[] obj );

    boolean hasTag( String name );
    void addTag( Tag tag );
    List<Tag> getTags();
}

