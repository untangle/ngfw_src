/*
 * $Id: IntrusionPreventionSnortStatisticsParser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

public class IntrusionPreventionSnortStatisticsParser
{
    /*
     * For every two steps forward, Snort seems to almost trip itself
     * then right itself again, taking a quarter step backward.  
     * 
     * First, it seems like it would be easier to use the performance
     * monitor.  However, it logs based on time and packet count.  It's
     * sort of understandable but results in erratic, unpredictable logging.
     * For example, if you say to log every 30 seconds with 1k packets, it
     * won't log until it sees 1k worth of packets and the documentation
     * includes an ominus warning about performance degradation if you
     * lower the packet count.  So you can't rely on it.
     * 
     * The second best is parsing the "human readable" snort statistics
     * appended to the snort log.  It would only make sense that Snort
     * allows you dump those stats to a separate file, right?  Or at least
     * prefix each report entry with text that could be captured by syslog
     * so we could do it ourselves, right?  Wrong on both accounts.
     * 
     * So we need to parse the human-readable statistics.  And the final
     * wrinkle is that while snort logs on a session basis, it reports
     * statistics on a packet basis.  We need to do work to bring everything
     * into as close to a session-based context as possible:
     * 
     * Blocked: Blacklist
     * Logged: Alerts - Blacklist
     * Sessions: Total packets - tcp+udp_packets + tcp+udp_sessions
     * 
     */
    
    /*
     * Statistics are organzied in sections separated by a 
     * string of "=" signs. 
     */
    private final Logger logger = Logger.getLogger(getClass());

    private enum State {
        NONE ("===============$"),
        BREAKDOWN ("Breakdown by protocol \\(includes rebuilt packets\\):$"),
        ACTION ("Action Stats:$"),
        STREAM ("Stream statistics:$");
        
        private final Pattern match;
        State( String match ){
            this.match = Pattern.compile(match);
        }
        public Pattern match(){
            return this.match;
        }
    }
    
    /*
     * Pull the approrpiate statistic from each section.
     */
    private enum Statistic {
        /*
         * Total number of packets for all protocols.
         */
        BREAKDOWN_TOTAL ("Total"),
        /*
         * TCP and UDP packets
         */
        BREAKDOWN_TCP ("TCP"),
        BREAKDOWN_UDP ("UDP"),
        /*
         * Alerts are both logged and logged + blocked.
         */
        ACTION_ALERTS ("Alerts"),
        /* 
         * The "Blocked" field is reported as packets.  
         * The blacklist is seemingly reported as sessions.
         */
        ACTION_BLACKLIST ("Blacklist"),
        /*
         * We have stream configured to process TCP and UDP as sessions.
         */
        STREAM_TOTAL_SESSIONS ("Total sessions"),
        STREAM_TCP_SESSIONS ("TCP sessions"),
        STREAM_UDP_SESSIONS ("UDP sessions");
        
        private final Pattern match;
        Statistic( String match ){
            this.match = Pattern.compile( "\\s+" + match + ":\\s*(\\d+)");
        }
        public Pattern match(){ 
            return this.match;
        }
    }
    
    private static final String SNORT_LOG = "/var/log/snort.log";
    private static final String SNORT_PID = "/var/run/snort_.pid";

    protected static ExecManager execManager = null;

    public IntrusionPreventionSnortStatisticsParser()
    {
        if ( IntrusionPreventionSnortStatisticsParser.execManager == null) {
            IntrusionPreventionSnortStatisticsParser.execManager = UvmContextFactory.context().createExecManager();
            IntrusionPreventionSnortStatisticsParser.execManager.setLevel( org.apache.log4j.Level.DEBUG );    
        }
    }

	public void parse( IntrusionPreventionApp ipsApp )
    {
        File f = new File( SNORT_PID );
        if( !f.exists() ){
            logger.warn("Snort pid not found");
            return;
        }

        RandomAccessFile raf = null;
        File file = new File(SNORT_LOG);
        try {
            raf = new RandomAccessFile( file, "r" );
        } catch( Exception e) {
            /*
             * The file likely does not exist but there's no need to 
             * fill up the logs with this as it will eventually be
             * created by snort.
             */
            return;
        }
        try {
            raf.seek( file.length() );
        } catch( IOException e ) {
            logger.warn("parse: Cannot seek snort log file: ", e);
            return;
        }
        long lastLength = file.length();

        long currentLength = file.length();
        long sleepInterval = 10;
        long maxTime = 1000 / sleepInterval;

        /**
         * I changed this to killall because if snort crashes it leaves its PID file in place
         * When that happens evenually something else takes the PID and we send it a signal
         * If java takes that PID, then we actually kill java with a SIGUSR1
         *
         * Given that the we dont check that snort owns that PID, I just changed kill to killall which is a bit safer.
         * I left the logic to check that the PID file still exists as that seems useful
         *
         * bug #12837 for more info
         */
        String cmd = "killall -SIGUSR1 snort";
        // String cmd = "/bin/kill -SIGUSR1 " + pid;
        ExecManagerResult result = IntrusionPreventionSnortStatisticsParser.execManager.exec( cmd );

        do {
            try {
                Thread.sleep(sleepInterval);
            } catch( InterruptedException e) {
                logger.warn("parse: Cannot sleep: ", e);
                break;
            }
            maxTime--;
            currentLength = file.length();
        } while( ( currentLength == lastLength ) && ( maxTime > 0 ) );
        
        try {
            long breakdownTotal = 0;
            long breakdownTcp = 0;
            long breakdownUdp = 0;
            long actionAlerts = 0;
            long actionBlacklist = 0;
            long streamTotalSessions = 0;
            long streamTcpSessions = 0;
            long streamUdpSessions = 0;
            
            State currentState = State.NONE;
            Matcher matcher;
            
            String line = null;
            while( true ){
                try {
                    line = raf.readLine();
                } catch( IOException e){
                    break;
                }
                if( line == null ){
                    break;
                }

                for( State state: State.values() ){
                    matcher = state.match().matcher(line);
                    if( matcher.find() ){
                        currentState = state;
                        break;
                    }
                }
                
                switch( currentState ){
                 case BREAKDOWN:
                    matcher = Statistic.BREAKDOWN_TOTAL.match().matcher(line);
                    if( matcher.find() ){
                        breakdownTotal = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.BREAKDOWN_TCP.match().matcher(line);
                    if( matcher.find() ){
                        breakdownTcp = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.BREAKDOWN_UDP.match().matcher(line);
                    if( matcher.find() ){
                        breakdownUdp = Long.valueOf(matcher.group(1)).longValue();
                    }
                    break;
                 case ACTION:
                    matcher = Statistic.ACTION_ALERTS.match().matcher(line);
                    if( matcher.find() ){
                        actionAlerts = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.ACTION_BLACKLIST.match().matcher(line);
                    if( matcher.find() ){
                        actionBlacklist = Long.valueOf(matcher.group(1)).longValue();
                    }
                    break;
                 case STREAM:
                    matcher = Statistic.STREAM_TOTAL_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        streamTotalSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.STREAM_TCP_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        streamTcpSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.STREAM_UDP_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        streamUdpSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    break;
                }
            } 

            long otherSessions = breakdownTotal - breakdownTcp - breakdownUdp;
            long blocked = actionBlacklist;
            long logged = actionAlerts;
            long sessions = otherSessions + streamTotalSessions;
            
            if ( logger.isDebugEnabled() ) {
                logger.debug("breakdownTotal: " + breakdownTotal);
                logger.debug("breakdownTcp: " + breakdownTcp);
                logger.debug("breakdownUDP: " + breakdownUdp);
                logger.debug("actionAlerts: " + actionAlerts);
                logger.debug("actionBlacklist: " + actionBlacklist);
                logger.debug("streamTotalSessions: " + streamTotalSessions);
                logger.debug("streamTcpSessions: " + streamTcpSessions);
                logger.debug("streamUdpSessions: " + streamUdpSessions);
                logger.debug("otherSessions: " + otherSessions);
                logger.debug("scanCount: " + sessions);
                logger.debug("detectCount: " + logged);
                logger.debug("blockCount: " + blocked);
            }
            
            ipsApp.setScanCount( sessions );
            ipsApp.setDetectCount( logged );
            ipsApp.setBlockCount( blocked );
            
        } catch( Exception e){
            logger.warn("parse: problem in loop:", e );
        }
        try {
            if( raf != null ){
                raf.close();
            }
        } catch( IOException e){
            logger.warn("parse: Cannot close: ", e);
        }
        
    }
}
