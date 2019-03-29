/**
 * $Id$
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.helpers.AbsoluteTimeDateFormat;
import static com.untangle.uvm.Dispatcher.SESSION_ID_MDC_KEY;

import com.untangle.uvm.Tag;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.IPStreamer;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.jvector.Crumb;
import com.untangle.jvector.DataCrumb;
import com.untangle.jvector.ObjectCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.SocketQueueListener;
import com.untangle.jvector.Vector;

/**
 * Abstract base class for all live sessions
 */
public abstract class AppSessionImpl implements AppSession
{
    protected Logger logger = Logger.getLogger(AppSessionImpl.class);

    protected boolean released = false;

    protected final short protocol;

    protected final int clientIntf;
    protected final int serverIntf;

    protected final InetAddress origClientAddr;
    protected final int origClientPort;
    protected final InetAddress origServerAddr;
    protected final int origServerPort;

    protected final InetAddress newClientAddr;
    protected final int newClientPort;
    protected final InetAddress newServerAddr;
    protected final int newServerPort;

    private static DateFormat formatter = new AbsoluteTimeDateFormat();

    protected final Dispatcher dispatcher;

    /**
     * writeQueue is two queues that represent the items stored to be written to
     * each side (server and client) currently you can put Crumbs and Streamers
     * only in the write queue
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    //generics array creation not supported java6 || java7
    protected final List<Object>[] writeQueue = new ArrayList[] { null, null };

    protected int maxInputSize = 0;
    protected int maxOutputSize = 0;

    protected final IncomingSocketQueue clientIncomingSocketQueue;
    protected final OutgoingSocketQueue clientOutgoingSocketQueue;

    protected final IncomingSocketQueue serverIncomingSocketQueue;
    protected final OutgoingSocketQueue serverOutgoingSocketQueue;

    protected final SessionGlobalState sessionGlobalState;

    protected boolean isServerShutdown = false;
    protected boolean isClientShutdown = false;

    protected final boolean isVectored;

    protected PipelineConnectorImpl pipelineConnector;

    protected final SessionEvent sessionEvent;

    protected HashMap<String, Object> stringAttachments = new HashMap<>();
    private static final String NO_KEY_VALUE = "NOKEY";

    /**
     * Constructor
     * 
     * @param dispatcher
     *        The dispatcher event
     * @param sessionEvent
     *        The session event
     * @param request
     *        The IP session request
     */
    protected AppSessionImpl(Dispatcher dispatcher, SessionEvent sessionEvent, IPNewSessionRequestImpl request)
    {
        this.dispatcher = dispatcher;
        this.pipelineConnector = dispatcher.pipelineConnector();
        this.sessionEvent = sessionEvent;
        boolean isVectored = (request.state() == IPNewSessionRequestImpl.REQUESTED || request.state() == IPNewSessionRequestImpl.ENDPOINTED);

        sessionGlobalState = request.sessionGlobalState();
        pipelineConnector = request.pipelineConnector();

        if (isVectored) {
            this.isVectored = true;

            clientIncomingSocketQueue = new IncomingSocketQueue(pipelineConnector().app().getAppSettings().getAppName() + " client side");
            clientOutgoingSocketQueue = new OutgoingSocketQueue(pipelineConnector().app().getAppSettings().getAppName() + " client side");

            serverIncomingSocketQueue = new IncomingSocketQueue(pipelineConnector().app().getAppSettings().getAppName() + " server side");
            serverOutgoingSocketQueue = new OutgoingSocketQueue(pipelineConnector().app().getAppSettings().getAppName() + " server side");
        } else {
            this.isVectored = false;

            clientIncomingSocketQueue = null;
            clientOutgoingSocketQueue = null;

            serverIncomingSocketQueue = null;
            serverOutgoingSocketQueue = null;
        }

        this.protocol = request.getProtocol();

        this.clientIntf = request.getClientIntf();
        this.serverIntf = request.getServerIntf();

        this.origClientAddr = request.getOrigClientAddr();
        this.origClientPort = request.getOrigClientPort();
        this.newClientAddr = request.getNewClientAddr();
        this.newClientPort = request.getNewClientPort();

        this.origServerPort = request.getOrigServerPort();
        this.origServerAddr = request.getOrigServerAddr();
        this.newServerPort = request.getNewServerPort();
        this.newServerAddr = request.getNewServerAddr();

        if (isVectored) {
            SocketQueueListener sqListener = new SessionSocketQueueListener();

            clientIncomingSocketQueue.registerListener(sqListener);
            clientOutgoingSocketQueue.registerListener(sqListener);
            serverIncomingSocketQueue.registerListener(sqListener);
            serverOutgoingSocketQueue.registerListener(sqListener);
        }
    }

    /**
     * Get the pipline connectors
     * 
     * @return The pipeline connectors
     */
    public PipelineConnectorImpl pipelineConnector()
    {
        return pipelineConnector;
    }

    /**
     * Attach an unnamed object to the session
     * 
     * @param ob
     *        The object to attach
     * @return The object
     */
    public Object attach(Object ob)
    {
        return attach(NO_KEY_VALUE, ob);
    }

