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

import com.untangle.uvm.vnet.event.SessionEventListener;


/**
 * Service-provider & manager class for MetaPipes.
 *
 * <p>A <code>MPipeManager</code> is a concrete subclass of this class
 * that has a zero-argument constructor and implements the abstract
 * methods herein.  A given Meta Node virtual machine maintains a single
 * system-wide default manager instance, which is returned by the {@link
 * #manager manager} method.  The first invocation of that method will locate
 * and cache the default provider as specified below.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface MPipeManager {
    /**
     * The <code>plumbLocal</code> activates a new section of MetaPipe
     * for the given node.  No attempt is made to limit nodes
     * to only one active MPipe at this level (e.g. casings have two).
     *
     * Remote doesn't exist yet. XX
     */
    MPipe plumbLocal(PipeSpec pipeSpec, SessionEventListener listener);
}
