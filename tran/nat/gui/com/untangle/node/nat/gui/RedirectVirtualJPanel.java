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
package com.untangle.tran.nat.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.networking.RedirectRule;
import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.tran.nat.*;

public class RedirectVirtualJPanel extends MEditTableJPanel {

    public RedirectVirtualJPanel(com.untangle.tran.nat.gui.MTransformControlsJPanel mTransformControlsJPanel) {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        RedirectVirtualTableModel redirectVirtualTableModel = new RedirectVirtualTableModel(mTransformControlsJPanel);
        //redirectVirtualTableModel.setOrderModelIndex(0);
        this.setTableModel( redirectVirtualTableModel );
    }



    class RedirectVirtualTableModel extends MSortedTableModel<Object>{

        private static final int  T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_EDIT_MIN_WIDTH; /* # */
        private static final int  C2_MW = 75;   /* redirect */
        private static final int  C3_MW = 55;   /* log */
        private static final int  C4_MW = 100;  /* traffic type */
        private static final int  C5_MW = 190;  /* destination address */
        private static final int  C6_MW = 110;  /* destination port */
        private static final int  C7_MW = 130;  /* redirect to new address */
        private static final int  C8_MW = 110;  /* redirect to new port */
        private static final int  C9_MW = 120;  /* category */
        private final int C10_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW), 120); /* description */

        private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(), ProtocolMatcherFactory.getProtocolDefault() );
        private com.untangle.tran.nat.gui.MTransformControlsJPanel mTransformControlsJPanel;

        public RedirectVirtualTableModel(com.untangle.tran.nat.gui.MTransformControlsJPanel mTransformControlsJPanel){
            this.mTransformControlsJPanel = mTransformControlsJPanel;
        }

        private DefaultComboBoxModel interfaceModel = new DefaultComboBoxModel();
        private void updateInterfaceModel(){
            interfaceModel.removeAllElements();
            for( IPDBMatcher ipDBMatcher : mTransformControlsJPanel.getLocalMatcherList() )
                interfaceModel.addElement( ipDBMatcher );
        }

        protected boolean getSortable(){ return false; }

        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min     rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW,  false, false, true, false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW,  false, true,  false, false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW,  false, true,  false, false, Boolean.class, "true", sc.bold("enable") );
            addTableColumn( tableColumnModel,  3,  C3_MW,  false, true,  false, false, Boolean.class, "false",  sc.bold("log"));
            addTableColumn( tableColumnModel,  4,  C4_MW,  false, true,  false, false, ComboBoxModel.class, protocolModel, sc.html("traffic<br>type") );
            addTableColumn( tableColumnModel,  5,  C5_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("destination<br>address") );
            addTableColumn( tableColumnModel,  6,  C6_MW,  true,  true,  false, false, String.class, "80", sc.html("destination<br>port") );
            addTableColumn( tableColumnModel,  7,  C7_MW,  true,  true,  false, false, String.class, "192.168.1.10", sc.html("redirect to<br>new address") );
            addTableColumn( tableColumnModel,  8,  C8_MW,  true,  true,  false, false, String.class, "unchanged", sc.html("redirect to<br>new port") );
            addTableColumn( tableColumnModel,  9,  C9_MW,  true,  true,  true,  false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
            addTableColumn( tableColumnModel,  10, C10_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
            addTableColumn( tableColumnModel,  11, 10,     false, false, true,  false, RedirectRule.class, null, "");
            return tableColumnModel;
        }


        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            List elemList = new ArrayList(tableVector.size());
            RedirectRule newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : tableVector ){
                rowIndex++;
                newElem = (RedirectRule) rowVector.elementAt(11);
                newElem.setLive( (Boolean) rowVector.elementAt(2) );
                newElem.setLog( (Boolean) rowVector.elementAt(3) );
                newElem.setProtocol( ProtocolMatcherFactory.parse(((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem().toString()) );
                newElem.setDstAddress( (IPDBMatcher) ((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem());
                try{ newElem.setDstPort( PortMatcherFactory.parse((String) rowVector.elementAt(6)) ); }
                catch(Exception e){ throw new Exception("Invalid \"destination port\" in row: " + rowIndex); }
                try{ newElem.setRedirectAddress((String) rowVector.elementAt(7)); }
                catch(Exception e){ throw new Exception("Invalid \"redirect address\" in row: " + rowIndex); }
                try{ newElem.setRedirectPort((String) rowVector.elementAt(8)); }
                catch(Exception e){ throw new Exception("Invalid \"redirect port\" in row: " + rowIndex); }
                newElem.setCategory( (String) rowVector.elementAt(9) );
                newElem.setDescription( (String) rowVector.elementAt(10) );
                newElem.setDstRedirect( true );  // For now, all redirects are destination redirects
                newElem.setLocalRedirect( true );
                elemList.add(newElem);
            }

            // SAVE SETTINGS ////////////
            if( !validateOnly ){
                NatCommonSettings natSettings = (NatCommonSettings) settings;
                natSettings.setLocalRedirectList( elemList );
            }
        }


        public Vector<Vector> generateRows(Object settings) {
            NatCommonSettings natSettings = (NatCommonSettings) settings;
            List<RedirectRule> redirectList = (List<RedirectRule>) natSettings.getLocalRedirectList();
            Vector<Vector> allRows = new Vector<Vector>(redirectList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            updateInterfaceModel();

            for( RedirectRule redirectRule : redirectList ){
                rowIndex++;
                tempRow = new Vector(12);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( redirectRule.isLive() );
                tempRow.add( redirectRule.getLog() );
                tempRow.add( super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(), redirectRule.getProtocol().toString() ) );
                tempRow.add( super.generateComboBoxModel( mTransformControlsJPanel.getLocalMatcherList(), redirectRule.getDstAddress())  );
                tempRow.add( redirectRule.getDstPort().toString() );
                tempRow.add( redirectRule.getRedirectAddressString().toString() );
                tempRow.add( redirectRule.getRedirectPortString() );
                tempRow.add( redirectRule.getCategory() );
                tempRow.add( redirectRule.getDescription() );
                tempRow.add( redirectRule );
                allRows.add( tempRow );
            }

            return allRows;
        }

    }

}
