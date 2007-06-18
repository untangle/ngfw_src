/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
