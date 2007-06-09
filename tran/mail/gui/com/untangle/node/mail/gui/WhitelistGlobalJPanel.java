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

package com.untangle.node.mail.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.configuration.EmailCompoundSettings;
import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.networking.*;

public class WhitelistGlobalJPanel extends MEditTableJPanel{

    public WhitelistGlobalJPanel() {
        super(true, true);
        super.setFillJButtonEnabled(false);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        WhitelistGlobalTableModel whitelistGlobalTableModel = new WhitelistGlobalTableModel();
        this.setTableModel( whitelistGlobalTableModel );
    }




    class WhitelistGlobalTableModel extends MSortedTableModel<EmailCompoundSettings>{

        private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
        private static final int  C2_MW = 200;  /* email address */


        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, true,  true,  false, false, String.class,  "",   sc.html("Email Address") );
            return tableColumnModel;
        }


        public void generateSettings(EmailCompoundSettings emailCompoundSettings,
                                     Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            List<String> elemList = new ArrayList<String>(tableVector.size());

            for( Vector rowVector : tableVector ){
                elemList.add( (String) rowVector.elementAt(2) );
            }

            // SAVE SETTINGS //////////
            if( !validateOnly ){
                ((MailNodeCompoundSettings)emailCompoundSettings.getMailNodeCompoundSettings()).setGlobalSafelist( elemList );
            }
        }

        public Vector<Vector> generateRows(EmailCompoundSettings emailCompoundSettings) {
            List<String> safeList = ((MailNodeCompoundSettings)emailCompoundSettings.getMailNodeCompoundSettings()).getGlobalSafelist();
            Vector allRows = new Vector(safeList.size());
            int rowIndex = 0;
            Vector tempRow = null;

            for( String address : safeList ){
                rowIndex++;
                tempRow = new Vector(3);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( address );
                allRows.add( tempRow );
            }
            return allRows;
        }


    }

}
