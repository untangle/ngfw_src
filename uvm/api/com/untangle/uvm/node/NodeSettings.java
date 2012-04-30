/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Node Settings.
 */
@SuppressWarnings("serial")
public class NodeSettings implements Serializable, JSONString, Comparable<NodeSettings>
{
    private Long id = null;
    private Long policyId = null;
    private String nodeName = null;
    private NodeState targetState = NodeState.INITIALIZED;
    
    public enum NodeState {
        LOADED,
        INITIALIZED,
        RUNNING, 
        DESTROYED
    }
    
    public NodeSettings() {}

    public NodeSettings(Long id, Long policyId, String nodeName)
    {
        this.id = id;
        this.policyId = policyId;
        this.nodeName = nodeName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeState getTargetState() { return targetState; }
    public void setTargetState(NodeState targetState) { this.targetState = targetState; }
    
    public int compareTo(NodeSettings tid)
    {
        return id < tid.getId() ? -1 : (id > tid.getId() ? 1 : 0);
    }

    public String toString()
    {
        return toJSONString();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof NodeSettings)) {
            return false;
        }
        NodeSettings t = (NodeSettings)o;

        return id.equals(t.getId());
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
