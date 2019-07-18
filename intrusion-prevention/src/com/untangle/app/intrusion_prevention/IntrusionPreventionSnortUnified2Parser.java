/**
 * $Id: IntrusionPreventionUnified2Parser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;

import com.untangle.app.intrusion_prevention.IntrusionPreventionLogEvent;
import com.untangle.app.intrusion_prevention.IntrusionPreventionEventMap;
import com.untangle.app.intrusion_prevention.IntrusionPreventionEventMapSignature;

/**
 * Process snort's unified2 events for inclusion in Untangle event database.
 */
public class IntrusionPreventionSnortUnified2Parser
{
    private final Logger logger = Logger.getLogger(getClass());
    private final static int MaximumProcessedRecords = 5000;

    public static final String EVENT_MAP = "/etc/suricata/intrusion-prevention.event.map.conf";

	private FileChannel fc;
	private ByteBuffer bufSerialHeader = null;
    
    private byte[] ipv4bytes;
    private byte[] ipv6bytes;
    
    private IntrusionPreventionSnortUnified2SerialHeader serialHeader;
    private IntrusionPreventionEventMap ipsEventMap;
    
    /* Serial Header */
    public static final int SERIAL_HEADER_TYPE_SIZE = 4;
    public static final int SERIAL_HEADER_LENGTH_SIZE = 4;
	public static final int SERIAL_HEADER_SIZE = 
      SERIAL_HEADER_TYPE_SIZE + 
      SERIAL_HEADER_LENGTH_SIZE;
    
    /* IDS Event */
    public static final int IDS_EVENT_SENSOR_ID_SIZE = 4;
    public static final int IDS_EVENT_EVENT_ID_SIZE = 4;
    public static final int IDS_EVENT_SECOND_SIZE = 4;
    public static final int IDS_EVENT_MICROSECOND_SIZE = 4;
    public static final int IDS_EVENT_SIG_ID_SIZE = 4;
    public static final int IDS_EVENT_GID_ID_SIZE = 4;
    public static final int IDS_EVENT_SIG_REV_SIZE = 4;
    public static final int IDS_EVENT_CLASS_ID_SIZE = 4;
    public static final int IDS_EVENT_PRIORITY_ID_SIZE = 4;
    public static final int IDS_EVENT_IP_SRC_SIZE = 4;
    public static final int IDS_EVENT_IP_DST_SIZE = 4;
    /* IPV6 */
    public static final int IDS_EVENTIP6_IP_SRC_SIZE = 16;
    public static final int IDS_EVENTIP6_IP_DST_SIZE = 16;
    public static final int IDS_EVENT_PORT_SRC_SIZE = 2;
    public static final int IDS_EVENT_PORT_DST_SIZE = 2;
    public static final int IDS_EVENT_PROTOCOL_SIZE = 1;
    public static final int IDS_EVENT_IMPACT_FLAG_SIZE = 1;
    public static final int IDS_EVENT_IMPACT_SIZE = 1;
    public static final int IDS_EVENT_BLOCKED_SIZE = 1;
    /* V2 */
    public static final int IDS_EVENTV2_MPLS_LABEL_SIZE = 4;
    public static final int IDS_EVENTV2_VLAN_ID_SIZE = 2;
    public static final int IDS_EVENTV2_PADDING_SIZE = 2;

    public static final int IDS_EVENT_MAX_SIZE = 
        IDS_EVENT_SENSOR_ID_SIZE +
        IDS_EVENT_EVENT_ID_SIZE +
        IDS_EVENT_SECOND_SIZE +
        IDS_EVENT_MICROSECOND_SIZE +
        IDS_EVENT_SIG_ID_SIZE +
        IDS_EVENT_GID_ID_SIZE +
        IDS_EVENT_SIG_REV_SIZE +
        IDS_EVENT_CLASS_ID_SIZE +
        IDS_EVENT_PRIORITY_ID_SIZE +
        IDS_EVENTIP6_IP_SRC_SIZE +
        IDS_EVENTIP6_IP_DST_SIZE +
        IDS_EVENT_PORT_SRC_SIZE +
        IDS_EVENT_PORT_DST_SIZE +
        IDS_EVENT_PROTOCOL_SIZE +
        IDS_EVENT_IMPACT_FLAG_SIZE +
        IDS_EVENT_IMPACT_SIZE +
        IDS_EVENT_BLOCKED_SIZE +
        IDS_EVENTV2_MPLS_LABEL_SIZE +
        IDS_EVENTV2_VLAN_ID_SIZE +
        IDS_EVENTV2_PADDING_SIZE;
   
