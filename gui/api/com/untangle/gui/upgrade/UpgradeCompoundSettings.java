/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.upgrade;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.MOneButtonJDialog;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.UpgradeSettings;

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
                                      "The upgrade server could not be contacted.",
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
