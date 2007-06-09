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

package com.untangle.mvvm.engine;

import com.untangle.mvvm.tapi.AbstractEventHandler;
import com.untangle.mvvm.tran.Transform;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for released
 * sessions and whenever the transform has no smithEventListener.  We just use everything
 * from AbstractEventHandler.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
class ReleasedEventHandler extends AbstractEventHandler {
    ReleasedEventHandler(Transform transform) {
        super(transform);
    }
}
