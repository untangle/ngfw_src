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



package com.untangle.node.spyware.gui;


import java.awt.Insets;
import java.util.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.node.spyware.*;
//import javax.swing.event.*;

public class SubnetConfigJPanel extends MEditTableJPanel{


    public SubnetConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spyware sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);

        // create actual table model
        SpyTableModel spyTableModel = new SpyTableModel();
        this.setTableModel( spyTableModel );
        spyTableModel.setSortingStatus(2, SpyTableModel.ASCENDING);
    }
}


class SpyTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = 140; /* subnet */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = 55; /* log */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */



    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, IPMaddrString.class,  "1.2.3.4/5", "subnet");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  true,  false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "true", sc.bold("log"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, IPMaddrRule.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
        IPMaddrRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (IPMaddrRule) rowVector.elementAt(7);
            // newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setName( (String) rowVector.elementAt(2) );
            try{
                IPMaddr newIPMaddr = IPMaddr.parse( ((IPMaddrString) rowVector.elementAt(3)).getString() );
                newElem.setIpMaddr( newIPMaddr );
            }
            catch(Exception e){ throw new Exception("Invalid \"subnet\" specified at row: " + rowIndex);  }
            newElem.setLive( (Boolean) rowVector.elementAt(4) );
            newElem.setLog( (Boolean) rowVector.elementAt(5) );
            newElem.setDescription( (String) rowVector.elementAt(6) );

            elemList.add(newElem);
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            SpywareSettings spywareSettings = (SpywareSettings) settings;
            spywareSettings.setSubnetRules( elemList );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        SpywareSettings spywareSettings = (SpywareSettings) settings;
        List<IPMaddrRule> subnetRules = (List<IPMaddrRule>) spywareSettings.getSubnetRules();
        Vector<Vector> allRows = new Vector<Vector>(subnetRules.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( IPMaddrRule newElem : subnetRules ){
            rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            // tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getName() );
            tempRow.add( new IPMaddrString(newElem.getIpMaddr()) );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getLog() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
