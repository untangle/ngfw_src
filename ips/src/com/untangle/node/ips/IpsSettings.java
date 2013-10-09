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

    /**
     * maxChunks is the maximum number of chunks scanned in a session
     */
    private int maxChunks = 4;

    /**
     * sessionBypassLimit is the maximum number of sessions that will
     * simultaneously be scanned. If sessionBypassLimit sessions are
     * currently be scanned, then new sessions will be bypassed
     */
    private int sessionBypassLimit = 10;

    /**
     * loadBypassLimit is the maximum load for which new sessions will
     * be scanned. If the 1-minute load exceeds this new sessions will
     * be bypassed
     */
    private float loadBypassLimit = 2.0f;
    
    public IpsSettings() {}

    public void updateStatistics(IpsStatistics argStats)
    {
        argStats.setRulesLength(null == rules ? 0 : rules.size());
        argStats.setVariablesLength(null == variables ? 0 : variables.size());
        argStats.setImmutableVariablesLength(null == immutables ? 0 : immutables.size());

        int logging = 0;
        int blocking = 0;

        for( IpsRule rule : rules) {
            if(rule.isLive()) blocking++;
            if(rule.getLog()) logging++;
        }

        argStats.setTotalAvailable(null == rules ? 0 : rules.size());
        argStats.setTotalBlocking(blocking);
        argStats.setTotalLogging(logging);
    }

    public int getSessionBypassLimit() { return this.sessionBypassLimit; }
    public void setSessionBypassLimit( int newValue ) { this.sessionBypassLimit = newValue; }

    public float getLoadBypassLimit() { return this.loadBypassLimit; }
    public void setLoadBypassLimit( float newValue ) { this.loadBypassLimit = newValue; }
    
    public int getMaxChunks() { return maxChunks; }
    public void setMaxChunks( int newValue ) { this.maxChunks = newValue; }

    public List<IpsRule> getRules() { return new LinkedList<IpsRule>(this.rules); }
    public void setRules( List<IpsRule> newValue ) { this.rules = new HashSet<IpsRule>(newValue); }

    public List<IpsVariable> getVariables()  { return new LinkedList<IpsVariable>(this.variables); }
    public void setVariables( List<IpsVariable> variables) { this.variables = new HashSet<IpsVariable>(variables); }

    public List<IpsVariable> getImmutables() { return new LinkedList<IpsVariable>(this.immutables); }
    public void setImmutables(List<IpsVariable> immutables) { this.immutables = new HashSet<IpsVariable>(immutables); }
}
