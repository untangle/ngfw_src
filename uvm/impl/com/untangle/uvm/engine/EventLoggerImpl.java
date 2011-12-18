/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.NodeContext;

/**
 * Implementation of EventLogger.
 */
class EventLoggerImpl<E extends LogEvent> extends EventLogger<E>
{
    private static final boolean LOGGING_DISABLED;

    private final NodeContext nodeContext;
    private final BlockingQueue<LogEventDesc> inputQueue;
    private final String tag;

    private final Logger logger = Logger.getLogger(getClass());

    public EventLoggerImpl()
    {
        this.nodeContext = null;
        inputQueue = UvmContextImpl.getInstance().loggingManager().getInputQueue();
        this.tag = "uvm[0]: ";
    }

    public EventLoggerImpl(NodeContext nodeContext)
    {
        this.nodeContext = nodeContext;
        inputQueue = UvmContextImpl.getInstance().loggingManager().getInputQueue();
        String name = nodeContext.getNodeDesc().getSyslogName();
        this.tag = name + "[" + nodeContext.getNodeId().getId() + "]: ";
    }

    public void log(E e)
    {
        if (LOGGING_DISABLED) {
            return;
        }

        if (!inputQueue.offer(new LogEventDesc(this, e, tag))) {
            logger.warn("dropping logevent: " + e);
        }
    }

    NodeContext getNodeContext()
    {
        return nodeContext;
    }

    static {
        LOGGING_DISABLED = LoggingManagerImpl.isLoggingDisabled();
    }
}
