/**
 * $Id$
 */
package com.untangle.uvm.networking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

class InterfaceTester
{
    private static final InterfaceTester INSTANCE = new InterfaceTester();

    private static final String UNKNOWN = "unknown";

    /* Script to run whenever the interfaces should be reconfigured */
    private static final String ALPACA_SCRIPT = "/usr/share/untangle-net-alpaca/scripts/";
    private static final String INTERFACE_STATUS_SCRIPT = ALPACA_SCRIPT + "/get-interface-status";

    /* The Flag is the value returned by the script, they are separated so the script
     * doesn't have to change if the value displayed inside of the GUI is going to change
     */
    private static final int CONNECTION_INDEX = 0;
    private static final int SPEED_INDEX = 1;
    private static final int DUPLEX_INDEX = 2;
    private static final int DATA_COUNT = 3; // Expected size of the data array

    private static final String FLAG_CONNECTION_CONNECTED = "connected";
    private static final String FLAG_CONNECTION_DISCONNECTED = "disconnected";

    private static final String FLAG_SPEED_100 = "100";
    private static final String FLAG_SPEED_10  = "10";

    private static final String FLAG_DUPLEX_FULL = "full-duplex";
    private static final String FLAG_DUPLEX_HALF = "half-duplex";

    private static final String CONNECTION_CONNECTED = "connected";
    private static final String CONNECTION_DISCONNECTED = "disconnected";

    private final Logger logger = Logger.getLogger(getClass());

    private InterfaceTester()
    {
    }

    /**
     * Test the status of the interfaces listed inside of the interface array.
     * The status for each interface is updated.
     */
    void updateLinkStatus( NetworkConfiguration settings )
    {
        logger.debug( "Updating link status" );

        String[] args = getArgs( settings );

        Map<String,String> statusMap;

        try {
            statusMap = getStatus( args, settings );
        } catch ( Exception e ) {
            logger.warn( "Unable to update status, using unknown", e );
            statusMap = new HashMap<String,String>();
        }


        for ( InterfaceConfiguration intf : settings.getInterfaceList()) {
            String intfName = intf.getSystemName();
            String intfStatus = statusMap.get( intfName );
            intf.setConnectionState( UNKNOWN );
            intf.setCurrentMedia( UNKNOWN );

            if ( null == intfStatus ) {
                logger.warn( "Unable to update the status for interface: '" + intfName + "'" );
                continue;
            }

            String data[] = intfStatus.split( " " );
            if ( data.length != DATA_COUNT ) {
                logger.warn( "Interface '"+ intfName + "' has invalid data '" + data + "'" );
                continue;
            }

            if ( FLAG_CONNECTION_CONNECTED.equals( data[CONNECTION_INDEX] )) {
                intf.setConnectionState( CONNECTION_CONNECTED  );
            } else if ( FLAG_CONNECTION_DISCONNECTED.equals( data[CONNECTION_INDEX] )) {
                intf.setConnectionState( CONNECTION_DISCONNECTED  );
            }

            if ( FLAG_SPEED_100.equals( data[SPEED_INDEX] )) {
                if ( FLAG_DUPLEX_FULL.equals( data[DUPLEX_INDEX] )) {
                    intf.setCurrentMedia( EthernetMedia.FULL_DUPLEX_100.toString());
                } else if ( FLAG_DUPLEX_HALF.equals( data[DUPLEX_INDEX] )) {
                    intf.setCurrentMedia( EthernetMedia.HALF_DUPLEX_100.toString());
                }
            } else if ( FLAG_SPEED_10.equals( data[SPEED_INDEX] )) {
                if ( FLAG_DUPLEX_FULL.equals( data[DUPLEX_INDEX] )) {
                    intf.setCurrentMedia( EthernetMedia.FULL_DUPLEX_10.toString());
                } else if ( FLAG_DUPLEX_HALF.equals( data[DUPLEX_INDEX] )) {
                    intf.setCurrentMedia( EthernetMedia.HALF_DUPLEX_10.toString());
                }
            }
        }
    }

    private String[] getArgs( NetworkConfiguration settings )
    {
        List<InterfaceConfiguration> interfaceList = settings.getInterfaceList();

        String args[] = new String[interfaceList.size()];

        int c = 0;
        for ( InterfaceConfiguration intf : interfaceList )
            args[c++] = intf.getSystemName();

        return args;
    }

    private Map<String,String> getStatus( String[] args, NetworkConfiguration settings )
    {
        Map<String,String> map = new HashMap<String,String>();

        String status;
        try {
            status = UvmContextFactory.context().execManager().execOutput( INTERFACE_STATUS_SCRIPT + UvmContextFactory.context().execManager().argBuilder(args) );
        } catch (Exception e) {
            logger.error("Failed to fetch interface information: ",e);
            return null;
        }

        logger.debug( "Script returned: \n" + status );

        for ( String intfStatus : status.split( "\n" )) {
            String data[] = intfStatus.split( ":" );
            if ( data.length != 2 ) {
                logger.warn( "Unable to use the string: " + intfStatus );
                continue;
            }

            map.put( data[0].trim(), data[1].trim());
        }

        return map;
    }

    static InterfaceTester getInstance()
    {
        return INSTANCE;
    }

}
