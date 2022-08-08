/**
 * $Id: IntrusionPreventionSuricataFastParser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.app.intrusion_prevention.IntrusionPreventionEventMap;
import com.untangle.app.intrusion_prevention.IntrusionPreventionEventMapSignature;
import com.untangle.app.intrusion_prevention.IntrusionPreventionLogEvent;

/**
 * Process Suricata's fast file log format and add entries IPS event log table.
 * 
 * The format of these log entries is as follows:
 *  08/01/2022-13:23:33.897885  [**] [1:1999999:0] CompanySecret [**] [Classification: Attempted Administrator Privilege Gain] [Priority: 1] {TCP} 192.168.253.51:33836 -> 13.59.216.148:80
 *  08/01/2022-13:24:32.559808  [Drop] [**] [2400:1999999:0] CompanySecret [**] [Classification: Attempted Administrator Privilege Gain] [Priority: 1] {TCP} 192.168.253.51:33838 -> 13.59.216.148:80
 * 
 * With the following fields of interest:
 * action       If non-empty, indicates a block action.  Log otherwise.
 * gid          Group id of signature
 * sid          Signature id
 * protocol     Name of protocol such as UDP, TCP, ICMP, etc.
 * source ip    Source IP address
 * source port  Source port number
 * dest ip      Destination IP address
 * dest port    Destination port number
 * 
 * After parsing the Suricata log, the parser uses the generated event map to fill in extra fields such as signature message, rule, categorie, classtype, protocol.
 * The event map is generated as part of the suricata configuration.
 */
