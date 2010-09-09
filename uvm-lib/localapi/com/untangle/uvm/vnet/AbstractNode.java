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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Abstract base class for a Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class AbstractNode extends NodeBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private PipeSpec[] pipeSpecs;

    // no-op methods ----------------------------------------------------------

    protected abstract PipeSpec[] getPipeSpecs();

    // NodeBase methods --------------------------------------------------

    protected void connectMPipe()
    {
        if (null == pipeSpecs) {
            PipeSpec[] pss = getPipeSpecs();
            pipeSpecs = null == pss ? new PipeSpec[0] : pss;
            for (PipeSpec ps : pipeSpecs) {
                ps.connectMPipe();
            }
        } else {
            logger.warn("MPipes already connected");
        }
    }

    protected void disconnectMPipe()
    {
        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                ps.disconnectMPipe();
            }
            pipeSpecs = null;
        } else {
            logger.warn("MPipes not connected");
        }
    }

    // Node methods ------------------------------------------------------

    public void dumpSessions()
    {
        for (PipeSpec ps : pipeSpecs) {
            ps.dumpSessions();
        }
    }

    public List<IPSessionDesc> liveSessionDescs()
    {
        List<IPSessionDesc> sessList = new LinkedList<IPSessionDesc>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (IPSessionDesc isd : ps.liveSessionDescs()) {
                    sessList.add(isd);
                }
            }
        }

        return sessList;
    }

    public List<IPSession> liveSessions()
    {
        List<IPSession> sessions = new LinkedList<IPSession>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (IPSession sess : ps.liveSessions()) {
                    sessions.add(sess);
                }
            }
        }

        return sessions;
    }
}
