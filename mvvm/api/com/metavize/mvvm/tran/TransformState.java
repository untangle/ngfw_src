/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the runtime state of a transform instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public enum TransformState
{
    /**
     * Instantiated, but not initialized. This is a transient state,
     * just after the main transform class has been instantiated, but
     * before init has been called.
     */
    LOADED,

    /**
     * Initialized, but not running. The transform instance enters
     * this state after it has been initialized, or when it is
     * stopped.
     */
    INITIALIZED,

    /**
     * Running.
     */
    RUNNING,

    /**
     * Destroyed, this instance should not be used.
     */
    DESTROYED,

    /**
     * Disabled.
     */
    DISABLED;
}
