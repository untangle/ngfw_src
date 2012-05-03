/**
 * $Id$
 */
package com.untangle.node.ips;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ips node settings.
 */
@SuppressWarnings("serial")
public class IpsSettings implements Serializable
{
    private Set<IpsRule> rules = new HashSet<IpsRule>();
    private Set<IpsVariable> variables = new HashSet<IpsVariable>();
    private Set<IpsVariable> immutables = new HashSet<IpsVariable>();
    private int maxChunks;

    public IpsSettings() {}

    public void updateStatistics(IpsStatistics argStats)
    {
        argStats.setRulesLength(null == rules ? 0 : rules.size());
        argStats.setVariablesLength(null == variables ? 0 : variables.size());
        argStats.setImmutableVariablesLength(null == immutables ? 0 : immutables.size());

        int logging = 0;
        int blocking = 0;

        for( IpsRule rule : rules)
            {
                if(rule.isLive()) blocking++;
                if(rule.getLog()) logging++;
            }

        argStats.setTotalAvailable(null == rules ? 0 : rules.size());
        argStats.setTotalBlocking(blocking);
        argStats.setTotalLogging(logging);
    }

    // maxChunks -----------------------------------------------------------------

    public int getMaxChunks()
    {
        return maxChunks;
    }

    public void setMaxChunks(int maxChunks)
    {
        this.maxChunks = maxChunks;
    }

    // rules --------------------------------------------------------------------

    public Set<IpsRule> grabRules()
    {
        return this.rules;
    }

    public List<IpsRule> getRules()
    {
        List<IpsRule> local = new LinkedList<IpsRule>(this.rules);
        return local;
    }

    public void pokeRules(Set<IpsRule> rules)
    {
        this.rules = rules;
    }

    public void setRules(List<IpsRule> rules)
    {
        this.rules = new HashSet<IpsRule>(rules);
    }

    // variables -----------------------------------------------------------------

    public Set<IpsVariable> grabVariables()
    {
        return this.variables;
    }

    public List<IpsVariable> getVariables()
    {
        List<IpsVariable> local = new LinkedList<IpsVariable>(this.variables);
        return local;
    }

    public void pokeVariables(Set<IpsVariable> variables)
    {
        this.variables = variables;
    }

    public void setVariables(List<IpsVariable> variables)
    {
        this.variables = new HashSet<IpsVariable>(variables);
    }

    // immutables ----------------------------------------------------------------

    public Set<IpsVariable> grabImmutables()
    {
        return this.immutables;
    }

    public List<IpsVariable> getImmutables()
    {
        List<IpsVariable> local = new LinkedList<IpsVariable>(this.immutables);
        return local;
    }

    public void pokeImmutables(Set<IpsVariable> immutables)
    {
        this.immutables = immutables;
    }

    public void setImmutables(List<IpsVariable> immutables)
    {
        this.immutables = new HashSet<IpsVariable>(immutables);
    }
}
