/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;

/**
 * Reviewed callable operations for SafeLanguageManager.
 */
public interface SafeLanguageManager
{
    /**
     * getLanguageSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    LanguageSettings getLanguageSettings();

    /**
     * getLanguagesList through the reviewed RPC contract.
     * @return The delegated result
     */
    List<LocaleInfo> getLanguagesList();

    /**
     * getTranslations through the reviewed RPC contract.
     * @param module The module argument
     * @return The delegated result
     */
    Map<String, String> getTranslations(String module);

    /**
     * setLanguageSettings through the reviewed RPC contract.
     * @param langSettings The langSettings argument
     */
    void setLanguageSettings(LanguageSettings langSettings);

    /**
     * synchronizeLanguage through the reviewed RPC contract.
     */
    void synchronizeLanguage();
}
