package com.untangle.uvm.webui.service;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.webui.WebuiUvmException;

public interface NodeService {
	Node getNode(String name, Long nodeId) throws WebuiUvmException;
}
