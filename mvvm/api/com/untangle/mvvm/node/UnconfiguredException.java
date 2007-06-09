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
package com.untangle.mvvm.tran;

public class UnconfiguredException extends TransformStartException
{
    public UnconfiguredException( String message )
    {
        super( message );
    }

    public UnconfiguredException( Exception e )
    {
        super( e );
    }

    public UnconfiguredException( String message, Exception e )
    {
        super( message, e );
    }

}
