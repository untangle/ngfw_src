/**
 * $Id$
 */
package com.untangle.node.util;

import com.untangle.uvm.node.IPMaskedAddress;

public interface IPSet {

    public void add (IPMaskedAddress mask, Object result);

    public Object getMostSpecific  (IPMaskedAddress mask);
    public Object getLeastSpecific (IPMaskedAddress mask);

}
