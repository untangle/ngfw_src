/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TCPSessionDesc.java,v 1.5 2005/01/06 02:39:41 jdi Exp $
 */

package com.metavize.mvvm.tapi;

public interface TCPSessionDesc extends IPSessionDesc {

    /**
     * TCP clients and servers have a state of <code>HALF_OPEN_INPUT</code> when the
     * input side has closed but the output side has not.  This should
     * be extremely unusual currently.
     */
    static final byte HALF_OPEN_INPUT = 5;


    /**
     * TCP clients and servers have a state of <code>HALF_OPEN_OUTPUT</code> when the
     * output side has closed but the input side has not.  This happens
     * for '% rsh host sort < file' for example.
     */
    static final byte HALF_OPEN_OUTPUT = 6;


    // boolean isClientShutdown();
 
    // boolean isServerShutdown();


}
