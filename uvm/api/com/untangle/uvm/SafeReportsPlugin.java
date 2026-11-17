/**
 * Reviewed normal CMD plugin contract.
 */
package com.untangle.uvm;

import java.util.ArrayList;

/**
 * Retained CMD operations without unrestricted plugin lifecycle helpers.
 */
public interface SafeReportsPlugin extends SafePlugin
{
    /**
     * ExecuteReportQuery preserving the existing CMD plugin contract.
     * @param argument0 The original plugin argument
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    ArrayList ExecuteReportQuery(String argument0) throws Exception;

    /**
     * GetLastError preserving the existing CMD plugin contract.
     * @return The original plugin result
     * @throws Exception If the original plugin operation fails
     */
    String GetLastError() throws Exception;
}
