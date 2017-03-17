Ext.define('Webui.config.about', {
    extend: 'Ung.StatusWin',
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
        var kernelVersion, modificationState, rebootCount, activeSize, maxActiveSize;
        try {
            kernelVersion = rpc.adminManager.getKernelVersion();
            modificationState = rpc.adminManager.getModificationState();
            rebootCount = rpc.adminManager.getRebootCount();
            activeSize = rpc.hostTable.getCurrentActiveSize();
            maxActiveSize = rpc.hostTable.getMaxActiveSize();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.panelServer = Ext.create('Ext.panel.Panel',{
            name: 'Server',
            helpSource: 'about_server',
            title: i18n._('Server'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                title: i18n._('About'),
                name: 'About',
                xtype: 'fieldset',
                items: [{
                    xtype: 'textarea',
                    name: 'UID',
                    hideLabel: true,
                    readOnly: true,
                    style: 'font-weight: bold;',
                    width: 600,
                    height: 60,
                    value: i18n._('Do not publicly post or share the UID or account information.') + "\n" +
                        i18n._('UID')+": " + rpc.serverUID
                }, {
                    xtype: 'textarea',
                    name: 'About',
                    hideLabel: true,
                    readOnly: true,
                    style: 'font-weight: bold;',
                    width: 600,
                    height: 300,
                    value: i18n._('Build') + ": " + rpc.fullVersionAndRevision + "\n" +
                        i18n._('Kernel') + ": " + kernelVersion + "\n" +
                        i18n._('History') + ": " + modificationState + "\n" +
                        i18n._('Reboots') + ": " + rebootCount + "\n" +
                        i18n._('Current active device count') + ": " + activeSize + "\n" +
                        i18n._('Highest active device count since reboot') + ": " + maxActiveSize

                }]
            }]
        });
        // if the UID looks valid update the account info from the store
        if ( rpc.serverUID && rpc.serverUID.length == 19 ) {
            Ext.data.JsonP.request({
                url: Ung.Main.storeUrl + '?action=find_account&uid=' + rpc.serverUID,
                type: 'GET',
                success: function(response, opts) {
                    if( response!=null && response.account) {
                        if(!me || !me.isVisible()) {
                            return;
                        }
                        var uidComponent = me.panelServer.down('textarea[name="UID"]');
                        uidComponent.setValue(uidComponent.getValue() + "\n" + i18n._('Account') + ": " + response.account);
                    }
                },
                failure: function(response, opts) {
                    console.log("Failed to get account info fro UID:", rpc.serverUID);
                }
            });
        }
    },
    buildLicenseAgreement: function() {
        this.panelLicenseAgreement = Ext.create('Ext.panel.Panel',{
            name: 'License Agreement',
            helpSource: 'about_license_agreement',
            title: i18n._('License Agreement'),
            cls: 'ung-panel',
            bodyStyle: 'padding:5px 5px 0px; 5px;',
            items: [{
                xtype: "button",
                text: i18n._("View License"),
                name: "View License",
                iconCls: "reboot-icon",
                handler: function() {
                    Ung.Main.openLegal();
                }
            }]

       });
    },
    buildLicenses: function() {
        this.buildGridLicenses();

        this.panelLicenses = Ext.create('Ext.panel.Panel',{
            name: 'Licenses',
            helpSource: 'about_licenses',
            title: i18n._('Licenses'),
            cls: 'ung-panel',
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                title: i18n._('Licenses'),
                flex: 0,
                html: Ext.String.format(i18n._('Licenses determine entitlement to paid applications and services. Click Refresh to force reconciliation with the license server.'),'<b>','</b>')
             }, this.gridLicenses]
       });
    },
    buildGridLicenses: function() {
        this.gridLicenses = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: "gridLicenses",
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            title: i18n._("Licenses"),
            //TODO ext5 - qtip is not displayed, fix this
            qtip: i18n._("The Current list of Licenses available on this Server."),
            bbar: new Ext.Toolbar({
                items: [ '-', {
                    xtype: 'button',
                    text: i18n._('Refresh'),
                    name: "Refresh",
                    tooltip: i18n._('Refresh'),
                    iconCls: 'icon-refresh',
                    handler: Ext.bind(function() {
                        //reload licenses for each app in rack
                        Ung.Main.reloadLicenses();
                        //reload grid
                        this.gridLicenses.reload();
                    }, this)
                }]
            }),
            dataFn: Ung.Main.getLicenseManager().getLicenses,
            recordJavaClass: "com.untangle.uvm.app.License",
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
                header: i18n._("Name"),
                dataIndex: "displayName",
                width: 150
            },{
                header: i18n._("App"),
                dataIndex: "name",
                width: 150
            },{
                header: i18n._("UID"),
                dataIndex: "UID",
                width: 150
            },{
                header: i18n._("Start Date"),
                dataIndex: "start",
                width: 240,
                renderer: Ext.bind(function(value) { return i18n.timestampFormat(value*1000);}, this)
            },{
                header: i18n._("End Date"),
                dataIndex: "end",
                width: 240,
                renderer: Ext.bind(function(value) { return i18n.timestampFormat(value*1000); }, this)
            },{
                header: i18n._("Seats"),
                dataIndex: "seats",
                width: 50
            },{
                header: i18n._("Valid"),
                dataIndex: "valid",
                width: 50
            },{
                header: i18n._("Status"),
                dataIndex: "status",
                width: 150
            }]
        });
    }
});
//# sourceURL=about.js