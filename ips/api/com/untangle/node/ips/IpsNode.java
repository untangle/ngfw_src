/**
 * $Id$
 */
package com.untangle.node.ips;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface IpsNode extends Node
{
    EventLogQuery[] getEventQueries();

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
