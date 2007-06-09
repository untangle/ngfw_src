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
package com.untangle.node.sasl;
import static com.untangle.node.util.ASCIIUtil.*;


/**
 * Observer for ANONYMOUS (RFC 2245) mechanism.
 */
class ANONYMOUSObserver
    extends InitialIDObserver {

    static final String[] MECH_NAMES = new String[] {
        "ANONYMOUS".toLowerCase()
    };

    ANONYMOUSObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }
}
