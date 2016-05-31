/**
 * $Id: ConntrackMonitorImpl.java,v 1.00 2016/05/27 14:23:01 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.node.SessionTupleImpl;
import com.untangle.uvm.node.SessionMinuteEvent;
import com.untangle.uvm.util.Pulse;

public class ConntrackMonitorImpl
{
    private static final Logger logger = Logger.getLogger(ConntrackMonitorImpl.class);
    private static final int FREQUENCY = 60*1000; /* 60 seconds */
    
    private final Pulse pulse;
    private LinkedHashMap<Long,ConntrackEntryState> conntrackEntries = new LinkedHashMap<Long,ConntrackEntryState>();

    public ConntrackMonitorImpl()
    {
        this.pulse = new Pulse("conntrack-monitor", new ConntrackPulse(), FREQUENCY, calculateInitialDelay());
        this.pulse.start();
    }

    protected void stop()
    {
        pulse.stop();
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
        private void doAccounting( Conntrack conntrack, ConntrackEntryState state, String action )
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
                logger.warn("Negative diffC2sBytes: " + diffC2sBytes + " oldC2sBytes: " + oldC2sBytes + " newC2sBytes: " + newC2sBytes + " action: " + action + " conntrack: " + conntrack.toSummaryString());
            }
            if ( diffS2cBytes < 0 ) {
                logger.warn("Negative diffS2cBytes: " + diffS2cBytes + " oldS2cBytes: " + oldS2cBytes + " newS2cBytes: " + newS2cBytes + " action: " + action + " conntrack: " + conntrack.toSummaryString());
            }
            
            logger.info("CONNTRACK " + action + "| " + conntrack.toSummaryString() + " client: " + (((diffC2sBytes)/60)/1000) + "kB/s" + " server: "+ (((diffS2cBytes/60)/1000)) + "kB/s");

            //log event
            SessionMinuteEvent event = new SessionMinuteEvent( state.sessionId, diffC2sBytes, diffS2cBytes );
            UvmContextFactory.context().logEvent( event );
        }
        
        public void run()
        {
            synchronized( ConntrackMonitorImpl.class ) {

                LinkedHashMap<Long,ConntrackEntryState> oldConntrackEntries = conntrackEntries;
                LinkedHashMap<Long,ConntrackEntryState> newConntrackEntries = new LinkedHashMap<Long, ConntrackEntryState>(conntrackEntries.size()*2);
                
                List<Conntrack> dumpEntries = com.untangle.jnetcap.Netcap.getInstance().getConntrackDump();            

                for ( Conntrack conntrack : dumpEntries ) {
                    long conntrackId = conntrack.getConntrackId();
                    if ( conntrackId == 0 ) {
                        logger.warn("Missing conntrack ID: " + conntrack);
                        continue;
                    }
                    ConntrackEntryState state = oldConntrackEntries.remove( conntrackId );

                    long sessionId = 0;
                    /**
                     * If we already know about this session, then pull the sessionId from the state
                     */
                    if ( state != null )
                        sessionId = state.sessionId;
                    /**
                     * If we don't know about this session, ask the Conntrack Hook to see if it knows about it
                     * The Connntrack Hook stores a list of all the "bypassed" sessions and all of the conntrack IDs and session IDs
                     * for those sessions.
                     */
                    if ( sessionId == 0 ) {
                        Long sid = NetcapConntrackHook.getInstance().lookupSessionIdByConntrackId( conntrackId );
                        if ( sid != null )
                            sessionId = sid;
                    }
                    /**
                     * If we still don't know it,
                     * lookup the tuple in the session table for a live session
                     * or lookup the tuple in the recently completed tcp session table
                     */
                    if ( sessionId == 0 ) {
                        SessionTupleImpl tuple = new SessionTupleImpl( conntrack.getProtocol(),
                                                                       conntrack.getClientIntf(),
                                                                       conntrack.getServerIntf(),
                                                                       conntrack.getPreNatClient(),
                                                                       conntrack.getPreNatServer(),
                                                                       conntrack.getPreNatClientPort(),
                                                                       conntrack.getPreNatServerPort() );

                        SessionGlobalState session;
                        if ( sessionId == 0 ) {
                            session = SessionTableImpl.getInstance().lookupTuple( tuple );
                            if ( session != null )
                                sessionId = session.id();
                        }
                        
                        if ( sessionId == 0 ) {
                            session = SessionTableImpl.getInstance().lookupTupleTcpCompleteDSessions( tuple );
                            if ( session != null )
                                sessionId = session.id();
                        }
                    }
                    if ( sessionId == 0 ) {
                        // unable to find the session ID for this session
                        // we can't log events without a session ID
                        continue;
                    } 


                    String action = null;
                    if ( state == null ) {
                        action = "NEW    ";
                        state = new ConntrackEntryState( conntrack, sessionId );
                    } else {
                        action = "UPDATE ";
                    }

                    // put the entry in the new map
                    newConntrackEntries.put( conntrackId, state );

                    // log event 
                    doAccounting( conntrack, state, action );
                }

                /**
                 * Replace the original map with the new one
                 */
                conntrackEntries = newConntrackEntries;

                for ( ConntrackEntryState state : oldConntrackEntries.values() ) {
                    // log event 
                    doAccounting( state.conntrack, state, "REMOVE " );

                    // raze forces it to free the struct nf_conntrack* now
                    if ( state.conntrack != null ) {
                        state.conntrack.raze();
                        state.conntrack = null;
                    }
                }
            }
        }
    }

    private class ConntrackEntryState
    {
        protected Conntrack conntrack;
        protected long sessionId;
        
        protected long c2sBytes = 0;
        protected long s2cBytes = 0;

        protected ConntrackEntryState( Conntrack conntrack, long sessionId )
        {
            this.conntrack = conntrack;
            this.sessionId = sessionId;
        }
        
    }
}

