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


import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.StringRule;
import com.untangle.node.spyware.*;

public class CookieConfigJPanel extends MEditTableJPanel{


    public CookieConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Cookies");
        super.setDetailsTitle("cookie details");
        super.setAddRemoveEnabled(true);

        // create actual table model
        CookieTableModel cookieTableModel = new CookieTableModel();
        this.setTableModel( cookieTableModel );
        cookieTableModel.setSortingStatus(2, SpyTableModel.ASCENDING);
    }
}


class CookieTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 240; /* identification */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */



    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.empty( "no identification" ), "identification");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        StringRule newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (StringRule) rowVector.elementAt(5);
            newElem.setString( (String) rowVector.elementAt(2) );
            newElem.setLive( (Boolean) rowVector.elementAt(3) );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS //////////
        if( !validateOnly ){
            SpywareSettings spywareSettings = (SpywareSettings) settings;
            spywareSettings.setCookieRules(elemList);
        }

    }

    public Vector<Vector> generateRows(Object settings){
        SpywareSettings spywareSettings = (SpywareSettings) settings;
        List<StringRule> cookieRules = (List<StringRule>) spywareSettings.getCookieRules();
        Vector<Vector> allRows = new Vector<Vector>(cookieRules.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( StringRule newElem : cookieRules ){
            rowIndex++;
            tempRow = new Vector(6);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getString() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
