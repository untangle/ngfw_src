/*
 * $Id: IdpsSnortStatisticsParser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.node.idps;

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

import com.untangle.node.idps.IdpsNode;

public class IdpsSnortStatisticsParser {
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
        STREAM5 ("Stream5 statistics:$");
        
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
         * We have stream5 configured to process TCP and UDP as sessions.
         */
        STREAM5_TOTAL_SESSIONS ("Total sessions"),
        STREAM5_TCP_SESSIONS ("TCP sessions"),
        STREAM5_UDP_SESSIONS ("UDP sessions");
        
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

    public IdpsSnortStatisticsParser(){
        if ( IdpsSnortStatisticsParser.execManager == null) {
            IdpsSnortStatisticsParser.execManager = UvmContextFactory.context().createExecManager();
            IdpsSnortStatisticsParser.execManager.setLevel( org.apache.log4j.Level.DEBUG );    
        }
    }

	public void parse( IdpsNode idpsNode ){
        String pid = null;

        File f = new File( SNORT_PID );
        if( !f.exists() ){
            logger.warn("Snort pid not found");
            return;
        }

        try{
            pid = new String(Files.readAllBytes(Paths.get(SNORT_PID))).trim();
        }catch ( IOException e ){
            logger.warn("Can't get snort pid: ", e);
            return;
        }
        
        RandomAccessFile raf = null;
        File file = new File(SNORT_LOG);
        try{
            raf = new RandomAccessFile( file, "r" );
        }catch( FileNotFoundException e){
            logger.warn("parse: snort log does not exist: ", e);
            return;
        }
        try{
            raf.seek( file.length() );
        }catch( IOException e ){
            logger.warn("parse: Cannot seek snort log file: ", e);
            return;
        }
        long lastLength = file.length();

        String cmd = "/bin/kill -SIGUSR1 " + pid;
        ExecManagerResult result = IdpsSnortStatisticsParser.execManager.exec( cmd );

        long currentLength = file.length();
        long sleepInterval = 10;
        long maxTime = 1000 / sleepInterval;
        do{
            try{
                Thread.sleep(sleepInterval);
            }catch( InterruptedException e){
                logger.warn("parse: Cannot sleep: ", e);
                break;
            }
            maxTime--;
            currentLength = file.length();
        }while( ( currentLength == lastLength ) && ( maxTime > 0 ) );
        
        try{
            long breakdownTotal = 0;
            long breakdownTcp = 0;
            long breakdownUdp = 0;
            long actionAlerts = 0;
            long actionBlacklist = 0;
            long stream5TotalSessions = 0;
            long stream5TcpSessions = 0;
            long stream5UdpSessions = 0;
            
            State currentState = State.NONE;
            Matcher matcher;
            
            String line = null;
            while( true ){
                try{
                    line = raf.readLine();
                }catch( IOException e){
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
                 case STREAM5:
                    matcher = Statistic.STREAM5_TOTAL_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        stream5TotalSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.STREAM5_TCP_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        stream5TcpSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    matcher = Statistic.STREAM5_UDP_SESSIONS.match().matcher(line);
                    if( matcher.find() ){
                        stream5UdpSessions = Long.valueOf(matcher.group(1)).longValue();
                    }
                    break;
                }
            }
            
            long blocked = actionBlacklist;
            long logged = actionAlerts;
            long sessions = breakdownTotal - breakdownTcp - breakdownUdp + stream5TotalSessions;
            
            idpsNode.setScanCount( sessions );
            idpsNode.setDetectCount( logged );
            idpsNode.setBlockCount( blocked );
            
        }catch( Exception e){
            logger.warn("parse: problem in loop:", e );
        }
        try{
            if( raf != null ){
                raf.close();
            }
        }catch( IOException e){
            logger.warn("parse: Cannot close: ", e);
        }
        
    }
}
