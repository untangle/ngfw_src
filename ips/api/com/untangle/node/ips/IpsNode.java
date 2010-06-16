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

package com.untangle.node.ips;

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface IpsNode extends Node
{
    EventManager<IpsLogEvent> getEventManager();

    IpsBaseSettings getBaseSettings();
    void setBaseSettings(IpsBaseSettings baseSettings);

    List<IpsRule> getRules(int start, int limit, String... sortColumns);
    void updateRules(List<IpsRule> added, List<Long> deleted, List<IpsRule> modified);

    List<IpsVariable> getVariables(int start, int limit, String... sortColumns);
    void updateVariables(List<IpsVariable> added, List<Long> deleted, List<IpsVariable> modified);

    List<IpsVariable> getImmutableVariables(int start, int limit, String... sortColumns);
    void updateImmutableVariables(List<IpsVariable> added, List<Long> deleted, List<IpsVariable> modified);
    
    /**
     * Update all settings once, in a single transaction
     */
    void updateAll(IpsBaseSettings baseSettings, List<IpsRule>[] rules, List<IpsVariable>[] variables, List<IpsVariable>[] immutableVariables);
    
}
