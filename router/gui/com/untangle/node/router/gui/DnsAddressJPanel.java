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


import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.networking.DnsStaticHostRule;
import com.untangle.uvm.node.*;
import com.untangle.uvm.node.firewall.*;
import com.untangle.node.router.*;

public class DnsAddressJPanel extends MEditTableJPanel{


    public DnsAddressJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        DnsAddressTableModel dnsAddressTableModel = new DnsAddressTableModel();
        this.setTableModel( dnsAddressTableModel );

    }





    class DnsAddressTableModel extends MSortedTableModel<Object>{


        private static final int  T_TW = Util.TABLE_TOTAL_WIDTH;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* line number */
        private static final int  C2_MW = 130;  /* host name */
        private static final int  C3_MW = 130;  /* IP address */
        private static final int  C4_MW = 120;  /* category */
        private final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 120); /* description */



        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, true,  true,  false, false, String.class, sc.empty("no hostname"), sc.html("translate this<br><b>hostname</b>") );
            addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  false, false, IPaddrString.class, "1.2.3.4", sc.html("into this<br><b>IP address</b>") );
            addTableColumn( tableColumnModel,  4,  C4_MW, true,  true,  true, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
            addTableColumn( tableColumnModel,  5,  C5_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
            addTableColumn( tableColumnModel,  6,  10,    false, false, true,  false, DnsStaticHostRule.class, null, "");
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            RouterCommonSettings routerSettings = (RouterCommonSettings) settings;
            List elemList = new ArrayList(tableVector.size());
            DnsStaticHostRule newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : tableVector ){
                rowIndex++;
                newElem = (DnsStaticHostRule) rowVector.elementAt(6);
                try{ newElem.setHostNameList( HostNameList.parse( (String)rowVector.elementAt(2)) ); }
                catch(Exception e){ throw new Exception("Invalid \"hostname\" in row: " + rowIndex); }
                try{ newElem.setStaticAddress( IPaddr.parse( ((IPaddrString)rowVector.elementAt(3)).getString()) ); }
                catch(Exception e){ throw new Exception("Invalid \"IP address\" in row: " + rowIndex); }
                newElem.setCategory( (String) rowVector.elementAt(4) );
                newElem.setDescription( (String) rowVector.elementAt(5) );
                elemList.add(newElem);
            }

            // SAVE SETTINGS ////////
            if( !validateOnly ){
                routerSettings.setDnsStaticHostList( elemList );
            }
        }


        public Vector<Vector> generateRows(Object settings) {
            RouterCommonSettings routerSettings = (RouterCommonSettings) settings;
            List<DnsStaticHostRule> dnsStaticHostList = (List<DnsStaticHostRule>) routerSettings.getDnsStaticHostList();
            Vector<Vector> allRows = new Vector<Vector>(dnsStaticHostList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            for( DnsStaticHostRule hostRule : dnsStaticHostList ){
                rowIndex++;
                tempRow = new Vector(7);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( hostRule.getHostNameList().toString() );
                tempRow.add( new IPaddrString(hostRule.getStaticAddress()) );
                tempRow.add( hostRule.getCategory() );
                tempRow.add( hostRule.getDescription() );
                tempRow.add( hostRule );
                allRows.add( tempRow);
            }
            return allRows;
        }

    }

}
