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

package com.untangle.uvm.node;

import java.io.InputStream;

import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.IPSessionDesc;

/**
 * Holds the context for a Node instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface NodeContext
{
    /**
     * Get the NodeId for this instance.
     *
     * @return the node id.
     */
    NodeId getNodeId();

    /**
     * Get the node for this context.
     *
     * @return this context's node.
     */
    Node node();

    /**
     * Returns desc from uvm-node.xml.
     *
     * @return the NodeDesc.
     */
    NodeDesc getNodeDesc();

    /**
     * Returns the node preferences.
     *
     * @return the NodePreferences.
     */
    NodePreferences getNodePreferences();

    /**
     * Get the {@link MackageDesc} corresponding to this instance.
     *
     * @return the MackageDesc.
     */
    MackageDesc getMackageDesc();

    // XXX should be LocalNodeContext ------------------------------------

    // XXX
    boolean runTransaction(TransactionWork<?> tw);

    InputStream getResourceAsStream(String resource);

    /**
     * <code>resourceExists</code> returns true if the given resources exists
     * for this Node.  False if it does not exist.
     *
     * @param resource a <code>String</code> naming the resource
     * @return a <code>boolean</code> true if the resource exists,
     * false otherwise.
     */
    boolean resourceExists(String resource);

    // call-through methods ---------------------------------------------------

    IPSessionDesc[] liveSessionDescs();

    NodeState getRunState();
}
