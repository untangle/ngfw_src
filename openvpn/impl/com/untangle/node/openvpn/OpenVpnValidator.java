/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/impl/com/untangle/node/firewall/OpenVpnValidator.java $
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
package com.untangle.node.openvpn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.AddressValidator;
import com.untangle.uvm.node.ValidationResult;

public class OpenVpnValidator extends AddressValidator {

    public static final String VALIDATION_CODE_GROUP_LIST = "GROUP_LIST";
    public static final String VALIDATION_CODE_SITE_LIST = "SITE_LIST";
    public static final String VALIDATION_CODE_EXPORT_LIST = "EXPORT_LIST";
    
    public static final String ERR_CODE_GROUP_LIST_OVERLAP = "ERR_GROUP_LIST_OVERLAP";
    public static final String ERR_CODE_SITE_LIST_OVERLAP = "ERR_SITE_LIST_OVERLAP";
    public static final String ERR_CODE_EXPORT_LIST_OVERLAP = "ERR_EXPORT_LIST_OVERLAP";
    
    @SuppressWarnings("unchecked")
	public ValidationResult validate(Object data) {

        try {
            if (data != null) {
                for(Map.Entry<String,  List<Object>> entry : ((HashMap<String,  List<Object>>) data).entrySet()) {
                    String validationCode = entry.getKey();
                    List entries = entry.getValue();
                    
                    if (VALIDATION_CODE_GROUP_LIST.equals(validationCode)){
                        GroupList groupList = new GroupList( entries );
                        ValidationResult result = super.validate(groupList.buildAddressRange());
                        if (!result.isValid()) {
                            result.setErrorCode(ERR_CODE_GROUP_LIST_OVERLAP);
                            return result;
                        }
                    } else if (VALIDATION_CODE_SITE_LIST.equals(validationCode)){
                        SiteList siteList = new SiteList( entries );
                        ValidationResult result = super.validate(siteList.buildAddressRange());
                        if (!result.isValid()) {
                            result.setErrorCode(ERR_CODE_SITE_LIST_OVERLAP);
                            return result;
                        }
                    } else if (VALIDATION_CODE_EXPORT_LIST.equals(validationCode)){
                        ExportList exportList = new ExportList( entries );
                        ValidationResult result = super.validate(exportList.buildAddressRange());
                        if (!result.isValid()) {
                            result.setErrorCode(ERR_CODE_EXPORT_LIST_OVERLAP);
                            return result;
                        }
                    } 
                }
            }
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage(), e);
        }

        return new ValidationResult(true);
    }
}
