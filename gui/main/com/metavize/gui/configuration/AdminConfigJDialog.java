/*
 * AdminConfigJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


/**
 *
 * @author  inieves
 */
public class AdminConfigJDialog extends MConfigJDialog {
    
    private static final String NAME_ADMIN_ACCOUNTS = "Administrator Accounts";

    public AdminConfigJDialog( ) {
    }

    protected void generateGui(){
        // create graphical context
        MEditTableJPanel mEditTableJPanel = new MEditTableJPanel(true, false);
        mEditTableJPanel.setInsets(new Insets(4, 4, 2, 2));
        mEditTableJPanel.setTableTitle(NAME_ADMIN_ACCOUNTS);
        mEditTableJPanel.setDetailsTitle("rule notes");
        mEditTableJPanel.setAddRemoveEnabled(true);

        // create actual table model
        AdminConfigTableModel configTableModel = new AdminConfigTableModel();
	super.savableMap.put(NAME_ADMIN_ACCOUNTS, configTableModel);
	super.refreshableMap.put(NAME_ADMIN_ACCOUNTS, configTableModel);

        mEditTableJPanel.setTableModel( configTableModel );
        this.contentJPanel.add(mEditTableJPanel);
        this.contentJTabbedPane.setTitleAt(0, NAME_ADMIN_ACCOUNTS);
        this.setTitle(NAME_ADMIN_ACCOUNTS);
    }
    
    public void sendSettings(Object settings) throws Exception {
	Util.getAdminManager().setAdminSettings( (AdminSettings) settings );
    }
    public void refreshSettings(){
	super.settings = Util.getAdminManager().getAdminSettings();
    }

}



    
class AdminConfigTableModel extends MSortedTableModel {

    private static final int MIN_PASSWD_LENGTH = 3;
    private Hashtable loginHashtable = new Hashtable();

