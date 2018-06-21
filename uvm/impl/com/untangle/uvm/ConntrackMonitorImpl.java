/**
 * $Id: ConntrackMonitorImpl.java,v 1.00 2016/05/27 14:23:01 dmorris Exp $
 */

package com.untangle.uvm;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.app.SessionMinuteEvent;
import com.untangle.uvm.util.Pulse;

/**
 * Class to do periodic monitoring and maintenance on the internal conntrack
 * table.
 * 
 */
public class ConntrackMonitorImpl
{
    private static final int CONNTRACK_PULSE_FREQUENCY_MS = 60 * 1000; // 1 minute
    private static final float CONNTRACK_PULSE_FREQUENCY_SEC = 60f; // 1 minute
    private static final int CLEANER_PULSE_FREQUENCY = 560 * 1000; // 5 minutes
    private static final long LIFETIME_MS = (long) 1000 * 60 * 3; // 2 minutes (amount of time to keep complete sessions in table)
    private static final Logger logger = Logger.getLogger(ConntrackMonitorImpl.class);

    private static ConntrackMonitorImpl INSTANCE = null;

    private final Pulse mainPulse;

    private final Pulse deadTcpSessionsCleaner;

    private LinkedHashMap<SessionTuple, ConntrackEntryState> conntrackEntries = new LinkedHashMap<SessionTuple, ConntrackEntryState>();
    private LinkedHashMap<SessionTuple, DeadTcpSessionState> deadTcpSessions = new LinkedHashMap<SessionTuple, DeadTcpSessionState>();

    /**
     * Constructor
     */
    private ConntrackMonitorImpl()
    {
        this.mainPulse = new Pulse("conntrack-monitor", new ConntrackPulse(), CONNTRACK_PULSE_FREQUENCY_MS, calculateInitialDelay());
        this.mainPulse.start();

        this.deadTcpSessionsCleaner = new Pulse("conntrack-dead-tcp-table-cleaner", new TcpCompletedSessionsCleaner(), CLEANER_PULSE_FREQUENCY);
        this.deadTcpSessionsCleaner.start();
    }

    /**
     * Get the singleton instance
     * 
     * @return The instance
     */
    public static ConntrackMonitorImpl getInstance()
    {
        if (INSTANCE == null) INSTANCE = new ConntrackMonitorImpl();

        return INSTANCE;
    }

    /**
     * Lookup a conntrack entry for a given tuple
     * 
     * @param sessionTuple
     *        The tupple
     * @return The corresponding conntrack entry
     */
    public ConntrackEntryState lookupTuple(SessionTuple sessionTuple)
    {
        if (sessionTuple == null) return null;
        synchronized (ConntrackMonitorImpl.INSTANCE) {
            return conntrackEntries.get(sessionTuple);
        }
    }

    /**
     * Stop the monitor
     */
    protected void stop()
    {
        this.mainPulse.stop();
        this.deadTcpSessionsCleaner.stop();
    }

    /**
     * Add a session to the dead TCP session table
     * 
     * @param session
     *        The session to add
     */
    protected void addDeadTcpSession(SessionGlobalState session)
    {
        if (session == null) {
            logger.warn("Invalid arguments");
            return;
        }
        if (session.getClientSideTuple() == null) {
            logger.warn("Missing tuple in session: " + session);
            return;
        }
        if (session.getEndTime() == 0) {
            logger.warn("Session has not ended: " + session);
            return;
        }

        DeadTcpSessionState state = new DeadTcpSessionState(session.id(), session.getEndTime());
        if (logger.isDebugEnabled()) {
            logger.debug("Adding to deadTcpSessions: " + session.getClientSideTuple());
        }
        synchronized (this.deadTcpSessions) {
            this.deadTcpSessions.put(session.getClientSideTuple(), state);
        }
    }

