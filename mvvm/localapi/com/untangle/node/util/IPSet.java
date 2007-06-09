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
package com.untangle.tran.util;

import com.untangle.mvvm.tran.IPMaddr;

public interface IPSet {

    public void add (IPMaddr mask, Object result);

    public Object getMostSpecific  (IPMaddr mask);
    public Object getLeastSpecific (IPMaddr mask);

}