    private static final StringConstants sc = StringConstants.getInstance();
    

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, false, false, String.class,     null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, false, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  85, true,  true,  false, false, String.class,     sc.EMPTY_NAME,  sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3,  85, true,  true,  false, false, String.class,     "[no login]", "login");
        addTableColumn( tableColumnModel,  4,  85, true,  false, true,  false, MPasswordField.class,     PasswordUtil.encrypt("12345678"),     "<html><center>original<br>password</center></html>");
        addTableColumn( tableColumnModel,  5, 260, true,  true,  false, false, MPasswordField.class, "",     "<html><center>set new password<br>(for new accounts or to change password)</center></html>");
        addTableColumn( tableColumnModel,  6, 260, true,  true,  false, false, MPasswordField.class, "",     "<html><center>confirm new password<br>(for new accounts or to change password)</center></html>");
        addTableColumn( tableColumnModel,  7, 250, true,  true,  false, false, MPasswordField.class, "12345678",     "<html><center><b>original user's password</b><br>(required to change or remove account)</center></html>");
        addTableColumn( tableColumnModel,  8,  85, true,  true,  false, false, String.class,     "[no email]", "<html><center>email<br>address</center></html>");
        addTableColumn( tableColumnModel,  9,  85, true,  true,  false, false, Boolean.class,    "false",    "<html><center>send critical<br>alerts</center></html>");
        return tableColumnModel;
    }


    private void prevalidate() throws Exception {
                        
        loginHashtable.clear();
	boolean oneValidAccount = false;
        int rowIndex = 1;
        // go through all the rows and perform some tests
	Util.printMessage("STATUS: STARTING VERIFICATION of " + this.getOriginalDataVector().size() + " user(s)");	
        for( Vector tempUser : (Vector<Vector>) this.getOriginalDataVector() ){
            
            byte[] origPasswd = ((MPasswordField)tempUser.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)tempUser.elementAt(5)).getPassword();
            char[] newConfPasswd  = ((MPasswordField)tempUser.elementAt(6)).getPassword();
            char[] proceedPasswd  = ((MPasswordField)tempUser.elementAt(7)).getPassword();
            String loginName = (String) tempUser.elementAt(3);
            
            // verify that the login name is not duplicated
            if( loginHashtable.containsKey(loginName) ){
		throw new Exception("The login name: \"" + loginName + "\" in row: " + rowIndex + " already exists.");
            }
	    else{
		loginHashtable.put(loginName, loginName);
	    }
            
            // verify that all of the new and new confirm passwords and match up (and they are of the proper size)
            if( !java.util.Arrays.equals(newPasswd, newConfPasswd) ){
		throw new Exception("The \"new\" and \"new confirm\" passwords are not the same for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }
            if( (newPasswd.length > 0) && (newPasswd.length < MIN_PASSWD_LENGTH) ){
		throw new Exception("The \"new\" and//or \"new confirm\" passwords for: \"" + loginName + "\" in row: " + rowIndex + " are shorter than the minimum " + MIN_PASSWD_LENGTH + " characters.");
            }            
            // verify that all of the original and proceed passwords are the same for any non saved row
            if(    !((String)tempUser.elementAt(0)).equals(super.ROW_SAVED)
		   &&  !PasswordUtil.check( String.valueOf(proceedPasswd), origPasswd )){
		throw new Exception("The \"original\" password is not correct for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }
            // verify that if this is a new row, a new password has been chosen
            if( ((String)tempUser.elementAt(0)).equals(super.ROW_ADD) && (newPasswd.length == 0) ){
		throw new Exception("The \"new\" password has not been set for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }

            // record if the row was not removed, that way we know we had at least one valid account
            if( !((String)tempUser.elementAt(0)).equals(super.ROW_REMOVE) ){
                oneValidAccount = true;
            }
	    rowIndex++;
        }

        // verify that there is at least one valid entry after all operations
        if(!oneValidAccount){
	    throw new Exception("There must always be at least one valid account.");
        }

    }
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {        

	prevalidate();
	Set allRows = new LinkedHashSet();

	Util.printMessage("STATUS: STARTING SAVING of accounts");
	int rowIndex = 1;
        for( Vector rowVector : (Vector<Vector>) this.getOriginalDataVector() ){
	    User newElem;
            
            byte[] origPasswd = ((MPasswordField)rowVector.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)rowVector.elementAt(5)).getPassword();
            
	    Util.printMessage("> UPDATE: row status " + (String) rowVector.elementAt(0) );

            if( ((String)rowVector.elementAt(0)).equals(super.ROW_REMOVE) ){
                Util.printMessage("> UPDATE: removing row " + rowIndex);
                continue; // those removed and of a strange state are never even added            
            }
            else{
                try{
                if( newPasswd.length > 0 ){
                    Util.printMessage("> UPDATE: saving row (changed password) " + rowIndex);
		    newElem = new User( (String)rowVector.elementAt(3), new String(newPasswd), (String)rowVector.elementAt(2) );
                }
                else{
                    Util.printMessage("> UPDATE: saving row (unchanged password) " + rowIndex);
		    newElem = new User( (String)rowVector.elementAt(3), origPasswd, (String)rowVector.elementAt(2) );
                }   
                }catch(Exception e) {
		    Util.handleExceptionNoRestart("--> ERROR:  problem changing row", e);
		    continue;
                }
            }

            newElem.setEmail( (String) rowVector.elementAt(8) );
            newElem.setSendAlerts( ((Boolean) rowVector.elementAt(9)).booleanValue() );            
            allRows.add(newElem);
	    rowIndex++;
        }
        
	// SAVE SETTINGS /////////////
	if( !validateOnly ){
	    AdminSettings adminSettings = (AdminSettings) settings;
	    adminSettings.setUsers(allRows);
	}

    }

    public Vector generateRows(Object settings) {
	AdminSettings adminSettings = (AdminSettings) settings;
        Vector configVector = new Vector();

        int count = 1;
	for( User newElem : (Set<User>) adminSettings.getUsers() ){
            Vector rowVector = new Vector();
            rowVector.add(super.ROW_SAVED);
            rowVector.add( new Integer(count) );

            rowVector.add( newElem.getName() );
            rowVector.add( newElem.getLogin() );

            /* XXX What is this */
            MPasswordField tempMPasswordField = new MPasswordField( newElem.getPassword() );
            rowVector.add( tempMPasswordField );
            rowVector.add( new MPasswordField() );
            rowVector.add( new MPasswordField() );
	    MPasswordField originalMPasswordField = new MPasswordField();
	    originalMPasswordField.setGeneratesChangeEvent(false);
            rowVector.add( originalMPasswordField );
            rowVector.add( newElem.getEmail() );
            rowVector.add( new Boolean(newElem.getSendAlerts()) );
            
            configVector.add(rowVector);
            count++;
        }
        
        return configVector;
    }





}
