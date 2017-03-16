Ext.define('Webui.openvpn.settings', {
    extend:'Ung.AppWin',
    groupsStore: null,
    panelClient: null,
    gridRemoteServers: null,
    panelServer: null,
    gridConnectionEventLog: null,
    getAppSummary: function() {
        return i18n._("OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.");
    },
    initComponent: function(container, position) {
        // Register the VTypes, need i18n to be initialized for the text
        Ext.applyIf(Ext.form.VTypes, {
            openvpnNameRe: /^[A-Za-z0-9]([-.0-9A-Za-z]*[0-9A-Za-z])?$/,
            openvpnName: function(v) {
                return Ext.form.VTypes['openvpnNameRe'].test(v);
            },
            openvpnNameMask: /[-.0-9A-Za-z]/i,
            openvpnNameText: i18n._( "A name should only contains numbers, letters, dashes and periods.  Spaces are not allowed." )
        });

        this.buildServer();
        this.buildClient();

        this.buildTabPanel( [ this.panelServer, this.panelClient] );
        this.callParent(arguments);
    },
    getGroupsStore: function(force) {
        if (this.groupsStore == null ) {
            this.groupsStore = Ext.create('Ext.data.Store', {
                fields: ['groupId', 'name', 'javaClass'],
                data: this.getSettings().groups.list
            });
            force = false;
        }

        if(force) {
            this.groupsStore.loadData( this.getSettings().groups.list );
        }

        return this.groupsStore;
    },
    getDefaultGroupId: function(forceReload) {
        if (forceReload || this.defaultGroupId === undefined) {
            var defaultGroup = this.getGroupsStore().getCount()>0 ? this.getGroupsStore().getAt(0).data : null;
            this.defaultGroupId = defaultGroup == null ? null : defaultGroup.groupId;
        }
        return this.defaultGroupId;
    },
    // active connections/sessions grip
    buildClientStatusGrid: function() {
        this.gridClientStatus = Ext.create('Ung.grid.Panel', {
            name: "gridClientStatus",
            margin: '0 10 20 10',
            height: 220,
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Connected Remote Clients"),
            qtip: i18n._("The Connected Remote Clients list shows connected clients."),
            dataFn: this.getRpcNode().getActiveClients,
            recordJavaClass: "com.untangle.app.openvpn.OpenVpnStatusEvent",
            fields: [{
                name: "address",
                sortType: 'asIp'
            }, {
                name: "clientName"
            }, {
                name: "poolAddress",
                sortType: 'asIp'
            }, {
                name: "start",
                sortType: 'asTimestamp'
            }, {
                name: "bytesRxTotal"
            }, {
                name: "bytesTxTotal"
            }, {
                name: "id"
            }],
            columns: [{
                header: i18n._("Address"),
                dataIndex:'address',
                width: 150
            }, {
                header: i18n._("Client"),
                dataIndex:'clientName',
                width: 200
            }, {
                header: i18n._("Pool Address"),
                dataIndex:'poolAddress',
                width: 150
            }, {
                header: i18n._("Start Time"),
                dataIndex:'start',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            }, {
                header: i18n._("Rx Data"),
                dataIndex:'bytesRxTotal',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
            }, {
                header: i18n._("Tx Data"),
                dataIndex:'bytesTxTotal',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
            }]
        });
    },

    // active connections/sessions grip
    buildServerStatusGrid: function() {
        this.gridServerStatus = Ext.create('Ung.grid.Panel', {
            name: "gridServerStatus",
            margin: '0 10 20 10',
            height: 220,
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Remote Server Status"),
            qtip: i18n._("The Remote Server Status list shows the current status of the configured remote servers."),
            dataFn: this.getRpcNode().getRemoteServersStatus,
            fields: [{
                name: "name"
            }, {
                name: "connected"
            }, {
                name: "bytesRead"
            }, {
                name: "bytesWritten"
            }, {
                name: "id"
            }],
            columns: [{
                header: i18n._("Name"),
                dataIndex:'name',
                width: 150
            }, {
                header: i18n._("Connected"),
                dataIndex:'connected',
                width: 75
            }, {
                header: i18n._("Rx Data"),
                dataIndex:'bytesRead',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
            }, {
                header: i18n._("Tx Data"),
                dataIndex:'bytesWritten',
                width: 180,
                renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
            }]
        });
    },
    // Status panel
    buildStatus: function() {
        this.buildClientStatusGrid();
        this.buildServerStatusGrid();

        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'openvpn_status',
            itemsAfterLicense: [this.gridClientStatus, this.gridServerStatus]
        });
    },
    getGroupsColumn: function() {
        return {
            header: i18n._("Group"),
            width: 160,
            dataIndex: 'groupId',
            renderer: Ext.bind(function(value, metadata, record,rowIndex,colIndex,store,view) {
                var group = this.getGroupsStore().findRecord("groupId",value);
                if (group != null)
                    return group.get("name");
                return "";
            }, this ),
            editor: Ext.create('Ext.form.ComboBox', {
                store: this.getGroupsStore(),
                displayField: 'name',
                valueField: 'groupId',
                editable: false,
                queryMode: 'local'
            })
        };
    },
    buildGridServers: function() {
        this.gridRemoteServers = Ext.create('Ung.grid.Panel', {
            hasAdd: false,
            settingsCmp: this,
            name: 'Remote Servers',
            flex: 1,
            margin: '0 20 0 20',
            title: i18n._("Remote Servers"),
            dataProperty: "remoteServers",
            recordJavaClass: "com.untangle.app.openvpn.OpenVpnRemoteServer",
            emptyRow: {
                "enabled": true,
                "name": ""
            },
            fields: [{
                name: 'enabled'
            }, {
                name: 'name'
            }, {
                name: 'originalName',
                mapping: 'name'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                width: 80,
                resizable: false
            }, {
                header: i18n._("Server Name"),
                width: 130,
                dataIndex: 'name',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter server name]"),
                    allowBlank: false,
                    vtype: 'openvpnName'
                }
            }],
            rowEditorInputLines: [{
                xtype: 'checkbox',
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enabled")
            }, {
                xtype: 'container',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: "textfield",
                    name: "Server name",
                    dataIndex: "name",
                    fieldLabel: i18n._("Server name"),
                    emptyText: i18n._("[enter server name]"),
                    allowBlank: false,
                    vtype: 'openvpnName',
                    width: 300
                },{
                    xtype: 'label',
                    html: i18n._("only alphanumerics allowed") + " [A-Za-z0-9-]",
                    cls: 'boxlabel'
                }]
            }]
        });
    },
    buildClient: function() {
        this.buildGridServers();

        this.submitForm = Ext.create('Ext.form.Panel', {
            flex: 0,
            margin: '5 0 0 0',
            border: false,
            items: [{
                xtype: 'fieldset',
                title: i18n._("Configure a new Remote Server connection"),
                items: [{
                    xtype: 'filefield',
                    name: 'uploadConfigFileName',
                    fieldLabel: i18n._('Configuration File'),
                    allowBlank: false,
                    validateOnBlur: false,
                    labelWidth: 150,
                    width: 400
                }, {
                    xtype: 'button',
                    id: "submitUpload",
                    text: i18n._('Submit'),
                    name: "Submit",
                    handler: Ext.bind(function() {
                        var filename = this.submitForm.down('textfield[name="uploadConfigFileName"]').getValue();
                        if ( filename == null || filename.length == 0 ) {
                            Ext.MessageBox.alert(i18n._( "Select File" ), i18n._( "Please choose a file to upload." ));
                            return;
                        }

                        this.submitForm.submit({
                            url: "/openvpn/uploadConfig",
                            success: Ext.bind(function( form, action, handler ) {
                                Ext.MessageBox.alert(i18n._( "Success" ), i18n._( "The configuration has been imported." ));
                                this.getSettings(function () {
                                    this.gridRemoteServers.reload(this.getSettings().remoteServers);
                                });
                            }, this),
                            failure: Ext.bind(function( form, action ) {
                                Ext.MessageBox.alert(i18n._( "Failure" ), i18n._( "Import failure" ) + ": " + action.result.code);
                            }, this)
                        });
                    }, this)
                }]
            }]
        });

        this.panelClient = Ext.create('Ext.panel.Panel', {
            name: 'Client',
            title: i18n._('Client'),
            helpSource: 'openvpn_client',
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._('Client'),
                items: [{
                    xtype: 'component',
                    html: "<i>" + i18n._("These settings configure how OpenVPN will connect to remote servers as a client.") + "</i>"
                }, {
                    xtype: 'component',
                    html: "<i>" + i18n._("Remote Servers is a list remote OpenVPN servers that OpenVPN should connect to as a client.") + "</i>"
                }]
            }, this.gridRemoteServers, this.submitForm]
        });
    },
    getDistributeWindow: function() {
        if(!this.distributeWindow) {
            this.distributeWindow = Ext.create('Ung.Window', {
                title: i18n._('Download OpenVPN Client'),
                settingsCmp: this,
                i18n: i18n,
                items: [{
                    xtype: 'panel',
                    items: [{
                        xtype: 'fieldset',
                        title: i18n._('Download'),
                        margin: 10,
                        defaults: {
                            margin: 10
                        },
                        items: [{
                            xtype: 'component',
                            html: i18n._('These files can be used to configure your Remote Clients.')
                        }, {
                            xtype: 'component',
                            name: 'downloadWindowsInstaller',
                            html:  " "
                        }, {
                            xtype: 'component',
                            name: 'downloadGenericConfigurationFile',
                            html: " "
                        }, {
                            xtype: 'component',
                            name: 'downloadUntangleConfigurationFile',
                            html: " "
                        }, {
                            xtype: 'component',
                            html: '<BR>'
                        }, {
                            xtype: 'component',
                            html: i18n._('This file can be used to configure Chromebook clients.  On the target device, browse to <b>chrome://net-internals#chromeos</b> and use Import ONC file.')
                        }, {
                            xtype: 'component',
                            name: 'downloadChromebookConfigurationFile',
                            html: " "
                        }]
                    }]
                }],
                bbar: ['->', {
                    name: 'close',
                    iconCls: 'cancel-icon',
                    text: i18n._('Close'),
                    handler: function() {
                        this.distributeWindow.close();
                    },
                    scope: this
                }],
                closeWindow: function() {
                    this.hide();
                },
                populate: function( record ) {
                    this.record = record;
                    this.show();
                    var windowsLink = this.down('[name="downloadWindowsInstaller"]');
                    var genericLink = this.down('[name="downloadGenericConfigurationFile"]');
                    var untangleLink = this.down('[name="downloadUntangleConfigurationFile"]');
                    var chromebookLink = this.down('[name="downloadChromebookConfigurationFile"]');

                    windowsLink.update(i18n._('Loading...'));
                    genericLink.update(i18n._('Loading...'));
                    untangleLink.update(i18n._('Loading...'));
                    chromebookLink.update(i18n._('Loading...'));

                    Ext.MessageBox.wait(i18n._( "Building OpenVPN Client..." ), i18n._( "Please Wait" ));
                    // populate download links
                    var loadSemaphore = 3;

                    this.settingsCmp.getRpcNode().getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        windowsLink.update('<a href="'+result+'" target="_blank">'+i18n._('Click here to download this client\'s Windows setup.exe file.') + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "exe" );

                    this.settingsCmp.getRpcNode().getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        chromebookLink.update('<a href="'+result+'" target="_blank">'+i18n._('Click here to download this client\'s configuration onc file for Chromebook.') + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "onc" );

                    this.settingsCmp.getRpcNode().getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        genericLink.update('<a href="'+result+'" target="_blank">'+i18n._('Click here to download this client\'s configuration zip file for other OSs (apple/linux/etc).') + '</a>');
                        untangleLink.update('<a href="'+result+'" target="_blank">'+i18n._('Click here to download this client\'s configuration file for remote Untangle OpenVPN clients.') + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "zip" );
                }
            });
            this.subCmps.push(this.distributeWindow);
        }
        return this.distributeWindow;
    },
    buildGridRemoteClients: function() {
        this.gridRemoteClients = Ext.create('Ung.grid.Panel', {
            settingsCmp: this,
            name: 'Remote Clients',
            title: i18n._("Remote Clients"),
            dataProperty: "remoteClients",
            recordJavaClass: "com.untangle.app.openvpn.OpenVpnRemoteClient",
            emptyRow: {
                "enabled": true,
                "name": "",
                "groupId": this.getDefaultGroupId(),
                "address": null,
                "export":false,
                "exportNetwork":null
            },
            fields: [{
                name: 'enabled'
            }, {
                name: 'name'
            }, {
                name: 'originalName',
                mapping: 'name'
            }, {
                name: 'groupId'
            }, {
                name: 'export'
            }, {
                name: 'exportNetwork'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                width: 80,
                resizable: false
            }, {
                header: i18n._("Client Name"),
                width: 130,
                dataIndex: 'name',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter client name]"),
                    allowBlank: false,
                    vtype: 'openvpnName'
                }
            },
            this.getGroupsColumn(),
            {
                header: i18n._("Download Client"),
                xtype: 'actioncolumn',
                width: 120,
                items: [{
                    tooltip: i18n._("Click to download client"),
                    iconCls: 'icon-row icon-export',
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        if(record.data.internalId<0) {
                            Ext.MessageBox.alert(i18n._("Failed"), i18n._("New clients must be saved before downloading the client."));
                        } else if(!Ung.Node.getCmp(this.nodeId).isRunning()) {
                            Ext.MessageBox.alert(i18n._("Failed"), i18n._("OpenVPN must be turned ON in order to download the client."));
                        } else {
                            this.getDistributeWindow().populate(record);
                        }
                    }, this)
                }]
            }]
        });
        this.gridRemoteClients.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            inputLines: [{
                xtype: 'checkbox',
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enabled")
            }, {
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: "textfield",
                        name: "Client Name",
                        dataIndex: "name",
                        fieldLabel: i18n._("Client Name"),
                        emptyText: i18n._("[enter client name]"),
                        allowBlank: false,
                        vtype: 'openvpnName',
                        width: 300
                    },{
                        xtype: 'label',
                        html: i18n._("only alphanumerics allowed") + " [A-Za-z0-9-]",
                        cls: 'boxlabel'
                    }]
            }, {
                xtype: "combo",
                name: "Group",
                dataIndex: "groupId",
                fieldLabel: i18n._("Group"),
                store: this.getGroupsStore(),
                displayField: 'name',
                valueField: 'groupId',
                editable: false,
                queryMode: 'local',
                width: 300
            }, {
                xtype: "combo",
                name: "Type",
                dataIndex: "export",
                fieldLabel: i18n._("Type"),
                displayField: 'name',
                editable: false,
                store: [[false,i18n._('Individual Client')], [true,i18n._('Network')]],
                queryMode: 'local',
                width: 300,
                listeners: {
                    "change": {
                        fn: function(elem, newValue) {
                            this.gridRemoteClients.rowEditor.syncComponents();
                        },
                        scope: this
                    }
                }
            }, {
                xtype: "textfield",
                name: "Remote Networks",
                dataIndex: "exportNetwork",
                fieldLabel: i18n._("Remote Networks"),
                allowBlank: false,
                vtype: 'cidrBlockList',
                width: 300
            }],
            syncComponents: function () {
                var type = this.down('combo[dataIndex="export"]');
                var exportNetwork  = this.down('textfield[dataIndex="exportNetwork"]');
                if (type.value) {
                    exportNetwork.enable();
                } else {
                    exportNetwork.disable();
                }
            }
        }));
    },
    buildGridExports: function() {
        this.gridExports = Ext.create('Ung.grid.Panel', {
            settingsCmp: this,
            name: 'Exports',
            title: i18n._("Exported Networks"),
            dataProperty: 'exports',
            recordJavaClass: "com.untangle.app.openvpn.OpenVpnExport",
            emptyRow: {
                "enabled": true,
                "name": "",
                "network": "192.168.1.0/24"
            },
            fields: [{
                name: 'enabled'
            }, {
                name: 'name'
            }, {
                name: 'network',
                sortType: 'asIp'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                width: 80,
                resizable: false
            }, {
                header: i18n._("Export Name"),
                width: 150,
                dataIndex: 'name',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter export name]"),
                    allowBlank: false
                }
            }, {
                header: i18n._("Network"),
                width: 150,
                dataIndex: 'network',
                flex:1,
                editor: {
                    xtype:'textfield',
                    allowBlank: false,
                    vtype: 'cidrBlock'
                }
            }],
            rowEditorInputLines: [{
                xtype: 'checkbox',
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enabled")
            }, {
                xtype: "textfield",
                name: "Export name",
                dataIndex: "name",
                fieldLabel: i18n._("Export Name"),
                emptyText: i18n._("[enter export name]"),
                allowBlank: false,
                width: 300
            }, {
                xtype: "textfield",
                name: "Export network",
                dataIndex: "network",
                fieldLabel: i18n._("Network"),
                allowBlank: false,
                vtype: 'cidrBlock',
                width: 300
            }]
        });
    },
    buildGridGroups: function() {
        this.gridGroups = Ext.create('Ung.grid.Panel', {
            settingsCmp: this,
            name: 'Groups',
            addAtTop: false,
            title: i18n._("Groups"),
            dataProperty: 'groups',
            recordJavaClass: "com.untangle.app.openvpn.OpenVpnGroup",
            emptyRow: {
                "groupId": -1,
                "name": "",
                "pushDns": false,
                "pushDnsSelf": false,
                "fullTunnel": false
            },
            fields: [{
                name: 'groupId'
            }, {
                name: 'name'
            }, {
                name: 'fullTunnel'
            }, {
                name: 'pushDns'
            }, {
                name: 'pushDnsSelf'
            }, {
                name: 'pushDns1'
            }, {
                name: 'pushDns2'
            }, {
                name: 'pushDnsDomain'
            }],
            columns: [{
                header: i18n._("Group Name"),
                width: 160,
                dataIndex: 'name',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter group name]"),
                    allowBlank:false
                }
            },{
                id: "fullTunnel",
                header: i18n._("Full Tunnel"),
                dataIndex: 'fullTunnel',
                width: 90,
                resizable: false
            },{
                id: "pushDns",
                header: i18n._("Push DNS"),
                dataIndex: 'pushDns',
                width: 90,
                resizable: false
            }],
            rowEditorInputLines: [{
                xtype: "textfield",
                name: "Group Name",
                dataIndex: "name",
                fieldLabel: i18n._("Group Name"),
                emptyText: i18n._("[enter group name]"),
                allowBlank: false,
                width: 300
            }, {
                xtype: 'checkbox',
                name: "Full Tunnel",
                dataIndex: "fullTunnel",
                fieldLabel: i18n._("Full Tunnel")
            }, {
                xtype: 'checkbox',
                name: "Push DNS",
                dataIndex: "pushDns",
                fieldLabel: i18n._("Push DNS"),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            if ( newValue ) {
                                Ext.getCmp('pushDnsSettings').show();
                            } else {
                                Ext.getCmp('pushDnsSettings').hide();

                            }
                        }, this)
                    },
                    "render": {
                        fn: Ext.bind(function(field) {
                            if ( field.value ) {
                                Ext.getCmp('pushDnsSettings').show();
                            } else {
                                Ext.getCmp('pushDnsSettings').hide();
                            }
                        }, this),
                        scope: this
                    }
                }
            }, {
                xtype: 'fieldset',
                id: "pushDnsSettings",
                title: i18n._('Push DNS Configuration'),
                defaults: {
                    labelWidth: 150,
                    width: 350
                },
                items: [{
                    xtype: "combo",
                    name: "Push DNS Server",
                    dataIndex: "pushDnsSelf",
                    fieldLabel: i18n._("Push DNS Server"),
                    displayField: 'name',
                    editable: false,
                    store: [[true,i18n._('OpenVPN Server')], [false,i18n._('Custom')]],
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                if (newValue) {
                                    Ext.getCmp('pushDns1').disable();
                                    Ext.getCmp('pushDns2').disable();
                                } else {
                                    Ext.getCmp('pushDns1').enable();
                                    Ext.getCmp('pushDns2').enable();
                                }
                            }, this)
                        },
                        "render": {
                            fn: Ext.bind(function(field) {
                                if (field.value) {
                                    Ext.getCmp('pushDns1').disable();
                                    Ext.getCmp('pushDns2').disable();
                                } else {
                                    Ext.getCmp('pushDns1').enable();
                                    Ext.getCmp('pushDns2').enable();
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype: "textfield",
                    id: "pushDns1",
                    dataIndex: "pushDns1",
                    fieldLabel: i18n._("Push DNS Custom 1"),
                    allowBlank: true,
                    vtype: 'ipAddress'
                }, {
                    xtype: "textfield",
                    id: "pushDns2",
                    dataIndex: "pushDns2",
                    fieldLabel: i18n._("Push DNS Custom 2"),
                    allowBlank: true,
                    vtype: 'ipAddress'
                }, {
                    xtype: "textfield",
                    id: "pushDnsDomain",
                    dataIndex: "pushDnsDomain",
                    fieldLabel: i18n._("Push DNS Domain"),
                    allowBlank: true
                }]
            }]
        });
    },

    buildServer: function() {
        this.gridRemoteClients = null;
        this.gridExports = null;
        this.gridGroups = null;

        this.buildGridRemoteClients();
        this.buildGridExports();
        this.buildGridGroups();

        this.tabPanelServer = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            deferredRender: false,
            flex: 1,
            margin: '0 20 0 20',
            items: [ this.gridRemoteClients, this.gridGroups, this.gridExports ]
        });

        this.reRenderFn = Ext.bind( function (newValue) {
            this.getSettings().serverEnabled = newValue;

            this.tabPanelServer.disable();
            Ext.getCmp('openvpn_options_client_to_client').disable();
            Ext.getCmp('openvpn_options_port').disable();
            Ext.getCmp('openvpn_options_protocol').disable();
            Ext.getCmp('openvpn_options_cipher').disable();
            Ext.getCmp('openvpn_options_addressSpace').disable();
            Ext.getCmp('openvpn_options_nat').disable();
            Ext.getCmp('openvpn_options_nat_comment').hide();
            if (newValue) {
                this.tabPanelServer.enable();
                Ext.getCmp('openvpn_options_client_to_client').enable();
                Ext.getCmp('openvpn_options_port').enable();
                Ext.getCmp('openvpn_options_protocol').enable();
                Ext.getCmp('openvpn_options_cipher').enable();
                Ext.getCmp('openvpn_options_addressSpace').enable();
                Ext.getCmp('openvpn_options_nat').enable();
                Ext.getCmp('openvpn_options_nat_comment').show();
            }
        }, this);

        var publicUrl;
        try {
            publicUrl = rpc.networkManager.getPublicUrl();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        this.panelServer = Ext.create('Ext.panel.Panel', {
            name: 'Server',
            helpSource: 'openvpn_server',
            title: i18n._("Server"),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Server'),
                flex: 0,
                defaults: {
                    labelWidth: 160
                },
                items: [{
                    xtype: 'component',
                    html: "<i>" + i18n._("These settings configure how OpenVPN will be a server for remote clients.") + "</i>",
                    margin: '0 0 5 0'
                }, {
                    xtype: 'textfield',
                    width: 300,
                    fieldLabel: i18n._('Site Name'),
                    name: 'Site Name',
                    value: this.getSettings().siteName,
                    vtype: 'openvpnName',
                    id: 'openvpn_options_siteName',
                    allowBlank: false,
                    blankText: i18n._("You must enter a site name."),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().siteName = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'displayfield',
                    width: 300,
                    fieldLabel: i18n._('Site URL'),
                    name: 'Site URL',
                    value: publicUrl.split(":")[0]+":"+this.getSettings().port,
                    id: 'openvpn_options_siteUrl'
                }, {
                    xtype: 'checkbox',
                    name: "Server Enabled",
                    fieldLabel: i18n._("Server Enabled"),
                    checked: this.getSettings().serverEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.reRenderFn( newValue );
                            }, this)
                        },
                        "render": {
                            fn: Ext.bind(function(field) {
                                this.reRenderFn( field.value );
                            }, this),
                            scope: this
                        }
                    }
                }, {
                    xtype: 'checkbox',
                    hidden: Ung.Util.hideDangerous,
                    name: 'Client To Client',
                    fieldLabel: i18n._('Client To Client Allowed'),
                    checked: this.getSettings().clientToClient,
                    id: 'openvpn_options_client_to_client',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().clientToClient = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'numberfield',
                    hidden: Ung.Util.hideDangerous,
                    width: 300,
                    fieldLabel: i18n._('Port'),
                    name: 'Port',
                    value: this.getSettings().port,
                    id: 'openvpn_options_port',
                    allowBlank: false,
                    vtype: "port",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().port = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    hidden: Ung.Util.hideDangerous,
                    editable: false,
                    width: 300,
                    fieldLabel: i18n._('Protocol'),
                    name: 'Protocol',
                    store: [["udp", "UDP"], ["tcp", "TCP"]],
                    value: this.getSettings().protocol,
                    id: 'openvpn_options_protocol',
                    allowBlank: false,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                Ext.MessageBox.confirm(i18n._("Warning"),
                                    i18n._("Changing the protocol will require redistributing ALL of the openvpn clients.") + "<br/><br/>" +
                                    i18n._("Do you want to continue anyway?"),
                                    Ext.bind(function(btn, text) {
                                        if (btn == 'yes') {
                                            this.getSettings().protocol = newValue;
                                        }
                                    }, this));
                            }, this)
                        }
                    }
                }, {
                    xtype: 'textfield',
                    hidden: Ung.Util.hideDangerous,
                    width:300,
                    fieldLabel: i18n._('Cipher'),
                    name: 'Cipher',
                    value: this.getSettings().cipher,
                    id: 'openvpn_options_cipher',
                    allowBlank: false,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().cipher = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'textfield',
                    width: 300,
                    fieldLabel: i18n._('Address Space'),
                    name: 'Address Space',
                    value: this.getSettings().addressSpace,
                    id: 'openvpn_options_addressSpace',
                    allowBlank: false,
                    vtype: "cidrBlock",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.getSettings().addressSpace = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'container',
                    layout: { type: 'hbox', align: 'middle'},
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'checkbox',
                        labelWidth: 160,
                        name: "NAT OpenVPN Traffic",
                        fieldLabel: i18n._("NAT OpenVPN Traffic"),
                        checked: this.getSettings().natOpenVpnTraffic,
                        id: 'openvpn_options_nat',
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().natOpenVpnTraffic = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'label',
                        id: 'openvpn_options_nat_comment',
                        html: "(" + i18n._("NAT all LAN-bound OpenVPN traffic to a local address") + ")",
                        cls: 'boxlabel'
                    }]
                }]
            }, this.tabPanelServer]
        });
    },

    // validation function
    validate: function() {
        return  this.validateServer() && this.validateGroups() && this.validateVpnClients();
    },

    //validate OpenVPN Server settings
    validateServer: function() {
        //validate site name
        var siteCmp = Ext.getCmp("openvpn_options_siteName");
        if(!siteCmp.validate()) {
            Ext.MessageBox.alert(i18n._("Failed"), i18n._("You must enter a site name."),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.panelServer);
                    siteCmp.focus(true);
                }, this)
            );
            return false;
        }
        return true;
    },

    validateGroups: function() {
        var i;
        var groups=this.gridGroups.getList(false, true);

        // verify that there is at least one group
        if(groups.length <= 0 ) {
            Ext.MessageBox.alert(i18n._('Failed'), i18n._("You must create at least one group."),
                Ext.bind(function () {
                    this.tabs.setActiveTab(this.gridGroups);
                }, this)
            );
            return false;
        }

        // removed groups should not be referenced
        var removedGroups = this.gridGroups.getDeletedList();
        if(removedGroups.length>0) {
            var clientList = this.gridRemoteClients.getList();
            for( i=0; i<removedGroups.length;i++) {
                for(var j=0; j<clientList.length;j++) {
                    if (removedGroups[i].groupId == clientList[j].groupId) {
                        Ext.MessageBox.alert(i18n._('Failed'),
                            Ext.String.format(i18n._("The group: {0} cannot be deleted because it is being used by the client: {1} in the Client To Site List."), removedGroups[i].name, clientList[j].name),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.gridGroups);
                            }, this)
                        );
                        return false;
                    }
                }
            }
        }

        // Group names must all be unique
        var groupNames = {};

        for( i=0;i<groups.length;i++) {
            var group = groups[i];
            var groupName = group.name.toLowerCase();

            if ( groupNames[groupName] != null ) {
                Ext.MessageBox.alert(i18n._('Failed'), Ext.String.format(i18n._("The group name: {0} in row: {1} already exists."), group.name, i+1),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridGroups);
                    }, this)
                );
                return false;
            }
            // Save the group name
            groupNames[groupName] = true;
        }
        return true;
    },

    validateVpnClients: function() {
        var clientList=this.gridRemoteClients.getList(false, true);
        var clientNames = {};

        for(var i=0;i<clientList.length;i++) {
            var client = clientList[i];
            var clientName = client.name.toLowerCase();

            if(client.internalId>=0 && client.name!=client.originalName) {
                Ext.MessageBox.alert(i18n._("Failed"), i18n._("Changing name is not allowed. Create a new user."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelClients);
                    }, this)
                );
                return false;
            }

            if ( clientNames[clientName] != null ) {
                Ext.MessageBox.alert(i18n._('Failed'),
                    Ext.String.format(i18n._("The client name: {0} in row: {1} already exists."), clientName, i),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.gridRemoteClients);
                    }, this)
                );
                return false;
            }
            clientNames[clientName] = true;
        }
        return true;
    },
    beforeSave: function(isApply, handler) {
        this.getSettings().groups.list = this.gridGroups.getList();
        this.getSettings().exports.list = this.gridExports.getList();
        this.getSettings().remoteClients.list = this.gridRemoteClients.getList();
        this.getSettings().remoteServers.list = this.gridRemoteServers.getList();
        handler.call(this, isApply);
    },
    afterSave: function() {
        // Assume the config state hasn't changed
        this.getGroupsStore(true);
        this.getDefaultGroupId(true);
        this.gridRemoteClients.emptyRow.groupId = this.getDefaultGroupId();

        Ext.getCmp( "openvpn_options_siteName" ).setValue( this.getSettings().siteName );
        Ext.getCmp( "openvpn_options_port" ).setValue( this.getSettings().port );
        Ext.getCmp( "openvpn_options_protocol" ).setValue( this.getSettings().protocol );
        Ext.getCmp( "openvpn_options_cipher" ).setValue( this.getSettings().cipher );
        Ext.getCmp( "openvpn_options_addressSpace" ).setValue( this.getSettings().addressSpace );
        Ext.getCmp( "openvpn_options_nat" ).setValue( this.getSettings().natOpenVpnTraffic );
    }
});
//# sourceURL=openvpn-settings.js
