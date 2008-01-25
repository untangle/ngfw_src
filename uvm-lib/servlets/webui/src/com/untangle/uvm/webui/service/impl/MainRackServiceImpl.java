package com.untangle.uvm.webui.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.untangle.uvm.UvmException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.MackageInstallException;
import com.untangle.uvm.toolbox.MackageUninstallException;
import com.untangle.uvm.webui.WebuiUvmException;
import com.untangle.uvm.webui.domain.ConfigItem;
import com.untangle.uvm.webui.service.MainRackService;

public class MainRackServiceImpl implements MainRackService {
	protected final Log logger = LogFactory.getLog(getClass());
	
	private RemoteUvmContextManager remoteUvmContextManager = null;
	private List<ConfigItem> configItems = null;
	
	public List<MackageDesc> getStoreItems() throws WebuiUvmException {
        // CHECK FOR STORE CONNECTIVITY AND AVAILABLE ITEMS
        List<MackageDesc> storeItemsVisible = new ArrayList<MackageDesc>();
        try{
        	//TODO we should do only one update...
            //getRemoteUvmContextManager().getToolboxManager().update();
        	
        	MackageDesc[] storeItemsAvailable = getRemoteUvmContextManager().toolboxManager().uninstalled();
            if( storeItemsAvailable == null ) {
                logger.debug("items: null");
            } else {
                for(MackageDesc mackageDesc : storeItemsAvailable){
                    if( isMackageStoreItem(mackageDesc)  && isMackageVisible(mackageDesc)) { // is library item && is visible
                    	storeItemsVisible.add(mackageDesc);
                    }
                    logger.debug("store items: " + storeItemsVisible.size());                        
                }
            }
        }
        catch(Exception e){
            logger.debug("Error: unable to connect to store", e);
            throw new WebuiUvmException("error.rack.connect-to-store", e);
        }
        return storeItemsVisible;
        
	}
	
	public List<MackageDesc> getToolboxItems() throws WebuiUvmException {
        List<MackageDesc> toolboxItemsVisible = new ArrayList<MackageDesc>();
        
        // NODES && SECURITY
        for( MackageDesc mackageDesc : getRemoteUvmContextManager().toolboxManager().installedVisible() ){
        	if ( isMackageNode(mackageDesc) && (mackageDesc.isCore() || mackageDesc.isSecurity())) {
        		toolboxItemsVisible.add(mackageDesc);
        	}
        }
		return toolboxItemsVisible;
	}

	public void setConfigItems(List<ConfigItem> configItems) {
		this.configItems = configItems;
	}

	public List<ConfigItem> getConfigItems() {
		return configItems;
	}
	
	public List<Policy> getVirtualRacks() throws WebuiUvmException {
        List<Policy> virtualRacks = new ArrayList<Policy>();
        for( Policy policy : getRemoteUvmContextManager().policyManager().getPolicies() ){
        	virtualRacks.add(policy);
        }
		return virtualRacks;
	}

	public List<NodeContext> getNodes(String policyName) throws WebuiUvmException {
		List<NodeContext> nodes = new ArrayList<NodeContext>();
		
		// SECURITY
		// TODO we should use policyId and extend policy manager to get policy by id
		Policy policy = getRemoteUvmContextManager().policyManager().getPolicy(policyName);
		if (policy != null ) {
	        for( Tid tid : getRemoteUvmContextManager().nodeManager().nodeInstancesVisible(policy) ){
	            // GET THE NODE CONTEXT AND MACKAGE DESC
	            NodeContext nodeContext = getRemoteUvmContextManager().nodeManager().nodeContext(tid);
	            nodes.add(nodeContext);
	        }
		}
		
        // NON-SECURITY (CORE & UTIL & SERVICES)
        for( Tid tid : getRemoteUvmContextManager().nodeManager().nodeInstancesVisible(null) ){
            NodeContext nodeContext = getRemoteUvmContextManager().nodeManager().nodeContext(tid);
            nodes.add(nodeContext);
        }
		
		return nodes;
	}

	public void purchase(String installName) throws WebuiUvmException {
        // MAKE SURE NOT PREVIOUSLY INSTALLED AS PART OF A BUNDLE
        MackageDesc[] originalUninstalledMackages = getRemoteUvmContextManager().toolboxManager().uninstalled();
        boolean installed = true;
        for( MackageDesc mackageDesc : originalUninstalledMackages ){
            if(installName.equals(mackageDesc.getName())){
                installed = false;
                break;
            }
        }
        if( installed )
            return;
		
        try {
			long key = getRemoteUvmContextManager().toolboxManager().install(installName);
		} catch (MackageInstallException e) {
			logger.error("Error moving from store to toolbox", e);
			throw new WebuiUvmException("error.rack.move-to-toolbox", e);
		}
        // TODO progress
//        while (true) {
//            java.util.List<InstallProgress> lip = getRemoteUvmContextManager().getToolboxManager().getProgress(key);
//        }
        
	}
	
	public void returnToStore(String installName) throws WebuiUvmException {
        try {
            // UNINSTALL IN UVM
			getRemoteUvmContextManager().toolboxManager().uninstall(installName);
		} catch (MackageUninstallException e) {
			logger.error("Error moving from toolbox to store", e);
			throw new WebuiUvmException("error.rack.move-to-store", e);
		}
	}

