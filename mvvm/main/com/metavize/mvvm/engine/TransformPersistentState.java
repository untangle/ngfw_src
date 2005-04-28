/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;


import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformState;

/**
 * Internal transform state.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TRANSFORM_PERSISTENT_STATE"
 */
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

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
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
     * @hibernate.many-to-one
     * cascade="none"
     * column="TID"
     */
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
     * @hibernate.property
     * column="NAME"
     * not-null="true"
     * length="64"
     */
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
     * @hibernate.list
     * table="TRANSFORM_ARGS"
     * @hibernate.collection-key
     * column="TPS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-element
     * type="string"
     * column="ARG"
     * not-null="true"
    */
    List getArgs()
    {
        return args;
    }

    String[] getArgArray()
    {
        return (String[])args.toArray(new String[args.size()]);
    }

    void setArgs(List args)
    {
        this.args = args;
    }

    /**
     * Not really used.
     * XXX move into Tid, or associate with TID?
     * XXX for now length of 16 because not really used anyway
     *
     * @return public key
     * @hibernate.property
     * type="binary"
     * column="PUBLIC_KEY"
     * length="16"
     * not-null="true"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.TransformStateUserType"
     * column="TARGET_STATE"
     * not-null="true"
     */
    TransformState getTargetState()
    {
        return targetState;
    }

    void setTargetState(TransformState targetState)
    {
        this.targetState = targetState;
    }
}
