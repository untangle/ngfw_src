/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Skin informations
 */
public class SkinInfo
{
    private String name = null;
    private String displayName = null;
    
    // specifies if the skin provides styling for admin pages
    private boolean adminSkin = false;
    private int adminSkinVersion = 0;
    
    // specifies if the skin provides styling for user facing pages
    private boolean userFacingSkin = false;
    private int userFacingSkinVersion = 0;
    private String extjsTheme = null;

    /* Compatible skin version */
    private static final int COMPATIBLE_ADMIN_SKIN = 3;
    private static final int COMPATIBLE_USER_SKIN = 2;

    public SkinInfo() {}
    
    public SkinInfo(String name, String displayName, boolean adminSkin, int adminSkinVersion, boolean userFacingSkin, int userFacingSkinVersion, String extjsTheme)
    {
        super();
        this.name = name;
        this.displayName = displayName;
        this.adminSkin = adminSkin;
        this.adminSkinVersion = adminSkinVersion;
        this.userFacingSkin = userFacingSkin;
        this.userFacingSkinVersion = userFacingSkinVersion;
        this.setExtjsTheme(extjsTheme);
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
}
