/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.FileReader;
import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * The Manager for system-based authentication
 */
public class AuthenticationManagerImpl implements AuthenticationManager
{
    protected List<OAuthDomain> OAuthConfigList = new ArrayList<>();

    private final String OAUTH_DOMAIN_CONFIG = System.getProperty("uvm.home") + "/conf/ut-oauth.conf";

    private final Logger logger = Logger.getLogger(this.getClass());

    private UrisSaveHookCallback urisSaveHookCallback = new UrisSaveHookCallback();

    /**
     * Constructor
     */
    protected AuthenticationManagerImpl()
    {
        loadOAuthList();
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.URIS_SETTINGS_CHANGE, urisSaveHookCallback);
    }

    /**
     * @return List of OAuthDomain
     */
    public List<OAuthDomain> getOAuthConfig()
    {
        return OAuthConfigList;
    }

    /**
     * Loads the list of OAuth domains that must be allowed for client auth from
     * a config file in the format PROVIDER|MATCH|NAME
     * 
     * @return The list of oauthDomain's
     */
    public void loadOAuthList()
    {
        synchronized(this.OAuthConfigList){
            this.OAuthConfigList = new ArrayList<OAuthDomain>();
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(OAUTH_DOMAIN_CONFIG))) {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")){
                        // ignore lines that start with a comment character
                        continue;
                    }
                    if (line.length() < 5){
                        // ignore lines too short to be valid
                        continue;
                    }
                    String[] values = line.split(java.util.regex.Pattern.quote("|"), 3);
                    if (values.length != 3){
                        // ignore lines that don't have exactly three fields
                        continue;
                    }
                    OAuthDomain rec = new OAuthDomain();
                    rec.provider = values[0].toLowerCase();
                    rec.match = values[1].toLowerCase();
                    rec.name = values[2].toLowerCase();
                    UriTranslation ut = UvmContextFactory.context().uriManager().getUriTranslationByHost(rec.name);
                    if(ut != null){
                        logger.warn("getHost:" + ut.getHost());
                        rec.name = ut.getHost();
                    }
                    OAuthConfigList.add(rec);
                    logger.debug("Loaded OAuth config: " + rec.provider + " | " + rec.match + " | " + rec.name);
                }
            } catch (Exception exn) {
                logger.warn("Exception loading OAuth domains:" + OAUTH_DOMAIN_CONFIG, exn);
            }
        }
    }

    /**
     * This hook is called when uris settings are changed to force reload of OAuthConfigList
     */
    private class UrisSaveHookCallback implements HookCallback
    {
        /**
         * Constructor
         */
        UrisSaveHookCallback(){}

        /**
         * Get the name of our callback hook
         * 
         * @return The name
         */
        public String getName()
        {
            return "authentication-manager-uris-settings-change-hook";
        }

        /**
         * Callback function
         * 
         * @param args
         *        Callback arguments
         */
        public void callback(Object... args)
        {
            loadOAuthList();
        }
    }

}