/**
 * $Id$
 */
package com.untangle.node.protofilter;

import com.untangle.uvm.node.Node;
import java.util.LinkedList;
import java.util.List;

public interface ProtoFilter extends Node
{
    ProtoFilterSettings getSettings();
    void setSettings(ProtoFilterSettings settings);

    int getPatternsTotal();
    int getPatternsLogged();
    int getPatternsBlocked();
}
