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

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeException;

public interface ProtoFilter extends Node
{
    ProtoFilterBaseSettings getBaseSettings();
    void setBaseSettings(ProtoFilterBaseSettings baseSettings);

    List<ProtoFilterPattern> getPatterns(int start, int limit, String... sortColumns);
    void updatePatterns(List<ProtoFilterPattern> added, List<Long> deleted, List<ProtoFilterPattern> modified);
    
    /**
     * Update all settings once
     */
    @SuppressWarnings("unchecked")
	void updateAll(List[] patternsChanges);
    
    /**
     * Reconfigure node. This method should be called after some settings are updated
     * in order to reconfigure the node accordingly.
     *
     * @throws NodeException if an exception occurs when reconfiguring.
     */
	void reconfigure() throws NodeException;

    
    EventManager<ProtoFilterLogEvent> getEventManager();
    
}
