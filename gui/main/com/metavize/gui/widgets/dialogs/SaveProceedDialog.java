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
final public class SaveProceedDialog extends MTwoButtonJDialog {
    
    public SaveProceedDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        this.cancelJButton.setText("<html><b>Cancel</b> save</html>");
        this.proceedJButton.setText("<html><b>Continue</b> saving</html>");
        messageJLabel.setText("<html><center>" + applianceName + " is about to save its settings.  These settings are critical to proper network operation and you should be sure these are the settings you want.<br><br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
