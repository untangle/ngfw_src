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

package com.untangle.uvm.portal;

import java.security.Principal;

/**
 * Base interface to the PortalManager.
 *
 * @author <a href="mailto:jdi@slabuntu">John Irwin</a>
 * @version 1.0
 */
public interface BasePortalManager
{
    LocalApplicationManager applicationManager();

    boolean isLive(Principal p);

    void destroy();
}
