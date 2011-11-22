/*
 * $Id$
 */
package com.untangle.node.util;

import com.untangle.uvm.node.IPMaskedAddress;

public class IPSetTrie implements IPSet
{
    private IPSetTrieNode _root = null;

    public IPSetTrie()
    {
        _root = new IPSetTrieNode();
    }

    public void add (IPMaskedAddress mask, Object result)
    {
        _root.add(mask.bitString(),result);
    }

    public Object getMostSpecific  (IPMaskedAddress mask)
    {
        return _root.getMostSpecific(mask.bitString(),0);
    }

    public Object getLeastSpecific (IPMaskedAddress mask)
    {
        return _root.getLeastSpecific(mask.bitString(),0);
    }

}


