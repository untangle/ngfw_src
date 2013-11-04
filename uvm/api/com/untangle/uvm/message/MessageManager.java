/**
 * $Id: MessageManager.java,v 1.00 2012/04/01 18:07:20 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.util.List;
import java.util.Map;

public interface MessageManager
{
    MessageQueue getMessageQueue();
    MessageQueue getMessageQueue( Long policyId );
}
