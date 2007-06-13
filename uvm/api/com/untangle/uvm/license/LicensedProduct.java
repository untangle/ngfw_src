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

package com.untangle.uvm.license;

/**
 * An empty interface used to indicate that a part is licensed.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface LicensedProduct
{
    /* This is the unique identifier for the product, this should be
     * independent from the "marketing" name just so it is easier to
     * upgrade and licenses always work after upgrade.
     */
     public String productIdentifier();
}
