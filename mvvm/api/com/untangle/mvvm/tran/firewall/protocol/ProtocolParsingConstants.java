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

package com.untangle.mvvm.tran.firewall.protocol;

/**
 * A set of string constants for the various matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class ProtocolParsingConstants
{
    /* String identifier for TCP */
    public static final String MARKER_TCP         = "TCP";

    /* String identifier for UDP */
    public static final String MARKER_UDP         = "UDP";

    /* String identifier for TCP and UDP */
    public static final String MARKER_TCP_AND_UDP = "TCP & UDP";

    /* String identifier for ping */
    public static final String MARKER_PING        = "PING";

    /* String identifier for any */
    public static final String MARKER_ANY         = "ANY";
}
