package protofilter.src.com.untangle.uvm.webui.service.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.webui.WebuiUvmException;
import com.untangle.uvm.webui.service.NodeService;

public class NodeServiceImpl implements NodeService {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private RemoteUvmContextManager remoteUvmContextManager = null;
	
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
