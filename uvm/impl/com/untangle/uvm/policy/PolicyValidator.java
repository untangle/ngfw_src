/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/impl/com/untangle/node/firewall/FirewallValidator.java $
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
package com.untangle.uvm.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.ValidationResult;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;

public class PolicyValidator implements Validator {

    public static final String VALIDATION_CODE_CLIENT_ADDRESS = "CLIENT_ADDR";
    public static final String VALIDATION_CODE_SERVER_ADDRESS = "SERVER_ADDR";
    public static final String VALIDATION_CODE_SERVER_PORT = "SERVER_PORT";
    
    public static final String ERR_CODE_INVALID_CLIENT_ADDRESS = "INVALID_CLIENT_ADDR";
    public static final String ERR_CODE_INVALID_SERVER_ADDRESS = "INVALID_SERVER_ADDR";
    public static final String ERR_CODE_INVALID_SERVER_PORT = "INVALID_SERVER_PORT";
    
    @SuppressWarnings("unchecked") 
	public ValidationResult validate(Object data) 
	{
		try {
			if (data != null) {
                for(Map.Entry<String,  List<String>> entry : ((HashMap<String,  List<String>>) data).entrySet()) {
                    String validationCode = entry.getKey();
                    List<String> entries = entry.getValue();
                    
                    String invalidValue = null;
                    if (VALIDATION_CODE_CLIENT_ADDRESS.equals(validationCode)){
                        if ((invalidValue = getInvalidAddress(entries)) != null){
                            return new ValidationResult(false, ERR_CODE_INVALID_CLIENT_ADDRESS, invalidValue);
                        }
                    } else if (VALIDATION_CODE_SERVER_ADDRESS.equals(validationCode)){
                        if ((invalidValue = getInvalidAddress(entries)) != null){
                            return new ValidationResult(false, ERR_CODE_INVALID_SERVER_ADDRESS, invalidValue);
                        }
                    } else if (VALIDATION_CODE_SERVER_PORT.equals(validationCode)){
                        if ((invalidValue = getInvalidPort(entries)) != null){
                            return new ValidationResult(false, ERR_CODE_INVALID_SERVER_PORT, invalidValue);
                        }
                    } 
                }
			}
		} catch (Exception e) {
			return new ValidationResult(false, e.getMessage(), e);
		}

		return new ValidationResult(true);
	}
	
	// get first occurrence of an invalid address; null if all are valid
	private String getInvalidAddress(List<String> values) {
        for (String value : values) {
            try {
                new IPMatcher(value);
            } catch (Exception e) {
                return value;
            }
        }
        return null;
	}

    // get first occurrence of an invalid port; null if all are valid
    private String getInvalidPort(List<String> values) {
        for (String value : values) {
            try {
                new PortMatcher(value);
            } catch (java.lang.IllegalArgumentException e) {
                return value;
            }
        }
        return null;
    }
}
