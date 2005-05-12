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
final public class ValidateFailureDialog extends MOneButtonJDialog {
    
    public ValidateFailureDialog(String applianceName, String componentName, String failureMessage) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to save settings<br>for the following reason:<br><br><b>" + failureMessage + "<br>in \"" + componentName  + "\"</b><br><br>Please correct this and then save again.</center></html>");
        this.setVisible(true);
        this.dispose();
    }
    
}
