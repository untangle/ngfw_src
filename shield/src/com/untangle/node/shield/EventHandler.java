/*
 * $Id$
 */
package com.untangle.node.shield;

import java.net.InetAddress;
import java.util.Map;
import java.util.LinkedList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Load;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

class EventHandler extends AbstractEventHandler
{
    private StatsTableHashMap<InetAddress,Load> hostStatsTable = new StatsTableHashMap<InetAddress,Load>();

    private ShieldNodeImpl node;
    
    private final Logger logger = Logger.getLogger(EventHandler.class);

    private long lastLoggedWarningTime = System.currentTimeMillis();
    
    public EventHandler( ShieldNodeImpl node )
    {
        super( node );
        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        if ( ! node.getSettings().isShieldEnabled() ) {
            request.release();
            return;
        }

        InetAddress clientAddr = request.getOrigClientAddr();
        Load load;
        
        synchronized ( this.node ) {
            load = hostStatsTable.get( clientAddr );
            if ( load == null ) {
                load = new Load( 5*60 );
                hostStatsTable.put( clientAddr, load );
            }
        }

        LinkedList<ShieldRule> rules = node.getSettings().getRules();
        int multiplier = 1;
        if (rules != null ) {
            for (ShieldRule rule : rules) {
                if (rule.isMatch(request.getProtocol(),
                                 request.getClientIntf(), request.getServerIntf(),
                                 request.getOrigClientAddr(), request.getNewServerAddr(),
                                 request.getOrigClientPort(), request.getNewServerPort())) {
                    multiplier = rule.getMultiplier();
                    break;
                }
            }
        }


        double currentLoad = load.getLoad();

        if ( multiplier > 0 && currentLoad > ( node.getSettings().getRequestPerSecondLimit() * 5 * multiplier ) ) {
            if ( System.currentTimeMillis() - this.lastLoggedWarningTime > 10000 ) {
                this.lastLoggedWarningTime = System.currentTimeMillis();
                logger.info("Host " + clientAddr.getHostAddress() + " exceeded limit. 5-second load: " + String.format("%.2f",currentLoad) );
            }

            ShieldEvent evt = new ShieldEvent( request.sessionEvent(), true );
            node.logEvent( evt );
            
            if (protocol == Protocol.UDP) {
                request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE );
            } else {
                ((TCPNewSessionRequest)request).rejectReturnRst();
            }
        } else {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Session allowed for " + clientAddr.getHostAddress() + "." + " 5-second-load: " + String.format("%.2f",currentLoad) );
            }

            //update load
            currentLoad = load.incrementLoad();
            request.release();
        }
    }

    @SuppressWarnings("serial")
    private class StatsTableHashMap<K,V> extends LinkedHashMap<K,V>
    {
        @Override
        protected boolean removeEldestEntry( Map.Entry<K,V> eldest )
        {
            if ( size() > 10000 ) return true;
            return false;
        }

    }
}
