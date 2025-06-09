/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.app.GenericRule;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterSettings;
import com.untangle.uvm.app.App;

/**
 * The Web Filter application. The bulk of the functionality lives in the
 * WebFilterBase class.
 * 
 * @author mahotz
 * 
 */
public class WebFilterApp extends WebFilterBase
{
    private final Logger logger = LogManager.getLogger(getClass());

    private final WebFilterDecisionEngine engine = new WebFilterDecisionEngine(this);

    private final WebFilterHttpsSniHandler sniHandler = new WebFilterHttpsSniHandler(this);
    private final WebFilterQuicHandler quicHandler = new WebFilterQuicHandler(this);

    private final Subscription httpsSub = new Subscription(Protocol.TCP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));
    private final Subscription quicSub = new Subscription(Protocol.UDP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));

    private final PipelineConnector httpConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-http", this, null, new WebFilterHandler(this), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 2, true);
    private final PipelineConnector httpsSniConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-https-sni", this, httpsSub, sniHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true);
    private final PipelineConnector quicConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-quic", this, quicSub, quicHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true);
    private final PipelineConnector[] connectors = new PipelineConnector[] { httpConnector, httpsSniConnector, quicConnector };

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WebFilterApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);
    }

    /**
     * Clears the decision engine cache
     */
    public void clearCache()
    {
        this.engine.clearCache();
    }

    /**
     * Called to lookup a specific site
     * 
     * @param url
     *        The site to lookup
     * 
     * @return The list of categories
     */
    public List<Integer> lookupSite(String url)
    {
        return this.engine.lookupSite(url);
    }

    /**
     * Called to submit request to recategorize site
     * 
     * @param url
     *        The site to be recategorized
     * @param category
     *        The new category
     * @return The result
     */
    public int recategorizeSite(String url, int category)
    {
        return this.engine.recategorizeSite(url, category);
    }

    /**
     * Called to get our decision engine instance
     * 
     * @return The decision engine
     */
    @Override
    public DecisionEngine getDecisionEngine()
    {
        return engine;
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        getDecisionEngine().start();

        super.preStart(isPermanentTransition);
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        getDecisionEngine().stop();

        super.postStop(isPermanentTransition);
    }

    /**
     * Called after the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        loadAllGlobalSettings(); //To load global setting 
        super.postStart(isPermanentTransition);

    }

    /**
     * Remove the globalSettings file when the last web-filter instance is destroyed.
     */
    @Override
    protected void postDestroy()
    {
        List<App> webFilterList = UvmContextFactory.context().appManager().appInstances("web-filter");
        if(webFilterList.size() == 1 && Files.exists(globalSettingFilePath)){
            try {
                Files.delete(globalSettingFilePath);
            } catch (IOException e) {
                logger.warn("Failed to delete global settings.", e);
            }        
        }
    }

    /**
     * Called to get pipeline connectors
     * 
     * @return Pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called to get the application title
     * 
     * @return The application title
     */
    @Override
    public String getAppTitle()
    {
        return "Web Filter";
    }

    /**
     * Called to get app name of the package
     * 
     * @return The app package name
     */
    @Override
    public String getName()
    {
        return "web_filter";
    }

    /**
     * Called to get the application name
     * 
     * @return The application name
     */
    @Override
    public String getAppName()
    {
        return "web-filter";
    }

    /**
     * Called to determine if the app is free or premium
     * 
     * @return
     */
    @Override
    public boolean isPremium()
    {
        return true;
    }

    /**
     * Called to initialize application settings
     * 
     * @param settings
     *        The new settings
     */
    @Override
    public void initializeSettings(WebFilterSettings settings)
    {
        LinkedList<GenericRule> categories = new LinkedList<GenericRule>();

        addCategories(categories);
        settings.setCategories(categories);
    }

}
