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

package com.untangle.uvm.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of TemplateValues which can chain together several
 * TemplateValues.  This is useful if a given Template references data
 * from many sources.
 */
public class TemplateValuesChain
    implements TemplateValues {

    private List<TemplateValues> m_valueList;

    public TemplateValuesChain() {
        this(new TemplateValues[] {});
    }

    public TemplateValuesChain(TemplateValues v1) {
        this(new TemplateValues[] {v1});
    }

    public TemplateValuesChain(TemplateValues... tvs) {
        m_valueList = Arrays.asList(tvs);
    }

    /**
     * Append a TemplateValues to the chain
     *
     * @param tv the new TemplateValues
     *
     * @return this (useful for method chaining).
     */
    public TemplateValuesChain append(TemplateValues tv) {
        //For thread safety, copy the list and
        //re-assign the reference.
        List<TemplateValues> newList = new ArrayList<TemplateValues>(m_valueList);
        newList.add(tv);
        m_valueList = newList;
        return this;
    }

    public String getTemplateValue(String key) {
        //Assign method-local reference, for thread
        //safety
        List<TemplateValues> list = m_valueList;
        for(TemplateValues values : list) {
            if(values == null) {
                continue;
            }
            String ret = values.getTemplateValue(key);
            if(ret != null) {
                return ret;
            }
        }
        return null;
    }

}
