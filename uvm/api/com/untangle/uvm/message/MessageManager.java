/**
 * $Id: MessageManager.java,v 1.00 2012/04/01 18:07:20 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.util.List;
import java.util.Map;

public interface MessageManager
{
    MessageQueue getMessageQueue();
    MessageQueue getMessageQueue( Integer key );
    MessageQueue getMessageQueue( Integer key, Long policyId );

    List<Message> getMessages();
    List<Message> getMessages( Integer key );
    Integer       getMessageKey();
    
    StatDescs getStatDescs( Long nodeId );
    Stats getStats( Long nodeId );
    Stats getAllStats( Long nodeId );
    Map<String, Object> getSystemStats();

    Counters getUvmCounters();
    Counters getCounters( Long nodeId );
    List<NodeMetric> getActiveMetrics( Long nodeId );

    void submitMessage( Message m );

    void setActiveMetrics( Long nodeId, List<NodeMetric> activeStats );
    void setActiveMetrics( Long nodeId, BlingBlinger... blingers );
}
