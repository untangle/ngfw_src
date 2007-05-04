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
package com.untangle.gui.test;

/**
 * Callback interface used when a {@link com.untangle.gui.test.MVUITest MVUITest}
 * has been requested to run by the user.
 */
public interface MVUITestAction {

    /**
     * Callback indicating that the given
     * action has been selected (the button
     * was pushed).
     *
     * @param panel the panel, with all the method needed to
     *        communicate with the user of the UI.
     */
    public void actionSelected(TestPanel panel) throws Exception;
}
