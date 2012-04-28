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
    
    void submitMessage( Message m );
}
