/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPEndpoints.java,v 1.4 2005/01/17 21:12:10 rbscott Exp $
 */


package com.metavize.jnetcap;

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