public class IntrusionPreventionSuricataFastParser
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final String FAST_LOG_FILE = "/var/log/suricata/fast.log";
    public static final String EVENT_MAP = "/etc/suricata/intrusion-prevention.event.map.conf";

    private IntrusionPreventionEventMap ipsEventMap = new IntrusionPreventionEventMap();

    private long lastTime = System.currentTimeMillis() - IntrusionPreventionEventMonitor.SLEEP_TIME_MSEC;
    private long lastPosition = 0L;

    private static final Pattern LogEntry;

    static {
        LogEntry = Pattern.compile("^[^ ]+ ( \\[([^\\]]+)\\] | )\\[\\*\\*\\] \\[(\\d+):(\\d+):(\\d+)\\] .+ \\[\\*\\*\\] \\[[^\\]]+\\] \\[[^\\]]+\\] \\{([^\\}]+)\\} ([\\d+\\.]+)\\:(\\d+) [^ ]+ ([\\d+\\.]+)\\:(\\d+).*");
    }
    /**
     * Indexes into regex capture.
     */
    private static int LogEntryFieldAction = 2;
    private static int LogEntryFieldGid = 4;
    private static int LogEntryFieldSid = 3;
    private static int LogEntryFieldProtocol = 6;
    private static int LogEntryFieldSourceIp = 7;
    private static int LogEntryFieldSourcePort = 8;
    private static int LogEntryFieldDestinationIp = 9;
    private static int LogEntryFieldDestinationPort = 10;
    
    /**
     * Initialize parser.
     */ 
	public IntrusionPreventionSuricataFastParser()
    {
        reloadEventMap();
    }

    /**
     * Load the map containing descriptions to the log entry identifiers.
     */
    public void reloadEventMap()
    {
        IntrusionPreventionEventMap newIntrusionPreventionEventMap = new IntrusionPreventionEventMap();
        File f = new File( EVENT_MAP );
        if (f.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            }catch (java.io.FileNotFoundException e) {
                logger.warn("Unable to open event map:", e);
            }

            BufferedReader reader = null;
            try {
                StringBuilder jsonString = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line+"\n");
                }

                JSONSerializer serializer = new JSONSerializer();
                try{
                    serializer.registerDefaultSerializers();
                }catch( Exception e){
                    logger.warn("registerDefaultSerializers exception=" + e);
                }
                serializer.setFixupDuplicates(false);
                serializer.setMarshallNullAttributes(false);

                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                newIntrusionPreventionEventMap = (IntrusionPreventionEventMap) serializer.fromJSON(jsonString.toString());
            } catch (IOException e) {
                logger.warn("Unable to process event map: ",e);
            } catch (UnmarshallException e) {
                logger.warn("UnmarshallException: ",e);
                for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                      logger.warn("Exception cause: ", cause);
                }
                logger.warn("Unable to unamarshall event map entry:", e );
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    logger.warn( "Unable to close event map:", e );
                }
            }
         }
        ipsEventMap = newIntrusionPreventionEventMap;
    }
    
    /**
     * Read Suricata fast log file and each entry, log to untangle event handler.
     * @param ipsApp
     *  Intrusion Prevention application.
     */
    public void parse(IntrusionPreventionApp ipsApp)
    {
        File logFile = null;
        try{
            logFile = new File( FAST_LOG_FILE );
            if(logFile.isFile() && ( logFile.lastModified() < lastTime )){
                lastTime = System.currentTimeMillis();
                return;
            }
        }catch(Exception e){
            logger.warn("Unable to open fast log file", e);
            lastTime = System.currentTimeMillis();
            return;
        }
        InputStream is = null;
        long pos = -1L;
        try {
            is = new FileInputStream(logFile);
        }catch (java.io.FileNotFoundException e) {
            logger.warn("Unable to open fast log:", e);
            lastTime = System.currentTimeMillis();
            return;
        }

        BufferedReader reader = null;
        long timestamp;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            if(lastPosition > logFile.length()){
                lastPosition = 0L;
            }else{
                reader.skip(lastPosition);
            }

            String line;
            long bytesRead = 0L;
            while ((line = reader.readLine()) != null) {
                bytesRead += line.length() + 1;
                if(lastPosition == 0){
                    /*
                     * We restarted reading the file but there may be old data
                     * we've already processed/don't care about.  
                     * Skip entries with old timestamps.
                     */
                    timestamp = getTimestamp(line);
                    if (timestamp < lastTime){
                        continue;
                    }
                }

                logger.warn("line=" + line);
                IntrusionPreventionLogEvent ipsEvent = null;
                try{
                    ipsEvent = parseEvent(line);
                }catch( Exception e ){
                    logger.debug( "parse: Unable to parse event:" + e );
                    continue;
                }
                if(ipsEvent != null){
                    ipsApp.logEvent( ipsEvent );
                }    
            }
            lastPosition += bytesRead;
            logger.warn("bytesRead=" + bytesRead);
        } catch (IOException e) {
            logger.warn("Unable to process fast log: ",e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                logger.warn( "Unable to close fast log:", e );
            }
        }
        lastTime = System.currentTimeMillis();
    }

    /**
     * Pull first field (space-delimited) to get entry timestamp and convert to timestamp.
     * 
     * @param logEvent fast.log entry
     * @return long of timestamp.  0 if cannot parse.
     */
    private long getTimestamp(String logEvent)
    {
        long timestamp = 0L;
        int spacePos = logEvent.indexOf(' ', 0);
        if ( spacePos > 0 ){
            String timestampString = logEvent.substring(0, spacePos);
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/YYYY-hh:mm:ss.SSSSSS");
                timestamp = dateFormat.parse(timestampString).getTime();
            } catch(Exception e) {}
        }

        return timestamp;
    }

    /**
     * Pull fields from fast log format and add to new IntrusionPreventionLogEvent.
     *
     * @param logEvent
     * @return
     *  IPS log event.
     * @throws Exception
     *  For various detected size mismatches.
     */
	public IntrusionPreventionLogEvent parseEvent(String logEvent) throws Exception
    {
        IntrusionPreventionLogEvent ipsEvent = null;

        long timestamp = getTimestamp(logEvent);

        Matcher m = LogEntry.matcher(logEvent);
        if (m.matches()) {
            int sourcePort = 0, destinationPort = 0;
            InetAddress sourceIpAddress = null, destinationIpAddress = null;

            try {
                sourcePort = Integer.parseInt(m.group(LogEntryFieldSourcePort));
                destinationPort = Integer.parseInt(m.group(LogEntryFieldDestinationPort));
                sourceIpAddress = InetAddress.getByName(m.group(LogEntryFieldSourceIp));
                destinationIpAddress = InetAddress.getByName(m.group(LogEntryFieldDestinationIp));
            }catch( Exception e){
                logger.warn("Unable to parse fast log entry", e);
            }

            long signatureId = Long.parseLong(m.group(LogEntryFieldGid));
            long generatorId = Long.parseLong(m.group(LogEntryFieldSid));
            String msg = "";
            String classtype = "";
            String category = "";
            String ruleId = "";
            String protocol = "";
            IntrusionPreventionEventMapSignature mapSignature = ipsEventMap.getSignatureBySignatureAndGeneratorId( signatureId, generatorId );
            if( mapSignature != null ){
                msg = mapSignature.getMsg();
                classtype = mapSignature.getClasstype();
                category = mapSignature.getCategory();
                ruleId = mapSignature.getRid();
                protocol = mapSignature.getProtocol();
            }

            ipsEvent = new IntrusionPreventionLogEvent(timestamp, 
                generatorId,
                signatureId,
                sourceIpAddress,
                sourcePort,
                destinationIpAddress,
                destinationPort,
                msg,
                classtype,
                category,
                ruleId,
                protocol,
                m.group(LogEntryFieldAction) != null ? true : false
            );
        }

        return ipsEvent;
	}
}
