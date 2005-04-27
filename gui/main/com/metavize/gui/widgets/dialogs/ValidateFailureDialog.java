/*
 * ValidateFailureDialog.java
 *
 * Created on April 27, 2005, 2:03 PM
 */

package com.metavize.gui.widgets.dialogs;


/**
 *
 * @author inieves
 */
public class ValidateFailureDialog extends MOneButtonJDialog {
    
    public ValidateFailureDialog(String applianceName, String failureMessage) {
        this.setTitle("Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to save settings because of an invalid setting at:<br>" + failureMessage + "<br><br>Please correct any incorrect settings and then save again.</center></html>");
        this.setVisible(true);
        this.dispose();
    }
    
}
