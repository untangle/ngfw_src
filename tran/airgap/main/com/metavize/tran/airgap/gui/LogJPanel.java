/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.airgap.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.airgap.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){
        List<ShieldRejectionLogEntry> logList = 
            ((AirgapTransform)super.logTransform).getLogs(depthJSlider.getValue());

        Vector allEvents = new Vector();
        Vector event = null;

	for ( ShieldRejectionLogEntry log : logList ) {
            event = new Vector(6);
            event.add( Util.getLogDateFormat().format( log.getCreateDate() ));
	    event.add( log.getClient() );
            event.add( log.getClientIntf() );
	    event.add( log.getReputationString() );
            event.add( log.getLimited() );
            event.add( log.getDropped() );
            event.add( log.getRejected() );

            allEvents.insertElementAt(event,0);
        }
        return allEvents;
    }
    

    
    class LogTableModel extends MSortedTableModel{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ               def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, String.class, null,  "timestamp" );
	    addTableColumn( tableColumnModel,  1,  100, true,  false, false, false, String.class, null,  sc.html("source") );
	    addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null,  sc.html("source<br>interface") );
	    addTableColumn( tableColumnModel,  3,  75,  true,  false, false, false, String.class, null,  "reputation" );
	    addTableColumn( tableColumnModel,  4,  55,  true,  false, false, false, Integer.class, null, "limited" );
	    addTableColumn( tableColumnModel,  5,  75,  true,  false, false, false, Integer.class, null, "droppped" );
	    addTableColumn( tableColumnModel,  6,  55,  true,  false, false, false, Integer.class, null, "reject" );
	    return tableColumnModel;
	}
	
	public void generateSettings(Object settings, boolean validateOnly) throws Exception {}
	
	public Vector generateRows(Object settings) {
	    return LogJPanel.this.generateRows(null);
	}
    }       

}
