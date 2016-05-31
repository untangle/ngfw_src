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

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.node.SessionTupleImpl;
import com.untangle.uvm.node.SessionMinuteEvent;
import com.untangle.uvm.util.Pulse;

public class ConntrackMonitorImpl
{
    private static final int CONNTRACK_PULSE_FREQUENCY = 60*1000; /* 1 minute */
    private static final int CLEANER_PULSE_FREQUENCY = 560*1000; /* 5 minutes */
    private static final long LIFETIME_MS = 1000*60*2; /* 2 minutes */ /* Amount of time to keep complete sessions in table */
    private static final Logger logger = Logger.getLogger(ConntrackMonitorImpl.class);
    
    private static ConntrackMonitorImpl INSTANCE = null;

    private final Pulse mainPulse;

    private final Pulse deadTcpSessionsCleaner;

    private LinkedHashMap<SessionTupleImpl,ConntrackEntryState> conntrackEntries = new LinkedHashMap<SessionTupleImpl,ConntrackEntryState>();
    private LinkedHashMap<SessionTupleImpl,DeadTcpSessionState> deadTcpSessions = new LinkedHashMap<SessionTupleImpl,DeadTcpSessionState>();
    
    private ConntrackMonitorImpl()
    {
        this.mainPulse = new Pulse("conntrack-monitor", new ConntrackPulse(), CONNTRACK_PULSE_FREQUENCY, calculateInitialDelay());
        this.mainPulse.start();

        this.deadTcpSessionsCleaner = new Pulse("conntrack-dead-tcp-table-cleaner", new TcpCompletedSessionsCleaner(), CLEANER_PULSE_FREQUENCY);
        this.deadTcpSessionsCleaner.start();
    }

    public static ConntrackMonitorImpl getInstance()
    {
        if ( INSTANCE == null )
            INSTANCE = new ConntrackMonitorImpl();
        
        return INSTANCE;
    }

    protected void stop()
    {
        this.mainPulse.stop();
        this.deadTcpSessionsCleaner.stop();
    }

    protected void addDeadTcpSession( SessionGlobalState session )
    {
        if ( session == null ) {
            logger.warn("Invalid arguments");
            return;
        }
        if ( session.getSessionTuple() == null ) {
            logger.warn("Missing tuple in session: " + session);
            return;
        }
        if ( session.getEndTime() == 0 ) {
            logger.warn("Session has not ended: " + session);
            return;
        }
        
        DeadTcpSessionState state = new DeadTcpSessionState( session.id(), session.getEndTime() );
        synchronized ( this.deadTcpSessions ) {
            this.deadTcpSessions.put( session.getSessionTuple(), state );
        }
    }

