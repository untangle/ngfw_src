/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.util;

import com.metavize.mvvm.tran.IPMaddr;

public interface IPSet {

    public void add (IPMaddr mask, Object result);

    public Object getMostSpecific  (IPMaddr mask);
    public Object getLeastSpecific (IPMaddr mask);

}
