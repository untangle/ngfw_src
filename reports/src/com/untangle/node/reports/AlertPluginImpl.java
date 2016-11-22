package com.untangle.uvm;

import java.net.URI;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.http.auth.AuthScope;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import com.untangle.uvm.Plugin;
import com.untangle.uvm.node.License;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.HookManager;
import com.untangle.node.reports.AlertEvent;
import com.untangle.uvm.logging.LogEvent;

public class AlertPluginImpl implements Plugin
{
    private static final Logger logger = Logger.getLogger( AlertPluginImpl.class );

    private final AlertEventHookCallback alertCallback = new AlertEventHookCallback();
    private final SettingsChangeHookCallback settingsCallback = new SettingsChangeHookCallback();
    private final LicenseChangeHookCallback licenseCallback = new LicenseChangeHookCallback();

    private String uid = null;
    
    private AlertPluginImpl() {}
    
    public static Plugin instance()
    {
        return new AlertPluginImpl();
    }
    
    public final void run()
    {
        this.uid = UvmContextFactory.context().getServerUID();

        registerCallback();

        UvmContextFactory.context().hookManager().registerCallback( HookManager.UVM_SETTINGS_CHANGE, this.settingsCallback );
        UvmContextFactory.context().hookManager().registerCallback( HookManager.LICENSE_CHANGE, this.licenseCallback );
    }

    public final void stop()
    {
        unregisterCallback();

        UvmContextFactory.context().hookManager().unregisterCallback( HookManager.UVM_SETTINGS_CHANGE, this.settingsCallback );
        UvmContextFactory.context().hookManager().unregisterCallback( HookManager.LICENSE_CHANGE, this.licenseCallback );
    }

    private void registerCallback()
    {
        UvmContextFactory.context().hookManager().registerCallback( HookManager.REPORTS_EVENT_LOGGED, this.alertCallback );
    }

    private void unregisterCallback()
    {
        UvmContextFactory.context().hookManager().unregisterCallback( HookManager.REPORTS_EVENT_LOGGED, this.alertCallback );
    }

    private boolean isEnabled()
    {
        if ( !isLicenseValid() )
            return false;
        if ( !isCloudEnabled() )
            return false;
        return true;
    }

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.CLOUD_COMMAND))
            return true;
        return false;
    }

    private boolean isCloudEnabled()
    {
        try {
            return UvmContextFactory.context().systemManager().getSettings().getCloudEnabled();
        } catch (Exception e) {
            logger.warn( "Unable to check cloud settings:", e );
            return false;
        }
    }

    private void resumeProperState()
    {
        boolean callbackRegistered = UvmContextFactory.context().hookManager().isRegistered( HookManager.REPORTS_EVENT_LOGGED, this.alertCallback );

        if ( isEnabled() ) {
            if ( callbackRegistered ) {
                // should be enabled and is registered, nothing to do
                return;
            } else {
                // should be enabled but is not registered, register now
                registerCallback();
                return;
            }
        } else {
            if ( callbackRegistered ) {
                // should not be registered but is registered, unregister now
                unregisterCallback();
                return;
            } else {
                // should not be enabled and is not registered, nothing to do
                return;
            }
        }
    }

    private void sendEventToCloud( LogEvent event )
    {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpClientContext context = HttpClientContext.create();

            URIBuilder builder = new URIBuilder( "https://events.untangle.com/api/v1/123/FIXME/" );
            builder.addParameter( "uid" , this.uid );
            String url = builder.build().toString();

            HttpPost  post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            String jsonString = event.toJSONString();
            StringEntity body = new StringEntity( jsonString );
            post.setEntity( body );

            logger.info( "sending event: " + event.toJSONString() );
            CloseableHttpResponse response = httpClient.execute( post, context );

            String responseBody = EntityUtils.toString( response.getEntity(), "UTF-8" );
            responseBody = responseBody.trim();
            logger.debug( "response: " + responseBody );

            return;
        } catch (Exception e) {
            logger.warn( "send event exception: ", e );
        }
    }

    private class AlertEventHookCallback implements HookCallback
    {
        public String getName()
        {
            return "alert-plugin-alert-hook";
        }
        
        public void callback( Object o )
        {
            if ( ! (o instanceof AlertEvent) ) {
                return;
            }
            AlertEvent ae = (AlertEvent) o;

            sendEventToCloud( ae );
            logger.warn(o);
        }
    }

    private class LicenseChangeHookCallback implements HookCallback
    {
        public String getName()
        {
            return "alert-plugin-license-hook";
        }

        public void callback( Object o )
        {
            // check license
            resumeProperState();
        }
    }

    private class SettingsChangeHookCallback implements HookCallback
    {
        public String getName()
        {
            return "alert-plugin-settings-hook";
        }

        public void callback( Object o )
        {
            // check cloud setting
            resumeProperState();
        }
    }
    
}

