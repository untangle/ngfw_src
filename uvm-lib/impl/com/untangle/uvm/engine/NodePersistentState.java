/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.security.Tid;

/**
 * Internal node state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_node_persistent_state")
class NodePersistentState
{
    private Long id;
    private Tid tid;
    private String name;
    private List args;
    private NodeState targetState;
    private byte[] publicKey;

    // constructors -----------------------------------------------------------

    NodePersistentState() { }

    NodePersistentState(Tid tid, String name, byte[] publicKey)
    {
        this.tid = tid;
        this.name = name;
        this.publicKey = publicKey;

        this.targetState = NodeState.INITIALIZED;
        this.args = new LinkedList();
    }

    // bean methods -----------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Node id.
     *
     * @return tid for this instance.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    Tid getTid()
    {
        return tid;
    }

    void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Internal name of the node.
     *
     * @return the node's name.
     */
    @Column(nullable=false, length=64)
    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    /**
     * Node string arguments, used by some nodes rather than
     * database settings.
     *
     * @return node arguments.
     */
    @CollectionOfElements(fetch=FetchType.EAGER)
    @JoinTable(name="u_node_args",
               joinColumns=@JoinColumn(name="tps_id"))
    @Column(name="arg", nullable=false)
    @IndexColumn(name="position")
    List<String> getArgs()
    {
        return args;
    }

    void setArgs(List args)
    {
        this.args = args;
    }

    @Transient
    String[] getArgArray()
    {
        return (String[])args.toArray(new String[args.size()]);
    }

    /**
     * Not really used.
     * XXX move into Tid, or associate with TID?
     * XXX for now length of 16 because not really used anyway
     *
     * @return public key
     */
    @Column(name="public_key", length=16, nullable=false)
    byte[] getPublicKey()
    {
        return publicKey;
    }

    void setPublicKey(byte[] publicKey)
    {
        this.publicKey = publicKey;
    }

    /**
     * The desired state upon initial load. When the UVM starts, it
     * attempts to place the node in this state. Subsequent
     * changes in state at runtime become the new target state, such
     * that if the UVM is restarted, the node resumes its last
     * state.
     *
     * @return the target state.
     */
    @Column(name="target_state", nullable=false)
    @Type(type="com.untangle.uvm.type.NodeStateUserType")
    NodeState getTargetState()
    {
        return targetState;
    }

    void setTargetState(NodeState targetState)
    {
        this.targetState = targetState;
    }

    public String toString()
    {
        return "NodePersistentState (" + name + " targetState: " + targetState + ")";
    }
}
