/**
 * $Id: SkinSettings.java,v 1.00 2012/05/09 15:53:44 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * Uvm skin settings.
 */
@SuppressWarnings("serial")
public class SkinSettings implements Serializable
{
    private String skinName = "default";

    public SkinSettings() { }

    /**
     * Get the skin used in the administration client
     */
	public String getSkinName() { return skinName; }
	public void setSkinName( String skinName ) { this.skinName = skinName; }
}
