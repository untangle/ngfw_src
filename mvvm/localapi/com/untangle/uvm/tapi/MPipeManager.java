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

package com.untangle.mvvm.tapi;

import java.net.*;

import com.untangle.mvvm.tapi.event.SessionEventListener;


/**
 * Service-provider & manager class for MetaPipes.
 *
 * <p>A <code>MPipeManager</code> is a concrete subclass of this class
 * that has a zero-argument constructor and implements the abstract
 * methods herein.  A given Meta Node virtual machine maintains a single
 * system-wide default manager instance, which is returned by the {@link
 * #manager manager} method.  The first invocation of that method will locate
 * and cache the default provider as specified below.
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
public interface MPipeManager {
    /**
     * The <code>plumbLocal</code> activates a new section of MetaPipe
     * for the given transform.  No attempt is made to limit tranforms
     * to only one active MPipe at this level (e.g. casings have two).
     *
     * Remote doesn't exist yet. XX
     */
    MPipe plumbLocal(PipeSpec pipeSpec, SessionEventListener listener);
}
