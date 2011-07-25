/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Abstract base class for a Node.
 *
 */
public abstract class AbstractNode extends NodeBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private PipeSpec[] pipeSpecs;

    protected abstract PipeSpec[] getPipeSpecs();

    protected void connectArgonConnector()
    {
        if (null == pipeSpecs) {
            PipeSpec[] pss = getPipeSpecs();
            pipeSpecs = null == pss ? new PipeSpec[0] : pss;
            for (PipeSpec ps : pipeSpecs) {
                ps.connectArgonConnector();
            }
        } else {
            logger.warn("ArgonConnectors already connected");
        }
    }

    protected void disconnectArgonConnector()
    {
        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                ps.disconnectArgonConnector();
            }
            pipeSpecs = null;
        } else {
            logger.warn("ArgonConnectors not connected");
        }
    }

    public List<VnetSessionDesc> liveSessionDescs()
    {
        List<VnetSessionDesc> sessList = new LinkedList<VnetSessionDesc>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (VnetSessionDesc isd : ps.liveSessionDescs()) {
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
