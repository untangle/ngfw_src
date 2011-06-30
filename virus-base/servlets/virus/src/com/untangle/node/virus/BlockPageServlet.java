/*
 * $HeadURL$
 */
package com.untangle.node.virus;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;

import  com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeContext;

import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.node.http.BlockPageUtil;  
import com.untangle.node.virus.VirusBlockDetails;
import com.untangle.node.virus.VirusNodeImpl;

@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        NodeManager nm = uvm.nodeManager();

        Map<String,String> i18n_map = LocalUvmContextFactory.context().
            languageManager().getTranslations( "untangle-base-virus" );
        
        NodeId tid = new NodeId(Long.parseLong(request.getParameter( "tid" )));
        
        NodeContext nodeContext = nm.nodeContext( tid );
        if ( nodeContext == null ) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        Object oNode = nodeContext.node();
        if ( !(oNode instanceof VirusNodeImpl) || ( oNode == null )) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        VirusNodeImpl node = (VirusNodeImpl)nodeContext.node();
        String nonce = request.getParameter("nonce");

        VirusBlockDetails blockDetails = node.getDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "This request has expired.", i18n_map ));
            return;
        }
        request.setAttribute( "reason", blockDetails.getReason());
        VirusBlockPageParameters params = new VirusBlockPageParameters( nodeContext.getNodeDesc().getDisplayName(), blockDetails );
                                                         
        BlockPageUtil.getInstance().handle( request, response, this, params );        
    }
    
    private static class VirusBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final VirusBlockDetails blockDetails;
        private final String nodeTitle;

        public VirusBlockPageParameters( String nodeTitle, VirusBlockDetails blockDetails )
        {
            this.nodeTitle = nodeTitle;
            this.blockDetails = blockDetails;
        }

        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n()
        {
            return "untangle-base-virus";
        }
        
        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} | {1} Warning", 
                               new Object[]{bm.getCompanyName(), this.blockDetails.getVendor()}, i18n_map);
        }
        
        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return this.nodeTitle;
        }
        
        public String getFooter( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} Virus Blocker", bm.getCompanyName(), i18n_map);
        }
        
        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return null;
        }

        public String getAdditionalFields(Map<String,String> i18n_map)
        {
            return null;
        }
        
        /* Retrieve the description of why this page was blocked. */
        public String getDescription( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}This file was blocked{1} because it contained a virus.",
                    new Object[]{ "<b>","</b>" }, i18n_map);
        }
    
        public VirusBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }
    
        public String getUnblockMode()
        {
            return "None";
        }
    }
}
