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

package com.untangle.mvvm.tapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformState;

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