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

package com.untangle.uvm.vnet;

import java.util.List;

import com.untangle.uvm.node.Node;


/**
 * The <code>ArgonConnector</code> interface represents an active ArgonConnector.
 * Most nodes only have one active <code>ArgonConnector</code> at a time, the
 * rest have exactly 2 (casings).
 *
 * This class's instances represent and contain the subscription
 * state, pipeline state, and accessors to get the live sessions for
 * the pipe, as well as
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface ArgonConnector
{
    /**
     * Deactivates an active ArgonConnector and disconnects it from argon.
     * This kills all sessions and threads, and keeps any new sessions
     * or further commands from being issued.
     *
     * The ArgonConnector may not be used again.  State will be
     * <code>DEAD_ARGON</code> from here on out.
     */
    void destroy();

    PipeSpec getPipeSpec();

    int[] liveSessionIds();

    List<VnetSessionDesc> liveSessionDescs();

    List<IPSession> liveSessions();
    
    Node node();
}


