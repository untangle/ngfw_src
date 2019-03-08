/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.Serializable;

/**
 * Skin informations
 */
@SuppressWarnings("serial")
public class SkinInfo implements Serializable, org.json.JSONString
{
    private String name = null;
    private String displayName = null;
    
    // specifies if the skin provides styling for admin pages
    private boolean adminSkin = false;
    private int adminSkinVersion = 0;
    //extjs framework theme to be used for this skin
    private String extjsTheme = "gray";
    // application view type used in the administration client 
    private String appsViewType = "rack";
    

    /* Compatible skin version */
    private static final int COMPATIBLE_ADMIN_SKIN = 3;

    public SkinInfo() {}
    
    public SkinInfo(String name, String displayName, boolean adminSkin, int adminSkinVersion, String extjsTheme, String appsViewType)
    {
        super();
        this.name = name;
        this.displayName = displayName;
        this.adminSkin = adminSkin;
        this.adminSkinVersion = adminSkinVersion;
        this.extjsTheme = extjsTheme;
        this.appsViewType = appsViewType;
    }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isAdminSkin() { return adminSkin; }
    public void setAdminSkin(boolean adminSkin) { this.adminSkin = adminSkin; }

    public int getAdminSkinVersion() { return adminSkinVersion; }
    public void setAdminSkinVersion(int adminSkinVersion) { this.adminSkinVersion = adminSkinVersion; }

    public boolean isAdminSkinOutOfDate() { return getAdminSkinVersion() < COMPATIBLE_ADMIN_SKIN; }

    public String getExtjsTheme() { return extjsTheme; }
    public void setExtjsTheme(String extjsTheme) { this.extjsTheme = extjsTheme; }

    public String getAppsViewType() { return appsViewType; }
    public void setAppsViewType(String appsViewType) { this.appsViewType = appsViewType; }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
