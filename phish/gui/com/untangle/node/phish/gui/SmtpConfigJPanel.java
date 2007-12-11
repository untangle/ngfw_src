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


package com.untangle.node.phish.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.spam.*;


public class SmtpConfigJPanel extends MEditTableJPanel {

    public SmtpConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        SmtpTableModel smtpTableModel = new SmtpTableModel();
        this.setTableModel( smtpTableModel );
    }
}


class SmtpTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = 155; /* action if PHISH detected */
    private static final int C5_MW = 190; /* notification if PHISH detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  3, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>PHISH detected"));
        addTableColumn( tableColumnModel,  4, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, SpamSMTPConfig.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly)
        throws Exception
    {
        SpamSMTPConfig spamSMTPConfig = null;

        for( Vector rowVector : tableVector ){
            spamSMTPConfig = (SpamSMTPConfig) rowVector.elementAt(5);
            spamSMTPConfig.setScan( (Boolean) rowVector.elementAt(2) );
            spamSMTPConfig.setMsgAction( (SMTPSpamMessageAction) ((ComboBoxModel)rowVector.elementAt(3)).getSelectedItem() );
            spamSMTPConfig.setNotes( (String) rowVector.elementAt(4) );
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            SpamSettings spamSettings = (SpamSettings) settings;
            spamSettings.setSmtpConfig( spamSMTPConfig );
        }
    }

    public Vector<Vector> generateRows(Object settings) {
        SpamSettings spamSettings = (SpamSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(2);
        int rowIndex = 0;

        rowIndex++;
        Vector row = new Vector(7);
        SpamSMTPConfig spamSmtpConfig = spamSettings.getSmtpConfig();
        row.add( super.ROW_SAVED );
        row.add( rowIndex );
        row.add( spamSmtpConfig.getScan() );
        row.add( super.generateComboBoxModel(SMTPSpamMessageAction.getValues(), spamSmtpConfig.getMsgAction()) );
        row.add( spamSmtpConfig.getNotes() );
        row.add( spamSmtpConfig );
        allRows.add(row);

        return allRows;
    }
}
