if (!Ung.hasResource["Ung.SystemInfo"]) {
    Ung.hasResource["Ung.SystemInfo"] = true;

    Ung.SystemInfo = Ext.extend(Ung.ConfigWin, {
        panelVersion : null,
        panelRegistration : null,
        panelLicenseAgreement : null,
        panelBranding : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('System Info')
            }];
            Ung.SystemInfo.superclass.initComponent.call(this);
        },
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.SystemInfo.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildVersion();
            this.buildRegistration();
            this.buildLicenseAgreement();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelVersion, this.panelRegistration, this.panelLicenseAgreement]);
            this.tabs.activate(this.panelVersion);
            this.panelVersion.disable();
            this.panelRegistration.disable();
            this.panelLicenseAgreement.disable();
        },
        getTODOPanel : function(title) {
            return new Ext.Panel({
                title : this.i18n._(title),
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._(title),
                    autoHeight : true,
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : 'TODO',
                        name : 'todo',
                        allowBlank : false,
                        value : 'todo',
                        disabled : true
                    }]
                }]
            });
        },
        buildVersion : function() {
            this.panelVersion = this.getTODOPanel("Version");
        },
        buildRegistration : function() {
            this.panelRegistration = this.getTODOPanel("Registration");
        },
        buildLicenseAgreement : function() {
            this.panelLicenseAgreement = this.getTODOPanel("License Agreement");
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
//                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
//                rpc.brandingManager.setBaseSettings(function(result, exception) {
//                    Ext.MessageBox.hide();
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    this.cancelAction();
//                }.createDelegate(this), this.getBrandingBaseSettings());
            }
        }

    });

}