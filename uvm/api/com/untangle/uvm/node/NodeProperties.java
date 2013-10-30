/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.NodeSettings;

/**
 * The immutable properties of a Node
 */
@SuppressWarnings("serial")
public class NodeProperties implements Serializable, JSONString, Comparable<NodeProperties>
{
    private String name = null;
    private String displayName = null;
    private String className = null;
    private String nodeBase = null;

    public enum Type {
        FILTER,
        SERVICE,
        UNKNOWN
    }
    private Type type;
    
    private boolean hasPowerButton = true;
    private boolean autoStart = true;
    private boolean invisible = false;

    private List<String> parents = new LinkedList<String>();

    private int viewPosition = -1;

    public NodeProperties() {}
    
    /**
     * Internal name of the node.
     */
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    /**
     * Name of the main node Class.
     */
    public String getClassName() { return className; }
    public void setClassName( String className ) { this.className = className; }

    /**
     * The parent node, usually a casing.
     */
    public List<String> getParents() { return parents; }
    public void setParents( List<String> parents ) { this.parents = parents; }

    /**
     * The name of the node, for display purposes.
     */
    public String getDisplayName() { return displayName; }
    public void setDisplayName( String displayName ) { this.displayName = displayName; }
    
    /**
     * The nodeBase is the name of the base node. For example
     * clam-node's nodeBase is untangle-base-virus.
     */
    public String getNodeBase() { return nodeBase; }
    public void setNodeBase( String nodeBase ) { this.nodeBase = nodeBase; }

    /**
     * The type is the type of node
     */
    public Type getType() { return type; }
    public void setType( Type type ) { this.type = type; }

    /**
     * The view position in the rack
     */
    public int getViewPosition() { return viewPosition; }
    public void setViewPosition( int newValue ) { this.viewPosition = newValue; }
    
    /**
     * True if this node can be turned on and off.  False, otherwise.
     */
    public boolean getHasPowerButton() { return hasPowerButton; }
    public void setHasPowerButton( boolean newValue ) { this.hasPowerButton = newValue; }

    /**
     * True if this node should be started automatically.
     */
    public boolean getAutoStart() { return autoStart; }
    public void setAutoStart( boolean newValue ) { this.autoStart = newValue; }

    /**
     * True if this node should be started automatically.
     */
    public boolean getInvisible() { return invisible; }
    public void setInvisible( boolean newValue ) { this.invisible = newValue; }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof NodeProperties)) {
            return false;
        }

        NodeProperties td = (NodeProperties)o;

        return getName().equals( td.getName() );
    }

    public String toString()
    {
        return toJSONString();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public int compareTo( NodeProperties a )
    {
        return new Integer(getViewPosition()).compareTo(a.getViewPosition());
    }
}
