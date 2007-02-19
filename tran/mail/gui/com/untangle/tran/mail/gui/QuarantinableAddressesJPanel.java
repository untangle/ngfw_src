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

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.configuration.EmailCompoundSettings;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.tran.TransformContext;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.quarantine.*;

public class QuarantinableAddressesJPanel extends MEditTableJPanel{

    public QuarantinableAddressesJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        QuarantinableModel quarantinableModel = new QuarantinableModel();
        this.setTableModel( quarantinableModel );
    }
    



class QuarantinableModel extends MSortedTableModel<EmailCompoundSettings>{ 
    
    private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int  C2_MW = 150;  /* address */
    private static final int  C3_MW = 120;  /* category */
    private static final int  C4_MW = 120;  /* description */
    
    public QuarantinableModel(){
    }

    protected boolean getSortable(){ return false; }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, true,  true,  false, false, String.class, "someone@somewhere.com", sc.html("quarantinable<br>address") );
        addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  true, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
        addTableColumn( tableColumnModel,  4,  C4_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5,  10,    false, false, true,  false, EmailAddressRule.class, null, "");
        return tableColumnModel;
    }
    
    
    public void generateSettings(EmailCompoundSettings emailCompoundSettings,
				 Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List<EmailAddressRule> elemList = new ArrayList(tableVector.size());
	EmailAddressRule newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
	    rowIndex++;
	    newElem = (EmailAddressRule) rowVector.elementAt(5);
	    newElem.setAddress( (String) rowVector.elementAt(2) );
	    newElem.setCategory( (String) rowVector.elementAt(3) );
	    newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }
        
	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    QuarantineSettings quarantineSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings()).getQuarantineSettings();
	    quarantineSettings.setAllowedAddressPatterns(elemList);
	}
    }

    public Vector<Vector> generateRows(EmailCompoundSettings emailCompoundSettings) {
	QuarantineSettings quarantineSettings = ((MailTransformCompoundSettings)emailCompoundSettings.getMailTransformCompoundSettings()).getQuarantineSettings();
	List<EmailAddressRule> addressList = (List<EmailAddressRule>) quarantineSettings.getAllowedAddressPatterns();
        Vector<Vector> allRows = new Vector<Vector>(addressList.size());
	Vector tempRow = null;
        int rowIndex = 0;

        for( EmailAddressRule address : addressList ){
	    rowIndex++;
	    tempRow = new Vector(6);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
            tempRow.add( address.getAddress() );
            tempRow.add( address.getCategory() );
	    tempRow.add( address.getAddress().equals("*")?"All addresses have quarantines":address.getDescription() );
	    tempRow.add( address );
	    allRows.add( tempRow );
        }
        return allRows;
    }
    
    
}

}
