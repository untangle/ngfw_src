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

package com.untangle.node.mime;

/**
 * Callback interface for an Object wishing to
 * be informed when MIMEPart object changes.
 * Not called a "Listener" because I see no need
 * for there to be more than one.
 * <br>
 * Note that for all callbacks, the change has already
 * taken place (if the tense of the method verb didn't
 * already give that away).
 */
public interface MIMEPartObserver {


    /**
     * A MIMEPart has changed
     */
    public void mIMEPartChanged(MIMEPart part);

}
