if (!Ung.hasResource["Ung.SystemInfo"]) {
    Ung.hasResource["Ung.SystemInfo"] = true;

    Ung.SystemInfo = Ext.extend(Ung.ConfigWin, {
        panelVersion : null,
        panelLicenses : null,
        panelLicenseAgreement : null,
        dirty : false,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('System Info')
            }];
            this.buildVersion();
            this.buildLicenses();
            this.buildLicenseAgreement();
            
            // builds the tab panel with the tabs
            var pageTabs = [this.panelVersion, this.panelLicenses, this.panelLicenseAgreement];
            this.buildTabPanel(pageTabs);
            this.tabs.activate(this.panelVersion);
            Ung.SystemInfo.superclass.initComponent.call(this);
        },
        getSystemInfo : function(forceReload) {
            if (forceReload || this.rpc.systemInfo === undefined) {
                try {
                    this.rpc.systemInfo = rpc.adminManager.getSystemInfo();
                } catch (e) {
                    Ung.Util.rpcExHandler(e);
                }
                
            }
            return this.rpc.systemInfo;
        },
        buildVersion : function() {
            this.panelVersion = new Ext.Panel({
                name : 'Version',
                helpSource : 'version',
                parentId : this.getId(),
                title : this.i18n._('Version'),
                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                items : [{
                    title : this.i18n._('System Info'),
                    name : 'System Info',
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left',
                    items: [ new Ext.form.TextArea({
                        name : 'System Info',
                        hideLabel : true,
                        readOnly : true,
                        style : 'font-weight: bold;',
                        width : 600,
                        height : 400,
                        value : this.i18n._('Summary')+":\n"+
                            this.i18n._('UID')+": "+ this.getSystemInfo().serverUID + "\n" + 
                            this.i18n._('Build') + ": " + this.getSystemInfo().fullVersion + "\n" + 
                            this.i18n._('Java') + ": " + this.getSystemInfo().javaVersion 
                    })]
                }]
            });
        },
        buildLicenseAgreement : function() {
            this.panelLicenseAgreement = new Ext.Panel({
                name : 'License Agreement',
                helpSource : 'license_agreement',
                parentId : this.getId(),
                title : this.i18n._('License Agreement'),
                cls: 'ung-panel',
                items: [{
                    xtype : "button",
                    text : this.i18n._("View License"),
                    name : "View License",
                    iconCls : "reboot-icon",
                    handler : function() {window.open("../library/launcher?action=legal");}
                }]
                
           });
        },
        buildLicenses : function() {

            this.buildGridLicenses();

            this.panelLicenses = new Ext.Panel({
                name : 'Licenses',
                helpSource : 'licenses',
                parentId : this.getId(),
                title : this.i18n._('Licenses'),
                cls: 'ung-panel',
                items: [{
                    title : this.i18n._('Licenses'),
                    name : 'Status',
                    xtype : 'fieldset',
                    autoHeight : true,
                    items: [{
                        html : String.format(this.i18n._('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'),'<b>','</b>'),
                        cls: 'description',
                        border : false}]
                 }]
           });

            this.panelLicenses.add( this.gridLicenses );

        },
        buildGridLicenses : function()
        {
            this.gridLicenses = new Ung.EditorGrid({
                name : "gridLicenses",
                settingsCmp : this,
                height : 350,
                parentId : this.getId(),
                hasAdd : false,
                configAdd : null,
                hasEdit : false,
                configEdit : null,
                hasDelete : false,
                configDelete : null,
                columnsDefaultSortable : true,
                title : this.i18n._("Licenses"),
                qtip : this.i18n._("The Current list of Licenses available on this Server."),
                paginated : false,
                bbar : new Ext.Toolbar({
                    items : [
                        '-',
                        {
                            xtype : 'tbbutton',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : function() {
                                //force re-sync with server
                                main.getLicenseManager().reloadLicenses();
                                //reload grid
                                this.gridLicenses.store.reload();
                                //reload licenses for each node in rack
                                main.loadLicenses();
                            }.createDelegate(this)
                        }
                    ]
                }),
                recordJavaClass : "com.untangle.uvm.license.License",
                proxyRpcFn : main.getLicenseManager().getLicenses,
                plugins : [],
                fields : [{
                    name : "displayName"
                },{
                    name : "name"
                },{
                    name : "UID"
                },{
                    name : "start"
                },{
                    name : "end"
                },{
                    name : "valid"
                },{
                    name : "status"
                },{
                    name : "id"
                }],
                columns : [{
                    id : "displayName",
                    header : this.i18n._("Name"),
                    width : 150
                },{
                    id : "name",
                    header : this.i18n._("App"),
                    width : 150
                },{
                    id : "UID",
                    header : this.i18n._("UID"),
                    width : 150
                },{
                    id : "start",
                    header : this.i18n._("Start Date"),
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    id : "end",
                    header : this.i18n._("End Date"),
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    id : "valid",
                    header : this.i18n._("Valid"),
                    width : 50
                },{
                    id : "status",
                    header : this.i18n._("Status"),
                    width : 150
                }]
            });
        },
        onFieldChange : function() {
            this.dirty = true;
        },
        onSelectOther: function( checkbox, isChecked )
        {
            if ( isChecked ) {
                this.environmentOther.show();
                this.environmentOther.enable();
                this.environmentOther.getEl().up('.x-form-item').setDisplayed( true );
            } else {
                this.environmentOther.hide();
                this.environmentOther.disable();
                this.environmentOther.getEl().up('.x-form-item').setDisplayed( false );
            }
        },
        validateClient : function() {
            return true;
        },
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },
        reloadSettings : function()
        {
            this.dirty = false;
            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate(this));
        },
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },
        // save function
        commitSettings : function(callback)
        {
            if (!this.validate()) {
                return;
            }
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
        },
        isDirty : function()
        {
            return this.dirty;
        }
    });

}
