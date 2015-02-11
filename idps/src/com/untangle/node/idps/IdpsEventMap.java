/**
 * $Id: IpsSettings.java 38161 2014-07-23 23:01:44Z dmorris $
 */
package com.untangle.node.idps;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Idps event rule mapping
 */
@SuppressWarnings("serial")
public class IdpsEventMap implements Serializable
{
    private Set<IdpsEventMapRule> rules = new HashSet<IdpsEventMapRule>();

    public List<IdpsEventMapRule> getRules() { return new LinkedList<IdpsEventMapRule>(this.rules); }
    public void setRules( List<IdpsEventMapRule> newValue ) { this.rules = new HashSet<IdpsEventMapRule>(newValue); }

    public IdpsEventMapRule getRuleBySignatureAndGeneratorId( long signatureId, long generatorId ){
        IdpsEventMapRule bestMatchRule = null;
    	for( IdpsEventMapRule rule : this.rules ){
            if( ( rule.getSid() == signatureId ) &&
                ( rule.getGid() == generatorId ) ){
                /*
                 * Explicit signature and signature match.
                 */
                return rule;                
            }
    		if( rule.getSid() == signatureId ){
                /*
                 * Fall back to just a signature match
                 */
    			bestMatchRule = rule;
    		}
    	}
    	return bestMatchRule;
    }
    
}
