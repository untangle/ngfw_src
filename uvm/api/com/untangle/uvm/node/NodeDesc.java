/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.security.NodeId;

/**
 * Node settings and properties.
 */
@SuppressWarnings("serial")
public class NodeDesc implements Serializable
{
    private NodeId nodeId = null; 

    private String name = null;
    private String displayName = null;
    private String className = null;
    private String nodeBase = null;
    private String syslogName = null;
    private String type = null;
    
    private Boolean hasPowerButton = true;
    private Boolean autoStart = true;
    private Boolean singleInstance = false;

    private List<String> parents = new LinkedList<String>();
    private List<String> uvmResources = new LinkedList<String>();
    private List<String> annotatedClasses = new LinkedList<String>();

    private Integer viewPosition = null;

    public NodeDesc() {}
    
    /**
     * Node id.
     *
     * @return nodeId for this instance.
     */
    public NodeId getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId)
    {
        this.nodeId = nodeId;
    }
    
    /**
     * Internal name of the node.
     *
     * @return the node's name.
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Name of the main node Class.
     *
     * @return node class name.
     */
    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * The parent node, usually a casing.
     *
     * @return the parent node, null if node has no parent.
     */
    public List<String> getParents()
    {
        return parents;
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }

    /**
     * A list of uvm resources, resources to be loaded into the UVM Class Path.
     *
     * @return The list of UVM resources.
     */
    public List<String> getUvmResources()
    {
        return uvmResources;
    }

    public void setUvmResources(List<String> uvmResources)
    {
        this.uvmResources = uvmResources;
    }

    /**
     * A list of uvm resources, resources to be loaded into the UVM Class Path.
     *
     * @return The list of UVM resources.
     */
    public List<String> getAnnotatedClasses()
    {
        return annotatedClasses;
    }

    public void setAnnotatedClasses(List<String> annotatedClasses)
    {
        this.annotatedClasses = annotatedClasses;
    }
    
    /**
     * Only a single instance may be initialized in the system.
     *
     * @return true if only a single instance may be loaded.
     */
    public Boolean isSingleInstance()
    {
        return singleInstance;
    }

    public Boolean getSingleInstance()
    {
        return singleInstance;
    }

    public void setSingleInstance(Boolean singleInstance)
    {
        this.singleInstance = singleInstance;
    }
    
    /**
     * The name of the node, for display purposes.
     *
     * @return display name.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
    
    /**
     * The name of the node, for syslog purposes.
     *
     * @return syslog name.
     */
    public String getSyslogName()
    {
        return syslogName;
    }

    public void setSyslogName(String syslogName)
    {
        this.syslogName = syslogName;
    }

    /**
     * The nodeBase is the name of the base node. For example
     * clam-node's nodeBase is untangle-base-virus.
     *
     * @return the nodeBase, null if node does not have a base.
     */
    public String getNodeBase()
    {
        return nodeBase;
    }

    public void setNodeBase(String nodeBase)
    {
        this.nodeBase = nodeBase;
    }

    /**
     * The type is the type of node
     * "NODE|CASING|SERVICE|LIBRARY|BASE|LIB_ITEM|TRIAL|UNKNOW"
     *
     * @return the type, null if node does not have a type.
     */
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * The type is the type of node
     * "NODE|CASING|SERVICE|LIBRARY|BASE|LIB_ITEM|TRIAL|UNKNOW"
     *
     * @return the type, null if node does not have a type.
     */
    public Integer getViewPosition()
    {
        return viewPosition;
    }

    public void setViewPosition(Integer viewPosition)
    {
        this.viewPosition = viewPosition;
    }
    
    /**
     * True if this node can be turned on and off.  False, otherwise.
     */
    public Boolean getHasPowerButton()
    {
        return hasPowerButton;
    }

    public void setHasPowerButton(Boolean hasPowerButton)
    {
        this.hasPowerButton = hasPowerButton;
    }

    /**
     * True if this node should be started automatically.
     */
    public Boolean getAutoStart()
    {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart)
    {
        this.autoStart = autoStart;
    }

    // Object methods ---------------------------------------------------------

    /**
     * Equality based on the business key (nodeId).
     *
     * @param o the object to compare to.
     * @return true if equal.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof NodeDesc)) {
            return false;
        }

        NodeDesc td = (NodeDesc)o;

        return nodeId.equals(td.getNodeId());
    }

    @Override
    public String toString()
    {
        return "[NodeDesc name:" + getName() + "]";
    }
}
