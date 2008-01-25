package com.untangle.uvm.webui.service;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.webui.WebuiUvmException;
import com.untangle.uvm.webui.domain.ConfigItem;

public interface MainRackService {
	List<MackageDesc> getStoreItems() throws  WebuiUvmException;
	List<MackageDesc> getToolboxItems() throws  WebuiUvmException;
	List<ConfigItem> getConfigItems() throws  WebuiUvmException;
	List<Policy> getVirtualRacks() throws  WebuiUvmException;	
	List<NodeContext> getNodes(String policyName) throws  WebuiUvmException;
	
    /**
     * Get the MackageDesc for a node.
     *
     * @param name the name of the node.
     * @return the MackageDesc.
     */
	MackageDesc getMackageDesc(String name);
	
	/*
	 * Move from store to toolbox
	 */
	void purchase(String installName) throws WebuiUvmException;
	
	/*
	 * Move from toolbox to store
	 */
	void returnToStore(String installName) throws WebuiUvmException;
	
	/*
	 * Move from toolbox to rack
	 */
	NodeContext addToRack(String installName, String policyName) throws WebuiUvmException;
	
	/*
	 * Move from rack to toolbox
	 */
	void removeFromRack(String installName, Long nodeId) throws WebuiUvmException;
	
	void startNode(String name, Long nodeId) throws WebuiUvmException;
	void stopNode(String name, Long nodeId) throws WebuiUvmException;
	
	Node getNode(String name, Long nodeId) throws WebuiUvmException;
	
}
