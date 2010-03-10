if (!Ung.hasResource["Ung.Template"]) {
    Ung.hasResource["Ung.Template"] = true;
    Ung.NodeWin.registerClassName("untangle-node-template", "Ung.Template");

    Ung.Template = Ext.extend(Ung.NodeWin, {
        panelTemplate : null,

        initComponent : function()
        {
            // keep initial base settings
            this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());

            
            // builds the tabs
            this.buildTemplate();

            // builds the tab panel with the tabs
            this.buildTabPanel([ this.panelTemplate ]);

            Ung.Template.superclass.initComponent.call(this);
        },
        // Rules Panel
        buildTemplate : function()
        {

            this.panelTemplate = new Ext.Panel({
                name : "panelTemplate",
                helpSource : "panel_template",
                // private fields
                parentId : this.getId(),
                title : this.i18n._("Panel Template"),
                autoScroll : true,
                border : false,
                cls: "ung-panel",
                items : [{
                    title : this.i18n._("Note"),
                    cls: "description",
                    bodyStyle : "padding: 5px 5px 5px; 5px;",
                    html : this.i18n._("This is a sample panel.")
                }]
                         
            });
        },
        
        //apply function 
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            // exit settings screen
            this.closeWindow();
        },
        reloadSettings : function()
        {
            this.getRpcNode().getBaseSettings(this.completeReloadSettings.createDelegate( this ));
        },
        completeReloadSettings : function( result, exception )
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }

            this.rpc.baseSettings = result;
            this.initialBaseSettings = Ung.Util.clone(this.rpc.baseSettings);

            Ext.MessageBox.hide();
        },
        validateClient : function()
        {
            /* Validate all of the fields locally here. */
        },
        // commit function
        commitSettings : function(callback)
        {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                
                var wrapper = function( result, exception )
                {
                    if(Ung.Util.handleException(exception)) {
                        return;
                    }
                    callback();
                }.createDelegate(this);
                this.getRpcNode().setBaseSettings(wrapper, this.getBaseSettings());
            }
        },

        isDirty : function() {
            return !Ung.Util.equals(this.getBaseSettings(), this.initialBaseSettings);
        }
    });
}
