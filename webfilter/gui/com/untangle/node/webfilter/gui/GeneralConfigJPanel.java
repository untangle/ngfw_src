/*
 * $HeadURL: svn://chef/work/src/spyware/gui/com/untangle/node/spyware/gui/GeneralConfigJPanel.java $
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

package com.untangle.node.webfilter.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.webfilter.*;


public class GeneralConfigJPanel extends MEditTableJPanel {


    public GeneralConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel();
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, true,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        Vector tempRowVector;

        // user white list mode
        tempRowVector = tableVector.elementAt(0);
        UserWhitelistMode userWhitelistMode = (UserWhitelistMode) ((ComboBoxModel)tempRowVector.elementAt(3)).getSelectedItem();


        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            WebFilterSettings webfilterSettings = (WebFilterSettings) settings;
            webfilterSettings.setUserWhitelistMode( userWhitelistMode );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        WebFilterSettings webfilterSettings = (WebFilterSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(1);
        Vector tempRow = null;
        int rowIndex = 0;

        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "quick-passlist" );
        DefaultComboBoxModel whitelistComboBoxModel = new DefaultComboBoxModel();
        whitelistComboBoxModel.addElement(UserWhitelistMode.NONE);
        whitelistComboBoxModel.addElement(UserWhitelistMode.USER_ONLY);
        whitelistComboBoxModel.addElement(UserWhitelistMode.USER_AND_GLOBAL);
        whitelistComboBoxModel.setSelectedItem(webfilterSettings.getUserWhitelistMode());
        tempRow.add( whitelistComboBoxModel );
        tempRow.add( "This setting allows a user to unblock a webpage that was blocked by WebFilter.  The blocked webpage will have buttons that can be clicked." );
        allRows.add( tempRow );

        return allRows;
    }
}