    /**
     * Get the unnamed object attached to the session
     * 
     * @return The object
     */
    public Object attachment()
    {
        return attachment(NO_KEY_VALUE);
    }

    /**
     * Attach a named object to the session
     * 
     * @param key
     *        The name
     * @param ob
     *        The object
     * @return The object
     */
    public Object attach(String key, Object ob)
    {
        return this.stringAttachments.put(key, ob);
    }

    /**
     * Get a named object attached to the session
     * 
     * @param key
     *        The name
     * @return The object
     */
    public Object attachment(String key)
    {
        return this.stringAttachments.get(key);
    }

    /**
     * Attach a named object to the global session
     * 
     * @param key
     *        The name
     * @param ob
     *        The object
     * @return The object
     */
    public Object globalAttach(String key, Object ob)
    {
        return this.sessionGlobalState().attach(key, ob);
    }

    /**
     * Get a named object attached to the global session
     * 
     * @param key
     *        The name
     * @return The object
     */
    public Object globalAttachment(String key)
    {
        return this.sessionGlobalState().attachment(key);
    }

    /**
     * Get all of the global attachments
     * 
     * @return The map of attachments
     */
    public Map<String, Object> getAttachments()
    {
        return this.sessionGlobalState().getAttachments();
    }

    /**
     * Get the session global state
     * 
     * @return The state
     */
    public SessionGlobalState sessionGlobalState()
    {
        return sessionGlobalState;
    }

    /**
     * Get the pipeline description
     * 
     * @return The pipeline description
     */
    public String getPipelineDescription()
    {
        return this.sessionGlobalState().getPipelineDescription();
    }

    /**
     * Get the global state ID
     * 
     * @return The global state ID
     */
    public long id()
    {
        return sessionGlobalState.id();
    }

    /**
     * Get the global state ID
     * 
     * @return The global state ID
     */
    public long getSessionId()
    {
        return sessionGlobalState.id();
    }

    /**
     * Get the netcap session
     * 
     * @return The netcap session
     */
    public NetcapSession netcapSession()
    {
        return sessionGlobalState.netcapSession();
    }

    /**
     * Get the isVectored flag
     * 
     * @return The isVectored flag
     */
    public boolean isVectored()
    {
        return isVectored;
    }

    /**
     * Get the user
     * 
     * @return The user
     */
    public String user()
    {
        return sessionGlobalState.user();
    }

