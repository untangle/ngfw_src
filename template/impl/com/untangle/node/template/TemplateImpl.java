/*
 * $Id$
 */
package com.untangle.node.template;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class TemplateImpl extends AbstractNode implements Template
{   
    private final Logger logger = Logger.getLogger(TemplateImpl.class);

    private final EventHandler handler = new EventHandler(this);
    private final SoloPipeSpec pipeSpec = new SoloPipeSpec("template", this, handler, Fitting.OCTET_STREAM,Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private TemplateSettings settings;

    // constructor ------------------------------------------------------------

    public TemplateImpl()
    {
        this.settings = new TemplateSettings();
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");
        
        TemplateSettings settings = new TemplateSettings(this.getNodeSettings());
         
        try {
            setTemplateSettings(settings);
        } catch (Exception e) {
            logger.error( "Unable to initialize the settings", e );
            throw new IllegalStateException("Error initializing template", e);
        }
    }

    // TemplateNode methods --------------------------------------------------

    @Override
    public void setTemplateSettings(final TemplateSettings settings) throws Exception
    {
        if ( settings == this.settings ) {
            throw new IllegalArgumentException("Unable to update original settings, set this.settings to null first.");
        }
        
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                s.saveOrUpdate(settings);
                TemplateImpl.this.settings = settings;
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }

    public TemplateSettings getTemplateSettings()
    {
        return this.settings;
    }
    
    @Override
    public TemplateBaseSettings getBaseSettings()
    {
        TemplateBaseSettings baseSettings = this.settings.getBaseSettings();
        if ( baseSettings == null ) {
            baseSettings = new TemplateBaseSettings();
        }
                
        return baseSettings;        
    }
    
    @Override
    public void setBaseSettings(final TemplateBaseSettings baseSettings) throws Exception
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                TemplateImpl.this.settings.setBaseSettings( baseSettings );
                TemplateImpl.this.settings = (TemplateSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }
    
    @Override
    public void setAll( final TemplateBaseSettings baseSettings ) throws Exception
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                if ( baseSettings != null ) {
                    TemplateImpl.this.settings.setBaseSettings( baseSettings );
                }
                
                TemplateImpl.this.settings = (TemplateSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------   
    
    /**
     * Load the settings from the database.  This is called when the machine starts up.
     */
    protected void postInit()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
            public boolean doWork(Session s) {
                Query q = s.createQuery("from TemplateSettings cs where cs.nodeId = :nodeId");
                q.setParameter("nodeId", getNodeSettings());

                TemplateImpl.this.settings = (TemplateSettings) q.uniqueResult();
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        
        getNodeContext().runTransaction(tw);        
    }
   
    // private methods -------------------------------------------------------
    private void reconfigure() throws Exception
    {
        reconfigure(false);
    }
    
    /**
     * This contains all of the 
     * @param force
     */
    private void reconfigure(boolean force)
    {
    }
}