    private long calculateInitialDelay()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());  
        int milli = cal.get(Calendar.MILLISECOND);
        int sec = cal.get(Calendar.SECOND);
        /**
         * We want it to always run 1ms after the new minute.
         * We se extraInitialDelay = negative milliseconds in current minute (8:00:40.500 = -40000-500+1 = -44999)
         * this plus the normal delay (60000) means it will start exactly after the new minute starts.
         * 8:00:40.500 +60000 -44449 = 8:01:00.001
         */
         long extraInitialDelay = 0-(sec*1000+milli)+1; 
        return extraInitialDelay;
    }
    
    private class ConntrackPulse implements Runnable
    {
        private void doAccounting( Conntrack conntrack, ConntrackEntryState state, String desc, SessionTupleImpl tuple )
        {
            long oldC2sBytes = state.c2sBytes;
            long newC2sBytes = conntrack.getOriginalCounterBytes();
            long oldS2cBytes = state.s2cBytes;
            long newS2cBytes = conntrack.getReplyCounterBytes();
            long diffC2sBytes = newC2sBytes - oldC2sBytes;
            long diffS2cBytes = newS2cBytes - oldS2cBytes;
            state.c2sBytes = newC2sBytes;
            state.s2cBytes = newS2cBytes;
            if ( diffC2sBytes < 0 ) {
                logger.warn("Negative diffC2sBytes: " + diffC2sBytes + " oldC2sBytes: " + oldC2sBytes + " newC2sBytes: " + newC2sBytes);
                logger.warn("Action: " + desc);
                logger.warn("Conntrack: " + conntrack.toSummaryString());
                logger.warn("Tuple: " + tuple);
            }
            if ( diffS2cBytes < 0 ) {
                logger.warn("Negative diffS2cBytes: " + diffS2cBytes + " oldS2cBytes: " + oldS2cBytes + " newS2cBytes: " + newS2cBytes);
                logger.warn("Action: " + desc);
                logger.warn("Conntrack: " + conntrack.toSummaryString());
                logger.warn("Tuple: " + tuple);
            }
            logger.info("CONNTRACK " + desc + " | " + conntrack.toSummaryString() + " client: " + (((diffC2sBytes)/60)/1000) + "kB/s" + " server: "+ (((diffS2cBytes/60)/1000)) + "kB/s");

            //log event
            SessionMinuteEvent event = new SessionMinuteEvent( state.sessionId, diffC2sBytes, diffS2cBytes );
            UvmContextFactory.context().logEvent( event );
        }
        
        public synchronized void run()
        {
            LinkedHashMap<SessionTupleImpl,ConntrackEntryState> oldConntrackEntries = conntrackEntries;
            LinkedHashMap<SessionTupleImpl,ConntrackEntryState> newConntrackEntries = new LinkedHashMap<SessionTupleImpl, ConntrackEntryState>(conntrackEntries.size()*2);
                
            List<Conntrack> dumpEntries = com.untangle.jnetcap.Netcap.getInstance().getConntrackDump();            

            String type = null;
            for ( Conntrack conntrack : dumpEntries ) {
                SessionTupleImpl tuple = new SessionTupleImpl( conntrack.getProtocol(),
                                                               conntrack.getClientIntf(),
                                                               conntrack.getServerIntf(),
                                                               conntrack.getPreNatClient(),
                                                               conntrack.getPreNatServer(),
                                                               conntrack.getPreNatClientPort(),
                                                               conntrack.getPreNatServerPort() );
                ConntrackEntryState state = oldConntrackEntries.remove( tuple );
                DeadTcpSessionState deadSession = null;
                
                long sessionId = 0;
                /**
                 * If we already know about this session, then pull the sessionId from the state
                 */
                if ( state != null ) {
                    type = "existing";
                    sessionId = state.sessionId;
                    tuple = state.tuple;
                }
                /**
                 * If we don't know about this session, ask the Conntrack Hook to see if it knows about it
                 * The Connntrack Hook stores a list of all the "bypassed" sessions and all of the conntrack IDs and session IDs
                 * for those sessions.
                 */
                if ( sessionId == 0 ) {
                    Long sid = NetcapConntrackHook.getInstance().lookupSessionId( tuple );
                    if ( sid != null ) {
                        type = "bypassed";
                        sessionId = sid;
                    }
                }
                /**
                 * If we still don't know it,
                 * lookup the tuple in the session table for a live session
                 * or lookup the tuple in the recently completed tcp session table
                 */
                if ( sessionId == 0 ) {
                    SessionGlobalState session;
                    if ( sessionId == 0 ) {
                        session = SessionTableImpl.getInstance().lookupTuple( tuple );
                        if ( session != null ) {
                            type = "active  ";
                            sessionId = session.id();
                        }
                    }
                        
                    if ( sessionId == 0 ) {
                        synchronized ( deadTcpSessions ) {
                            deadSession = deadTcpSessions.get( tuple );
                        }
                        if ( deadSession != null ) {
                            type = "timewait";
                            sessionId = deadSession.sessionId;
                        }
                    }
                }
                if ( sessionId == 0 ) {
                    // unable to find the session ID for this session
                    // we can't log events without a session ID
                    conntrack.raze();
                    continue;
                } 


                String action = null;
                if ( state == null ) {
                    action = "NEW    ";
                    state = new ConntrackEntryState( sessionId, tuple );
                } else {
                    action = "UPDATE ";
                }

                // put the entry in the new map
                newConntrackEntries.put( tuple, state );

                // log event 
                doAccounting( conntrack, state, action + type, tuple );

                // free the conntrack
                conntrack.raze();
            }

            /**
             * Iterate through sessions that we not in the current conntrack dump
             * These have disappeared from the new entries so they are dead.
             * If they are TCP remove them from deadTcpSessions.
             */
            for ( ConntrackEntryState state : oldConntrackEntries.values() ) {
                if ( state.tuple != null && state.tuple.getProtocol() == 6 ) {
                    DeadTcpSessionState deadSession = deadTcpSessions.remove( state.tuple );
                    if ( deadSession != null ) {
                        logger.debug("Removed session from deadTcpSessions: " + state.tuple);
                    }
                }
            }
            
            /**
             * Replace the original map with the new one
             */
            conntrackEntries = newConntrackEntries;
        }
    }

    private class TcpCompletedSessionsCleaner implements Runnable
    {
        public void run()
        {
            long now = System.currentTimeMillis();
            
            synchronized( deadTcpSessions ) {
                for(Iterator<Map.Entry<SessionTupleImpl, DeadTcpSessionState>> i = deadTcpSessions.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry<SessionTupleImpl, DeadTcpSessionState> entry = i.next();
                    SessionTupleImpl tuple = entry.getKey();
                    DeadTcpSessionState state = entry.getValue();
                    if ( state == null ) {
                        logger.warn("Invalid state: " + state);
                        continue;
                    }
                    if ( state.endTime == 0 ) {
                        logger.warn("Invalid endTime: " + state.endTime + " session: " + tuple );
                    }
                    if( now - state.endTime > LIFETIME_MS ) {
                        logger.warn("Manually removing session from deadTcpSessions: " + tuple);
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

    private class ConntrackEntryState
    {
        protected long sessionId;
        protected SessionTupleImpl tuple;
        protected long c2sBytes = 0;
        protected long s2cBytes = 0;
        
        protected ConntrackEntryState( long sessionId, SessionTupleImpl tuple )
        {
            this.sessionId = sessionId;
            this.tuple = tuple;
        }
        
    }

    private class DeadTcpSessionState
    {
        protected long sessionId;
        protected long endTime;
            
        protected DeadTcpSessionState( long sessionId, long endTime )
        {
            this.sessionId = sessionId;
            this.endTime = endTime;
        }
    }
}

