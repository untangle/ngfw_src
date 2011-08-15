/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.protofilter;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;
import java.util.LinkedList;
import java.util.List;

public interface ProtoFilter extends Node
{
    ProtoFilterSettings getNodeSettings();
    void setNodeSettings(ProtoFilterSettings settings);

    LinkedList<ProtoFilterPattern> getPatterns();
    void setPatterns(LinkedList<ProtoFilterPattern> patterns);
    
    int getPatternsTotal();
    int getPatternsLogged();
    int getPatternsBlocked();

    /**
     * Reconfigure node. This method should be called after some settings are updated
     * in order to reconfigure the node accordingly.
     *
     * @throws Exception if an exception occurs when reconfiguring.
     */
    void reconfigure() throws Exception;

    EventManager<ProtoFilterLogEvent> getEventManager();
}
