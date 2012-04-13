/**
 * $Id$
 */
package com.untangle.uvm.engine;

import static com.untangle.uvm.engine.Dispatcher.SESSION_ID_MDC_KEY;

import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.helpers.AbsoluteTimeDateFormat;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.DataCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.argon.PipelineListener;
import com.untangle.uvm.argon.ArgonIPSession;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.event.IPStreamer;
import com.untangle.uvm.node.NodeManager;

/**
 * Abstract base class for all IP live sessions
 */
abstract class IPSessionImpl
    extends SessionImpl
    implements IPSession, PipelineListener
{
    protected boolean released = false;
    protected boolean needsFinalization = true;

    private static DateFormat formatter = new AbsoluteTimeDateFormat();
    
    protected final Dispatcher dispatcher;

    protected final SessionEvent sessionEvent;

    @SuppressWarnings("unchecked") //generics array creation not supported java6
        protected final List<Crumb>[] crumbs2write = new ArrayList[] { null, null };

    protected IPStreamer[] streamer = null;

    protected Logger logger;

    protected final RWSessionStats stats;

    private final Logger timesLogger;

    protected IPSessionImpl(Dispatcher disp, ArgonIPSession argonSession, SessionEvent pe)
    {
        super(disp.argonConnector(), argonSession);
        this.dispatcher = disp;
        this.stats = new RWSessionStats();
        this.sessionEvent = pe;
        if (RWSessionStats.DoDetailedTimes) {
            timesLogger = Logger.getLogger("com.untangle.uvm.vnet.SessionTimes");
        } else {
            timesLogger = null;
        }
        logger = disp.argonConnector().sessionLogger();
    }

    public short protocol()
    {
        return ((ArgonIPSession)argonSession).protocol();
    }

    public InetAddress clientAddr()
    {
        return ((ArgonIPSession)argonSession).clientAddr();
    }

    public InetAddress serverAddr()
    {
        return ((ArgonIPSession)argonSession).serverAddr();
    }

    public int clientPort()
    {
        return ((ArgonIPSession)argonSession).clientPort();
    }

    public int serverPort()
    {
        return ((ArgonIPSession)argonSession).serverPort();
    }

    public SessionStats stats()
    {
        return stats;
    }

    public SessionEvent sessionEvent()
    {
        return sessionEvent;
    }

    public void release()
    {
        // 8/8/05 jdi -- default changed to true -- finalization is
        // almost always important when it is defined.
        release(true);
    }

    // This one is for releasing once the session has been alive.
    public void release(boolean needsFinalization)
    {
        cancelTimer();

        released = true;
        this.needsFinalization = needsFinalization;
    }

    public int clientIntf()
    {
        return ((ArgonIPSession)argonSession).clientIntf();
    }

    public int serverIntf()
    {
        return ((ArgonIPSession)argonSession).serverIntf();
    }

    public boolean released()
    {
        return released;
    }

    public void scheduleTimer(long delay)
    {
        if (delay < 0)
            throw new IllegalArgumentException("Delay must be non-negative");
        // mpipe.scheduleTimer(this, delay);
    }

    public void cancelTimer()
    {
        // mpipe.cancelTimer(this);
    }

    protected Crumb getNextCrumb2Send(int side)
    {
        List<Crumb> crumbs = crumbs2write[side];
        assert crumbs != null;
        Crumb result = crumbs.get(0);
        assert result != null;
        // The following no longer applies since data can be null for ICMP packets: (5/05  jdi)
        // assert result.remaining() > 0 : "Cannot send zero length buffer";
        int len = crumbs.size() - 1;
        if (len == 0) {
            // Check if we sent em all, and if so remove the array.
            crumbs2write[side] = null;
        } else {
            crumbs.remove(0);
        }
        return result;
    }

    protected void addCrumb(int side, Crumb buf)
    {
        if (buf == null)
            return;
        
        OutgoingSocketQueue out;
        if (side == CLIENT)
            out = (argonSession).clientOutgoingSocketQueue();
        else
            out = (argonSession).serverOutgoingSocketQueue();

        if (out == null || out.isClosed()) {
            String sideName = side == CLIENT ? "client" : "server";
            logger.info("Ignoring crumb for dead " + sideName + " outgoing socket queue");
            return;
        }

        List<Crumb> crumbs = crumbs2write[side];

        if (crumbs == null) {
            crumbs = new ArrayList<Crumb>();
            crumbs2write[side] = crumbs;
        }
        crumbs.add(buf);
    }

    protected int sendCrumb(Crumb crumb, OutgoingSocketQueue out)
    {
        int size = 0;
        if (crumb instanceof DataCrumb)
            size = ((DataCrumb)crumb).limit();
        boolean success = out.write(crumb);
        if (logger.isDebugEnabled()) {
            logger.debug("writing " + crumb.type() + " crumb to " + out + ", size: " + size);
        }
        assert success;
        return size;
    }

    public void complete()
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: complete(in) for node in state " + xform.getRunState();
            logger.warn(message);
            // killSession(message);
            return;
        }
        NodeContext nodeContext = xform.getNodeContext();

        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            sendCompleteEvent();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while completing";
            // logger.error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    public void raze()
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: raze for node in state " + xform.getRunState();
            logger.warn(message);
            // No need to kill the session, it's already dead.
            // killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();

        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());
            if (released) {
                logger.debug("raze released");
            } else {
                if (logger.isDebugEnabled()) {
                    IncomingSocketQueue ourcin = (argonSession).clientIncomingSocketQueue();
                    IncomingSocketQueue oursin = (argonSession).serverIncomingSocketQueue();
                    OutgoingSocketQueue ourcout = (argonSession).clientOutgoingSocketQueue();
                    OutgoingSocketQueue oursout = (argonSession).serverOutgoingSocketQueue();
                    logger.debug("raze ourcin: " + ourcin +
                                 ", ourcout: " + ourcout + ", ourcsin: " + oursin + ", oursout: " + oursout +
                                 "  /  crumbs[CLIENT]: " + crumbs2write[CLIENT] + ", crumbs[SERVER]: " + crumbs2write[SERVER]);
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

    public void clientEvent(IncomingSocketQueue in)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: clientEvent(in) for node in state " + xform.getRunState();
            logger.warn(message);
            killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            readEvent(CLIENT, in);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    public void serverEvent(IncomingSocketQueue in)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: serverEvent(in) for node in state " + xform.getRunState();
            logger.warn(message);
            killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            readEvent(SERVER, in);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    public void clientEvent(OutgoingSocketQueue out)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: clientEvent(out) for node in state " + xform.getRunState();
            logger.warn(message);
            killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            writeEvent(CLIENT, out);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    public void serverEvent(OutgoingSocketQueue out)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: serverEvent(out) for node in state " + xform.getRunState();
            logger.warn(message);
            killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            writeEvent(SERVER, out);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
        }
    }

    /** The write side of the client has been closed from underneath
     * the node, this is the same as an EPIPE, but is delivered
     * as an event */
    public void clientOutputResetEvent(OutgoingSocketQueue out)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: output reset(client) for node in state " + xform.getRunState();
            logger.warn(message);
            // killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = (argonSession).clientIncomingSocketQueue();
            if (in != null)
                in.reset();
            sideDieing(CLIENT);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // logger.error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /** The write side of the server has been closed from underneath
     * the node, this is the same as an EPIPE, but is delivered
     * as an event */
    public void serverOutputResetEvent(OutgoingSocketQueue out)
    {
        Node xform = argonConnector().node();
        if (xform.getRunState() != NodeSettings.NodeState.RUNNING) {
            String message = "killing: output reset(server) for node in state " + xform.getRunState();
            logger.warn(message);
            // killSession(message);
            return;
        }

        NodeContext nodeContext = xform.getNodeContext();
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(nodeContext.getNodeSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = (argonSession).serverIncomingSocketQueue();
            if (in != null)
                in.reset();
            sideDieing(SERVER);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // logger.error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /**
     * <code>clientMark</code> returns the server-side socket mark for this session
     */
    public int  clientMark()
    {
        return this.argonSession.sessionGlobalState().netcapSession().clientMark();
    }

    /**
     * <code>clientMark</code> sets the server-side socket mark for this session
     */
    public void clientMark(int newmark)
    {
        this.argonSession.sessionGlobalState().netcapSession().clientMark(newmark);
    }

    /**
     * <code>orClientMark</code> bitwise ORs the provided bitmask with the current client-side conn-mark
     */
    public void orClientMark(int bitmask)
    {
        //java.lang.StringBuilder sb = new java.lang.StringBuilder();
        //java.util.Formatter formatter = new java.util.Formatter(sb, java.util.Locale.US);
        //logger.debug(formatter.format("Set ClientMark to 0x%08x",client_mark).toString()); sb.setLength(0);

        this.argonSession.sessionGlobalState().netcapSession().orClientMark(bitmask);
    }

    /**
     * <code>setClientQosMark</code> sets the connmark so this session' client-side packets get the provided QoS priority
     */
    public void setClientQosMark(int priority)
    {
        logger.debug("Set Client QosMark to " + priority);
        this.argonSession.sessionGlobalState().netcapSession().clientQosMark(priority);
    }
    
    /**
     * <code>serverMark</code> returns the server-side socket mark for this session
     */
    public int  serverMark()
    {
        return this.argonSession.sessionGlobalState().netcapSession().serverMark();
    }

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     */
    public void serverMark(int newmark)
    {
        this.argonSession.sessionGlobalState().netcapSession().serverMark(newmark);
    }

    /**
     * <code>orServerMark</code> bitwise ORs the provided bitmask with the current server-side conn-mark
     */
    public void orServerMark(int bitmask)
    {
        //java.lang.StringBuilder sb = new java.lang.StringBuilder();
        //java.util.Formatter formatter = new java.util.Formatter(sb, java.util.Locale.US);
        //logger.debug(formatter.format("Set ServerMark to 0x%08x",server_mark).toString()); sb.setLength(0);

        this.argonSession.sessionGlobalState().netcapSession().orServerMark(bitmask);
    }

    /**
     * <code>setServerQosMark</code> sets the connmark so this session' server-side packets get the provided QoS priority
     */
    public void setServerQosMark(int priority)
    {
        logger.debug("Set Server QosMark to " + priority);
        this.argonSession.sessionGlobalState().netcapSession().serverQosMark(priority);
    }
    
    
    // This is the main write hook called by the Vectoring machine
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

            IncomingSocketQueue ourin;
            OutgoingSocketQueue ourout, otherout;
            if (side == CLIENT) {
                ourin = (argonSession).serverIncomingSocketQueue();
                ourout = (argonSession).clientOutgoingSocketQueue();
                otherout = (argonSession).serverOutgoingSocketQueue();
            } else {
                ourin = (argonSession).clientIncomingSocketQueue();
                ourout = (argonSession).serverOutgoingSocketQueue();
                otherout = (argonSession).clientOutgoingSocketQueue();
            }
            assert out == ourout;

            if (logger.isDebugEnabled()) {
                logger.debug("write(" + sideName + ") out: " + out +
                             "   /  crumbs, write-queue  " +  crumbs2write[side] + ", " + out.numEvents() +
                             "(" + out.numBytes() + " bytes)" + "   opp-read-queue: " +
                             (ourin == null ? null : ourin.numEvents()));
            }

            if (!doWrite(side, ourout)) {
                sendWritableEvent(side);
                // We have to try more writing here in case we added stuff.
                doWrite(side, ourout);
                doWrite(1 - side, otherout);
            }
            if (streamer == null)
                setupForNormal();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while writing to " + sideName;
            logger.error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            MDC.remove(SESSION_ID_MDC_KEY);
        }

    }

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
            OutgoingSocketQueue cout = (argonSession).clientOutgoingSocketQueue();
            OutgoingSocketQueue sout = (argonSession).serverOutgoingSocketQueue();
            if (side == CLIENT) {
                ourin = (argonSession).clientIncomingSocketQueue();
                otherin = (argonSession).serverIncomingSocketQueue();
                ourout = sout;
                otherout = cout;
            } else {
                ourin = (argonSession).serverIncomingSocketQueue();
                otherin = (argonSession).clientIncomingSocketQueue();
                ourout = cout;
                otherout = sout;
            }
            assert in == ourin;

            if (logger.isDebugEnabled()) {
                logger.debug("read(" + sideName + ") in: " + in +
                             "   /  opp-write-crumbs: " + crumbs2write[1 - side] + ", opp-write-queue: " +
                             (ourout == null ? null : ourout.numEvents()));
            }

            assert streamer == null : "readEvent when streaming";;

            if (ourout == null || (crumbs2write[1 - side] == null && ourout.isEmpty())) {
                tryRead(side, in, true);
                doWrite(side, otherout);
                doWrite(1 - side, ourout);
                if (streamer != null) {
                    // We do this after the writes so that we try to write out first.
                    setupForStreaming();
                    return;
                }
            } else {
                logger.error("Illegal State: read(" + sideName + ") in: " + in +
                             "   /  opp-write-crumbs: " + crumbs2write[1 - side] + ", opp-write-queue: " +
                             (ourout == null ? null : ourout.numEvents()));
            }
            setupForNormal();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while reading from " + sideName;
            logger.error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    // Callback called on finalize
    protected void closeFinal()
    {
        cancelTimer();
        
        if (RWSessionStats.DoDetailedTimes) {
            long[] times = stats().times();
            times[SessionStats.FINAL_CLOSE] = MetaEnv.currentTimeMillis();
            if (timesLogger.isInfoEnabled())
                reportTimes(times);
        }

        dispatcher.removeSession(this);
    }

    boolean needsFinalization()
    {
        return needsFinalization;
    }

    /**
     * This one sets up the socket queues for streaming to begin.
     */
    private void setupForStreaming()
    {
        IncomingSocketQueue cin = (argonSession).clientIncomingSocketQueue();
        IncomingSocketQueue sin = (argonSession).serverIncomingSocketQueue();
        OutgoingSocketQueue cout = (argonSession).clientOutgoingSocketQueue();
        OutgoingSocketQueue sout = (argonSession).serverOutgoingSocketQueue();
        assert (streamer != null);

        if (cin != null)
            cin.disable();
        if (sin != null)
            sin.disable();
        if (streamer[CLIENT] != null) {
            if (cout != null)
                cout.enable();
            if (sout != null)
                sout.disable();
        } else {
            if (sout != null)
                sout.enable();
            if (cout != null)
                cout.disable();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("entering streaming mode c: " + streamer[CLIENT] + ", s: " + streamer[SERVER]);
        }
    }

    /**
     * This one sets up the socket queues for normal operation; used
     * when streaming ends.
     */
    private void setupForNormal()
    {
        IncomingSocketQueue cin = (argonSession).clientIncomingSocketQueue();
        IncomingSocketQueue sin = (argonSession).serverIncomingSocketQueue();
        OutgoingSocketQueue cout = (argonSession).clientOutgoingSocketQueue();
        OutgoingSocketQueue sout = (argonSession).serverOutgoingSocketQueue();
        assert (streamer == null);

        // We take care not to change the state unless it's really
        // changing, as changing the state calls notifymvpoll() every
        // time.
        if (sout != null && !sout.isEnabled())
            sout.enable();
        if (sout == null || (sout.isEmpty() && crumbs2write[SERVER] == null)) {
            if (cin != null && !cin.isEnabled())
                cin.enable();
        } else {
            if (cin != null && cin.isEnabled())
                cin.disable();
        }
        if (cout != null && !cout.isEnabled())
            cout.enable();
        if (cout == null || (cout.isEmpty() && crumbs2write[CLIENT] == null)) {
            if (sin != null && !sin.isEnabled())
                sin.enable();
        } else {
            if (sin != null && sin.isEnabled())
                sin.disable();
        }
    }

    /**
     * Returns true if we did something.
     *
     * @param side an <code>int</code> value
     * @param out an <code>OutgoingSocketQueue</code> value
     * @return a <code>boolean</code> value
     */
    private boolean doWrite(int side, OutgoingSocketQueue out)
        
    {
        boolean didSomething = false;
        if (out != null && out.isEmpty()) {
            if (crumbs2write[side] != null) {
                // Do this first, before checking streamer, so we
                // drain out any remaining buffer.
                tryWrite(side, out, true);
                didSomething = true;
            } else if (streamer != null) {
                IPStreamer s = streamer[side];
                if (s != null) {
                    // It's the right one.
                    addStreamBuf(side, s);
                    if (crumbs2write[side] != null) {
                        tryWrite(side, out, true);
                        didSomething = true;
                    }
                }
            }
        }
        return didSomething;
    }

    private void reportTimes(long[] times)
    {
        StringBuffer result = new StringBuffer("times for ");
        result.append(id());
        result.append("\n");

        for (int i = SessionStats.MIN_TIME_INDEX; i < SessionStats.MAX_TIME_INDEX; i++) {
            if (times[i] == 0)
                continue;
            String name = SessionStats.TimeNames[i];
            int len = name.length();
            int pad = 30 - len;
            result.append(name);
            result.append(": ");
            for (int j = 0; j < pad; j++)
                result.append(' ');
            formatter.format(new Date(times[i]), result, null);
            result.append("\n");
        }
        timesLogger.info(result.toString());
    }

    abstract public VnetSessionDesc makeDesc();

    /**
     * <code>isSideDieing</code> returns true if the incoming socket queue
     * contains an event that will cause the end of the session (at least on
     * that side). These are RST for TCP and EXPIRE for UDP.
     *
     * @param in an <code>IncomingSocketQueue</code> value
     * @return a <code>boolean</code> value
     */
    abstract protected boolean isSideDieing(int side, IncomingSocketQueue in);

    abstract protected void sideDieing(int side) ;

    abstract protected void killSession(String message);

    abstract protected void sendWritableEvent(int side) ;

    abstract protected void sendCompleteEvent() ;

    abstract protected void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable);

    abstract protected void addStreamBuf(int side, IPStreamer streamer);

    abstract protected void tryRead(int side, IncomingSocketQueue in, boolean warnIfUnable);

    abstract protected String idForMDC();

}
