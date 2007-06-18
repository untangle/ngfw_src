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


package com.untangle.node.test.gui;


import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import com.untangle.gui.test.*;
import com.untangle.gui.node.*;
import com.untangle.uvm.client.*;


public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // SOME LIST //
        javax.swing.JPanel someJPanel = new javax.swing.JPanel();
        addTab(NAME_SOME_LIST, null, someJPanel);

        addTab("Address Book Test", null, new AddressBookTestPanel());

        //super.savableMap.put(NAME_SOME_LIST, someJPanel);
        //super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

        // EVENT LOG /////
        //LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNodeContext().node(), this);
        //addTab(NAME_LOG, null, logJPanel);
        //addShutdownable(NAME_LOG, logJPanel);
    }

}
