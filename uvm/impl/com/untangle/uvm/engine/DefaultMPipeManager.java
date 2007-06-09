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

package com.untangle.uvm.engine;

import java.net.*;

import com.untangle.uvm.tapi.MPipeManager;


/**
 * Creates this platform's default MPipeManager
 */

class DefaultMPipeManager {
    /**
     * Prevent instantiation.
     */
    private DefaultMPipeManager() { }

    /**
     * Returns the default MPipeManager.
     */
    public static MPipeManager create() {
        // We already verified change-mPipe-manager permission, so don't worry about
        // enforcing singularity here.
        return new MPipeManagerImpl();
    }
}
