/**
 * $Id: IpsSettings.java 38161 2014-07-23 23:01:44Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IntrusionPrevention event signature mapping
 */
@SuppressWarnings("serial")
public class IntrusionPreventionEventMap implements Serializable
{
    private Set<IntrusionPreventionEventMapSignature> signatures = new HashSet<>();

    /**
     * Read the signatures.
     *
     * @return
     *  List of event map type signatures.
     */
    public List<IntrusionPreventionEventMapSignature> getSignatures() { 
        return new LinkedList<>(this.signatures); 
    }
    /**
     * Set the signatures.
     *
     * @param newValue
     *  List of event map type signatures.
     */
    public void setSignatures( List<IntrusionPreventionEventMapSignature> newValue ) { this.signatures = new HashSet<>(newValue); }

    /**
     * Look for signature signature.
     *
     * @param signatureId
     *  Signature id to match.
     * @param generatorId
     *  Generator id to match.
     * @return
     *  MapSignature containing the best match.
     */
    public IntrusionPreventionEventMapSignature getSignatureBySignatureAndGeneratorId( long signatureId, long generatorId ){
        IntrusionPreventionEventMapSignature bestMatchSignature = null;
    	for( IntrusionPreventionEventMapSignature signature : this.signatures ){
            if( ( signature.getSid() == signatureId ) &&
                ( signature.getGid() == generatorId ) ){
                /*
                 * Explicit signature and signature match.
                 */
                return signature;                
            }
    		if( signature.getSid() == signatureId ){
                /*
                 * Fall back to just a signature match
                 */
    			bestMatchSignature = signature;
    		}
    	}
    	return bestMatchSignature;
    }
    
}
