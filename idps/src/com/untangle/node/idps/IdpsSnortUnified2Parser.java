/*
 * $Id: IdpsUnified2Parser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.node.idps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.math.BigInteger;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.UnmarshallException;


import com.untangle.node.idps.IdpsNode;
import com.untangle.node.idps.IdpsLogEvent;
import com.untangle.node.idps.IdpsEventMap;
import com.untangle.node.idps.IdpsEventMapRule;

public class IdpsSnortUnified2Parser {

    private final Logger logger = Logger.getLogger(getClass());

	private FileChannel fc;
	private ByteBuffer bufSerialHeader = null;
    
    private byte[] ipv4bytes;
    private byte[] ipv6bytes;
    
    private IdpsSnortUnified2SerialHeader serialHeader;
    private IdpsSnortUnified2IdsEvent idsEvent;
    private IdpsEventMap idpsEventMap;
    
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
    
	public IdpsSnortUnified2Parser() {
		ipv4bytes = new byte[4];
		ipv6bytes = new byte[16];
        
		serialHeader = new IdpsSnortUnified2SerialHeader();
        serialHeader.clear();
		idsEvent = new IdpsSnortUnified2IdsEvent();
        idsEvent.clear();

        idpsEventMap = new IdpsEventMap();
        File f = new File( "/etc/snort/idps.event.map.conf" );
        if (f.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            }
            catch (java.io.FileNotFoundException e) {
//                throw new SettingsException("File not found: " + f);
            }

//            Object lock = this.getLock(f.getParentFile().getAbsolutePath());
//            synchronized(lock) {
//                return _loadInputStream(clz, is);
//            }
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

                logger.warn("serializing/marshalizing");
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
              idpsEventMap = (IdpsEventMap) serializer.fromJSON(jsonString.toString());
            } catch (IOException e) {
                logger.warn("IOException: ",e);
//                throw new SettingsException("Unable to the settings: '" + is + "'", e);
          } catch (UnmarshallException e) {
              logger.warn("UnmarshallException: ",e);
              for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause() ) {
                  logger.warn("Exception cause: ", cause);
              }
              logger.warn("Unable to unamarshall=" + e );
//              throw new SettingsException("Unable to unmarshal the settings: '" + is + "'", e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    logger.warn( idpsEventMap.getRules() );
                    logger.warn("LENGTH OF idEventMap.rules=" + idpsEventMap.getRules().size() );
                } catch (Exception e) {}
            }
         }

    }
    
    public long parse( File file, long startPosition, IdpsNode idpsNode ){
        fc = null;
		try {
			RandomAccessFile f = new RandomAccessFile( file, "r");
			fc = f.getChannel();
            if( startPosition > 0 ){
                fc.position( startPosition );
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		int packet_count = 0;
        int eventCount = 0;

		try {
            boolean abort = false;
			while (fc.position() != fc.size()) {
                logger.debug( "parse: packet count=" + ++packet_count + ", file size=" + fc.size() + ", position=" + fc.position() );
				try{
                    parseSerialHeader();
                }catch( Exception e ){
                    logger.debug( "parse: Unable to parse serial header:" + e );
                    abort = true;
                    break;
                }

                switch( (int) serialHeader.getType() ){
                    case IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT:
                    case IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_IPV6:
                    case IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2:
                    case IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6:
				        try{
                            parseIdsEvent();
                        }catch( Exception e ){
                            logger.debug( "parse: Unable to parse event:" + e );
                            abort = true;
                            break;
                        }
                        idpsNode.logEvent( new IdpsLogEvent( idsEvent ) );
                        break;
                    case IdpsSnortUnified2SerialHeader.TYPE_PACKET:
                    case IdpsSnortUnified2SerialHeader.TYPE_EXTRA_DATA:
                    default:
                        parseSkip();
                }
                
                if( abort == true ){
                    return fc.size();
                }
			}
            return fc.position();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return -1;
    }

	public void parseSerialHeader() throws Exception {
        
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
			e.printStackTrace();
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

	public void parseIdsEvent() throws Exception {

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
			e.printStackTrace();
		}        
        
        if( nread != eventLength ){
            throw new Exception("parseIdsEvent read size mismatch.");
        }
       
		bufIdsEvent.rewind();

		idsEvent.clear();
        
        idsEvent.setEventType( (int) serialHeader.getType() );
        
        int pos = 0;
		idsEvent.setSensorId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SENSOR_ID_SIZE;
        
		idsEvent.setEventId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_EVENT_ID_SIZE;

		idsEvent.setEventSecond( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SECOND_SIZE;
        
		idsEvent.setEventMicrosecond( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_MICROSECOND_SIZE;
        
		idsEvent.setSignatureId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SIG_ID_SIZE;
        
		idsEvent.setGeneratorId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_GID_ID_SIZE;
        
		idsEvent.setSignatureRevision( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_SIG_REV_SIZE;
        
		idsEvent.setClassificationId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_CLASS_ID_SIZE;
        
		idsEvent.setPriorityId( bufIdsEvent.getInt( pos ) );
        pos += IDS_EVENT_PRIORITY_ID_SIZE;

		bufIdsEvent.position(pos);
        if( ( idsEvent.getEventType() == IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_IPV6 ) ||
            ( idsEvent.getEventType() == IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6 ) ){
            System.out.println("parse: ipv6");
		    bufIdsEvent.get( ipv6bytes, 0, IDS_EVENTIP6_IP_SRC_SIZE );
            InetAddress si = null;
            try{
                si = InetAddress.getByAddress( ipv6bytes );
            }catch( Exception e) {
            }
		    idsEvent.setIpSource( si );
            pos += IDS_EVENTIP6_IP_SRC_SIZE;
        
		    bufIdsEvent.get( ipv6bytes, 0, IDS_EVENTIP6_IP_DST_SIZE );
            InetAddress di = null;
            try{
                di = InetAddress.getByAddress( ipv6bytes );
            }catch( Exception e) {
            }
		    idsEvent.setIpDestination( di );
            pos += IDS_EVENTIP6_IP_DST_SIZE;
        }else{
		    bufIdsEvent.get( ipv4bytes, 0, IDS_EVENT_IP_SRC_SIZE );
            InetAddress si = null;
            try{
                si = InetAddress.getByAddress( ipv4bytes );
            }catch( Exception e) {}
		    idsEvent.setIpSource( si );
            pos += IDS_EVENT_IP_SRC_SIZE;
        
		    bufIdsEvent.get( ipv4bytes, 0, IDS_EVENT_IP_DST_SIZE );
            InetAddress di = null;
            try{
                di = InetAddress.getByAddress( ipv4bytes );
            }catch( Exception e) {}
		    idsEvent.setIpDestination( di );
            pos += IDS_EVENT_IP_DST_SIZE;
        }
        
		idsEvent.setSportItype( bufIdsEvent.getShort( pos ) );
        pos += IDS_EVENT_PORT_SRC_SIZE;
        
		idsEvent.setDportIcode( bufIdsEvent.getShort( pos ) );
        pos += IDS_EVENT_PORT_DST_SIZE;
        
		idsEvent.setProtocol( (short) bufIdsEvent.get( pos ) );
        pos += IDS_EVENT_PROTOCOL_SIZE;
        
		idsEvent.setImpactFlag( (short) bufIdsEvent.get( pos ) );
        pos += IDS_EVENT_IMPACT_FLAG_SIZE;
        
		idsEvent.setImpact( (short) bufIdsEvent.get( pos ) );
        pos += IDS_EVENT_IMPACT_SIZE;
        
		idsEvent.setBlocked( (short) bufIdsEvent.get( pos ) );
        pos += IDS_EVENT_BLOCKED_SIZE;
  
        if( ( idsEvent.getEventType() == IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2 ) ||
            ( idsEvent.getEventType() == IdpsSnortUnified2SerialHeader.TYPE_IDS_EVENT_V2_IPV6 ) ){
		    idsEvent.setMplsLabel( bufIdsEvent.getInt( pos ) );
            pos += IDS_EVENTV2_MPLS_LABEL_SIZE;
            
		    idsEvent.setVlanId( bufIdsEvent.getShort( pos ) );
            pos += IDS_EVENTV2_VLAN_ID_SIZE;
        
		    idsEvent.setPadding( bufIdsEvent.getShort( pos ) );
            pos += IDS_EVENTV2_PADDING_SIZE;
        }

        IdpsEventMapRule mapRule = idpsEventMap.getRuleBySignatureId( idsEvent.getSignatureId() );
        if( mapRule != null ){
            idsEvent.setDescription( mapRule.getDescription() );
            idsEvent.setClasstype( mapRule.getClasstype() );
            idsEvent.setCategory( mapRule.getCategory() );
        }
	}

	public void parseSkip()
    {
		try {
            fc.position( fc.position() + serialHeader.getLength() );
        }catch( Exception e){
			e.printStackTrace();
        }
        return;
	}
}
