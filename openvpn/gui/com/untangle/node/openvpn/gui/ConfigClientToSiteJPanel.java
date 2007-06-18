/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package com.untangle.node.openvpn.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.node.firewall.*;
import com.untangle.node.openvpn.*;

import java.awt.Insets;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class ConfigClientToSiteJPanel extends MEditTableJPanel {

    public ConfigClientToSiteJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("client group rules");
        super.setDetailsTitle("rule notes");

        // create actual table model
        TableModelClientToSite tableModelClientToSite = new TableModelClientToSite();
        this.setTableModel( tableModelClientToSite );
    }
}

