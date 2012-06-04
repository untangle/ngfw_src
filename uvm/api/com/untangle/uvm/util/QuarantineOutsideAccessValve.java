/**
 * $Id$
 */
package com.untangle.uvm.util;

public class QuarantineOutsideAccessValve extends OutsideValve
{
    public QuarantineOutsideAccessValve() { }

    protected boolean isOutsideAccessAllowed()
    {
        return getSystemSettings().getOutsideHttpsQuarantineEnabled();
    }
}
