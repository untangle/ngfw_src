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

package com.untangle.node.router.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.networking.*;
import com.untangle.uvm.node.*;
import com.untangle.node.router.*;

public class SpaceRoutingJPanel extends MEditTableJPanel{

    public SpaceRoutingJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        RoutingTableModel routingTableModel = new RoutingTableModel();
        this.setTableModel( routingTableModel );
    }




    class RoutingTableModel extends MSortedTableModel<Object>{

        private static final int  T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_EDIT_MIN_WIDTH; /* # */
        private static final int  C2_MW = 65;  /* enable action */
        private static final int  C3_MW = 160; /* destination */
        private static final int  C4_MW = 160; /* next hop */
        private static final int  C5_MW = 150;  /* category */
        private final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */



        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, false, true,  false, false, Boolean.class, "false", sc.bold("enable<br>rule") );
            addTableColumn( tableColumnModel,  3,  C3_MW,  true, true,  false, false, String.class, "1.2.3.4/255.255.255.0", sc.html("traffic destined to<br>this <b>IP Network</b>") );
            addTableColumn( tableColumnModel,  4,  C4_MW,  true, true,  false, false, String.class, "1.2.3.4", sc.html("will be sent to<br>this <b>IP Address</b>") );
            addTableColumn( tableColumnModel,  5,  C5_MW,  true, true,  true,  false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
            addTableColumn( tableColumnModel,  6,  C6_MW,  true, true,  false,  true, String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
            addTableColumn( tableColumnModel,  7,   10,   false, false, true,  false, Route.class, null, "");
            return tableColumnModel;
        }


        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            List<Route> elemList = new ArrayList<Route>(tableVector.size());
            Route newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : tableVector ){
                rowIndex++;
                newElem = (Route) rowVector.elementAt(7);
                newElem.setLive( (Boolean) rowVector.elementAt(2) );
                try{ newElem.setDestination( IPNetwork.parse((String) rowVector.elementAt(3)) ); }
                catch(Exception e){ throw new Exception("Invalid IP Network in row: " + rowIndex); }
                try{ newElem.setNextHop( IPaddr.parse((String) rowVector.elementAt(4)) ); }
                catch(Exception e){ throw new Exception("Invalid IP Address in row: " + rowIndex); }
                newElem.setCategory( (String) rowVector.elementAt(5) );
                newElem.setDescription( (String) rowVector.elementAt(6) );
                elemList.add(newElem);
            }

            // SAVE SETTINGS //////////
            if( !validateOnly ){
                NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
                networkSpacesSettings.setRoutingTable( elemList );
            }
        }



        public Vector<Vector> generateRows(Object settings) {
            NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
            List<Route> routeList = (List<Route>) networkSpacesSettings.getRoutingTable();
            Vector<Vector> allRows = new Vector<Vector>(routeList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            for( Route route : routeList ){
                rowIndex++;
                tempRow = new Vector(8);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( route.isLive() );
                tempRow.add( route.getDestination().toString() );
                tempRow.add( route.getNextHop().toString() );
                tempRow.add( route.getCategory() );
                tempRow.add( route.getDescription() );
                tempRow.add( route );
                allRows.add( tempRow );
            }
            return allRows;
        }


    }

}
