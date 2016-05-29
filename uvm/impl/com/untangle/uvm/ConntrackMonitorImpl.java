/**
 * $Id: ConntrackMonitorImpl.java,v 1.00 2016/05/27 14:23:01 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.util.Pulse;

public class ConntrackMonitorImpl
{
    private static final Logger logger = Logger.getLogger(ConntrackMonitorImpl.class);
    private static final int FREQUENCY = 60*1000; /* 60 seconds */
    
    private final Pulse pulse = new Pulse("conntrack-monitor", true, new ConntrackPulse());

    public ConntrackMonitorImpl()
    {
        pulse.start(FREQUENCY);
    }

    protected void stop()
    {
        pulse.stop();
    }
    
    private class ConntrackPulse implements Runnable
    {
        public void run()
        {
            List<Conntrack> entries = com.untangle.jnetcap.Netcap.getInstance().getConntrackDump();            

            // for ( Conntrack conntrack : entries ) {
            //     logger.warn("");
            //     logger.warn("CONNTRACK: " + conntrack.toString());
                
            //     logger.warn("CONNTRACK: " +
            //                 conntrack.getConntrackId());
            //     logger.warn("CONNTRACK: " +
            //                 conntrack.getProtocol() + " " +
            //                 conntrack.getPreNatClient() + ":" + conntrack.getPreNatClientPort() + " -> " +
            //                 conntrack.getPostNatServer() + ":" + conntrack.getPostNatServerPort());
            //     logger.warn("CONNTRACK: " + 
            //                 "pkts: " + conntrack.getOriginalCounterPackets()  + " bytes: " + conntrack.getOriginalCounterBytes() + " " +
            //                 "pkts: " + conntrack.getReplyCounterPackets()  + " bytes: " + conntrack.getReplyCounterBytes());
                            
            //     logger.warn("");
            // }

            for ( Conntrack conntrack : entries ) {
                conntrack.raze();
            }

        }

    }
}

