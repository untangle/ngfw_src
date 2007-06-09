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

package com.untangle.uvm.tapi;

public interface UDPSessionDesc extends IPSessionDesc {
    
    /**
     * <code>isPing</code> returns true if the given UDP Session is a ping session, false
     * if it is a normal UDP session.
     *
     * Note that currently ping sessions are distinguished from normal UDP sessions by having
     * both server and client ports of 0.
     *
     * @return a <code>boolean</code> true if the session is a ping session
     */
    boolean isPing();

    /**
     * <code>icmpId</code> returns the id of the ICMP session.
     * only valid if isPing is true */
    int icmpId();
}
