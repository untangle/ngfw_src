/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Language informations: code & name.
 *
 */
@SuppressWarnings("serial")
@ValidSerializable
public class LocaleInfo implements Serializable {
    private String languageCode;
    private String languageName;
    private String countryCode;
    private String countryName;
    private String statistics = "";
    private boolean enabled = true;

    /**
     * Initialize empty instance of LocaleInfo.
     * @return Instance of LocaleInfo.
     */
    public LocaleInfo() { }
    
    /**
     * Initialize instance of LocaleInfo.
     * @param languageCode String of short language code.
     * @param languageName String of longer language name.
     * @return Instance of LocaleInfo.
     */
    public LocaleInfo(String languageCode, String languageName) {
        super();
        this.languageCode = languageCode;
        this.languageName = languageName;
    }

    /**
     * Initialize instance of LocaleInfo.
     * @param languageCode String of short language code.
     * @param languageName String of longer language name.
     * @param countryCode String of short country code.
     * @param countryName String of longer country name.
     * @param enabled True if enabled, false if not
     * @return Instance of LocaleInfo.
     */
    public LocaleInfo(String languageCode, String languageName,
            String countryCode, String countryName, boolean enabled) {
        super();
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.enabled = enabled;
    }

    /**
     * Initialize instance of LocaleInfo.
     * @param languageCode String of short language code.
     * @param languageName String of longer language name.
     * @param countryCode String of short country code.
     * @param countryName String of longer country name.
     * @param statistics String of statistics.
     * @param enabled True if enabled, false if not
     * @return Instance of LocaleInfo.
     */
    public LocaleInfo(String languageCode, String languageName,
            String countryCode, String countryName, String statistics, boolean enabled) {
        super();
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.statistics = statistics;
        this.enabled = enabled;
    }

    /**
     * Return language code.
     * @return String of language code.
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Specify language code.
     * @param languageCode String of language code.
     */
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * Return language name.
     * @return String of language name.
     */
    public String getLanguageName() {
        return languageName;
    }

    /**
     * Specify language name.
     * @param languageName String of language name.
     */
    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    /**
     * Return country code.
     * @return String of country code.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Specify country code.
     * @param countryCode String of country code.
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Return country name.
     * @return String of country name.
     */
    public String getCountryName() {
        return countryName;
    }

    /**
     * Specify country name.
     * @param countryName String of country name.
     */
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    /**
     * Return locale name.
     * @return String in format of "countryName name"
     */
    public String getName() {
        String name = languageName;
        if (countryName != null){
            name = countryName + " " + name;
        }
        return name;
    }
    
    /**
     * Return locale code.
     * @return String in format of "code_countryCode"
     */
    public String getCode() {
        String code = languageCode;
        if (countryCode != null){
            code = code + "_" + countryCode;
        }
        return code;
    }

    /**
     * Return statistics
     * @return String of statistics.
     */
    public String getStatistics(){
        return statistics;
    }

    /**
     * Return enabled
     * @return boolean of enabled
     */
    public boolean getEnabled(){
        return enabled;
    }
    /**
     * Set enabled
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

}
