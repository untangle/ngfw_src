/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.jnetcap;

public interface Endpoints {
    public Endpoint client();

    public Endpoint server();

    /** 
     * Retrieve the name of an interface of an empty string if the interface is unknown.
     */
    public String interfaceName();

    /**
     * Retrieve a unique interface identifier, or 0 if the interface is unknown
     */
    public byte interfaceId();
}
