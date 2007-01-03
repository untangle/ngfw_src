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

package com.untangle.mvvm.tran;

public interface Equivalence {
    /**
     * Check a new object against the current object for equivalence
     *
     * @author <a href="mailto:inieves@untangle.com">Ian Morris Nieves</a>
     * @version 1.0
     */
    public boolean equals(Object newObject);
}
