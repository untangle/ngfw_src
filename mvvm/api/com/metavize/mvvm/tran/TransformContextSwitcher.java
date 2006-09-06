/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

public class TransformContextSwitcher
{
    private final ClassLoader classLoader;

    public TransformContextSwitcher( ClassLoader  classLoader )
    {
        this.classLoader = classLoader;
    }
    
    public void run( Runnable event )
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        // entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader( this.classLoader );
        try {
            event.run();
        } finally {
            ct.setContextClassLoader( oldCl );
            // left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }
}