    /**
     * Number of bytes received from the client.
     * 
     * @return The value
     */
    public long c2tBytes()
    {
        return sessionGlobalState.clientSideListener().getRxBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     * 
     * @return The value
     */
    public long t2sBytes()
    {
        return sessionGlobalState.serverSideListener().getTxBytes();
    }

    /**
     * Number of bytes received from the server.
     * 
     * @return The value
     */
    public long s2tBytes()
    {
        return sessionGlobalState.serverSideListener().getRxBytes();
    }

    /**
     * Number of bytes transmitted to the client.
     * 
     * @return The value
     */
    public long t2cBytes()
    {
        return sessionGlobalState.clientSideListener().getTxBytes();
    }

    /**
     * Number of chunks received from the client.
     * 
     * @return The value
     */
    public long c2tChunks()
    {
        return sessionGlobalState.clientSideListener().getRxChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     * 
     * @return The value
     */
    public long t2sChunks()
    {
        return sessionGlobalState.serverSideListener().getTxChunks();
    }

    /**
     * Number of chunks received from the server.
     * 
     * @return The value
     */
    public long s2tChunks()
    {
        return sessionGlobalState.serverSideListener().getRxChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     * 
     * @return The value
     */
    public long t2cChunks()
    {
        return sessionGlobalState.clientSideListener().getTxChunks();
    }

    /*
     * These are not really useable without a little bit of tweaking to the
     * vector machine
     */

    /**
     * Get the maximum input size.
     * 
     * @return The value
     */
    public int maxInputSize()
    {
        return maxInputSize;
    }

    /**
     * Set the maximum input size.
     * 
     * @param size
     *        The new size
     */
    public void maxInputSize(int size)
    {
        /* Possibly throw an error on an invalid size */
        maxInputSize = size;
    }

    /**
     * Get the maximum output size
     * 
     * @return The value
     */
    public int maxOutputSize()
    {
        return maxOutputSize;
    }

    /**
     * Set the maximum output size
     * 
     * @param size
     *        The new size
     */
    public void maxOutputSize(int size)
    {
        /* Possibly throw an error on an invalid size */
        maxOutputSize = size;
    }

    /**
     * Shutdown the client side of the connection.
     */
    public void shutdownClient()
    {
        if (!clientOutgoingSocketQueue.isClosed()) {
            clientOutgoingSocketQueue.write(ShutdownCrumb.getInstance());
        }
    }

    /**
     * Shutdown the server side of the connection.
     */
    public void shutdownServer()
    {
        if (!serverOutgoingSocketQueue.isClosed()) {
            serverOutgoingSocketQueue.write(ShutdownCrumb.getInstance());
        }
    }

    /**
     * Kill the server and client side of the session, this should only be
     * called from the session thread.
     */
    public void killSession()
    {
        try {
            if (clientIncomingSocketQueue != null) clientIncomingSocketQueue.kill();
            if (clientOutgoingSocketQueue != null) clientOutgoingSocketQueue.kill();
            if (serverIncomingSocketQueue != null) serverIncomingSocketQueue.kill();
            if (serverOutgoingSocketQueue != null) serverOutgoingSocketQueue.kill();
        } catch (Exception ex) {
            logger.warn("Error while killing a session", ex);
        }

        try {
            Vector vector = sessionGlobalState.netcapHook().getVector();

            /* Last send the kill signal to the vectoring machine */
            if (vector != null) vector.shutdown();
            else logger.info("kill session called before vectoring started, ignoring vectoring.");
        } catch (Exception ex) {
            logger.warn("Error while killing a session", ex);
        }
    }

    /**
     * Package method to just call the raze method.
     */
    public void raze()
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            if (this.clientIncomingSocketQueue != null) this.clientIncomingSocketQueue.raze();
            if (this.serverIncomingSocketQueue != null) this.serverIncomingSocketQueue.raze();
            if (this.clientOutgoingSocketQueue != null) this.clientOutgoingSocketQueue.raze();
            if (this.serverOutgoingSocketQueue != null) this.serverOutgoingSocketQueue.raze();

            if (released) {
                logger.debug("raze released");
            } else {
                if (logger.isDebugEnabled()) {
                    IncomingSocketQueue ourcin = clientIncomingSocketQueue();
                    IncomingSocketQueue oursin = serverIncomingSocketQueue();
                    OutgoingSocketQueue ourcout = clientOutgoingSocketQueue();
                    OutgoingSocketQueue oursout = serverOutgoingSocketQueue();
                    logger.debug("raze " + " ourcin: " + ourcin + " ourcout: " + ourcout + " ourcsin: " + oursin + " oursout: " + oursout + " writeQueue[CLIENT]: " + writeQueue[CLIENT] + " writeQueue[SERVER]: " + writeQueue[SERVER]);
                }
            }
            closeFinal();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " in raze";
            logger.error(message, x);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * Get the server shutdown flag
     * 
     * @return The flag
     */
    public boolean getServerShutdown()
    {
        return this.isServerShutdown;
    }

    /**
     * Set the server shutdown flag
     * 
     * @param newValue
     *        The new flag value
     */
    public void setServerShutdown(boolean newValue)
    {
        this.isServerShutdown = newValue;
    }

    /**
     * Get the client shutdown flag
     * 
     * @return The flag
     */
    public boolean getClientShutdown()
    {
        return this.isClientShutdown;
    }

    /**
     * Set the client shutdown flag
     * 
     * @param newValue
     *        The new flag value
     */
    public void setClientShutdown(boolean newValue)
    {
        this.isClientShutdown = newValue;
    }

    /**
     * Handle shutdown event
     * 
     * @param isq
     *        The incoming socket queue
     */
    public void shutdownEvent(IncomingSocketQueue isq)
    {
        logger.debug("Incoming socket queue shutdown event: " + isq);
    }

    /**
     * Get the client incoming socket queue
     * 
     * @return The queue
     */
    public IncomingSocketQueue clientIncomingSocketQueue()
    {
        if (clientIncomingSocketQueue == null || clientIncomingSocketQueue.isClosed()) return null;
        return clientIncomingSocketQueue;
    }

    /**
     * Get the client outgoing socket queue
     * 
     * @return The queue
     */
    public OutgoingSocketQueue clientOutgoingSocketQueue()
    {
        if (clientOutgoingSocketQueue == null || clientOutgoingSocketQueue.isClosed()) return null;
        return clientOutgoingSocketQueue;
    }

    /**
     * Get the server incoming socket queue
     * 
     * @return The queue
     */
    public IncomingSocketQueue serverIncomingSocketQueue()
    {
        if (serverIncomingSocketQueue == null || serverIncomingSocketQueue.isClosed()) return null;
        return serverIncomingSocketQueue;
    }

    /**
     * Get the server outgoing socket queue
     * 
     * @return The queue
     */
    public OutgoingSocketQueue serverOutgoingSocketQueue()
    {
        if (serverOutgoingSocketQueue == null || serverOutgoingSocketQueue.isClosed()) return null;
        return serverOutgoingSocketQueue;
    }

    /**
     * Get the policy ID
     * 
     * @return The policy ID
     */
    public long getPolicyId()
    {
        return sessionEvent.getPolicyId();
    }

    /**
     * Get the session event
     * 
     * @return The event
     */
    public SessionEvent sessionEvent()
    {
        return sessionEvent;
    }

    /**
     * Release the sesssion
     */
    public void release()
    {
        cancelTimer();

        released = true;

        Vector vector = sessionGlobalState.netcapHook().getVector();
        if (vector == null || vector.isRazed()) return;

        int length = vector.length();

        if (clientIncomingSocketQueue != null && serverOutgoingSocketQueue != null && !clientIncomingSocketQueue.isRazed() && !serverOutgoingSocketQueue.isRazed()) {

            vector.compress(clientIncomingSocketQueue, serverOutgoingSocketQueue);
            length--;
        }

        if (serverIncomingSocketQueue != null && clientOutgoingSocketQueue != null && !serverIncomingSocketQueue.isRazed() && !clientOutgoingSocketQueue.isRazed()) {

            vector.compress(serverIncomingSocketQueue, clientOutgoingSocketQueue);
            length--;
        }

        /**
         * If we have reduced the chain to just two relays, there are no more
         * apps looking at this session just bypass the rest of the session
         */
        if (length == 2 && this.netcapSession() != null && this.netcapSession() instanceof NetcapUDPSession) {
            sessionGlobalState.netcapHook().releaseToBypass();
        }
    }

    /**
     * Get the session released flag
     * 
     * @return The flag
     */
    public boolean released()
    {
        return released;
    }

    /**
     * Schedule a timer
     * 
     * @param delay
     *        The timer delay
     */
    public void scheduleTimer(long delay)
    {
        if (delay < 0) throw new IllegalArgumentException("Delay must be non-negative");
        // mpipe.scheduleTimer(this, delay);
    }

    /**
     * Cancel the timer
     */
    public void cancelTimer()
    {
        // mpipe.cancelTimer(this);
    }

    /**
     * Send an object to the client
     * 
     * @param obj
     *        The object to send
     */
    public void sendObjectToClient(Object obj)
    {
        sendObject(CLIENT, obj);
    }

    /**
     * Send an object to the server
     * 
     * @param obj
     *        The object
     */
    public void sendObjectToServer(Object obj)
    {
        sendObject(SERVER, obj);
    }

    /**
     * Send an object
     * 
     * @param side
     *        The side to which the object should be sent
     * @param obj
     *        The object to send
     */
    public void sendObject(int side, Object obj)
    {
        if (obj.getClass().isArray()) {
            logger.warn("sendObject() called with array. Did you mean sendObjects() instead?", new Exception());
        }

        ObjectCrumb crumb = new ObjectCrumb(obj);
        addToWriteQueue(side, crumb);
    }

    /**
     * Send objects to the client
     * 
     * @param objs
     *        The objects to send
     */
    public void sendObjectsToClient(Object[] objs)
    {
        sendObjects(CLIENT, objs);
    }

    /**
     * Send objects to the server
     * 
     * @param objs
     *        The objects to send
     */
    public void sendObjectsToServer(Object[] objs)
    {
        sendObjects(SERVER, objs);
    }

    /**
     * Send objects
     * 
     * @param side
     *        The side to which the objects should be sent
     * @param objs
     *        The objects to send
     */
    public void sendObjects(int side, Object[] objs)
    {
        if (objs == null || objs.length == 0) return;
        for (int i = 0; i < objs.length; i++)
            sendObject(side, objs[i]);
    }

    /**
     * Add an object to the write queue
     * 
     * @param side
     *        The side for which the object should be queued
     * @param obj
     *        The object to queue
     */
    protected void addToWriteQueue(int side, Object obj)
    {
        if (obj == null) return;

        OutgoingSocketQueue out;
        if (side == CLIENT) out = clientOutgoingSocketQueue();
        else out = serverOutgoingSocketQueue();

        if (out == null || out.isClosed()) {
            String sideName = side == CLIENT ? "client" : "server";
            logger.info("Ignoring crumb for dead " + sideName + " outgoing socket queue");
            return;
        }

        List<Object> queue = writeQueue[side];

        if (queue == null) {
            queue = new ArrayList<>();
            writeQueue[side] = queue;
        }
        queue.add(obj);
    }

    /**
     * Send a crumb to an outgoing socket queue
     * 
     * @param crumb
     *        The crumb
     * @param out
     *        The queue
     * @return The size sent
     */
    protected int sendCrumb(Crumb crumb, OutgoingSocketQueue out)
    {
        int size = 0;
        if (crumb instanceof DataCrumb) size = ((DataCrumb) crumb).limit();
        boolean success = out.write(crumb);
        if (logger.isDebugEnabled()) {
            logger.debug("writing " + crumb.type() + " crumb to " + out + ", size: " + size);
        }
        assert success;
        return size;
    }

    /**
     * Get the next crumb to sent
     * 
     * @param side
     *        The side for which to get the crumb
     * @return The crumb
     */
    protected Crumb getNextCrumb2Send(int side)
    {
        List<Object> queue = writeQueue[side];
        if (queue == null) {
            logger.warn("write queue is null");
            return null;
        }
        Object result = queue.get(0);
        if (result == null) {
            logger.warn("Invalid entry in write queue: " + result);
            return null;
        }

        if (result instanceof Crumb) {
            Crumb crumb = (Crumb) result;
            queue.remove(0);
            if (queue.size() == 0) writeQueue[side] = null;

            return crumb;
        } else if (result instanceof IPStreamer) {
            IPStreamer streamer = (IPStreamer) result;
            Crumb crumb = readStreamer(streamer);
            if (crumb != null) return crumb;

            // null means its done streaming

            // if "closeWhenDone" then close the side
            if (streamer.closeWhenDone()) {
                if (side == CLIENT) shutdownClient();
                else shutdownServer();
            }

            // remove the streamer from the write queue
            queue.remove(0);
            if (queue.size() == 0) writeQueue[side] = null;

            return null;
        } else {
            logger.error("Unknown object in write queue: " + result.getClass() + " " + result);
            queue.remove(0);
        }

        return null;
    }

    /**
     * Send complete event
     */
    public void complete()
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            sendCompleteEvent();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while completing";
            // logger.error(message, x);
            killSession();
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * Handle a client incoming event
     * 
     * @param in
     *        The incoming socket queue
     */
    public void clientEvent(IncomingSocketQueue in)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            readEvent(CLIENT, in);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Handle a server incoming event
     * 
     * @param in
     *        The incoming socket queue
     */
    public void serverEvent(IncomingSocketQueue in)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            readEvent(SERVER, in);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Handle a client outgoing event
     * 
     * @param out
     *        The outgoing socket queue
     */
    public void clientEvent(OutgoingSocketQueue out)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            writeEvent(CLIENT, out);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Handle a server outgoing event
     * 
     * @param out
     *        The outgoing socket queue
     */
    public void serverEvent(OutgoingSocketQueue out)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            writeEvent(SERVER, out);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    /**
     * The write side of the client has been closed from underneath the app,
     * this is the same as an EPIPE, but is delivered as an event
     * 
     * @param out
     *        The outgoing socket queue
     */
    public void clientOutputResetEvent(OutgoingSocketQueue out)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = clientIncomingSocketQueue();
            if (in != null) in.reset();
            sideDieing(CLIENT);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // logger.error(message, x);
            killSession();
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * The write side of the server has been closed from underneath the app,
     * this is the same as an EPIPE, but is delivered as an event
     * 
     * @param out
     *        The outgoing socket queue
     */
    public void serverOutputResetEvent(OutgoingSocketQueue out)
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingApp(pipelineConnector().app().getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = serverIncomingSocketQueue();
            if (in != null) in.reset();
            sideDieing(SERVER);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // logger.error(message, x);
            killSession();
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * <code>clientMark</code> returns the server-side socket mark for this
     * session
     * 
     * @return The mark
     */
    public int clientMark()
    {
        return this.sessionGlobalState().netcapSession().clientMark();
    }

    /**
     * <code>clientMark</code> sets the server-side socket mark for this session
     * 
     * @param newmark
     *        The new mark
     */
    public void clientMark(int newmark)
    {
        this.sessionGlobalState().netcapSession().clientMark(newmark);
    }

    /**
     * <code>orClientMark</code> bitwise ORs the provided bitmask with the
     * current client-side conn-mark
     * 
     * @param bitmask
     *        The bitmask
     */
    public void orClientMark(int bitmask)
    {
        //java.lang.StringBuilder sb = new java.lang.StringBuilder();
        //java.util.Formatter formatter = new java.util.Formatter(sb, java.util.Locale.US);
        //logger.debug(formatter.format("Set ClientMark to 0x%08x",client_mark).toString()); sb.setLength(0);

        this.sessionGlobalState().netcapSession().orClientMark(bitmask);
    }

    /**
     * <code>setClientQosMark</code> sets the connmark so this session'
     * client-side packets get the provided QoS priority
     * 
     * @param priority
     *        The priority
     */
    public void setClientQosMark(int priority)
    {
        logger.debug("Set Client QosMark to " + priority);
        this.sessionGlobalState().netcapSession().clientQosMark(priority);
    }

    /**
     * <code>serverMark</code> returns the server-side socket mark for this
     * session
     * 
     * @return The mark
     */
    public int serverMark()
    {
        return this.sessionGlobalState().netcapSession().serverMark();
    }

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     * 
     * @param newmark
     *        The new mark
     */
    public void serverMark(int newmark)
    {
        this.sessionGlobalState().netcapSession().serverMark(newmark);
    }

    /**
     * <code>orServerMark</code> bitwise ORs the provided bitmask with the
     * current server-side conn-mark
     * 
     * @param bitmask
     *        The bitmask
     */
    public void orServerMark(int bitmask)
    {
        //java.lang.StringBuilder sb = new java.lang.StringBuilder();
        //java.util.Formatter formatter = new java.util.Formatter(sb, java.util.Locale.US);
        //logger.debug(formatter.format("Set ServerMark to 0x%08x",server_mark).toString()); sb.setLength(0);

        this.sessionGlobalState().netcapSession().orServerMark(bitmask);
    }

    /**
     * <code>setServerQosMark</code> sets the connmark so this session'
     * server-side packets get the provided QoS priority
     * 
     * @param priority
     *        The priority
     */
    public void setServerQosMark(int priority)
    {
        logger.debug("Set Server QosMark to " + priority);
        this.sessionGlobalState().netcapSession().serverQosMark(priority);
    }

    /**
     * This is the main write hook called by the Vectoring machine
     * 
     * @param side
     *        The side for the event
     * @param out
     *        The outgoing socket queue
     */
    public void writeEvent(int side, OutgoingSocketQueue out)
    {
        String sideName = side == CLIENT ? "client" : "server";
        MDC.put(SESSION_ID_MDC_KEY, idForMDC());
        try {
            assert out != null;
            if (!out.isEmpty()) {
                logger.warn("writeEvent to non empty outgoing queue on: " + sideName);
                return;
            }

            OutgoingSocketQueue ourout, otherout;
            if (side == CLIENT) {
                ourout = clientOutgoingSocketQueue();
                otherout = serverOutgoingSocketQueue();
            } else {
                ourout = serverOutgoingSocketQueue();
                otherout = clientOutgoingSocketQueue();
            }
            assert out == ourout;

            if (logger.isDebugEnabled()) {
                logger.debug("write(" + sideName + ") out: " + out + " out.numEvents(): " + out.numEvents() + " out.numBytes(): " + out.numBytes() + " write-queue: " + writeQueue[side]);
            }

            if (!doWrite(side, ourout)) {
                sendWritableEvent(side);
                // We have to try more writing here in case we added stuff.
                doWrite(side, ourout);
                doWrite(1 - side, otherout);
            }

            refreshSocketQueueState();

        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while writing to " + sideName;
            logger.error(message, x);
            killSession();
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * This is the main read hook called by the Vectoring machine
     * 
     * @param side
     *        The side for the event
     * @param in
     *        The incoming socket queue
     */
    public void readEvent(int side, IncomingSocketQueue in)
    {
        String sideName = side == CLIENT ? "client" : "server";
        MDC.put(SESSION_ID_MDC_KEY, idForMDC());
        try {
            assert in != null;

            // need to check if input contains RST (for TCP) or EXPIRE (for UDP)
            // independent of the write buffers.
            if (isSideDieing(side, in)) {
                sideDieing(side);
                return;
            }

            if (!in.isEnabled()) {
                logger.warn("ignoring readEvent called for disabled side " + side);
                return;
            }
            @SuppressWarnings("unused")
            IncomingSocketQueue ourin, otherin;
            OutgoingSocketQueue ourout, otherout;
            OutgoingSocketQueue cout = clientOutgoingSocketQueue();
            OutgoingSocketQueue sout = serverOutgoingSocketQueue();
            if (side == CLIENT) {
                ourin = clientIncomingSocketQueue();
                otherin = serverIncomingSocketQueue();
                ourout = sout;
                otherout = cout;
            } else {
                ourin = serverIncomingSocketQueue();
                otherin = clientIncomingSocketQueue();
                ourout = cout;
                otherout = sout;
            }
            assert in == ourin;

            if (logger.isDebugEnabled()) {
                logger.debug("read(" + sideName + ") in: " + in);
            }

            if (ourout == null || (writeQueue[1 - side] == null && ourout.isEmpty())) {
                handleRead(side, in);
                doWrite(side, otherout);
                doWrite(1 - side, ourout);
            } else {
                logger.error("Illegal State: read(" + sideName + ") in: " + in + " ourout: " + ourout + " writequeue: " + writeQueue[1 - side] + " empty:" + ourout.isEmpty());
            }

            refreshSocketQueueState();

        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while reading from " + sideName;
            logger.error(message, x);
            killSession();
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * The simulateClientData and simulateServerData functions allow injecting
     * data directly at the client and server session endpoints. They do this by
     * adding the argumented data to the incoming socket queue so that it can be
     * processed exactly as if the data had been received across the network
     * directly from the client or server. These functions were added
     * specifically to allow the client and server sides of the https-casing to
     * pass messages back and forth without the data passing through all the
     * other apps subscribed to HTTP tokens.
     * 
     * @param data
     *        The data
     */
    public void simulateClientData(ByteBuffer data)
    {
        byte local[] = new byte[data.limit()];
        data.get(local, 0, data.limit());
        DataCrumb crumb = new DataCrumb(local);
        IncomingSocketQueue isq = clientIncomingSocketQueue();
        if (isq != null) {
            isq.send_event(crumb);
        } else {
            logger.warn("simulateClientData() failed: null socket queue");
        }
    }

    /**
     * Simulate server data. See the simulateClientData comment for details
     * 
     * @param data
     *        The data
     */
    public void simulateServerData(ByteBuffer data)
    {
        byte local[] = new byte[data.limit()];
        data.get(local, 0, data.limit());
        DataCrumb crumb = new DataCrumb(data.array(), data.limit());
        IncomingSocketQueue isq = serverIncomingSocketQueue();
        if (isq != null) {
            isq.send_event(crumb);
        } else {
            logger.warn("simulateServerData() failed: null socket queue");
        }
    }

    /**
     * Get the protocol
     * 
     * @return The protocol
     */
    public short getProtocol()
    {
        return protocol;
    }

    /**
     * Get the client interface
     * 
     * @return The interface
     */
    public int getClientIntf()
    {
        return clientIntf;
    }

    /**
     * Get the server interface
     * 
     * @return The interface
     */
    public int getServerIntf()
    {
        return serverIntf;
    }

    /**
     * Get the original client address
     * 
     * @return The address
     */
    public InetAddress getOrigClientAddr()
    {
        return origClientAddr;
    }

    /**
     * Get the original client port
     * 
     * @return The port
     */
    public int getOrigClientPort()
    {
        return origClientPort;
    }

    /**
     * Get the new client address
     * 
     * @return The address
     */
    public InetAddress getNewClientAddr()
    {
        return newClientAddr;
    }

    /**
     * Get the new client port
     * 
     * @return The port
     */
    public int getNewClientPort()
    {
        return newClientPort;
    }

    /**
     * Get the original server address
     * 
     * @return The address
     */
    public InetAddress getOrigServerAddr()
    {
        return origServerAddr;
    }

    /**
     * Get the original server port
     * 
     * @return The port
     */
    public int getOrigServerPort()
    {
        return origServerPort;
    }

    /**
     * Get the new server address
     * 
     * @return The address
     */
    public InetAddress getNewServerAddr()
    {
        return newServerAddr;
    }

    /**
     * Get the new server port
     * 
     * @return The port
     */
    public int getNewServerPort()
    {
        return newServerPort;
    }

    /**
     * Get the client address
     * 
     * @return The client address
     */
    public InetAddress getClientAddr()
    {
        return origClientAddr;
    }

    /**
     * Get the client port
     * 
     * @return The port
     */
    public int getClientPort()
    {
        return origClientPort;
    }

    /**
     * Get the server address
     * 
     * @return The address
     */
    public InetAddress getServerAddr()
    {
        return newServerAddr;
    }

    /**
     * Get the server port
     * 
     * @return The port
     */
    public int getServerPort()
    {
        return newServerPort;
    }

    /**
     * See if the session has a tag
     * 
     * @param name
     *        The tag name
     * @return True if the tag is found, otherwise false
     */
    public boolean hasTag(String name)
    {
        return sessionGlobalState.hasTag(name);
    }

    /**
     * Add a tag to the session
     * 
     * @param tag
     *        The tag
     */
    public void addTag(Tag tag)
    {
        sessionGlobalState.addTag(tag);
    }

    /**
     * Get the session tags
     * 
     * @return The tags
     */
    public List<Tag> getTags()
    {
        return sessionGlobalState.getTags();
    }

    /**
     * Get a string representation of the session
     * 
     * @return The string
     */
    public String toString()
    {
        String origClientAddr = (getOrigClientAddr() != null ? getOrigClientAddr().getHostAddress() : "null");
        String newServerAddr = (getNewServerAddr() != null ? getNewServerAddr().getHostAddress() : "null");
        return "" + "[AppSession-" + System.identityHashCode(this) + "]" + "[Session-" + getSessionId() + "] " + getProtocol() + " " + origClientAddr + ":" + getOrigClientPort() + "->" + newServerAddr + ":" + getNewServerPort();
    }

    /**
     * Close cleanup
     */
    protected void closeFinal()
    {
        cancelTimer();

        dispatcher.removeSession(this);
    }

    /**
     * This one sets up the socket queues for normal operation; used when
     * streaming ends.
     */
    private void refreshSocketQueueState()
    {
        IncomingSocketQueue cin = clientIncomingSocketQueue();
        IncomingSocketQueue sin = serverIncomingSocketQueue();
        OutgoingSocketQueue cout = clientOutgoingSocketQueue();
        OutgoingSocketQueue sout = serverOutgoingSocketQueue();

        // We take care not to change the state unless it's really
        // changing, as changing the state calls notifymvpoll() every
        // time.
        if (sout != null && !sout.isEnabled()) sout.enable();
        if (sout == null || (sout.isEmpty() && writeQueue[SERVER] == null)) {
            if (cin != null && !cin.isEnabled()) cin.enable();
        } else {
            if (cin != null && cin.isEnabled()) cin.disable();
        }
        if (cout != null && !cout.isEnabled()) cout.enable();
        if (cout == null || (cout.isEmpty() && writeQueue[CLIENT] == null)) {
            if (sin != null && !sin.isEnabled()) sin.enable();
        } else {
            if (sin != null && sin.isEnabled()) sin.disable();
        }
    }

    /**
     * Returns true if we did something.
     * 
     * @param side
     *        an <code>int</code> value
     * @param out
     *        an <code>OutgoingSocketQueue</code> value
     * @return a <code>boolean</code> value
     */
    private boolean doWrite(int side, OutgoingSocketQueue out)
    {
        if (out == null || !out.isEmpty()) // no room
            return false;

        if (writeQueue[side] == null) // nothing to write
            return false;

        return tryWrite(side, out);
    }

    /**
     * <code>isSideDieing</code> returns true if the incoming socket queue
     * contains an event that will cause the end of the session (at least on
     * that side). These are RST for TCP and EXPIRE for UDP.
     * 
     * @param side
     *        The side to check
     * @param in
     *        an <code>IncomingSocketQueue</code> value
     * @return a <code>boolean</code> value
     */
    abstract protected boolean isSideDieing(int side, IncomingSocketQueue in);

    /**
     * Set the side dieing flag
     * 
     * @param side
     *        The side
     */
    abstract protected void sideDieing(int side);

    /**
     * Send a writable event
     * 
     * @param side
     *        The side
     */
    abstract protected void sendWritableEvent(int side);

    /**
     * Send a complete event
     */
    abstract protected void sendCompleteEvent();

    /**
     * Try writing
     * 
     * @param side
     *        The side
     * @param out
     *        The outgoing socket queue
     * @return The result
     */
    abstract protected boolean tryWrite(int side, OutgoingSocketQueue out);

    /**
     * Read a crumb from a streamer
     * 
     * @param streamer
     *        The streamer
     * @return The crumb
     */
    abstract protected Crumb readStreamer(IPStreamer streamer);

    /**
     * Handle read
     * 
     * @param side
     *        The side
     * @param in
     *        The incoming socket queue
     */
    abstract protected void handleRead(int side, IncomingSocketQueue in);

    /**
     * Get the id for MDC
     * 
     * @return The string
     */
    abstract protected String idForMDC();

    /**
     * Class to represent a session socket queue listener
     */
    private class SessionSocketQueueListener implements SocketQueueListener
    {
        /**
         * Constructor
         */
        SessionSocketQueueListener()
        {
        }

        /**
         * Handle an event
         * 
         * @param in
         *        The incoming socket queue
         */
        public void event(IncomingSocketQueue in)
        {
            if (in == serverIncomingSocketQueue) {
                if (logger.isDebugEnabled()) {
                    logger.debug("IncomingSocketQueueEvent: server - " + in + " " + sessionGlobalState);
                }

                serverEvent(in);
            } else if (in == clientIncomingSocketQueue) {
                if (logger.isDebugEnabled()) {
                    logger.debug("IncomingSocketQueueEvent: client - " + in + " " + sessionGlobalState);
                }

                clientEvent(in);
            } else {
                /* This should never happen */
                throw new IllegalStateException("Invalid socket queue: " + in);
            }
        }

        /**
         * Handle ana event
         * 
         * @param out
         *        The outgoing socket queue
         */
        public void event(OutgoingSocketQueue out)
        {
            /**
             * This is called every time a crumb is removed from the outgoing
             * socket queue (what it considers 'writable', but the TAPI defines
             * writable as empty) So, we drop all these writable events unless
             * it is empty. That converts the socketqueue's definition of
             * writable to the TAPI's You are at no risk of spinning because
             * this is only called when something is actually removed from the
             * SocketQueue
             **/
            if (!out.isEmpty()) return;

            if (out == serverOutgoingSocketQueue) {
                if (logger.isDebugEnabled()) {
                    logger.debug("OutgoingSocketQueueEvent: server - " + out + " " + sessionGlobalState);
                }

                serverEvent(out);
            } else if (out == clientOutgoingSocketQueue) {
                if (logger.isDebugEnabled()) {
                    logger.debug("OutgoingSocketQueueEvent: client - " + out + " " + sessionGlobalState);
                }

                clientEvent(out);
            } else {
                /* This should never happen */
                throw new IllegalStateException("Invalid socket queue: " + out);
            }
        }

        /**
         * Handle a shutdown event
         * 
         * @param in
         *        The incoming socket queue
         */
        public void shutdownEvent(IncomingSocketQueue in)
        {
            if (in == serverIncomingSocketQueue) {
                if (logger.isDebugEnabled()) logger.debug("ShutdownEvent: server - " + in);
            } else if (in == clientIncomingSocketQueue) {
                if (logger.isDebugEnabled()) logger.debug("ShutdownEvent: client - " + in);
            } else {
                /* This should never happen */
                throw new IllegalStateException("Invalid socket queue: " + in);
            }
        }

        /**
         * This occurs when the outgoing socket queue is shutdown
         * 
         * @param out
         *        The outgoing socket queue
         */
        public void shutdownEvent(OutgoingSocketQueue out)
        {
            boolean isDebugEnabled = logger.isDebugEnabled();
            if (out == serverOutgoingSocketQueue) {
                if (isDebugEnabled) {
                    logger.debug("ShutdownEvent: server - " + out + " closed: " + out.isClosed());
                }
                /* If the app hasn't closed the socket queue yet, send the even */
                if (!out.isClosed()) {
                    /* This is equivalent to an EPIPE */
                    serverOutputResetEvent(out);
                } else {
                    if (isDebugEnabled) logger.debug("shutdown event for closed sink");
                }
            } else if (out == clientOutgoingSocketQueue) {
                if (isDebugEnabled) {
                    logger.debug("ShutdownEvent: client - " + out + " closed: " + out.isClosed());
                }

                if (!out.isClosed()) {
                    /* This is equivalent to an EPIPE */
                    clientOutputResetEvent(out);
                } else {
                    if (isDebugEnabled) logger.debug("shutdown event for closed sink");
                }
            } else {
                /* This should never happen */
                throw new IllegalStateException("Invalid socket queue: " + out);
            }
        }
    }
}
