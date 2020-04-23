/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * the System Manager API
 */
public interface SyncSettings
{
    /**
     * Run sync settings on the specified filenames
     */
    Boolean run (String... filenames);

}