    /**
     * Initialize parser.
     */ 
	public IntrusionPreventionSnortUnified2Parser()
    {
		ipv4bytes = new byte[4];
		ipv6bytes = new byte[16];
        
		serialHeader = new IntrusionPreventionSnortUnified2SerialHeader();
        serialHeader.clear();

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
     * Read the snort unified2 event and for each entry, log to untangle event handler.
     *
     * @param file
     *  Snort unified2 log file handle.
     * @param startPosition
     *  Location to start parsing.
     * @param ipsApp
     *  Intrusion Prevention application.
     * @param currentTime
     *  Ignore entries older than this time.
     * @return
     *  Long value of the last position read.
     */
    public long parse( File file, long startPosition, IntrusionPreventionApp ipsApp, long currentTime )
    {
        fc = null;
        RandomAccessFile f = null;
		try {
			f = new RandomAccessFile( file, "r");
			fc = f.getChannel();
            if( startPosition > 0 ){
                fc.position( startPosition );
            }
		} catch (Exception e) {
            logger.warn( "Unable to open ips event for processing:", e );
		}
        
		int packetCount = 0;

        long pos = -1L;
        int recordCount = 0;
		try {
            boolean abort = false;
			while (fc.position() != fc.size()) {
                logger.debug( "parse: packet count=" + ++packetCount + ", file size=" + fc.size() + ", position=" + fc.position() );
				try{
                    parseSerialHeader();
                }catch( Exception e ){
                    logger.debug( "parse: Unable to parse serial header:" + e );
                    abort = true;
                    break;
                }

                switch( (int) serialHeader.getType() ){
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT:
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_IPV6:
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2:
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6:
                        IntrusionPreventionLogEvent ipsEvent = null;
				        try{
                            ipsEvent = parseIdsEvent();
                        }catch( Exception e ){
                            logger.debug( "parse: Unable to parse event:" + e );
                            abort = true;
                            break;
                        }
                        if((ipsEvent.getEventSecond() * 1000 ) < (currentTime - IntrusionPreventionEventMonitor.SLEEP_TIME_MSEC)){
                            // Snort log can contain older events.  Ignore these.
                            break;
                        }
                        ipsApp.logEvent( ipsEvent );
                        recordCount++;
                        break;
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_PACKET:
                    case IntrusionPreventionSnortUnified2SerialHeader.TYPE_EXTRA_DATA:
                    default:
                        parseSkip();
                }
                
                if( recordCount == MaximumProcessedRecords ){
                    break;
                }

                if( abort == true ){
                    pos = fc.size();
                    break;
                }
			}
            pos = fc.position();
		} catch (Exception e) {
            logger.warn("Unable to process ips event: ", e);
		}

        if( recordCount == MaximumProcessedRecords ){
            logger.warn( "Reached maximum record/interval threshold: " + MaximumProcessedRecords );
        }

        if( f != null ){
            try {
                f.close();
            } catch (Exception e) {
                logger.warn("Unable to close ips event: ", e);
            }
        }
        return pos;
    }

    /**
     * Read the serial header.
     *
     * @throws Exception
     *  Can occur if read size mismatches.
     */
	public void parseSerialHeader() throws Exception
    {
        if( bufSerialHeader == null ){
		    bufSerialHeader = ByteBuffer.allocate( SERIAL_HEADER_SIZE );
        }
		bufSerialHeader.clear();
        
		int nread = 0;		
		try {
			do {
				nread = fc.read( bufSerialHeader );
			} while (nread != -1 && bufSerialHeader.hasRemaining());
		} catch (Exception e) {
            logger.warn( "Unable to read serial header:", e );
		}
        
        if( nread != SERIAL_HEADER_SIZE ){
            throw new Exception("parseSerialHeader read size mismatch.");
        }
       
		bufSerialHeader.rewind();
		serialHeader.clear();

        int pos = 0;
		serialHeader.setType( bufSerialHeader.getInt( pos ) );
        pos += SERIAL_HEADER_TYPE_SIZE;
		serialHeader.setLength( bufSerialHeader.getInt( pos ) );
	}

    /**
     * Log current entry.
     *
     * @return
     *  IPS log event.
     * @throws Exception
     *  For various detected size mismatches.
     */
	public IntrusionPreventionLogEvent parseIdsEvent() throws Exception
    {
        IntrusionPreventionLogEvent ipsEvent = new IntrusionPreventionLogEvent();

        int eventLength = (int) serialHeader.getLength();
        
        if( eventLength > IDS_EVENT_MAX_SIZE ){
            throw new Exception("parseIdsEvent proposed event length is longer than maximum");
        }
        
        ByteBuffer bufIdsEvent = ByteBuffer.allocate( eventLength );
        
		int nread = 0;		
		try {
			do {
				nread = fc.read( bufIdsEvent );
			} while (nread != -1 && bufIdsEvent.hasRemaining() );
		} catch (Exception e) {
            logger.warn( "Unable to read event:", e );
		}        
        
        if( nread != eventLength ){
            throw new Exception("parseIdsEvent read size mismatch.");
        }
       
		bufIdsEvent.rewind();

        ipsEvent.setEventType( (int) serialHeader.getType() );
        
        int pos = 0;
		ipsEvent.setSensorId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SENSOR_ID_SIZE;
        
		ipsEvent.setEventId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_EVENT_ID_SIZE;

		ipsEvent.setEventSecond( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SECOND_SIZE;
        
		ipsEvent.setEventMicrosecond( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_MICROSECOND_SIZE;
        
		ipsEvent.setSignatureId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SIG_ID_SIZE;
        
		ipsEvent.setGeneratorId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_GID_ID_SIZE;
        
		ipsEvent.setSignatureRevision( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SIG_REV_SIZE;
        
		ipsEvent.setClassificationId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_CLASS_ID_SIZE;
        
		ipsEvent.setPriorityId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_PRIORITY_ID_SIZE;

		bufIdsEvent.position(pos);
        if( ( ipsEvent.getEventType() == IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_IPV6 ) ||
            ( ipsEvent.getEventType() == IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6 ) ){
		    bufIdsEvent.get( ipv6bytes, 0, IDS_EVENTIP6_IP_SRC_SIZE );
            InetAddress si = null;
            try{
                si = InetAddress.getByAddress( ipv6bytes );
            }catch( Exception e) {
                logger.warn( "Unable to process source IPv6 address:", e );
            }
		    ipsEvent.setIpSource( si );
            pos += IDS_EVENTIP6_IP_SRC_SIZE;
        
		    bufIdsEvent.get( ipv6bytes, 0, IDS_EVENTIP6_IP_DST_SIZE );
            InetAddress di = null;
            try{
                di = InetAddress.getByAddress( ipv6bytes );
            }catch( Exception e) {
                logger.warn( "Unable to process destination IPv6 address:", e );
            }
		    ipsEvent.setIpDestination( di );
            pos += IDS_EVENTIP6_IP_DST_SIZE;
        }else{
		    bufIdsEvent.get( ipv4bytes, 0, IDS_EVENT_IP_SRC_SIZE );
            InetAddress si = null;
            try{
                si = InetAddress.getByAddress( ipv4bytes );
            }catch( Exception e) {
                logger.warn( "Unable to process source IPv4 address:", e );
            }
		    ipsEvent.setIpSource( si );
            pos += IDS_EVENT_IP_SRC_SIZE;
        
		    bufIdsEvent.get( ipv4bytes, 0, IDS_EVENT_IP_DST_SIZE );
            InetAddress di = null;
            try{
                di = InetAddress.getByAddress( ipv4bytes );
            }catch( Exception e) {
                logger.warn( "Unable to process destination IPv4 address:", e );
            }
		    ipsEvent.setIpDestination( di );
            pos += IDS_EVENT_IP_DST_SIZE;
        }
        
		ipsEvent.setSportItype( bufIdsEvent.getShort( pos ) );
        pos += IDS_EVENT_PORT_SRC_SIZE;
        
		ipsEvent.setDportIcode( bufIdsEvent.getShort( pos ) );
        pos += IDS_EVENT_PORT_DST_SIZE;
        
		ipsEvent.setProtocol( (short) bufIdsEvent.get( pos ) );
        pos += IDS_EVENT_PROTOCOL_SIZE;

        // We originally implemented the parser to work with Snort's version of unified2.
        // When converting to Suricata, we decided that we'd already built the parser, so
        // why re-invent the wheel parsing one of their other log formats?
        // Hoever, we discovered that Suricata did not implement the following two fields.
        //
        // ipsEvent.setImpactFlag( (short) bufIdsEvent.get( pos ) );
        // pos += IDS_EVENT_IMPACT_FLAG_SIZE;
        //
        // ipsEvent.setImpact( (short) bufIdsEvent.get( pos ) );
        // pos += IDS_EVENT_IMPACT_SIZE;
        //
		ipsEvent.setBlocked( bufIdsEvent.get( pos ) != 0 ? true : false );
        pos += IDS_EVENT_BLOCKED_SIZE;
  
        if( ( ipsEvent.getEventType() == IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2 ) ||
            ( ipsEvent.getEventType() == IntrusionPreventionSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6 ) ){
		    ipsEvent.setMplsLabel( bufIdsEvent.getInt( pos ) );
            pos += IDS_EVENTV2_MPLS_LABEL_SIZE;
            
		    ipsEvent.setVlanId( bufIdsEvent.getShort( pos ) );
            pos += IDS_EVENTV2_VLAN_ID_SIZE;
        
		    ipsEvent.setPadding( bufIdsEvent.getShort( pos ) );
            pos += IDS_EVENTV2_PADDING_SIZE;
        }

        IntrusionPreventionEventMapSignature mapSignature = ipsEventMap.getSignatureBySignatureAndGeneratorId( ipsEvent.getSignatureId(), ipsEvent.getGeneratorId() );
        if( mapSignature != null ){
            ipsEvent.setMsg( mapSignature.getMsg() );
            ipsEvent.setClasstype( mapSignature.getClasstype() );
            ipsEvent.setCategory( mapSignature.getCategory() );
            ipsEvent.setRid( mapSignature.getRid() );
        }

        return ipsEvent;
	}

    /**
     * For any other snort event we can't determine, skip to the next position.
     */
	public void parseSkip()
    {
		try {
            fc.position( fc.position() + serialHeader.getLength() );
        }catch( Exception e){
            logger.warn("Unable to seek to new position:", e);
        }
        return;
	}
}
