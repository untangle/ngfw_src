/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;

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
import org.json.JSONObject;

/**
 * The cloud manager 
 */
public class CloudManagerImpl implements CloudManager
{
    private final Logger logger = Logger.getLogger(CloudManagerImpl.class);

    /**
     * The singleton instance
     */
    private static final CloudManagerImpl INSTANCE = new CloudManagerImpl();

    private CloudManagerImpl() {}

    public synchronized static CloudManagerImpl getInstance()
    {
        return INSTANCE;
    }

    public JSONObject accountLogin( String email, String password )
        throws Exception
    {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpClientContext context = HttpClientContext.create();

            URIBuilder builder = new URIBuilder(UvmContextImpl.getInstance().getStoreUrl() + "/account/login");
            builder.addParameter( "email" , email );
            builder.addParameter( "uid" , UvmContextImpl.getInstance().getServerUID() );
            String url = builder.build().toString();

            LinkedList<NameValuePair> bodyParams = new LinkedList<NameValuePair>();
            bodyParams.add(new BasicNameValuePair("password",password));
            UrlEncodedFormEntity body = new UrlEncodedFormEntity(bodyParams);
                       
            HttpPost  post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(body);
        
            CloseableHttpResponse response = httpClient.execute(post, context);

            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            responseBody = responseBody.trim();

            logger.info("accountLogin( " + email + " ) = " + responseBody );

            JSONObject json = new JSONObject( responseBody );
            return json;
        } catch (Exception e) {
            logger.warn("accountLogin Exception: ",e);
            throw e;
        }
    }

    public JSONObject accountCreate( String email, String password, String firstName, String lastName, String companyName, String uid, String applianceModel, String majorVersion )
        throws Exception
    {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpClientContext context = HttpClientContext.create();

            URIBuilder builder = new URIBuilder(UvmContextImpl.getInstance().getStoreUrl() + "/account/create");
            builder.addParameter( "email" , email );
            builder.addParameter( "fname" , firstName );
            builder.addParameter( "lname" , lastName );
            builder.addParameter( "cname" , companyName );
            builder.addParameter( "uid" , uid );
            builder.addParameter( "applianceModel" , applianceModel );
            builder.addParameter( "majorVersion" , majorVersion );
            
            String url = builder.build().toString();

            LinkedList<NameValuePair> bodyParams = new LinkedList<NameValuePair>();
            bodyParams.add(new BasicNameValuePair("password",password));
            UrlEncodedFormEntity body = new UrlEncodedFormEntity(bodyParams);
                       
            HttpPost  post = new HttpPost(url);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(body);
        
            CloseableHttpResponse response = httpClient.execute(post, context);

            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            responseBody = responseBody.trim();

            logger.info("accountCreate( " + email + " ) = " + responseBody );

            JSONObject json = new JSONObject( responseBody );
            return json;
        } catch (Exception e) {
            logger.warn("accountCreate Exception: ",e);
            throw e;
        }
    }
    
    
}
