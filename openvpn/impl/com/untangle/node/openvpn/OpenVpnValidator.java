/**
 * $Id: OpenVpnValidator.java,v 1.00 2012/05/24 11:42:59 dmorris Exp $
 */
package com.untangle.node.openvpn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.ValidationResult;

public class OpenVpnValidator extends AddressValidator
{

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
                    }
                }
            }
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage(), e);
        }

        return new ValidationResult(true);
    }
}
