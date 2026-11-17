/**
 * Reviewed normal CMD plugin contract.
 */
package com.untangle.uvm;

/**
 * Retained CMD operations without unrestricted plugin lifecycle helpers.
 */
public interface SafeCommandCenterPlugin extends SafePlugin
{
    /**
     * GetBackupData preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    String GetBackupData() throws Exception;

    /**
     * GenerateBackup preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    String GenerateBackup() throws Exception;

    /**
     * RestoreFromBackup preserving the existing CMD plugin contract.
     * @param argument0 The original plugin argument
     * @param argument1 The original plugin argument
     * @param argument2 The original plugin argument
     * @param argument3 The original plugin argument
     * @param argument4 The original plugin argument
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    boolean RestoreFromBackup(String argument0, String argument1, String argument2, boolean argument3, boolean argument4) throws Exception;

    /**
     * ExecuteCommand preserving the existing CMD plugin contract.
     * @param argument0 The original plugin argument
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    String ExecuteCommand(String argument0) throws Exception;

    /**
     * GetLastError preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    String GetLastError() throws Exception;
}
