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
 * Class to encapsulate a Test which can be executed from
 * the UI.  The "meat" of a test is its action, which is
 * a callback from the UI saying "do this test now".
 * <br>
 * The action can either be defined by subclassing
 * MVUITest (and overidding {@link #actionSelected actionSelected}
 * or by implementing {@link com.untangle.gui.test.MVUITestAction MVUITestAction}
 * (similar to the pattern for Java threads with the "Runnable"
 * interface).
 * <br>
 * Note that tests should be stateless, creating all dependent
 * objects on each call to {@link #actionSelected actionSelected}.
 */
public class MVUITest
    implements MVUITestAction {

    private final String m_name;
    private final String m_desc;
    private final MVUITestAction m_callback;

    public MVUITest(String name,
                    String desc) {
        this(name, desc, null);
    }

    public MVUITest(String name,
                    String desc,
                    MVUITestAction callback) {

        m_name = name;
        m_desc = desc;
        m_callback = callback;
    }

    public String getName() {
        return m_name;
    }
    public String getDescription() {
        return m_desc;
    }

    /**
     * Callback indicating that the given
     * action has been selected (the button
     * was pushed).  This may be overidden.
     *
     * @param panel the panel, with all the method needed to
     *        communicate with the user of the UI.
     *
     * @exception anything thrown by the test.  Note that this
     *            will make it to the display screen in some
     *            way to show the user what went wrong.
     */
    public void actionSelected(TestPanel panel)
        throws Exception {
        if(m_callback != null) {
            m_callback.actionSelected(panel);
        }
    }
}
