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

public interface Validatable {

    /**
     * Allows a data object to be checked for internal and external consistancy.
     *
     * @author <a href="mailto:inieves@untangle.com">Ian Morris Nieves</a>
     * @version 1.0
     */
    public void validate() throws ValidateException;
}
