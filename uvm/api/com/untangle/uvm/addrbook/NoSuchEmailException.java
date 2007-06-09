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

package com.untangle.uvm.addrbook;

/**
 * ...name says it all
 */
public class NoSuchEmailException
    extends Exception {

    public NoSuchEmailException(String email) {
        super("No such email address \"" + email + "\"");
    }

}


