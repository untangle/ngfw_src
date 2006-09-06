/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.openvpn.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.openvpn.*;

import java.awt.Insets;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class ConfigSiteToSiteJPanel extends MEditTableJPanel {

    public ConfigSiteToSiteJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("client group rules");
        super.setDetailsTitle("rule notes");

        // create actual table model
        TableModelSiteToSite tableModelSiteToSite = new TableModelSiteToSite();
        this.setTableModel( tableModelSiteToSite );
    }
}

