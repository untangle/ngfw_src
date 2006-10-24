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
package com.metavize.gui.test;

/**
  * Callback interface used when a {@link com.metavize.gui.test.MVUITest MVUITest}
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
