/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: ReleasedEventHandler.java,v 1.4 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi.impl;

import com.metavize.mvvm.tapi.AbstractEventHandler;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for released
 * sessions and whenever the transform has no smithEventListener.  We just use everything
 * from AbstractEventHandler.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
class ReleasedEventHandler extends AbstractEventHandler {
    ReleasedEventHandler() {
        super();
    }
}
