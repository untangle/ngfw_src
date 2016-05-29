/**
 * $Id: SessionUpdaterImpl.java,v 1.00 2016/04/26 16:51:55 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.node.SessionStatsEvent;

/**
 * The SessionUpdater is a periodic tasks that looks at active sessions and logs
 * an update event to update the byte statistics for longer lived sessions.
 *
 * If not enabled, all session stats (bytes) are updated when the session ends.
 * This is the most efficient way, however it means that the recent data on the dashboard
 * is not very accurate because long lived sessions are not counted yet.
 *
 * The SessionUpdater will look at the current sessions that have been alive for a while
 * and logs a SessionStatsEvent to update the bytes for current sessions
 * This data will later be overwritten by the final data, but the dashboard looks better
 */
public class SessionUpdaterImpl
{
    private static final Logger logger = Logger.getLogger( SessionUpdaterImpl.class );

    /**
     * UPDATE_PERIOD is how often the intermediate session updater runs
     */
    private static final int UPDATE_PERIOD = 1000 * 60; /* 1 minute */

    /**
     * SESSION_MIN_LIFE defines the minimum lifetime of a session before it gets an update
     * This is defined so we don't bother updating short lived sessions
     */
    private static final long SESSION_MIN_LIFE = 1000 * 60 * 3; /* 3 minutes */

    /**
     * SESSION_MIN_DIFF defines the minimum number of bytes transfered since last update to get an update
     * This is defined so we don't often update long lived but mostly idle sessions
     */
    private static final long SESSION_MIN_DIFF = 1000 * 50; /* 50k bytes */
    
    private final Pulse updaterPulse = new Pulse("session-updater", true, new SessionUpdaterPulse());

    public SessionUpdaterImpl()
    {
        if ( UvmContextFactory.context().networkManager().getNetworkSettings().getLogSessionUpdates() )
            start();
    }

    public void start()
    {
        updaterPulse.start( UPDATE_PERIOD );
    }

    public void stop()
    {
        updaterPulse.stop();
    }
    
    public void updateSessionStats()
    {
        List<SessionGlobalState> netcapSessions = SessionTableImpl.getInstance().getSessions();

        long cutoff = System.currentTimeMillis() - SESSION_MIN_LIFE;
        
        for ( SessionGlobalState session : netcapSessions ) {
            try {
                if ( logger.isDebugEnabled() )
                    logger.debug("Analyzing session: " + session.getSessionEvent());

                if ( session.getCreationTime() > cutoff ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug("Skipping  session [too young]: " + session.getSessionEvent());
                    continue;
                }
                if ( session.getEndTime() > 0 ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug("Skipping  session [ended]: " + session.getSessionEvent());
                    continue;
                }

                long c2pBytes = session.clientSideListener().rxBytes;
                long p2cBytes = session.clientSideListener().txBytes;
                long s2pBytes = session.serverSideListener().rxBytes;
                long p2sBytes = session.serverSideListener().txBytes;

                long sent = c2pBytes + s2pBytes;
                if ( sent - session.getLastUpdateBytes() < SESSION_MIN_DIFF ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug("Skipping  session [minimal change]: " + session.getSessionEvent());
                    continue;
                }
                session.setLastUpdateBytes( sent ); /* store the last value we updated */

                SessionStatsEvent statEvent = new SessionStatsEvent(session.getSessionEvent());
                statEvent.setC2pBytes(c2pBytes);
                statEvent.setP2cBytes(p2cBytes);
                statEvent.setS2pBytes(s2pBytes);
                statEvent.setP2sBytes(p2sBytes);
                if ( logger.isDebugEnabled() )
                    logger.debug("Logging  session: " + session.getSessionEvent());

                UvmContextFactory.context().logEvent( statEvent );
            } catch (Exception e) {
                logger.warn("Exception",e);
            }
        }
        

    }
    
    private class SessionUpdaterPulse implements Runnable
    {
        public void run()
        {
            if ( UvmContextFactory.context().networkManager().getNetworkSettings().getLogSessionUpdates() )
                updateSessionStats();
        }
    }
    
}
