/**
 * $Id: SkinSettings.java,v 1.00 2012/05/09 15:53:44 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * Uvm skin settings.
 */
@SuppressWarnings("serial")
public class SkinSettings implements Serializable{
    private Long id;
    private String administrationClientSkin = "default";
    private String userPagesSkin  = "default";
    private boolean outOfDate = false;

    public SkinSettings() { }

    private Long getId()
    {
        return id;
    }

	private void setId(Long id)
    {
        this.id = id;
    }
    
    /**
     * Get the skin used in the administration client
     *
     * @return skin name.
     */
	public String getAdministrationClientSkin()
    {
		return administrationClientSkin;
	}

	public void setAdministrationClientSkin(String administrationClientSkin)
    {
		this.administrationClientSkin = administrationClientSkin;
	}

    /**
     * Get the skin used in the user pages like quarantine and block pages.
     *
     * @return skin name.
     */
	public String getUserPagesSkin()
    {
		return userPagesSkin;
	}

	public void setUserPagesSkin(String userPagesSkin)
    {
		this.userPagesSkin = userPagesSkin;
	}

    public void copy(SkinSettings settings)
    {
        settings.setAdministrationClientSkin(this.administrationClientSkin);
        settings.setUserPagesSkin(this.userPagesSkin);
    }

    public boolean getOutOfDate()
    {
        return this.outOfDate;
    }

    public void setOutOfDate(boolean outOfDate)
    {
        this.outOfDate = outOfDate;
    }

}
