/**
 * $Id: NodeManagerSettings.java,v 1.00 2012/04/06 10:22:03 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * The Node Manager Settings
 * This stores most of the settings related to the nodes
 */
@SuppressWarnings("serial")
public class NodeManagerSettings implements Serializable, JSONString
{
    private long nextNodeId = 1;
    private LinkedList<NodeSettings> nodes = new LinkedList<NodeSettings>();

    public NodeManagerSettings() {}

    public long getNextNodeId() { return nextNodeId; }
    public void setNextNodeId( long nextNodeId ) { this.nextNodeId = nextNodeId; }

    public LinkedList<NodeSettings> getNodes() { return nodes; }
    public void setNodes( LinkedList<NodeSettings> nodes ) { this.nodes = nodes; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}


