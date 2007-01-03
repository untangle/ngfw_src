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

package com.untangle.mvvm.user;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tran.LocalTransformManager;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformContextSwitcher;
import com.untangle.tran.util.MVLogger;

/* a wrapper that automatically detects if an assistant needs to
 * switch context before executing */
class TransformAssistant implements Assistant, TransformContextSwitcher.Event<UserInfo>
{
    private static final MVLogger logger = new MVLogger( TransformAssistant.class );

    private final Assistant assistant;
    private final int priority;
    private final TransformContextSwitcher<UserInfo> transformContextSwitcher;

    private TransformAssistant( Assistant assistant, TransformContext transformContext )
    {
        this.transformContextSwitcher = new TransformContextSwitcher<UserInfo>( transformContext );
        this.assistant = assistant;
        this.priority = assistant.priority();
    }

    /* pass thru that guarantees the context is switched properly */
    public void lookup( UserInfo info )
    {
        transformContextSwitcher.run( this, info );
    }

    public int priority()
    {
        return this.priority;
    }

    /* TransformContextSwitcher.Event */
    public void handle( UserInfo info )
    {
        this.assistant.lookup( info );
    }

    /* ---------------- Package ---------------- */
    /* this returns the assistant this wrapper is wrapping */
    Assistant getAssistant()
    {
        return this.assistant;
    }

    /* Automatically detects whether or not a transform assistant is
     * necessary, if it is not then it will return the original
     * assistant, if it is, then it returns a new assistant that will
     * switch contexts as necessary */
    static Assistant fixTransformContext( Assistant assistant )
    {
        LocalTransformManager transformManager = MvvmContextFactory.context().transformManager();

        TransformContext transformContext = transformManager.threadContext();

        /* no transform context, nothing to do */
        if ( transformContext == null && !( assistant instanceof TransformAssistant )) {
            logger.debug( "the assistant ", assistant, " has no transform context, using as is." );
            return assistant;
        }

        logger.debug( "building a new transform assistant for ", assistant );
        return new TransformAssistant( assistant, transformContext );
    }
}