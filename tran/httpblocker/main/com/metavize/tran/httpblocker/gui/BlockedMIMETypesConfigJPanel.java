/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.httpblocker.*;
import com.metavize.mvvm.tran.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.List;
import javax.swing.event.*;

public class BlockedMIMETypesConfigJPanel extends MEditTableJPanel {
    
    
    public BlockedMIMETypesConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked MIME Types");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        MIMETypeTableModel mimeTypeTableModel = new MIMETypeTableModel();
        super.setTableModel( mimeTypeTableModel );
    }
}



class MIMETypeTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    // private static final int C2_MW = 150; /* category */
    private static final int C2_MW = 150; /* MIME type */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
        // addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "uncategorized", "category");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "no mime type", "MIME type");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }

    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception{
        List elemList = new ArrayList();        
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){
            
            MimeTypeRule newElem = new MimeTypeRule();
            // newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setMimeType( new MimeType( (String)rowVector.elementAt(2) ));
            newElem.setLive( ((Boolean) rowVector.elementAt(3)).booleanValue() );
            newElem.setName( (String) rowVector.elementAt(4) );
                                    
            elemList.add(newElem);  
        }
        
	// SAVE SETTINGS //////////////
	if( !validateOnly ){
	    HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
	    httpBlockerSettings.setBlockedMimeTypes( elemList );
	}

    }
    
    public Vector generateRows(Object settings){
	HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        Vector allRows = new Vector();
        int counter = 1;
	for( MimeTypeRule newElem : (List<MimeTypeRule>) httpBlockerSettings.getBlockedMimeTypes() ){

            Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(counter));
            // row.add(newElem.getCategory());
            row.add(newElem.getMimeType().getType());
            row.add(Boolean.valueOf(newElem.isLive()));
            row.add(newElem.getName());

            allRows.add(row);
            counter++;
        }
        return allRows;
    }
}
