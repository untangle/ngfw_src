/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NewSessionRequest.java,v 1.1 2005/01/06 02:39:41 jdi Exp $
 */

package com.metavize.mvvm.tapi;

public interface NewSessionRequest 
{
    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Argon.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    int id();

    /**
     * <code>mPipe</code> returns the Meta Pipe <code>MPipe</code> that this session lives on.
     *
     * @return the <code>MPipe</code> that this session is for
     */
    MPipe mPipe();
}
