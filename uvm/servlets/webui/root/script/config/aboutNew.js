Ext.define("Webui.config.aboutNew", {
    extend: "Ung.StatusWin",
    panelServer: null,
    panelLicenses: null,
    panelLicenseAgreement: null,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._('About')
        }];
        this.buildServer();
        this.buildLicenses();
        this.buildLicenseAgreement();
        
        // builds the tab panel with the tabs
        var pageTabs = [this.panelServer, this.panelLicenses, this.panelLicenseAgreement];
        this.buildTabPanel(pageTabs);
        this.callParent(arguments);
    },
    buildServer: function() {
        var me = this;
        var serverUID, fullVersionAndRevision, kernelVersion, modificationState, rebootCount, licensedSized, maxLicensedSize;
        try {
            serverUID = rpc.jsonrpc.UvmContext.getServerUID();
            fullVersionAndRevision = rpc.adminManager.getFullVersionAndRevision();
            kernelVersion = rpc.adminManager.getKernelVersion();
            modificationState = rpc.adminManager.getModificationState();
            rebootCount = rpc.adminManager.getRebootCount();
            licensedSized = rpc.hostTable.getCurrentLicensedSize();
            maxLicensedSize = rpc.hostTable.getMaxLicensedSize();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.panelServer = Ext.create('Ext.panel.Panel',{
            name: 'Server',
            helpSource: 'about_server',
            parentId: this.getId(),
            title: this.i18n._('Server'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                title: this.i18n._('About'),
                name: 'About',
                xtype: 'fieldset',
                buttonAlign: 'left',
                items: [{
                    xtype: 'textarea',
                    name: 'UID',
                    hideLabel: true,
                    readOnly: true,
                    style: 'font-weight: bold;',
                    width: 600,
                    height: 60,
                    value: this.i18n._('Do not publicly post or share the UID or account information.') + "\n" +
                        this.i18n._('UID')+": " + serverUID
                }, {
                    xtype: 'textarea',
                    name: 'About',
                    hideLabel: true,
                    readOnly: true,
                    style: 'font-weight: bold;',
                    width: 600,
                    height: 300,
                    value: this.i18n._('Build') + ": " + fullVersionAndRevision + "\n" + 
                        this.i18n._('Kernel') + ": " + kernelVersion + "\n" +
                        this.i18n._('History') + ": " + modificationState + "\n" +
                        this.i18n._('Reboots') + ": " + rebootCount + "\n" +
                        this.i18n._('Current "licensed" device count') + ": " + licensedSized + "\n" +
                        this.i18n._('Highest "licensed" device count since reboot') + ": " + maxLicensedSize

                }]
            }]
        });
        Ext.data.JsonP.request({
            url: rpc.storeUrl + "?" + "action=find_account&uid="+serverUID,
            type: 'GET',
            success: function(response, opts) {
                if( response!=null && response.account) {
                    if(!me || !me.isVisible()) {
                        return;
                    }
                    var uidComponent = me.panelServer.down('textarea[name="UID"]');
                    uidComponent.setValue(uidComponent.getValue() + "\n" + this.i18n._('Account') + ": " + response.account);
                }
            },
            failure: function(response, opts) {
                console.log("Failed to get account info fro UID:", serverUID);
            }
        });
    },
    buildLicenseAgreement: function() {
        this.panelLicenseAgreement = Ext.create('Ext.panel.Panel',{
            name: 'License Agreement',
            helpSource: 'about_license_agreement',
            parentId: this.getId(),
            title: this.i18n._('License Agreement'),
            cls: 'ung-panel',
            bodyStyle: 'padding:5px 5px 0px; 5px;',
            items: [{
                xtype: "button",
                text: this.i18n._("View License"),
                name: "View License",
                iconCls: "reboot-icon",
                handler: function() {
                    main.openLegal();
                }
            }]
            
       });
    },
    buildLicenses: function() {
        this.buildGridLicenses();

        this.panelLicenses = Ext.create('Ext.panel.Panel',{
            name: 'Licenses',
            helpSource: 'about_licenses',
            parentId: this.getId(),
            title: this.i18n._('Licenses'),
            cls: 'ung-panel',
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                cls: 'description',
                title: this.i18n._('Licenses'),
                flex: 0,
                html: Ext.String.format(this.i18n._('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'),'<b>','</b>')
             }, this.gridLicenses]
       });
    },
    buildGridLicenses: function() {
        this.gridLicenses = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: "gridLicenses",
            settingsCmp: this,
            parentId: this.getId(),
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            title: this.i18n._("Licenses"),
            //TODO: qtip is not displayed, fix this
            qtip: this.i18n._("The Current list of Licenses available on this Server."),
            bbar: new Ext.Toolbar({
                items: [
                    '-',
                    {
                        xtype: 'button',
                        id: "refresh_"+this.getId(),
                        text: i18n._('Refresh'),
                        name: "Refresh",
                        tooltip: i18n._('Refresh'),
                        iconCls: 'icon-refresh',
                        handler: Ext.bind(function() {
                            //reload licenses for each node in rack
                            main.loadLicenses();
                            //reload grid
                            this.gridLicenses.reload();
                        }, this)
                    }
                ]
            }),
            recordJavaClass: "com.untangle.uvm.node.License",
            dataFn: main.getLicenseManager().getLicenses,
            fields: [{
                name: "displayName"
            },{
                name: "name"
            },{
                name: "UID"
            },{
                name: "start"
            },{
                name: "end"
            },{
                name: "seats"
            },{
                name: "valid"
            },{
                name: "status"
            },{
                name: "id"
            }],
            columns: [{
                header: this.i18n._("Name"),
                dataIndex: "displayName",
                width: 150
            },{
                header: this.i18n._("App"),
                dataIndex: "name",
                width: 150
            },{
                header: this.i18n._("UID"),
                dataIndex: "UID",
                width: 150
            },{
                header: this.i18n._("Start Date"),
                dataIndex: "start",
                width: 240,
                renderer: Ext.bind(function(value) { return this.i18n.timestampFormat(value*1000);}, this)
            },{
                header: this.i18n._("End Date"),
                dataIndex: "end",
                width: 240,
                renderer: Ext.bind(function(value) { return this.i18n.timestampFormat(value*1000); }, this)
            },{
                header: this.i18n._("Seats"),
                dataIndex: "seats",
                width: 50
            },{
                header: this.i18n._("Valid"),
                dataIndex: "valid",
                width: 50
            },{
                header: this.i18n._("Status"),
                dataIndex: "status",
                width: 150
            }]
        });
    }
});
//# sourceURL=about.js