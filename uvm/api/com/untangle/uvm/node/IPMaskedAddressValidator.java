/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/impl/com/untangle/node/spyware/SpywareValidator.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.uvm.node;

import java.util.List;

public class IPMaskedAddressValidator implements Validator
{
    public static final String ERR_CODE_INVALID_IPMADDR = "INVALID_IPMADDR";

    @SuppressWarnings("unchecked") //cast
    public ValidationResult validate(Object data)
    {
        try {
            if (data != null) {
                for (String ipMaddrString : (List<String>) data) {
                    try {
                        IPMaskedAddress.parse(ipMaddrString);
                    } catch (Exception e) {
                        return new ValidationResult(false,
                                                    ERR_CODE_INVALID_IPMADDR, ipMaddrString);
                    }
                }
                
            }
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage(), e);
        }
        
        return new ValidationResult(true);
    }
}
