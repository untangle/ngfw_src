/**
 * $Id$
 */
package com.untangle.uvm.util;

public class AdministrationOutsideAccessValve extends OutsideValve
{
    public AdministrationOutsideAccessValve() { }

    protected boolean isOutsideAccessAllowed()
    {
        return getSystemSettings().getOutsideHttpsAdministrationEnabled();
    }
}
