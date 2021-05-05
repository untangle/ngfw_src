/**
 * $Id: BackupManager.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm;

import java.io.File;

public interface BackupManager
{
    public File createBackup();
    public String restoreBackup(File restoreFile, String maintainRegex);
}
