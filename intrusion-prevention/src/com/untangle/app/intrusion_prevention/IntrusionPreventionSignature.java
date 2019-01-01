/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;

/**
 * Intrusion prevention custom signature
 *
 * Again, only for custom, user-created signatures.  
 * Signatures from distributions like Emerging Threats are not stored in settings.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionSignature implements Serializable, JSONString
{
    private String signature = "";
    private String category = "";

    public IntrusionPreventionSignature() { }

    public IntrusionPreventionSignature(String signature, String category)
    {
        this.signature = signature;
        this.category = category;
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

