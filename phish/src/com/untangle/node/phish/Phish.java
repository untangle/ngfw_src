/**
 * $Id$
 */
package com.untangle.node.phish;

import com.untangle.uvm.node.EventEntry;
import com.untangle.node.spam.SpamNode;

public interface Phish extends SpamNode
{
    PhishSettings getSettings();
    void setSettings(PhishSettings phishSettings);
}
