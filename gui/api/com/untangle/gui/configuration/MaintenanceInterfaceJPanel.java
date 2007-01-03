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

package com.untangle.gui.configuration;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.untangle.mvvm.networking.*;

public class MaintenanceInterfaceJPanel extends MEditTableJPanel{


    public MaintenanceInterfaceJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(false);
	super.setAuxJPanelEnabled(true);

	// add a basic description
	JLabel descriptionJLabel = new JLabel("<html>Press the Refresh Settings Button to get an updated network interface readout</html>");
	descriptionJLabel.setFont(new Font("Default", 0, 12));
	auxJPanel.setLayout(new BorderLayout());
	auxJPanel.add(descriptionJLabel);

        // create actual table model
	InterfaceModel interfaceModel = new InterfaceModel();
	this.setTableModel( interfaceModel );

    }
    
class InterfaceModel extends MSortedTableModel<MaintenanceCompoundSettings>{ 
    
    private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int  C2_MW = 120;  /* network interface */
    private static final int  C3_MW = 230;  /* connection */
    private static final int  C4_MW = 175;  /* mode */
    private static final int  C5_MW = 65;   /* block ping */

    
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, false, false, false, false, String.class, null, sc.html("network<br>interface") );
        addTableColumn( tableColumnModel,  3,  C3_MW, false, false, false, false, String.class, null, sc.html("connection") );
        addTableColumn( tableColumnModel,  4,  C4_MW, false, true,  false, false, ComboBoxModel.class, null, sc.html("mode") );
        addTableColumn( tableColumnModel,  5,  C5_MW, false, true,  false, false, Boolean.class, null, sc.html("block<br>ping") );
        addTableColumn( tableColumnModel,  6,  10,    false, false, true,  false, Interface.class, null, "");
        return tableColumnModel;
    }
    
    
    public void generateSettings(MaintenanceCompoundSettings maintenanceCompoundSettings,
				 Vector<Vector> tableVector, boolean validateOnly) throws Exception {        
        List<Interface> elemList = new ArrayList(tableVector.size());
	Interface newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (Interface) rowVector.elementAt(6);
	    newElem.setEthernetMedia( (EthernetMedia) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
	    newElem.setIsPingable( !(Boolean) rowVector.elementAt(5) );
            elemList.add(newElem);
        }
        
	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    NetworkSpacesSettings networkSettings = maintenanceCompoundSettings.getNetworkSettings();
	    networkSettings.setInterfaceList(elemList);
	}
    }

    public Vector<Vector> generateRows(MaintenanceCompoundSettings maintenanceCompoundSettings) {
	NetworkSpacesSettings networkSettings = maintenanceCompoundSettings.getNetworkSettings();
	List<Interface> interfaceList = 
            (List<Interface>) networkSettings.getInterfaceList();
        Vector<Vector> allRows = new Vector<Vector>(interfaceList.size());
	Vector tempRow = null;
        int rowIndex = 0;

        for( Interface intf : interfaceList ){
	    rowIndex++;
	    tempRow = new Vector(7);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
            tempRow.add( intf.getName() );
	    tempRow.add( intf.getConnectionState() + (intf.getConnectionState().equals("connected")?" @ "+intf.getCurrentMedia():"") );
	    tempRow.add( super.generateComboBoxModel( EthernetMedia.getEnumeration(), intf.getEthernetMedia()) );
	    //tempRow.add( intf.getCurrentMedia() );
            /* The column is named block Ping, so must use the inverse, the variable is for
            allow ping. */
            tempRow.add( !intf.getIsPingable() );
	    tempRow.add( intf );
	    allRows.add( tempRow );
        }
        return allRows;
    }
    
    
}


}
