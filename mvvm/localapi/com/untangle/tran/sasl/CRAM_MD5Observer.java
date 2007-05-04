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
package com.untangle.tran.sasl;
import static com.untangle.tran.util.ASCIIUtil.*;


/**
 * Observer for CRAM-MD5 (RFC 2195) mechanism.  Does
 * not find the user's credentials, but serves as a
 * placeholder so we know that this mechanism
 * <b>cannot</b> result in an encrypted channel.
 */
class CRAM_MD5Observer
    extends ClearObserver {

    static final String[] MECH_NAMES = new String[] {
        "CRAM-MD5".toLowerCase()
    };

    CRAM_MD5Observer() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }
}
