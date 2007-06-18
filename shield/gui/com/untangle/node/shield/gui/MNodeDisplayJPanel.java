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


package com.untangle.node.shield.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.uvm.node.NodeContext;

public class MNodeDisplayJPanel extends com.untangle.gui.node.MNodeDisplayJPanel{

    public MNodeDisplayJPanel(MNodeJPanel mNodeJPanel){
        super(mNodeJPanel);
        
        super.activity0JLabel.setText("TCP OK");
        super.activity1JLabel.setText("UDP OK");
        
    }
    
    final protected boolean getUpdateActivity(){ return false; }
    final protected boolean getUpdateSessions(){ return false; }
    
}
