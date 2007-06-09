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

package com.untangle.node.router;


/* Just used to indicate that the outside interface is not configured, it happens in one very well defined
 * case, hence it doesn't need a message 
 */
class RouterUnconfiguredException extends Exception {
    private static RouterUnconfiguredException INSTANCE = new RouterUnconfiguredException();
    
    RouterUnconfiguredException()
    {
        super();
    }

    static RouterUnconfiguredException getInstance() {
        return INSTANCE;
    }
}

