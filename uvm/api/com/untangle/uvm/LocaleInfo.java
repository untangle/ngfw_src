/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * Language informations: code & name.
 *
 */
@SuppressWarnings("serial")
public class LocaleInfo implements Serializable {
    private String languageCode;
    private String languageName;
    private String countryCode;
    private String countryName;

    public LocaleInfo() { }
    
    public LocaleInfo(String languageCode, String languageName) {
        super();
        this.languageCode = languageCode;
        this.languageName = languageName;
    }

    public LocaleInfo(String languageCode, String languageName,
            String countryCode, String countryName) {
        super();
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.languageCode = languageCode;
        this.languageName = languageName;
    }


    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getName() {
        String name = languageName;
        if (countryName != null){
            name = countryName + " " + name;
        }
        return name;
    }
    
    public String getCode() {
        String code = languageCode;
        if (countryCode != null){
            code = code + "_" + countryCode;
        }
        return code;
    }

}
