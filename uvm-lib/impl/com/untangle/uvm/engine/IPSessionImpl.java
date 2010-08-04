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
import com.untangle.uvm.argon.PipelineListener;
import com.untangle.uvm.argon.ArgonIPSession;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.event.IPStreamer;

/**
 * Abstract base class for all IP live sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class IPSessionImpl
    extends SessionImpl
    implements IPSession, PipelineListener
{
    protected boolean released = false;
    protected boolean needsFinalization = true;

    protected final Dispatcher dispatcher;

    protected final PipelineEndpoints pipelineEndpoints;

    @SuppressWarnings("unchecked") //generics array creation not supported java6
    protected final List<Crumb>[] crumbs2write = new ArrayList[] { null, null };

    protected IPStreamer[] streamer = null;

    protected Logger logger;

    protected final RWSessionStats stats;

    private final Logger timesLogger;

    private final NodeManagerImpl nodeManager;

    protected IPSessionImpl(Dispatcher disp,
                            ArgonIPSession argonSession,
                            PipelineEndpoints pe)
    {
        super(disp.mPipe(), argonSession);
        this.dispatcher = disp;
        this.stats = new RWSessionStats();
        this.pipelineEndpoints = pe;
        if (RWSessionStats.DoDetailedTimes) {
            timesLogger = Logger.getLogger("com.untangle.uvm.vnet.SessionTimes");
        } else {
            timesLogger = null;
        }
        nodeManager = UvmContextImpl.getInstance().localNodeManager();
        logger = disp.mPipe().sessionLogger();
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

    public PipelineEndpoints pipelineEndpoints()
    {
        return pipelineEndpoints;
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

        // We could theoretically detach the attachment here (maybe
        // only when !needsFinalization) XXX

        /** Someday...
            try {
            Mnp req = RequestUtil.createReleaseNewSession();
            ReleaseNewSessionType rl = req.getReleaseNewSession();
            rl.setSessId(id);
            mpipe.requestNoReply(req);
            } catch (MPipeException x) {
            // Not expected, just log
            mpipe.sessionLogger().warn("Exception releasing new session", x);
            }
        */
        released = true;
        this.needsFinalization = needsFinalization;
        // Do more eventually (closing sockets.) XX
    }

    public byte clientIntf()
    {
        return ((ArgonIPSession)argonSession).clientIntf();
    }

    public byte serverIntf()
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

    protected Crumb getNextCrumb2Send(int side) {
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
        if (buf == null
            // The following no longer applies since data can be null for ICMP packets: (5/05  jdi)
            // assert result.remaining() > 0 : "Cannot send zero length buffer";
            //  buf.remaining() == 0
            )
            // Skip it.
            return;
        OutgoingSocketQueue out;
        if (side == CLIENT)
            out = (argonSession).clientOutgoingSocketQueue();
        else
            out = (argonSession).serverOutgoingSocketQueue();
        if (out == null || out.isClosed()) {
            String sideName = side == CLIENT ? "client" : "server";
            warn("Ignoring crumb for dead " + sideName + " outgoing socket queue");
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
            debug("writing " + crumb.type() + " crumb to " + out + ", size: " + size);
        }
        assert success;
        return size;
    }

    public void complete()
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: complete(in) for node in state " + xform.getRunState();
            warn(message);
            // killSession(message);
            return;
        }
        NodeContext tctx = xform.getNodeContext();

        try {
            nodeManager.registerThreadContext(tctx);
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            sendCompleteEvent();
        } catch (MPipeException x) {
            String message = "MPipeException while completing";
            error(message, x);
            // killSession(message);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while completing";
            // error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            nodeManager.deregisterThreadContext();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    public void raze()
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: raze for node in state " + xform.getRunState();
            warn(message);
            // No need to kill the session, it's already dead.
            // killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();

        try {
            nodeManager.registerThreadContext(tctx);
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());
            if (released) {
                debug("raze released");
            } else {
                if (logger.isDebugEnabled()) {
                    IncomingSocketQueue ourcin = (argonSession).clientIncomingSocketQueue();
                    IncomingSocketQueue oursin = (argonSession).serverIncomingSocketQueue();
                    OutgoingSocketQueue ourcout = (argonSession).clientOutgoingSocketQueue();
                    OutgoingSocketQueue oursout = (argonSession).serverOutgoingSocketQueue();
                    debug("raze ourcin: " + ourcin +
                          ", ourcout: " + ourcout + ", ourcsin: " + oursin + ", oursout: " + oursout +
                          "  /  crumbs[CLIENT]: " + crumbs2write[CLIENT] + ", crumbs[SERVER]: " + crumbs2write[SERVER]);
                }
            }
            closeFinal();
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " in raze";
            error(message, x);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            nodeManager.deregisterThreadContext();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    public void clientEvent(IncomingSocketQueue in)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: clientEvent(in) for node in state " + xform.getRunState();
            warn(message);
            killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            readEvent(CLIENT, in);
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    public void serverEvent(IncomingSocketQueue in)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: serverEvent(in) for node in state " + xform.getRunState();
            warn(message);
            killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            readEvent(SERVER, in);
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    public void clientEvent(OutgoingSocketQueue out)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: clientEvent(out) for node in state " + xform.getRunState();
            warn(message);
            killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            writeEvent(CLIENT, out);
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    public void serverEvent(OutgoingSocketQueue out)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: serverEvent(out) for node in state " + xform.getRunState();
            warn(message);
            killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            writeEvent(SERVER, out);
        } finally {
            nodeManager.deregisterThreadContext();
        }
    }

    /** The write side of the client has been closed from underneath
     * the node, this is the same as an EPIPE, but is delivered
     * as an event */
    public void clientOutputResetEvent(OutgoingSocketQueue out)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: output reset(client) for node in state " + xform.getRunState();
            warn(message);
            // killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = (argonSession).clientIncomingSocketQueue();
            if (in != null)
                in.reset();
            sideDieing(CLIENT);
        } catch (MPipeException x) {
            String message = "MPipeException while output resetting";
            error(message, x);
            // killSession(message);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            nodeManager.deregisterThreadContext();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    /** The write side of the server has been closed from underneath
     * the node, this is the same as an EPIPE, but is delivered
     * as an event */
    public void serverOutputResetEvent(OutgoingSocketQueue out)
    {
        Node xform = mPipe().node();
        if (xform.getRunState() != NodeState.RUNNING) {
            String message = "killing: output reset(server) for node in state " + xform.getRunState();
            warn(message);
            // killSession(message);
            return;
        }

        NodeContext tctx = xform.getNodeContext();
        try {
            nodeManager.registerThreadContext(tctx);
            MDC.put(SESSION_ID_MDC_KEY, idForMDC());

            IncomingSocketQueue in = (argonSession).serverIncomingSocketQueue();
            if (in != null)
                in.reset();
            sideDieing(SERVER);
        } catch (MPipeException x) {
            String message = "MPipeException while output resetting";
            error(message, x);
            // killSession(message);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while output resetting";
            // error(message, x);
            killSession(message);
        } catch (OutOfMemoryError x) {
            UvmContextImpl.getInstance().fatalError("SessionHandler", x);
        } finally {
            nodeManager.deregisterThreadContext();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
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
            debug("entering streaming mode c: " + streamer[CLIENT] + ", s: " + streamer[SERVER]);
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
        throws MPipeException
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

    //==================================================
    // 8/2/05 - wrs.  Added "MDC" stuff to cause
    // the session ID to be accessible from the
    // log appender
    //
    //===================================================

    // This is the main write hook called by the Vectoring machine
    public void writeEvent(int side, OutgoingSocketQueue out)
    {
        String sideName = side == CLIENT ? "client" : "server";
        MDC.put(SESSION_ID_MDC_KEY, idForMDC());
        try {
            assert out != null;
            if (!out.isEmpty()) {
                warn("writeEvent to non empty outgoing queue on: " + sideName);
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
                debug("write(" + sideName + ") out: " + out +
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
        } catch (MPipeException x) {
            String message = "MPipeException while writing to " + sideName;
            error(message, x);
            killSession(message);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while writing to " + sideName;
            error(message, x);
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
                warn("ignoring readEvent called for disabled side " + side);
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
                debug("read(" + sideName + ") in: " + in +
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
                error("Illegal State: read(" + sideName + ") in: " + in +
                      "   /  opp-write-crumbs: " + crumbs2write[1 - side] + ", opp-write-queue: " +
                      (ourout == null ? null : ourout.numEvents()));
            }
            setupForNormal();
        } catch (MPipeException x) {
            String message = "MPipeException while reading from " + sideName;
            error(message, x);
            killSession(message);
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " while reading from " + sideName;
            error(message, x);
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

    protected void error(String message)
    {
        logger.error(message.toString());
    }

    protected void error(String message, Exception x)
    {
        logger.error(message.toString(), x);
    }

    protected void warn(String message)
    {
        logger.warn(message.toString());
    }

    protected void warn(String message, Exception x)
    {
        logger.warn(message.toString(), x);
    }

    protected void info(String message)
    {
        if (logger.isInfoEnabled()) {
            logger.info(message.toString());
        }
    }

    protected void debug(String message)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(message.toString());
        }
    }

    protected void debug(String message, Exception x)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(message.toString(), x);
        }
    }

    /**
     * <code>isSideDieing</code> returns true if the incoming socket queue
     * contains an event that will cause the end of the session (at least on
     * that side). These are RST for TCP and EXPIRE for UDP.
     *
     * @param in an <code>IncomingSocketQueue</code> value
     * @return a <code>boolean</code> value
     */
    abstract protected boolean isSideDieing(int side, IncomingSocketQueue in);

    abstract protected void sideDieing(int side) throws MPipeException;

    abstract void killSession(String message);

    public abstract IPSessionDesc makeDesc();

    abstract void sendWritableEvent(int side) throws MPipeException;

    abstract void sendCompleteEvent() throws MPipeException;

    abstract void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable)
        throws MPipeException;

    abstract void addStreamBuf(int side, IPStreamer streamer)
        throws MPipeException;

    abstract void tryRead(int side, IncomingSocketQueue in, boolean warnIfUnable)
        throws MPipeException;

    abstract String idForMDC();

    private static DateFormat formatter = new AbsoluteTimeDateFormat();

    private void reportTimes(long[] times) {
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


    // Don't need equal or hashcode since we can only have one of
    // these objects per session (so the memory address is ok for
    // equals/hashcode).
}
