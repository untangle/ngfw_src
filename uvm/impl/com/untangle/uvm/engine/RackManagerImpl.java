/**
 * $Id: RackManagerImpl.java,v 1.00 2011/08/02 15:01:17 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.RackManager;
import com.untangle.uvm.RackView;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.message.MessageManager;

/**
 * Implements RackManager.
 * 
 * The public methods are documented in RackManager.java
 */
public class RackManagerImpl implements RackManager
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The global rackManager instance
     */
    private static RackManagerImpl RACK_MANAGER;

    /**
     * Private constructor to ensure singleton. use rackManager() to get singleton reference
     */
    private RackManagerImpl()
    {
    }

    /**
     * get the singleton rackManager
     */
    protected static synchronized RackManagerImpl rackManager()
    {
        if ( RACK_MANAGER == null ) {
            RACK_MANAGER = new RackManagerImpl();
        }
        return RACK_MANAGER;
    }

    public RackView getRackView( Long policyId )
    {
        NodeManagerImpl nm = (NodeManagerImpl)UvmContextFactory.context().nodeManager();
        LicenseManager lm = UvmContextFactory.context().licenseManager();

        /* This stores a list of installable nodes. (for this rack) */
        List<NodeProperties> installableNodes = new LinkedList<NodeProperties>();
        /* This stores a list of all licenses */
        Map<String, License> licenseMap = new HashMap<String, License>();
        
        /**
         * Build the license map
         */
        List<Node> visibleNodes = nm.visibleNodes( policyId );
        for (Node node : visibleNodes) {
            String n = node.getNodeProperties().getName();
            licenseMap.put(n, lm.getLicense(n));
        }

        /**
         * Build the rack state
         */
        Map<Long, NodeSettings.NodeState> runStates = nm.allNodeStates();

        /**
         * Iterate through nodes
         */
        for ( NodeProperties nodeProps : nm.getAllNodeProperties() ) {
            if ( !nodeProps.getInvisible() ) {
                installableNodes.add( nodeProps );
            } 
        }

        /**
         * Build the nodeMetrics (stats in the UI)
         * Remove visible installableNodes from installableNodes
         */
        Map<Long, List<NodeMetric>> nodeMetrics = new HashMap<Long, List<NodeMetric>>(visibleNodes.size());
        for (Node visibleNode : visibleNodes) {
            Long nodeId = visibleNode.getNodeSettings().getId();
            Long nodePolicyId = visibleNode.getNodeSettings().getPolicyId();
            MessageManager lmm = UvmContextFactory.context().messageManager();
            nodeMetrics.put( nodeId , visibleNode.getMetrics());

            if ( nodePolicyId == null || nodePolicyId.equals( policyId ) ) {
                installableNodes.remove( visibleNode.getNodeProperties().getDisplayName() );
            }
        }

        /**
         * SPECIAL CASE: If Web Filter is installed in this rack OR licensed for non-trial, hide Web Filter Lite
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            List<Node> sitefilterNodes = UvmContextFactory.context().nodeManager().nodeInstances( "untangle-node-sitefilter", policyId );
            if (sitefilterNodes != null && sitefilterNodes.size() > 0) {
                installableNodes.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
            }
            License sitefilterLicense = lm.getLicense(License.SITEFILTER);
            if ( sitefilterLicense != null && sitefilterLicense.getValid() && !sitefilterLicense.getTrial() ) {
                installableNodes.remove("Web Filter Lite"); /* hide web filter lite from left hand nav */
            }
        }
        
        /**
         * SPECIAL CASE: If Spam Blocker is installed in this rack OR licensed for non-trial, hide Spam Blocker Lite
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            List<Node> commtouchAsNodes = UvmContextFactory.context().nodeManager().nodeInstances( "untangle-node-commtouchas", policyId);
            if (commtouchAsNodes != null && commtouchAsNodes.size() > 0) {
                installableNodes.remove("Spam Blocker Lite"); /* hide spam blocker lite from left hand nav */
            }
            License commtouchAsLicense = lm.getLicense(License.COMMTOUCHAS);
            if ( commtouchAsLicense != null && commtouchAsLicense.getValid() && !commtouchAsLicense.getTrial() ) {
                installableNodes.remove("Spam Blocker Lite"); /* hide spam blocker lite from left hand nav */
            }
        }
        
        
        /**
         * Build the list of apps to show on the left hand nav
         */
        logger.debug("Building apps panel:");
        Collections.sort( installableNodes );

        List<NodeProperties> nodeProperties = new LinkedList<NodeProperties>();
        for (Node node : visibleNodes) {
            nodeProperties.add(node.getNodeProperties());
        }
        List<NodeSettings> nodeSettings  = new LinkedList<NodeSettings>();
        for (Node node : visibleNodes) {
            nodeSettings.add(node.getNodeSettings());
        }

        return new RackView(installableNodes, nodeSettings, nodeProperties, nodeMetrics, licenseMap, runStates);
    }

    public boolean isUpgradeServerAvailable()
    {
        for ( int tries = 0 ; tries < 3 ; tries++ ) {
            try {
                String host = "updates.untangle.com";
                InetAddress addr = InetAddress.getByName( host );
                InetSocketAddress remoteAddress = new InetSocketAddress(addr, 80);
                Socket sock = new Socket();
                sock.connect( remoteAddress, 5000 );
                sock.close();
                return true;
            }
            catch ( Exception e) {
                logger.warn("Failed to connect to updates.untangle.com: " + e);
            }
        }
        return false;
    }
    
    public void instantiate(final String name, final Long policyId) throws Exception
    {
        logger.info("instantiate( " + name + ")");
        
        synchronized (this) {
            /* FIXME get dependenencies (parents) */
            List<String> parents = new LinkedList<String>();
            
            /**
             * Instantiate all subpkgs that are nodes
             */
            for ( String node : parents ) {

                // if no node properties exists for this package, it isnt a node
                File nodeProperties = new File(System.getProperty("uvm.lib.dir") + "/" + node + "/nodeProperties.js");
                if (! nodeProperties.exists()) {
                    logger.warn( "Unable to find node: " + node );
                    continue;
                } 
                
                try {
                    logger.info("instantiate( " + node + ")");

                    Node thisNode = UvmContextImpl.getInstance().nodeManager().instantiate( node, policyId );
                    NodeProperties nd = thisNode.getNodeProperties();

                    if ( thisNode == null || nd == null ) {
                        logger.warn( "Failed to instantiate: " + node );
                    }
                    
                    if ( nd.getAutoStart() ) {
                        thisNode.start();
                    }
                    
                } catch (Exception exn) {
                    logger.warn("could not instantiate " + node, exn);
                } 
            }

            // MessageManager mm = uvmContext.messageManager();
            // Message m = new InstallAndInstantiateComplete(packageDesc);
            // mm.submitMessage(m);
        }

        logger.info("installAndInstantiate( " + name + ") return");
    }

    public void upgrade() throws Exception
    {
        /* FIXME */
    }
}