    /**
     * We want it to always run 1ms after the new minute. We set
     * extraInitialDelay = negative milliseconds in current minute (8:00:40.500
     * = -40000-500+1 = -44999) this plus the normal delay (60000) means it will
     * start exactly after the new minute starts. 8:00:40.500 +60000 -44449 =
     * 8:01:00.001
     * 
     * @return
     */
    private long calculateInitialDelay()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int milli = cal.get(Calendar.MILLISECOND);
        int sec = cal.get(Calendar.SECOND);
        long extraInitialDelay = 0 - ((long) sec * 1000 + milli) + 1;
        return extraInitialDelay;
    }

    /**
     * Class for periodic operations
     */
    private class ConntrackPulse implements Runnable
    {
        /**
         * Do the accounting calculations
         * 
         * @param conntrack
         *        The conntrack
         * @param state
         *        The conntrack entry state
         * @param desc
         *        The description
         * @param tuple
         *        The tuple
         */
        private void doAccounting(Conntrack conntrack, ConntrackEntryState state, String desc, SessionTuple tuple)
        {
            long oldC2sBytes = state.c2sBytes;
            long newC2sBytes = conntrack.getOriginalCounterBytes();
            long oldS2cBytes = state.s2cBytes;
            long newS2cBytes = conntrack.getReplyCounterBytes();
            long oldTotalBytes = state.totalBytes;
            long newTotalBytes = (newC2sBytes + newS2cBytes);
            long diffC2sBytes = newC2sBytes - oldC2sBytes;
            long diffS2cBytes = newS2cBytes - oldS2cBytes;
            long diffTotalBytes = newTotalBytes - oldTotalBytes;
            float c2sRateBps = (diffC2sBytes / (CONNTRACK_PULSE_FREQUENCY_SEC));
            float s2cRateBps = (diffS2cBytes / (CONNTRACK_PULSE_FREQUENCY_SEC));
            float totalRateBps = (diffTotalBytes / (CONNTRACK_PULSE_FREQUENCY_SEC));

            /**
             * In some cases specifically UDP a new session takes the place of
             * an old session with the same tuple in this case the counts go
             * down because its actually a new session. If the total bytes is
             * low, this is probably the case and just assume it went from zero
             * to current total bytes If not, just discard the event with a
             * warning
             */
            if (diffC2sBytes < 0 || diffS2cBytes < 0) {
                if (diffC2sBytes < 0) {
                    logger.warn("Negative diffC2sBytes: " + diffC2sBytes + " oldC2sBytes: " + oldC2sBytes + " newC2sBytes: " + newC2sBytes);
                    logger.warn("Action: " + desc);
                    logger.warn("Conntrack: " + conntrack.toSummaryString());
                    logger.warn("Tuple: " + tuple);
                }
                if (diffS2cBytes < 0) {
                    logger.warn("Negative diffS2cBytes: " + diffS2cBytes + " oldS2cBytes: " + oldS2cBytes + " newS2cBytes: " + newS2cBytes);
                    logger.warn("Action: " + desc);
                    logger.warn("Conntrack: " + conntrack.toSummaryString());
                    logger.warn("Tuple: " + tuple);
                }
                return;
            }

            state.c2sBytes = newC2sBytes;
            state.s2cBytes = newS2cBytes;
            state.totalBytes = newTotalBytes;
            state.c2sRateBps = c2sRateBps;
            state.s2cRateBps = s2cRateBps;
            state.totalRateBps = totalRateBps;

            if (logger.isDebugEnabled()) {
                logger.debug("CONNTRACK " + desc + " | " + conntrack.toSummaryString() + " client: " + Math.round(c2sRateBps / 1000.0) + " kB/s" + " server: " + Math.round(s2cRateBps / 1000.0) + " kB/s" + " total: " + Math.round(totalRateBps / 1000.0) + " kB/s");
            }

            // do quota accounting
            doQuotaAccounting(conntrack.getPreNatClient(), diffTotalBytes);
            doQuotaAccounting(conntrack.getPostNatServer(), diffTotalBytes);

            // log SessionMinute event
            SessionMinuteEvent event = new SessionMinuteEvent(state.sessionId, diffC2sBytes, diffS2cBytes, conntrack.getTimeStampStartMillis());
            UvmContextFactory.context().logEvent(event);
        }

        /**
         * Do the quota accounting
         * 
         * @param address
         *        The address
         * @param bytes
         *        The number of bytes
         */
        private void doQuotaAccounting(InetAddress address, long bytes)
        {
            if (bytes == 0) return; /* no data with this event. return */

            UvmContextFactory.context().hostTable().decrementQuota(address, bytes);

            com.untangle.uvm.HostTableEntry hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry(address);
            if (hostEntry != null && hostEntry.getUsername() != null && !"".equals(hostEntry.getUsername())) {
                UvmContextFactory.context().userTable().decrementQuota(hostEntry.getUsername(), bytes);
            }
        }

        /**
         * Periodic run function
         */
        public void run()
        {
            LinkedHashMap<SessionTuple, ConntrackEntryState> oldConntrackEntries = conntrackEntries;
            LinkedHashMap<SessionTuple, ConntrackEntryState> newConntrackEntries = new LinkedHashMap<SessionTuple, ConntrackEntryState>(conntrackEntries.size() * 2);

            List<Conntrack> dumpEntries = com.untangle.jnetcap.Netcap.getInstance().getConntrackDump();
            boolean logBypassed = UvmContextFactory.context().networkManager().getNetworkSettings().getLogBypassedSessions();

            synchronized (ConntrackMonitorImpl.INSTANCE) {
                String type = null;
                for (Conntrack conntrack : dumpEntries) {
                    try {
                        long sessionId = 0;
                        SessionTuple tuple = new SessionTuple(conntrack.getProtocol(), conntrack.getPreNatClient(), conntrack.getPreNatServer(), conntrack.getPreNatClientPort(), conntrack.getPreNatServerPort());
                        /**
                         * Lookup the session from the previous conntrack
                         * entries
                         */
                        ConntrackEntryState state = oldConntrackEntries.remove(tuple);

                        /**
                         * In some cases (especially UDP) a conntrack entry
                         * expires, and shortly after a new session replaces it
                         * with the same tuple. If this is the case then set the
                         * state to null so that we resolve it like a new
                         * session.
                         */
                        if (state != null && (state.sessionStartTime != conntrack.getTimeStampStart())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Conntrack does not match " + tuple + " " + state.sessionStartTime + " != " + conntrack.getTimeStampStart());
                            }
                            state = null;
                        }

                        /**
                         * If we already know about this session, then pull the
                         * sessionId from the state
                         */
                        if (state != null) {
                            type = "existing";
                            sessionId = state.sessionId;
                            tuple = state.tuple;
                        }
                        /**
                         * If we don't know about this session, ask the
                         * Conntrack Hook to see if it knows about it The
                         * Connntrack Hook stores a list of all the "bypassed"
                         * sessions and all of the conntrack IDs and session IDs
                         * for those sessions.
                         */
                        if (sessionId == 0) {
                            Long sid = NetcapConntrackHook.getInstance().lookupSessionId(tuple);
                            if (sid != null) {
                                if (!logBypassed) continue;
                                type = "bypassed";
                                sessionId = sid;
                            }
                        }
                        /**
                         * If we still don't know it, lookup the tuple in the
                         * session table for a live session or lookup the tuple
                         * in the recently completed tcp session table
                         */
                        if (sessionId == 0) {
                            SessionGlobalState session;
                            if (sessionId == 0) {
                                session = SessionTableImpl.getInstance().lookupTuple(tuple);
                                if (session != null) {
                                    type = "active  ";
                                    sessionId = session.id();
                                }
                            }

                            if (sessionId == 0) {
                                DeadTcpSessionState deadSession = null;
                                synchronized (deadTcpSessions) {
                                    deadSession = deadTcpSessions.get(tuple);
                                }
                                if (deadSession != null) {
                                    type = "timewait";
                                    sessionId = deadSession.sessionId;
                                }
                            }
                        }
                        if (sessionId == 0) {
                            // unable to find the session ID for this session
                            // we can't log events without a session ID
                            continue;
                        }

                        String action = null;
                        if (state == null) {
                            action = "NEW    ";
                            state = new ConntrackEntryState(sessionId, tuple, conntrack.getTimeStampStart());
                        } else {
                            action = "UPDATE ";
                        }

                        // put the entry in the new map
                        newConntrackEntries.put(tuple, state);

                        // log event 
                        doAccounting(conntrack, state, action + type, tuple);
                    } finally {
                        // free the conntrack
                        conntrack.raze();
                    }
                }

                /**
                 * Iterate through sessions that we not in the current conntrack
                 * dump These have disappeared from the new entries so they are
                 * dead. If they are TCP remove them from deadTcpSessions. If
                 * they are UDP remove the from the sessiontable if they still
                 * exist.
                 */
                for (ConntrackEntryState state : oldConntrackEntries.values()) {
                    if (state.tuple != null && state.tuple.getProtocol() == 6) {
                        DeadTcpSessionState deadSession;
                        synchronized (deadTcpSessions) {
                            deadSession = deadTcpSessions.remove(state.tuple);
                        }
                        if (logger.isDebugEnabled()) {
                            if (deadSession != null) logger.debug("Removed session from deadTcpSessions: " + state.tuple);
                            else logger.debug("Failed to remove session from deadTcpSessions: " + state.tuple);
                        }
                    }
                }

                /**
                 * Replace the original map with the new one
                 */
                conntrackEntries = newConntrackEntries;
            }
        }
    }

    /**
     * The TcpCompletedSessionsCleaner task goes through and cleans up old
     * entries in deadTcpSessions
     * 
     * Usually sessions are removed from deadTcpSessions when the conntrack
     * disappears However, in some cases the conntrack (which is the client side
     * of the connection) is closed and disappears before the session thread
     * closes the server side and exits In this case the session gets added to
     * deadTcpSessions after the conntrack has disappeared. In this case this
     * task will remove this session after its old enough
     */
    private class TcpCompletedSessionsCleaner implements Runnable
    {
        /**
         * Periodic run function
         */
        public void run()
        {
            long now = System.currentTimeMillis();

            synchronized (deadTcpSessions) {
                for (Iterator<Map.Entry<SessionTuple, DeadTcpSessionState>> i = deadTcpSessions.entrySet().iterator(); i.hasNext();) {
                    Map.Entry<SessionTuple, DeadTcpSessionState> entry = i.next();
                    SessionTuple tuple = entry.getKey();
                    DeadTcpSessionState state = entry.getValue();
                    if (state == null) {
                        logger.warn("Invalid state: " + state);
                        continue;
                    }
                    if (state.endTime == 0) {
                        logger.warn("Invalid endTime: " + state.endTime + " session: " + tuple);
                    }
                    if (now - state.endTime > LIFETIME_MS) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Manually removing session from deadTcpSessions: " + tuple);
                        }
                        i.remove();
                    } else {
                        // Because we are using a LinkedHashMap, they ordering is maintained and the younger elements are later in the list
                        // Because this entry has not yet expired, none of the entries after it have expired either
                        // We can quit iteration now.
                        break;
                    }
                }
            }
            return;
        }
    }

    /**
     * Class to represent a conntrack entry state
     */
    public class ConntrackEntryState
    {
        protected long sessionId;
        protected long sessionStartTime;
        protected SessionTuple tuple;
        protected long c2sBytes = 0;
        protected long s2cBytes = 0;
        protected long totalBytes = 0;
        protected float c2sRateBps = 0.0f;
        protected float s2cRateBps = 0.0f;
        protected float totalRateBps = 0.0f;

        /**
         * Constructor
         * 
         * @param sessionId
         *        The session ID
         * @param tuple
         *        The tuple
         * @param sessionStartTime
         *        The session start time
         */
        protected ConntrackEntryState(long sessionId, SessionTuple tuple, long sessionStartTime)
        {
            this.sessionId = sessionId;
            this.sessionStartTime = sessionStartTime;
            this.tuple = tuple;
        }
    }

    /**
     * Class to represent a dead TCP session
     */
    private class DeadTcpSessionState
    {
        protected long sessionId;
        protected long endTime;

        /**
         * Constructor
         * 
         * @param sessionId
         *        The session ID
         * @param endTime
         *        The end time
         */
        protected DeadTcpSessionState(long sessionId, long endTime)
        {
            this.sessionId = sessionId;
            this.endTime = endTime;
        }
    }
}
