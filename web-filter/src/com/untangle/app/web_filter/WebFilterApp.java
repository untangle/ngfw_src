/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.catalina.Valve;
import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterSettings;
import com.untangle.app.web_filter.WebFilterEvent;

public class WebFilterApp extends WebFilterBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final WebFilterDecisionEngine engine = new WebFilterDecisionEngine(this);

    private final WebFilterHttpsSniHandler sniHandler = new WebFilterHttpsSniHandler(this);
    private final WebFilterQuicHandler quicHandler = new WebFilterQuicHandler(this);
    
    private final Subscription httpsSub = new Subscription(Protocol.TCP,IPMaskedAddress.anyAddr,PortRange.ANY,IPMaskedAddress.anyAddr,new PortRange(443,443));
    private final Subscription quicSub = new Subscription(Protocol.UDP,IPMaskedAddress.anyAddr,PortRange.ANY,IPMaskedAddress.anyAddr,new PortRange(443,443));
    
    private final PipelineConnector httpConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-http", this, null, new WebFilterHandler(this), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 2, true );
    private final PipelineConnector httpsSniConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-https-sni", this, httpsSub, sniHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true );
    private final PipelineConnector quicConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-quic", this, quicSub, quicHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true );
    private final PipelineConnector[] connectors = new PipelineConnector[] { httpConnector, httpsSniConnector, quicConnector };

    public WebFilterApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super( appSettings, appProperties );
    }

    public void clearCache( boolean expireAll )
    {
        this.engine.clearCache( expireAll );
    }

    public List<String> lookupSite( String url )
    {
        return this.engine.lookupSite( url );
    }

    public int recategorizeSite( String url, int category )
    {
        return this.engine.recategorizeSite( url, category );
    }

    // this is used for the UI alert test
    public String encodeDnsQuery( String domain, String uri, String command )
    {
        return this.engine.encodeDnsQuery( domain, uri, command );
    }

    // this is used for the UI alert test
    public String encodeDnsQuery( String domain, String uri )
    {
        return this.engine.encodeDnsQuery( domain, uri );
    }

    @Override
    public DecisionEngine getDecisionEngine()
    {
        return engine;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        if ( ! isLicenseValid() ) {
            throw new RuntimeException( "invalid license" );
        }

        engine.getDiaKey();
        
        super.preStart( isPermanentTransition );
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected WebFilterReplacementGenerator buildReplacementGenerator()
    {
        return new WebFilterReplacementGenerator(getAppSettings());
    }

    @Override
    public String getAppTitle()
    {
        return "Web Filter";
    }

    @Override
    public String getName()
    {
        return "web_filter";
    }

    @Override
    public String getAppName()
    {
        return "web-filter";
    }

    @Override
    public boolean isPremium()
    {
        return true;
    }
    
    @Override
    public void initializeSettings(WebFilterSettings settings)
    {
        LinkedList<GenericRule> categories = new LinkedList<GenericRule>();

        addCategories(categories);
        settings.setCategories(categories);
    }

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_FILTER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_FILTER_OLDNAME))
            return true;
        return false;
    }
}
