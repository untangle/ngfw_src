/*
 * $HeadURL:$
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

import java.util.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.Node;
import com.untangle.node.shield.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);
        setTableModel(new LogTableModel());
        queryJComboBox.setVisible(false);
    }

    protected void refreshSettings(){
        settings = ((ShieldNode)super.logNode).getLogs(getEventDepth());
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null,  "timestamp" );
            addTableColumn( tableColumnModel,  1,  120, true,  false, false, false, String.class, null,  sc.html("source") );
            addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null,  sc.html("source<br>interface") );
            addTableColumn( tableColumnModel,  3,  75,  true,  false, false, false, Double.class, null,  "reputation" );
            addTableColumn( tableColumnModel,  4,  55,  true,  false, false, false, Integer.class, null, "limited" );
            addTableColumn( tableColumnModel,  5,  75,  true,  false, false, false, Integer.class, null, "dropped" );
            addTableColumn( tableColumnModel,  6,  55,  true,  false, false, false, Integer.class, null, "reject" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<ShieldRejectionLogEntry> logList = (List<ShieldRejectionLogEntry>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for ( ShieldRejectionLogEntry log : logList ) {
                event = new Vector(7);
                event.add( log.getCreateDate() );
                event.add( log.getClient() );
                event.add( log.getClientIntf() );
                event.add( log.getReputation() );
                event.add( log.getLimited() );
                event.add( log.getDropped() );
                event.add( log.getRejected() );
                allEvents.add( event );
            }
            return allEvents;
        }

    }

}
