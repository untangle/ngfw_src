/**
 * $Id: MessageQueue.java,v 1.00 2012/04/06 12:44:16 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class MessageQueue implements Serializable
{
    private final List<Message> messages;
    private final Map<Long, Stats> stats;
    private final Map<String, Object> systemStats;

    public MessageQueue(List<Message> messages, Map<Long, Stats> stats, Map<String, Object> systemStats)
    {
        this.messages = messages;
        this.stats = stats;
        this.systemStats = systemStats;
    }

    public Map<Long, Stats> getStats()
    {
        return stats;
    }

    public List<Message> getMessages()
    {
        return messages;
    }

    public Map<String, Object> getSystemStats()
    {
        return systemStats;
    }
}