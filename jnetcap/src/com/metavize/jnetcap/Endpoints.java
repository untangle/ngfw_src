/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Endpoints.java,v 1.3 2004/12/20 19:54:06 rbscott Exp $
 */

package com.metavize.jnetcap;

public interface Endpoints {
    public Endpoint client();
    public Endpoint server();
}
