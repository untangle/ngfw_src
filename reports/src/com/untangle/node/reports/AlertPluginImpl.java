package com.untangle.uvm;

import java.net.URI;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;

import com.untangle.uvm.Plugin;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.node.reports.AlertEvent;

public class AlertPluginImpl implements Plugin
{
    private static final Logger logger = Logger.getLogger( AlertPluginImpl.class );

    protected PipelineConnector connector = null;

    private final AlertEventHookCallback alertCallback = new AlertEventHookCallback();
    
    private AlertPluginImpl() {}
    
    public static Plugin instance()
    {
        return new AlertPluginImpl();
    }
    
    public final void run()
    {
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.REPORTS_EVENT_LOGGED, this.alertCallback );
    }

    public final void stop()
    {
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.REPORTS_EVENT_LOGGED, this.alertCallback );
    }

    private class AlertEventHookCallback implements HookCallback
    {
        public String getName()
        {
            return "alert-plugin-alert-hook";
        }
        
        public void callback( Object o )
        {
            if ( ! (o instanceof AlertEvent) ) {
                return;
            }

            logger.warn(o);
        }
    }
    
}

