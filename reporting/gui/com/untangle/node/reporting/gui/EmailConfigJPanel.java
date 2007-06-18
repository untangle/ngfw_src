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



package com.untangle.node.reporting.gui;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.MailSettings;
import com.untangle.uvm.node.*;
import com.untangle.node.reporting.*;

public class EmailConfigJPanel extends MEditTableJPanel {

    public EmailConfigJPanel() {
        super(true, true);
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("Reports via Email");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        EmailTableModel emailTableModel = new EmailTableModel( this );
        this.setTableModel( emailTableModel );
    }

}


class EmailTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 150; /* email address */
    //    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW), 120); /* description */

    private EmailConfigJPanel emailConfigJPanel;
    public EmailTableModel( EmailConfigJPanel emailConfigJPanel ){
        this.emailConfigJPanel = emailConfigJPanel;
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "reportrecipient@example.com", "Email address");
        //addTableColumn( tableColumnModel,  3, C3_MW, true,   true, false,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }



    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        StringBuilder elemList = new StringBuilder(tableVector.size());
        String newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = ((String) rowVector.elementAt(2)).trim();

            if(rowIndex != 1)
                elemList.append(", ");
            elemList.append(newElem);
        }

        // SAVE SETTINGS /////
        if( !validateOnly ){
            MailSettings mailSettings = Util.getAdminManager().getMailSettings();
            mailSettings.setReportEmail(elemList.toString());
            Util.getAdminManager().setMailSettings( (MailSettings) mailSettings );
        }
    }

    public Vector<Vector> generateRows(Object settings){
        MailSettings mailSettings = Util.getAdminManager().getMailSettings();
        String recipients = mailSettings.getReportEmail();
        StringTokenizer recipientsTokenizer = new StringTokenizer(recipients, ",");
        Vector<Vector> allRows = new Vector<Vector>(recipientsTokenizer.countTokens());
        Vector tempRow = null;
        int rowIndex = 0;

        while( recipientsTokenizer.hasMoreTokens() ){
            rowIndex++;
            tempRow = new Vector(3);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( recipientsTokenizer.nextToken().trim() );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
