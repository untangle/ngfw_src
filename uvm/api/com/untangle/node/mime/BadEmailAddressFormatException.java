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

package com.untangle.tran.mime;

/**
 * Exception thrown when some sequence of
 * characters cannot be converted into a
 * valid email address.
 */
public class BadEmailAddressFormatException
    extends Exception {

    public BadEmailAddressFormatException() {
        super();
    }
    public BadEmailAddressFormatException(Exception ex) {
        super(ex);
    }
    public BadEmailAddressFormatException(String msg) {
        super(msg);
    }
    public BadEmailAddressFormatException(String msg, Exception ex) {
        super(msg, ex);
    }

}
