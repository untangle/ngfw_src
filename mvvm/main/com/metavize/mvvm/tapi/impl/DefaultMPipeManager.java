/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: DefaultMPipeManager.java,v 1.1 2004/12/18 00:44:23 jdi Exp $
 */

package com.metavize.mvvm.tapi.impl;

import com.metavize.mvvm.tapi.MPipeManager;
import java.net.*;


/**
 * Creates this platform's default MPipeManager
 */

public class DefaultMPipeManager {
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
