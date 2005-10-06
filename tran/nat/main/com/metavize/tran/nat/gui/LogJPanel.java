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

package com.metavize.tran.nat.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.nat.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel
{
    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel)
    {
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
    }

    protected void refreshSettings(){
	settings = ((Nat)super.logTransform).getLogs( depthJSlider.getValue());
    }

    class LogTableModel extends MSortedTableModel {
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ               def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
	    addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  55,  true,  false, false, false, String.class, null, "protocol" );
	    addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, String.class, null, "source" );
            addTableColumn( tableColumnModel,  4,  165, true,  false, false, false, String.class, null, "original destination" );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, String.class, null, "redirected destination" );
	    addTableColumn( tableColumnModel,  6,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  7,  100, true,  false, false, false, String.class, null, sc.html("client interface") );
	    addTableColumn( tableColumnModel,  8,  105, true,  false, false, false, String.class, null, sc.html("redirected<br>server interface") );
	    return tableColumnModel;
	}
	
	public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly ) throws Exception {}

	public Vector<Vector> generateRows(Object settings){
	    List<NatRedirectLogEntry> logList = (List<NatRedirectLogEntry>) settings;
	    Vector<Vector> allEvents = new Vector<Vector>(logList.size());
	    Vector event;
	    
	    for( NatRedirectLogEntry log : logList ){
		event = new Vector(8);
		event.add( log.getCreateDate() );
		event.add( log.getAction() );
		event.add( log.getProtocol() );
		event.add( log.getClient() );
		event.add( log.getOriginalServer() );
		event.add( log.getRedirectServer() );
		event.add( log.getReason() );
		event.add( log.getClientIntf());
                event.add( log.getServerIntf());
		allEvents.add( event );
	    }
	    
	    return allEvents;
	}
	
    }       
}
