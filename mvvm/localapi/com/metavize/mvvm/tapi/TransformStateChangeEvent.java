/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformState;

public class TransformStateChangeEvent extends EventObject
{
    private final TransformState transformState;
    private final List<String> args;

    TransformStateChangeEvent(Transform t, TransformState transformState,
                              String[] args)
    {
        this(t, transformState, (List<String>)(null == args ? Collections.emptyList() :  Arrays.asList(args)));
    }

    TransformStateChangeEvent(Transform t, TransformState transformState,
                              List<String> args)
    {
        super(t);

        this.transformState = transformState;
        this.args = Collections.unmodifiableList(args);
    }

    public TransformState getTransformState()
    {
        return transformState;
    }

    public List<String> getArgs()
    {
        return args;
    }
}