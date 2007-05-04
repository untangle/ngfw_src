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

package com.untangle.tran.mail.papi.quarantine;
import java.io.Serializable;

/**
 * Exception thrown when a Token (a sequence of characters
 * used to have obscured information passed back
 * in a URL) is of a bad format (garbage).
 */
public class BadTokenException
    extends Exception
    implements Serializable {

    public BadTokenException(String token) {
        super("Bad Token \"" + token + "\"");
    }


}