	public NodeContext addToRack(String installName, String policyName) throws WebuiUvmException {
        try {
        	MackageDesc mackageDesc = getMackageDesc(installName);
        	Policy policy = null;
    		// Cannot specify a policy for a service/util/core
        	if (!mackageDesc.isService() && !mackageDesc.isUtil() && !mackageDesc.isCore()) {
        		// TODO we should use policyId and extend policy manager to get policy by id
        		policy = getRemoteUvmContextManager().policyManager().getPolicy(policyName);
        	}
    		
            // INSTANTIATE IN UVM
            Tid tid = getRemoteUvmContextManager().nodeManager().instantiate(installName, policy);
            // CREATE APPLIANCE
            NodeContext nodeContext = getRemoteUvmContextManager().nodeManager().nodeContext( tid );
            return nodeContext;
           
		} catch (Exception e) {
			logger.error("Error moving from toolbox to rack", e);
			throw new WebuiUvmException("error.rack.move-to-rack", e);
		}
		
	}
	
	public void removeFromRack(String installName, Long nodeId) throws WebuiUvmException {
        try {
    		Tid tid = getTid(installName, nodeId);
    		
    		if (tid != null) {
                // DESTROY IN UVM
        		getRemoteUvmContextManager().nodeManager().destroy(tid);
    		} else {
    			logger.error("Node not installed: " + installName + "; " + nodeId );
    			throw new WebuiUvmException("error.rack.node-not-installed");
    		}
           
		} catch (Exception e) {
			logger.error("Error moving from rack to toolbox", e);
			throw new WebuiUvmException("error.rack.remove-from-rack", e);
		}
		
	}
	
	public void startNode(String uvmNodeName, Long tidId) throws WebuiUvmException {
        try {
    		Tid tid = getTid(uvmNodeName, tidId);
    		
    		if (tid != null) {
                // Start the node
        		getRemoteUvmContextManager().nodeManager().nodeContext(tid).node().start();
    		} else {
    			logger.error("Node not installed: " + uvmNodeName + "; " + tidId );
    			//TODO return error msg to client
    			throw new UvmException("Node not installed");
    		}
           
		} catch (Exception e) {
			logger.error("Error starting node", e);
            // TODO Should have an error object for errors which should be reported to client
			throw new WebuiUvmException("Error starting node", e);
		}
	}
	
	public void stopNode(String uvmNodeName, Long tidId) throws WebuiUvmException {
        try {
    		Tid tid = getTid(uvmNodeName, tidId);
    		
    		if (tid != null) {
                // Stop the node
        		getRemoteUvmContextManager().nodeManager().nodeContext(tid).node().stop();
    		} else {
    			logger.error("Node not installed: " + uvmNodeName + "; " + tidId );
    			//TODO return error msg to client
    			throw new UvmException("Node not installed");
    		}
           
		} catch (Exception e) {
			logger.error("Error stopping node", e);
            // TODO Should have an error object for errors which should be reported to client
			throw new WebuiUvmException("Error stopping node", e);
		}
	}

    /**
     * Get the MackageDesc for a node.
     *
     * @param name the name of the node.
     * @return the MackageDesc.
     */
	public MackageDesc getMackageDesc(String name) {
		return getRemoteUvmContextManager().toolboxManager().mackageDesc(name);
	}
	
	public Node getNode(String name, Long nodeId) throws WebuiUvmException {
		Tid tid = getTid(name, nodeId);
		
		if (tid != null) {
    		return getRemoteUvmContextManager().nodeManager().nodeContext(tid).node();
		} else {
			logger.error("Node not installed: " + name + "; " + nodeId );
			throw new WebuiUvmException("error.rack.node-not-installed");
		}
           
	}
	
	
	public RemoteUvmContextManager getRemoteUvmContextManager() {
		return remoteUvmContextManager;
	}
	public void setRemoteUvmContextManager(
			RemoteUvmContextManager remoteUvmContextManager) {
		this.remoteUvmContextManager = remoteUvmContextManager;
	}
	
	// TODO these methods should be located in MackageDesc class
    private boolean isMackageVisible(MackageDesc mackageDesc){
        return mackageDesc.getViewPosition() >= 0;
    }
    private boolean isMackageStoreItem(MackageDesc mackageDesc){
        return MackageDesc.Type.LIB_ITEM == mackageDesc.getType();
    }
    private boolean isMackageTrial(MackageDesc mackageDesc){
        return MackageDesc.Type.TRIAL == mackageDesc.getType();
    }
    private boolean isMackageNode(MackageDesc mackageDesc){
        return MackageDesc.Type.NODE == mackageDesc.getType();
    }
    private boolean isMackageCasing(MackageDesc mackageDesc){
        return MackageDesc.Type.CASING == mackageDesc.getType();
    }
    
	private Tid getTid(String nodeName, Long nodeId) {
		// TODO we should use only nodeId which is Tid.id and extend node manager to get Tid by nodeId
		Tid tid = null;
		List<Tid> tids = getRemoteUvmContextManager().nodeManager().nodeInstances(nodeName);
		for (Tid t : tids) {
			if (t.getId().equals(nodeId)){
				tid = t;
				break;
			}
		}
		return tid;
	}
	
}
