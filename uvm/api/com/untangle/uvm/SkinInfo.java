/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm/api/com/untangle/uvm/SkinInfo.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.uvm;


/**
 * Skin informations
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
public class SkinInfo {

    private String name = null;
    private String displayName = null;
    
    // specifies if the skin provides styling for admin pages
    private boolean adminSkin = false;
    private int adminSkinVersion = 0;
    
    // specifies if the skin provides styling for user facing pages
    private boolean userFacingSkin = false;
    private int userFacingSkinVersion = 0;

    /* Compatible skin version */
    private static final int COMPATIBLE_ADMIN_SKIN = 2;
    private static final int COMPATIBLE_USER_SKIN = 2;

    
	public SkinInfo() {
	}
	
	public SkinInfo(String name, String displayName, boolean adminSkin,
			int adminSkinVersion, boolean userFacingSkin, int userFacingSkinVersion) {
		super();
		this.name = name;
		this.displayName = displayName;
		this.adminSkin = adminSkin;
		this.adminSkinVersion = adminSkinVersion;
		this.userFacingSkin = userFacingSkin;
		this.userFacingSkinVersion = userFacingSkinVersion;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public boolean isAdminSkin() {
		return adminSkin;
	}
	public void setAdminSkin(boolean adminSkin) {
		this.adminSkin = adminSkin;
	}

	public int getAdminSkinVersion() {
		return adminSkinVersion;
	}
	public void setAdminSkinVersion(int adminSkinVersion) {
		this.adminSkinVersion = adminSkinVersion;
	}

	public boolean isAdminSkinOutOfDate() {
	    return getAdminSkinVersion() < COMPATIBLE_ADMIN_SKIN;
	}

	public boolean isUserFacingSkin() {
		return userFacingSkin;
	}
	public void setUserFacingSkin(boolean userFacingSkin) {
		this.userFacingSkin = userFacingSkin;
	}    

	public int getUserFacingSkinVersion() {
		return userFacingSkinVersion;
	}    
	public void setUserFacingSkinVersion(int userFacingSkinVersion) {
		this.userFacingSkinVersion = userFacingSkinVersion;
	}    
	public boolean isUserFacingSkinOutOfDate() {
	    return getUserFacingSkinVersion() < COMPATIBLE_USER_SKIN;
	}

}
