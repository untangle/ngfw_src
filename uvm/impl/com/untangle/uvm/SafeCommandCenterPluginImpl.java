/**
 * Reviewed normal CMD plugin contract.
 */
package com.untangle.uvm;

import java.util.Objects;

/**
 * Preserves the current plugin instance and exact CMD overloads.
 */
public final class SafeCommandCenterPluginImpl implements SafeCommandCenterPlugin
{
    private final Plugin delegate;

    /**
     * Create the reviewed plugin facade.
     * @param delegate The current underlying plugin
     */
    SafeCommandCenterPluginImpl(Plugin delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * GetBackupData preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public String GetBackupData() throws Exception
    {
        return (String) SafePluginInvocation.invoke(delegate, "GetBackupData", new Class<?>[] {}, new Object[] {});
    }

    /**
     * GenerateBackup preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public String GenerateBackup() throws Exception
    {
        return (String) SafePluginInvocation.invoke(delegate, "GenerateBackup", new Class<?>[] {}, new Object[] {});
    }

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
    @Override
    public boolean RestoreFromBackup(String argument0, String argument1, String argument2, boolean argument3, boolean argument4) throws Exception
    {
        return (Boolean) SafePluginInvocation.invoke(delegate, "RestoreFromBackup", new Class<?>[] {String.class, String.class, String.class, boolean.class, boolean.class}, new Object[] {argument0, argument1, argument2, argument3, argument4});
    }

    /**
     * ExecuteCommand preserving the existing CMD plugin contract.
     * @param argument0 The original plugin argument
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public String ExecuteCommand(String argument0) throws Exception
    {
        return (String) SafePluginInvocation.invoke(delegate, "ExecuteCommand", new Class<?>[] {String.class}, new Object[] {argument0});
    }

    /**
     * GetLastError preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    @Override
    public String GetLastError() throws Exception
    {
        return (String) SafePluginInvocation.invoke(delegate, "GetLastError", new Class<?>[] {}, new Object[] {});
    }
}
