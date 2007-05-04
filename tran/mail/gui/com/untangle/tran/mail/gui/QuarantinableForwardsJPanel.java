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

package com.untangle.tran.mail.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.configuration.EmailCompoundSettings;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.quarantine.*;

public class QuarantinableForwardsJPanel extends MEditTableJPanel{

    public QuarantinableForwardsJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        ForwardsModel forwardsModel = new ForwardsModel();
        this.setTableModel( forwardsModel );
    }




    class ForwardsModel extends MSortedTableModel<EmailCompoundSettings>{

        private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
        private static final int  C2_MW = 150;  /* d-list address */
        private static final int  C3_MW = 150;  /* send-to address */
        private static final int  C4_MW = 120;  /* category */
        private static final int  C5_MW = 120;  /* description */

        public ForwardsModel(){
        }

        protected boolean getSortable(){ return false; }

        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, true,   true,  false, false, String.class, "distributionlistrecipient@example.com", sc.html("distribution<br>list address") );
            addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  false, false, String.class, "quarantinelistowner@example.com", sc.html("send to<br>address") );
            addTableColumn( tableColumnModel,  4,  C4_MW, true,  true,  true, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
            addTableColumn( tableColumnModel,  5,  C5_MW, true,  true,  true, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
            addTableColumn( tableColumnModel,  6,  10,    false, false, true,  false, EmailAddressPairRule.class, null, "");
            return tableColumnModel;
        }


        public void generateSettings(EmailCompoundSettings emailCompoundSettings,
                                     Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            List<EmailAddressPairRule> elemList = new ArrayList(tableVector.size());
            EmailAddressPairRule newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : tableVector ){
                rowIndex++;
                newElem = (EmailAddressPairRule) rowVector.elementAt(6);
                newElem.setAddress1( (String) rowVector.elementAt(2) );
                newElem.setAddress2( (String) rowVector.elementAt(3) );
                newElem.setCategory( (String) rowVector.elementAt(4) );
                newElem.setDescription( (String) rowVector.elementAt(5) );
                elemList.add(newElem);
            }

            // SAVE SETTINGS //////////
            if( !validateOnly ){
                QuarantineSettings quarantineSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings()).getQuarantineSettings();
                quarantineSettings.setAddressRemaps(elemList);
            }
        }

        public Vector<Vector> generateRows(EmailCompoundSettings emailCompoundSettings) {
            QuarantineSettings quarantineSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings()).getQuarantineSettings();
            List<EmailAddressPairRule> addressList = (List<EmailAddressPairRule>) quarantineSettings.getAddressRemaps();
            Vector<Vector> allRows = new Vector<Vector>(addressList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            for( EmailAddressPairRule address : addressList ){
                rowIndex++;
                tempRow = new Vector(7);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( address.getAddress1() );
                tempRow.add( address.getAddress2() );
                tempRow.add( address.getCategory());
                tempRow.add( address.getDescription());
                tempRow.add( address );
                allRows.add( tempRow );
            }
            return allRows;
        }
    }
}
