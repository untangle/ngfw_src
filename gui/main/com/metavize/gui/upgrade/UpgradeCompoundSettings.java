/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.upgrade;

import com.metavize.gui.transform.CompoundSettings;
import com.metavize.gui.util.Util;
import com.metavize.gui.widgets.dialogs.MOneButtonJDialog;
import com.metavize.mvvm.toolbox.MackageDesc;
import com.metavize.mvvm.toolbox.UpgradeSettings;

public class UpgradeCompoundSettings implements CompoundSettings {

    // UPGRADE SETTINGS //
    private UpgradeSettings upgradeSettings;
    public UpgradeSettings getUpgradeSettings(){ return upgradeSettings; }

    // MACKAGE DESCS //
    private MackageDesc[] upgradableMackageDescs;
    public MackageDesc[] getUpgradableMackageDescs(){ return upgradableMackageDescs; }

    public void save() throws Exception {
	Util.getToolboxManager().setUpgradeSettings(upgradeSettings);
    }

    public void refresh() throws Exception {
	upgradeSettings = Util.getToolboxManager().getUpgradeSettings();

	// HANDLE THE CASE WHERE THE STORE IS UNREACHABLE
	try{
	    Util.getToolboxManager().update();
	    upgradableMackageDescs = Util.getToolboxManager().upgradable();
	}
	catch(Exception e){
	    Util.getMMainJFrame().updateJButton(Util.UPGRADE_UNAVAILABLE);
	    Util.setUpgradeCount(Util.UPGRADE_UNAVAILABLE);

	    MOneButtonJDialog.factory(UpgradeJDialog.getInstance(), "",
				      "The upgrade server could not be contacted.  " +
				      "Please contact Metavize technical support.",
				      "Upgrade Failure Warning", "");
	    return;
	}

	if( Util.isArrayEmpty(upgradableMackageDescs) ){
	    Util.getMMainJFrame().updateJButton(0);
	    Util.setUpgradeCount(0);
	}
	else{
	    Util.getMMainJFrame().updateJButton(upgradableMackageDescs.length);
	    Util.setUpgradeCount(upgradableMackageDescs.length);
	}
    }

    public void validate() throws Exception {

    }
}
