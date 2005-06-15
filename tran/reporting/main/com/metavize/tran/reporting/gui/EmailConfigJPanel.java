/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.reporting.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.MailSettings;
import com.metavize.tran.reporting.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;


import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.event.*;

public class EmailConfigJPanel extends MEditTableJPanel {
    
    
    public EmailConfigJPanel() {
        super(true, true);
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("Reports via Email");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(true);

        // create actual table model
        EmailTableModel emailTableModel = new EmailTableModel();
        this.setTableModel( emailTableModel );
    }

}


class EmailTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 150; /* email address */
    //    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW), 120); /* description */

    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,   true, false, false, String.class,  "person@domain.com", "Email address");
	//addTableColumn( tableColumnModel,  3, C3_MW, true,   true, false,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        // ArrayList elemList = new ArrayList();
	StringBuilder elemList = new StringBuilder();
	int rowIndex = 1;
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

	    String newElem;
	    newElem = ((String) rowVector.elementAt(2)).trim();

	    if(rowIndex != 1)
		elemList.append(", ");	    
	    elemList.append(newElem);

	    rowIndex++;
        }

	// SAVE SETTINGS /////
	if( !validateOnly ){
	    MailSettings mailSettings = Util.getAdminManager().getMailSettings();
	    mailSettings.setReportEmail(elemList.toString()); 
	    Util.getAdminManager().setMailSettings( (MailSettings) mailSettings );
	}
    }
    
    public Vector generateRows(Object settings){
	MailSettings mailSettings = Util.getAdminManager().getMailSettings();
	String recipients = mailSettings.getReportEmail();
	StringTokenizer recipientsTokenizer = new StringTokenizer(recipients, ",");
        Vector allRows = new Vector();
        int count = 1;
	while( recipientsTokenizer.hasMoreTokens() ){
	    Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(count));
            row.add(recipientsTokenizer.nextToken().trim());

            allRows.add(row);
	    count++;
	}
        return allRows;
    }


} 
