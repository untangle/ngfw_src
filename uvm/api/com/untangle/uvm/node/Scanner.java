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

package com.untangle.uvm.node;

public interface Scanner
{

    /**
     * Gets the name of the vendor of this scanner, used for logging & reporting
     *
     * @return a <code>String</code> giving the name of the vendor of this scanner
     */
    String getVendorName();
}
