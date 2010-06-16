/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.util;

import java.util.LinkedList;

public class IPSetTrieNode {

    private IPSetTrieNode _0Node = null;
    private IPSetTrieNode _1Node = null;

    private Object  result = null;
    
    public IPSetTrieNode () {}
    
    public IPSetTrieNode (LinkedList<Boolean> bitString, Object result)
    {
        this.add(bitString,result);
    }

    public void add (LinkedList<Boolean> bitString, Object result)
    {
        if (bitString.size() == 0) {
            this.result = result;
            return;
        }

        boolean b = ((Boolean)bitString.removeFirst()).booleanValue();

        if (b) {
            if (_1Node != null)
                _1Node.add(bitString,result);
            else {
                _1Node = new IPSetTrieNode(bitString, result);
            }
        }
        else {
            if (_0Node != null)
                _0Node.add(bitString,result);
            else
                _0Node = new IPSetTrieNode(bitString, result);
        }
    }

    public Object getLeastSpecific (LinkedList<Boolean> bitString, int index)
    {
        Boolean b;
        try {b = (Boolean)bitString.get(index);}
        catch (IndexOutOfBoundsException e) {return this.result;}

        /* check locally first (less specific) */
        if (this.result != null)
            return this.result;
        
        /* check down-tree second (more specific) */
        if (b.booleanValue()) {
            if (_1Node != null) 
                return _1Node.getLeastSpecific(bitString,index+1);
        }
        else {
            if (_0Node != null) 
                return _0Node.getLeastSpecific(bitString,index+1);
        }
        
        return null;
    }

    public Object getMostSpecific (LinkedList<Boolean> bitString, int index)
    {
        Boolean b;
        try {b = (Boolean)bitString.get(index);}
        catch (IndexOutOfBoundsException e) {
            /* You matched correctly to get here, but don't match the next level */
            return this.result;
        }

        /* check down-tree first (more specific) */
        Object lower_res = null;
        if (b.booleanValue()) {
            if (_1Node != null) {
                lower_res = _1Node.getMostSpecific(bitString,index+1);
            }
        }
        else {
            if (_0Node != null) {
                lower_res = _0Node.getMostSpecific(bitString,index+1);
            }

        }

        if (lower_res != null) {
            return lower_res;
        }

        /* check locally second (less specific) */
        return this.result;
    }

}

