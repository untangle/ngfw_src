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


package com.untangle.jnetcap;

public interface TCPEndpoints extends Endpoints {
    public int fd();

    /**
     * Configure a TCP File descriptor for blocking or non-blocking mode.<p/>
     *
     * @param mode <code>true</code> enable blocking, <code>false</code> to disable blocking.
     */
    public void blocking( boolean mode );

    public int read( byte[] data );

    public int write( byte[] data );
    public int write( String data );

    public void close();
}
