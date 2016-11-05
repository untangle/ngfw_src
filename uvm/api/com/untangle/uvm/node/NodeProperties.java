/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

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
    private boolean autoLoad = false;
    private boolean invisible = false;
    private int     viewPosition = -1;

    private List<String> parents = new LinkedList<String>();

    private List<String> supportedArchitectures = Arrays.asList("any");
    private Long         minimumMemory;

    public NodeProperties() {}
    
    /**
     * Internal name of the node.
     */
    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    /**
     * Name of the main node Class.
     */
    public String getClassName() { return className; }
    public void setClassName( String newValue ) { this.className = newValue; }

    /**
     * The parent node, usually a casing.
     */
    public List<String> getParents() { return parents; }
    public void setParents( List<String> newValue ) { this.parents = newValue; }

    /**
     * Get supported architectures
     */
    public List<String> getSupportedArchitectures() { return supportedArchitectures; }
    public void setSupportedArchitectures( List<String> newValue ) { this.supportedArchitectures = newValue; }

    /**
     * Get minimum memory requirements (null if none)
     */
    public Long getMinimumMemory() { return minimumMemory; }
    public void setMinimumMemory( Long newValue ) { this.minimumMemory = newValue; }

    /**
     * The name of the node, for display purposes.
     */
    public String getDisplayName() { return displayName; }
    public void setDisplayName( String newValue ) { this.displayName = newValue; }
    
    /**
     * The nodeBase is the name of the base node. For example
     * clam-node's nodeBase is untangle-base-virus-blocker.
     */
    public String getNodeBase() { return nodeBase; }
    public void setNodeBase( String newValue ) { this.nodeBase = newValue; }

    /**
     * The type is the type of node
     */
    public Type getType() { return type; }
    public void setType( Type newValue ) { this.type = newValue; }

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
     * True if this node should be started automatically (once loaded).
     */
    public boolean getAutoStart() { return autoStart; }
    public void setAutoStart( boolean newValue ) { this.autoStart = newValue; }

    /**
     * True if this node should be loaded automatically.
     */
    public boolean getAutoLoad() { return autoLoad; }
    public void setAutoLoad( boolean newValue ) { this.autoLoad = newValue; }
    
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

    public int hashCode( )
    {
        if ( getClassName() != null )
            return getClassName().hashCode();
        else
            return 0;
    }
}
