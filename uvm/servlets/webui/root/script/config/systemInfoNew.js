if (!Ung.hasResource["Ung.SystemInfo"]) {
    Ung.hasResource["Ung.SystemInfo"] = true;

     Ext.define("Ung.SystemInfo", {
    	extend: "Ung.StatusWin",
        panelVersion : null,
        panelLicenses : null,
        panelLicenseAgreement : null,
        dirty : false,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : Ext.bind(function() {
                    this.cancelAction();
                },this)
            }, {
                title : i18n._('System Info')
            }];
            this.buildVersion();
            this.buildLicenses();
            this.buildLicenseAgreement();
            
            // builds the tab panel with the tabs
            var pageTabs = [this.panelVersion, this.panelLicenses, this.panelLicenseAgreement];
            this.buildTabPanel(pageTabs);
            this.callParent(arguments);
        },
        buildVersion : function() {
            this.panelVersion = Ext.create('Ext.panel.Panel',{
                name : 'Version',
                helpSource : 'version',
                parentId : this.getId(),
                title : this.i18n._('Version'),
                cls: 'ung-panel',
                autoScroll : true,
                items : [{
                    title : this.i18n._('System Info'),
                    name : 'System Info',
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left',
                    items: [{
                    	xtype: 'textarea',
                        name : 'UID',
                        hideLabel : true,
                        readOnly : true,
                        style : 'font-weight: bold;',
                        width : 600,
                        height : 40,
                        value : this.i18n._('Do not publicly post or share the UID.') + "\n" +
                            this.i18n._('UID')+": " + rpc.jsonrpc.UvmContext.getServerUID()
                    }, {
                    	xtype: 'textarea',
                        name : 'System Info',
                        hideLabel : true,
                        readOnly : true,
                        style : 'font-weight: bold;',
                        width : 600,
                        height : 400,
                        value : this.i18n._('Build') + ": " + rpc.adminManager.getFullVersionAndRevision() + "\n" + 
                            this.i18n._('Kernel') + ": " + rpc.adminManager.getKernelVersion() + "\n" +
                            this.i18n._('History') + ": " + rpc.adminManager.getModificationState() + "\n" +
                            this.i18n._('Reboots') + ": " + rpc.adminManager.getRebootCount()
                    }]
                }]
            });
        },
        buildLicenseAgreement : function() {
            this.panelLicenseAgreement = Ext.create('Ext.panel.Panel',{
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

            this.panelLicenses = Ext.create('Ext.panel.Panel',{
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
                        html : Ext.String.format(this.i18n._('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'),'<b>','</b>'),
                        cls: 'description',
                        border : false}]
                 }]
           });

            this.panelLicenses.add( this.gridLicenses );

        },
        buildGridLicenses : function()
        {
            this.gridLicenses = Ext.create('Ung.EditorGrid',{
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
                            xtype : 'button',
                            id: "refresh_"+this.getId(),
                            text : i18n._('Refresh'),
                            name : "Refresh",
                            tooltip : i18n._('Refresh'),
                            iconCls : 'icon-refresh',
                            handler : Ext.bind(function() {
                                //reload licenses for each node in rack
                                main.loadLicenses();
                                //reload grid
                                this.gridLicenses.reloadGrid();
                            },this)
                        }
                    ]
                }),
                recordJavaClass : "com.untangle.uvm.node.License",
                dataFn : main.getLicenseManager().getLicenses,
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
                    header : this.i18n._("Name"),
                    dataIndex: "displayName",
                    width : 150
                },{
                    header : this.i18n._("App"),
                    dataIndex: "name",
                    width : 150
                },{
                    header : this.i18n._("UID"),
                    dataIndex: "UID",
                    width : 150
                },{
                    header : this.i18n._("Start Date"),
                    dataIndex: "start",
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    header : this.i18n._("End Date"),
                	dataIndex : "end",
                    width : 240,
                    renderer : function(value) { return new Date(value*1000); }
                },{
                    header : this.i18n._("Valid"),
                	dataIndex : "valid",
                    width : 50
                },{
                    header : this.i18n._("Status"),
                	dataIndex : "status",
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
            this.commitSettings(Ext.bind(this.reloadSettings,this));
        },
        reloadSettings : function()
        {
            this.dirty = false;
            Ext.MessageBox.hide();
        },
        saveAction : function()
        {
            this.commitSettings(Ext.bind(this.completeSaveAction,this));
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

            //nothing to be saved

            callback();
        },
        isDirty : function()
        {
            return this.dirty;
        }
    });

}
//@ sourceURL=systemInfoNew.js
