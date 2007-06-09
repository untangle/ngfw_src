/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformState;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

/**
 * Internal transform state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="transform_persistent_state")
class TransformPersistentState
{
    private Long id;
    private Tid tid;
    private String name;
    private List args;
    private TransformState targetState;
    private byte[] publicKey;

    // constructors -----------------------------------------------------------

    TransformPersistentState() { }

    TransformPersistentState(Tid tid, String name, byte[] publicKey)
    {
        this.tid = tid;
        this.name = name;
        this.publicKey = publicKey;

        this.targetState = TransformState.INITIALIZED;
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
     * Transform id.
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
     * Internal name of the transform.
     *
     * @return the transform's name.
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
     * Transform string arguments, used by some transforms rather than
     * database settings.
     *
     * @return transform arguments.
     */
    @CollectionOfElements(fetch=FetchType.EAGER)
    @JoinTable(name="transform_args",
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
     * The desired state upon initial load. When the MVVM starts, it
     * attempts to place the transform in this state. Subsequent
     * changes in state at runtime become the new target state, such
     * that if the MVVM is restarted, the transform resumes its last
     * state.
     *
     * @return the target state.
     */
    @Column(name="target_state", nullable=false)
    @Type(type="com.untangle.mvvm.type.TransformStateUserType")
    TransformState getTargetState()
    {
        return targetState;
    }

    void setTargetState(TransformState targetState)
    {
        this.targetState = targetState;
    }
}
