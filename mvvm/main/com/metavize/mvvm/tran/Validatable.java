/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Validatable.java 229 2005-05-25 22:25:00Z inieves $
 */

package com.metavize.mvvm.tran;

public interface Validatable {

    /**
     * Allows a data object to be checked for internal and external consistancy.
     *
     * @author <a href="mailto:inieves@metavize.com">Ian Morris Nieves</a>
     * @version 1.0
     */
    public void validate() throws ValidateException;
}
