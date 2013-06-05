if (!Ung.hasResource["Ung.OpenVPN"]) {
    Ung.hasResource["Ung.OpenVPN"] = true;
    Ung.NodeWin.registerClassName('untangle-node-openvpn', "Ung.OpenVPN");

    Ext.namespace('Ung');
    Ext.namespace('Ung.Node');
    Ext.namespace('Ung.Node.OpenVPN');

    Ext.define('Ung.Node.OpenVPN.DistributeClient', {
        extend: 'Ung.Window',
        constructor: function( config ) {
            this.title = config.i18n._('Distribute VPN Client');
            this.i18n = config.i18n;
            this.node = config.node;
            this.callParent(arguments);
        },
        initComponent: function() {
            this.bbar = ['->', {
                name: 'close',
                iconCls: 'cancel-icon',
                text: this.i18n._('Close'),
                handler: Ext.bind(this.close, this )
            }];
            this.items = {
                xtype: 'panel',
                items: [{
                    xtype: 'fieldset',
                    cls: "description",
                    title: this.i18n._('Download Config'),
                    labelWidth: 150,
                    items: [{
                        html: this.i18n._('These files can be used to configure your Remote Clients.'),
                        border: false,
                        cls: "description"
                    }, {
                        name: 'downloadWindowsInstaller',
                        html:  " ",
                        border: false,
                        height: 25,
                        cls: "description"
                    }, {
                        name: 'downloadGenericConfigurationFile',
                        html: " ",
                        border: false,
                        height: 25,
                        cls: "description"
                    }, {
                        name: 'downloadUntangleConfigurationFile',
                        html: " ",
                        border: false,
                        height: 25,
                        cls: "description"
                    }]
                }]
            };
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.hide();
        },
        populate: function( record ) {
            this.record = record;
            this.show();

            if(!this.downloadWindowsInstallerEl) {
                this.downloadWindowsInstallerEl = this.items.get(0).query('[name="downloadWindowsInstaller"]')[0].getEl();
            }
            this.downloadWindowsInstallerEl.dom.innerHTML = this.i18n._('Loading...');
            if(!this.downloadGenericConfigurationFileEl) {
                this.downloadGenericConfigurationFileEl = this.items.get(0).query('[name="downloadGenericConfigurationFile"]')[0].getEl();
            }
            this.downloadGenericConfigurationFileEl.dom.innerHTML = this.i18n._('Loading...');
            if(!this.downloadUntangleConfigurationFileEl) {
                this.downloadUntangleConfigurationFileEl = this.items.get(0).query('[name="downloadUntangleConfigurationFile"]')[0].getEl();
            }
            this.downloadUntangleConfigurationFileEl.dom.innerHTML = this.i18n._('Loading...');
            
            Ext.MessageBox.wait(this.i18n._( "Building OpenVPN Client..." ), this.i18n._( "Please Wait" ));
            // populate download links
            var loadSemaphore = 2;
            this.node.getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;

                this.downloadWindowsInstallerEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' +
                    this.i18n._('Click here to download this client\'s Windows setup.exe file.') + '</a>';

                loadSemaphore--;
                if(loadSemaphore == 0) {
                    Ext.MessageBox.hide();
                }
            }, this), this.record.data.name, "exe" );
            this.node.getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;

                this.downloadGenericConfigurationFileEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' +
                    this.i18n._('Click here to download this client\'s configuration zip file for other OSs (apple/linux/etc). ') + '</a>';
                this.downloadUntangleConfigurationFileEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' +
                    this.i18n._('Click here to download this client\'s configuration file for remote Untangle OpenVPN clients.') + '</a>';

                loadSemaphore--;
                if(loadSemaphore == 0) {
                    Ext.MessageBox.hide();
                }

            }, this), this.record.data.name, "zip" );
        }
    });

    Ext.define('Ung.OpenVPN', {
        extend:'Ung.NodeWin',
        groupsStore: null,
        panelStatus: null,
        panelRemoteServers: null,
        gridRemoteServers: null,
        panelRemoteClients: null,
        gridRemoteClients: null,
        panelExports: null,
        gridExports: null,
        panelGroups: null,
        gridGroups: null,
        panelGeneralOptions: null,
        gridConnectionEventLog: null,
        initComponent: function(container, position) {
            this.buildStatus();
            var tabs = [this.panelStatus];

            // Register the VTypes, need i18n to be initialized for the text
            if(Ext.form.VTypes["openvpnClientNameVal"]==null) {
                Ext.form.VTypes["openvpnClientNameVal"] = /^[A-Za-z0-9]([-_.0-9A-Za-z]*[0-9A-Za-z])?$/;
                Ext.form.VTypes["openvpnClientName"] = function(v) {
                    return Ext.form.VTypes["openvpnClientNameVal"].test(v);
                };
                Ext.form.VTypes["openvpnClientNameMask"] = /[-_.0-9A-Za-z]*/;
                Ext.form.VTypes["openvpnClientNameText"] = this.i18n._( "A client name should only contains numbers, letters, dashes and periods.  Spaces are not allowed." );
            }

            this.buildGeneralOptions();
            this.buildRemoteServers();
            this.buildRemoteClients();
            this.buildExports();
            this.buildGroups();
            this.buildConnectionEventLog();

            tabs.push(this.panelGeneralOptions);
            tabs.push(this.panelRemoteClients);
            tabs.push(this.panelGroups);
            tabs.push(this.panelExports);
            tabs.push(this.panelRemoteServers);
            tabs.push(this.gridConnectionEventLog);

            this.buildTabPanel(tabs);
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
                var defaultGroup = this.getGroupsStore().getCount()>0 ? this.getGroupsStore().getAt(0).data:null;
                this.defaultGroupId = defaultGroup == null ? null : defaultGroup.groupId;
            }
            return this.defaultGroupId;
        },
        // active connections/sessions grip
        buildActiveClientsGrid: function() {
            this.gridActiveClients = Ext.create('Ung.EditorGrid', {
                anchor: '100% -110',
                name: "gridActiveClients",
                settingsCmp: this,
                emptyRow: {
                    "address": "",
                    "clientName": "",
                    "start": "",
                    "bytesRxTotal": "",
                    "bytesTxTotal": ""
                },
                hasAdd: false,
                hasEdit: false,
                hasDelete: false,
                columnsDefaultSortable: true,
                title: this.i18n._("Connected Remote Clients"),
                qtip: this.i18n._("The Connected Remote Clients list shows connected clients."),
                paginated: false,
                bbar: Ext.create('Ext.toolbar.Toolbar', {
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
                                this.gridActiveClients.reload();
                            }, this)
                        }
                    ]
                }),
                recordJavaClass: "com.untangle.node.openvpn.OpenVpnStatusEvent",
                dataFn: this.getRpcNode().getActiveClients,
                fields: [{
                    name: "address"
                }, {
                    name: "clientName"
                }, {
                    name: "start"
                }, {
                    name: "bytesRxTotal"
                }, {
                    name: "bytesTxTotal"
                }, {
                    name: "id"
                }],
                columns: [{
                    header: this.i18n._("Address"),
                    dataIndex:'address',
                    width: 150
                }, {
                    header: this.i18n._("Client"),
                    dataIndex:'clientName',
                    width: 200,
                    flex: 1
                }, {
                    header: this.i18n._("Start Time"),
                    dataIndex:'start',
                    width: 180,
                    renderer: function(value) { return i18n.timestampFormat(value); }
                }, {
                    header: this.i18n._("Rx Data"),
                    dataIndex:'bytesRxTotal',
                    width: 180,
                    renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
                }, {
                    header: this.i18n._("Tx Data"),
                    dataIndex:'bytesTxTotal',
                    width: 180,
                    renderer: function(value) { return (Math.round(value/100000)/10) + " Mb"; }
                }]
            });
        },

        // Status panel
        buildStatus: function() {
            var statusLabel = "";
            this.buildActiveClientsGrid();

            var runState = this.getRpcNode().getRunState();
            var statusDescription = "";
            if (runState === "RUNNING") {
                statusDescription = "<font color=\"green\">" + this.i18n._("OpenVPN is currently running.") + "</font>";
            } else {
                statusDescription = "<font color=\"red\">" + this.i18n._("OpenVPN is not currently running.") + "</font>";
            }
            
            this.panelStatus = Ext.create('Ext.panel.Panel', {
                name: 'Status',
                helpSource: 'status',
                title: this.i18n._("Status"),
                parentId: this.getId(),
                layout: 'anchor',
                cls: 'ung-panel',
                isDirty: function() {
                    return false;
                },
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Status'),
                    items: [{
                        html: "<i>" + statusDescription + "</i>",
                        cls: 'description',
                        border: false
                    }]
                }, this.gridActiveClients]
            });
        },

        // Connections Event Log
        buildConnectionEventLog: function() {
            this.gridConnectionEventLog = Ext.create('Ung.GridEventLog', {
                settingsCmp: this,
                eventQueriesFn: this.getRpcNode().getStatusEventsQueries,
                name: "Event Log",
                title: i18n._('Event Log'),
                fields: [{
                    name: 'start',
                    mapping: 'start_time',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'end',
                    mapping: 'end_time',
                    sortType: Ung.SortTypes.asTimestamp
                }, {
                    name: 'name',
                    mapping: 'client_name'
                }, {
                    name: 'address',
                    mapping: 'remote_address'
                }, {
                    name: 'bytesTxTotal',
                    mapping: 'tx_bytes',
                    convert: function(val) {
                        return parseFloat(val) / 1024;
                    }
                }, {
                    name: 'bytesRxTotal',
                    mapping: 'rx_bytes',
                    convert: function(val) {
                        return parseFloat(val) / 1024;
                    }
                }],
                columns: [{
                    header: this.i18n._("Start Time"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: this.i18n._("End Time"),
                    width: Ung.Util.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: this.i18n._("Client Name"),
                    sortable: true,
                    dataIndex: 'name'
                }, {
                    header: this.i18n._("Client Address"),
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: this.i18n._("KB Sent"),
                    width: 80,
                    sortable: true,
                    dataIndex: 'bytesTxTotal',
                    renderer: Ext.bind(function( value ) {
                        return Math.round(( value + 0.0 ) * 10 ) / 10;
                    }, this )
                }, {
                    header: this.i18n._("KB Received"),
                    width: 80,
                    sortable: true,
                    dataIndex: 'bytesRxTotal',
                    renderer: Ext.bind(function( value ) {
                        return Math.round(( value + 0.0 ) * 10 ) / 10;
                    }, this )
                }]
            });
        },
        getDistributeColumn: function() {
            return Ext.create('Ext.grid.column.Action', {
                width: 110,
                header: this.i18n._("Distribute"),
                dataIndex: null,
                i18n: this.i18n,
                iconCls:'',
                handler: function(view, rowIndex, colIndex) {
                    var record = view.getStore().getAt(rowIndex);
                    view.ownerCt.distributeWindow.populate(record);
                },
                renderer: Ext.bind(function(value, metadata, record,rowIndex,colIndex,store,view) {
                    var out= '';
                    if(record.data.internalId>=0) {
                        //adding the x-action-col-0 class to force the processing of click event
                        out= '<div class="x-action-col-0 ung-button button-column" style="text-align:center;">' + this.i18n._("Distribute Client") + '</div>';
                    }
                    return out;
                }, this )
            });
        },
        getGroupsColumn: function() {
            return {
                header: this.i18n._("Group"),
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
            this.gridRemoteServers = Ext.create('Ung.EditorGrid', {
                hasAdd: false,
                settingsCmp: this,
                name: 'Remote Servers',
                sortable: true,
                paginated: false,
                anchor: "100% 40%",
                style: "margin-bottom:10px;",
                emptyRow: {
                    "enabled": true,
                    "name": this.i18n._("newServer")
                },
                title: this.i18n._("Remote Servers"),
                recordJavaClass: "com.untangle.node.openvpn.OpenVpnRemoteServer",
                dataProperty: "remoteServers",
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
                        header: this.i18n._("Enabled"),
                        dataIndex: 'enabled',
                        width: 80,
                        resizable: false
                    }, {
                        header: this.i18n._("Server Name"),
                        width: 130,
                        dataIndex: 'name',
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            maskRe: /[A-Za-z0-9-]/,
                            vtype: 'openvpnClientName'
                        }
                    }],
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: "textfield",
                            name: "Server name",
                            dataIndex: "name",
                            fieldLabel: this.i18n._("Server name"),
                            allowBlank: false,
                            maskRe: /[A-Za-z0-9-]/,
                            vtype: 'openvpnClientName',
                            width: 300
                        },{
                            xtype: 'label',
                            html: this.i18n._("only alphanumerics allowed") + " [A-Za-z0-9-]",
                            cls: 'boxlabel'
                        }]
                }]
            });
        },
        buildRemoteServers: function() {
            this.buildGridServers();

            this.submitForm = Ext.create('Ext.form.Panel', {
                border: false,
                xtype: 'fieldset',
                cls: 'description',
                items: [{
                        html: "<i>" + this.i18n._("Configure a new Remote Server connection") + "</i>",
                        cls: 'description',
                        border: false
                    }, {
                        xtype: 'fieldset',
                        buttonAlign: 'left',
                        labelWidth: 150,
                        labelAlign: 'right',
                        items: [{
                            xtype: 'filefield',
                            name: 'uploadConfigFileName',
                            fieldLabel: this.i18n._('Configuration File'),
                            allowBlank: false,
                            width: 300,
                            size: 50
                        }, {
                            xtype: 'button',
                            id: "submitUpload",
                            text: i18n._('Submit'),
                            name: "Submit",
                            handler: Ext.bind(function() {
                                var filename = this.submitForm.query('textfield[name="uploadConfigFileName"]')[0].getValue();
                                if ( filename == null || filename.length == 0 ) {
                                    Ext.MessageBox.alert(this.i18n._( "Select File" ), this.i18n._( "Please choose a file to upload." ));
                                    return;
                                }

                                this.submitForm.submit({
                                    url: "/openvpn/uploadConfig",
                                    success: Ext.bind(function( form, action, handler ) {
                                        Ext.MessageBox.alert(this.i18n._( "Success" ), this.i18n._( "The configuration has been imported." ));
                                    }, this),
                                    failure: Ext.bind(function( form, action ) {
                                        Ext.MessageBox.alert(this.i18n._( "Failure" ), this.i18n._( action.result.code ));
                                    }, this)
                                });
                                
                                
                            }, this)
                        }]
                    }]
                });

            this.panelRemoteServers = Ext.create('Ext.panel.Panel', {
                name: 'Servers',
                helpSource: 'servers',
                parentId: this.getId(),
                title: this.i18n._('Remote Servers'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Remote Servers'),
                    items: [{
                        html: "<i>" + this.i18n._("This is a list remote OpenVPN servers that OpenVPN should connect to as a client.") + "</i>",
                        cls: 'description',
                        border: false
                    }]
                }, this.gridRemoteServers, this.submitForm]
            });
        },
        
        buildGridClients: function() {
            this.gridRemoteClients = Ext.create('Ung.EditorGrid', {
                initComponent: function() {
                    this.distributeWindow = Ext.create('Ung.Node.OpenVPN.DistributeClient', {
                        i18n: this.settingsCmp.i18n,
                        node: this.settingsCmp.getRpcNode()
                    });
                    this.subCmps.push(this.distributeWindow);
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp: this,
                name: 'Remote Clients',
                sortable: true,
                paginated: false,
                anchor: "100% 75%",
                style: "margin-bottom:10px;",
                emptyRow: {
                    "enabled": true,
                    "name": this.i18n._("newClient"),
                    "groupId": this.getDefaultGroupId(),
                    "address": null,
                    "export":false,
                    "exportNetwork":null
                },
                title: this.i18n._("Remote Clients"),
                recordJavaClass: "com.untangle.node.openvpn.OpenVpnRemoteClient",
                dataProperty: "remoteClients",
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
                        header: this.i18n._("Enabled"),
                        dataIndex: 'enabled',
                        width: 80,
                        resizable: false
                    }, {
                        header: this.i18n._("Client Name"),
                        width: 130,
                        dataIndex: 'name',
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            maskRe: /[A-Za-z0-9-]/,
                            vtype: 'openvpnClientName'
                        }
                    },
                    this.getGroupsColumn(),
                    this.getDistributeColumn()],
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                        xtype: 'container',
                        layout: 'column',
                        margin: '0 0 5 0',
                        items: [{
                            xtype: "textfield",
                            name: "Client name",
                            dataIndex: "name",
                            fieldLabel: this.i18n._("Client name"),
                            allowBlank: false,
                            maskRe: /[A-Za-z0-9-]/,
                            vtype: 'openvpnClientName',
                            width: 300
                        },{
                            xtype: 'label',
                            html: this.i18n._("only alphanumerics allowed") + " [A-Za-z0-9-]",
                            cls: 'boxlabel'
                        }]
                }, {
                    xtype: "combo",
                    name: "Group",
                    dataIndex: "groupId",
                    fieldLabel: this.i18n._("Group"),
                    store: this.getGroupsStore(),
                    displayField: 'name',
                    valueField: 'groupId',
                    editable: false,
                    queryMode: 'local',
                    width: 300
                }]
            });
        },
        buildRemoteClients: function() {
            this.buildGridClients();
            this.panelRemoteClients = Ext.create('Ext.panel.Panel', {
                name: 'Clients',
                helpSource: 'clients',
                parentId: this.getId(),
                title: this.i18n._('Remote Clients'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Remote Clients'),
                    items: [{
                        html: "<i>" + this.i18n._("This is a list of remote OpenVPN clients allowed to connect to this server.") + "</i>",
                        cls: 'description',
                        border: false
                    }]
                }, this.gridRemoteClients]
            });
        },
        generateGridExports: function() {
            // live is a check column
            var exports=[];
            exports=this.getSettings().exports.list;

            var gridExports = Ext.create('Ung.EditorGrid', {
                settingsCmp: this,
                name: 'Exports',
                helpSource: 'exports',
                // the total records is set from the base settings
                sortable: true,
                paginated: false,
                anchor: "100% 75%",
                style: "margin-bottom:10px;",
                emptyRow: {
                    "enabled": true,
                    "name": this.i18n._("[no name]"),
                    "network": "192.168.1.0/24"
                },
                title: this.i18n._("Exported Networks"),
                recordJavaClass: "com.untangle.node.openvpn.OpenVpnExport",
                data: exports,
                // the list of fields
                fields: [{
                    name: 'enabled'
                }, {
                    name: 'name'
                }, {
                    name: 'network'
                }],
                autoExpandMin: 250,
                // the list of columns for the column model
                columns: [{
                        xtype:'checkcolumn',
                        header: this.i18n._("Enabled"),
                        dataIndex: 'enabled',
                        width: 80,
                        resizable: false
                    }, {
                        header: this.i18n._("Export Name"),
                        width: 150,
                        dataIndex: 'name',
                        editor: {
                            xtype:'textfield',
                            allowBlank: false
                        }
                    }, {
                        header: this.i18n._("Network"),
                        width: 150,
                        dataIndex: 'network',
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            vtype: 'cidrBlock'
                        }
                    }],
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "enabled",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                    xtype: "textfield",
                    name: "Export name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Export Name"),
                    allowBlank: false,
                    width: 300
                }, {
                    xtype: "textfield",
                    name: "Export network",
                    dataIndex: "network",
                    fieldLabel: this.i18n._("Network"),
                    allowBlank: false,
                    vtype: 'cidrBlock',
                    width: 300
                }]
            });
            return gridExports;
        },
        buildExports: function() {
            this.gridExports = this.generateGridExports();
            this.panelExports = Ext.create('Ext.panel.Panel', {
                name: 'Exports',
                helpSource: 'exports',
                parentId: this.getId(),
                title: this.i18n._('Exports'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Exports'),
                    items: [{
                        html: "<i>" + this.i18n._("The exported networks will be reachable by connected clients.") + "</i>",
                        cls: 'description',
                        border: false
                    }]
                }, this.gridExports]
            });
        },
        buildGroups: function() {
            this.gridGroups = this.generateGridGroups();
            this.panelGroups = Ext.create('Ext.panel.Panel', {
                name: 'Groups',
                helpSource: 'groups',
                parentId: this.getId(),
                title: this.i18n._('Groups'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Groups'),
                    items: [{
                        html: "<i>" + this.i18n._("This is the groups available for grouping clients by similar configuration.") + "</i>",
                        cls: 'description',
                        border: false
                    }]
                }, this.gridGroups]
            });
        },
        generateGridGroups: function() {
            var gridGroups = Ext.create('Ung.EditorGrid', {
                settingsCmp: this,
                name: 'Groups',
                // the total records is set from the base settings
                sortable: true,
                paginated: false,
                anchor: "100% 75%",
                style: "margin-bottom:10px;",
                addAtTop: false,
                emptyRow: {
                    "groupId": -1,
                    "name": this.i18n._("[no name]"),
                    "pushDns": false,
                    "fullTunnel": false,
                    "isDnsOverrideEnabled": false
                },
                title: this.i18n._("Groups"),
                recordJavaClass: "com.untangle.node.openvpn.OpenVpnGroup",
                dataProperty: 'groups',
                // the list of fields
                fields: [{
                    name: 'groupId'
                }, {
                    name: 'name'
                }, {
                    name: 'pushDns'
                }, {
                    name: 'fullTunnel'
                }, {
                    name: 'isDnsOverrideEnabled'
                }, {
                    name: 'dnsOverride1'
                }, {
                    name: 'dnsOverride2'
                }],
                // the list of columns for the column model
                columns: [{
                    header: this.i18n._("Group Name"),
                    width: 160,
                    dataIndex: 'name',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                },{
                    id: "fullTunnel",
                    header: this.i18n._("Full Tunnel"),
                    dataIndex: 'fullTunnel',
                    width: 90,
                    resizable: false
                },{
                    id: "pushDns",
                    header: this.i18n._("Push DNS"),
                    dataIndex: 'pushDns',
                    width: 90,
                    resizable: false
                },{
                    id: "isDnsOverrideEnabled",
                    header: this.i18n._("DNS Override"),
                    dataIndex: 'isDnsOverrideEnabled',
                    width: 90,
                    resizable: false
                }],
                // sortField: 'name',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: "textfield",
                    name: "Group Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Group Name"),
                    allowBlank: false,
                    width: 300
                }, {
                    xtype: 'checkbox',
                    name: "Full Tunnel",
                    dataIndex: "fullTunnel",
                    fieldLabel: this.i18n._("Full Tunnel")
                }, {
                    xtype: 'checkbox',
                    name: "Push DNS",
                    dataIndex: "pushDns",
                    fieldLabel: this.i18n._("Push DNS")
                }, {
                    xtype: 'checkbox',
                    name: "DNS Override",
                    dataIndex: "isDnsOverrideEnabled",
                    fieldLabel: this.i18n._("DNS Override"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                if (newValue) {
                                    Ext.getCmp('dnsOverride1').enable();
                                    Ext.getCmp('dnsOverride2').enable();
                                } else {
                                    Ext.getCmp('dnsOverride1').disable();
                                    Ext.getCmp('dnsOverride2').disable();
                                }
                            }, this)
                        },
                        "render": {
                            fn: Ext.bind(function(field) {
                                if (field.value) {
                                    Ext.getCmp('dnsOverride1').enable();
                                    Ext.getCmp('dnsOverride2').enable();
                                } else {
                                    Ext.getCmp('dnsOverride1').disable();
                                    Ext.getCmp('dnsOverride2').disable();
                                }
                            }, this),
                            scope: this
                        }
                    }
                }, {
                    xtype: "textfield",
                    id: "dnsOverride1",
                    name: "DNS Override 1",
                    dataIndex: "dnsOverride1",
                    fieldLabel: this.i18n._("DNS Override 1"),
                    allowBlank: false,
                    vtype: 'ipAddress',
                    width: 300
                }, {
                    xtype: "textfield",
                    id: "dnsOverride2",
                    name: "DNS Override 2",
                    dataIndex: "dnsOverride2",
                    fieldLabel: this.i18n._("DNS Override 2"),
                    allowBlank: false,
                    vtype: 'ipAddress',
                    width: 300
                }]
            });
            return gridGroups;
        },

        buildGeneralOptions: function() {
            this.panelGeneralOptions = Ext.create('Ext.panel.Panel', {
                name: 'GeneralOptions',
                helpSource: 'general',
                title: this.i18n._("Options"),
                parentId: this.getId(),

                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor:"98%",
                    xtype: 'fieldset',
                    buttonAlign: 'left'
                },
                items: [{
                    xtype: 'fieldset',
                    items: [{
                        xtype: 'textfield',
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Site Name'),
                        name: 'Site Name',
                        value: this.getSettings().siteName,
                        id: 'openvpn_options_siteName',
                        allowBlank: false,
                        blankText: this.i18n._("You must enter a site name."),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().siteName = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'checkbox',
                        labelWidth: 160,
                        name: "Server Enabled",
                        fieldLabel: this.i18n._("Server Enabled"),
                        checked: this.getSettings().serverEnabled,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().serverEnabled = newValue;

                                    Ext.getCmp('grid_remote_clients').disable();
                                    Ext.getCmp('openvpn_options_port').disable();
                                    Ext.getCmp('openvpn_options_protocol').disable();
                                    Ext.getCmp('openvpn_options_cipher').disable();
                                    Ext.getCmp('openvpn_options_addressSpace').disable();
                                    if (newValue) {
                                        Ext.getCmp('grid_remote_clients').enable();
                                        Ext.getCmp('openvpn_options_port').enable();
                                        Ext.getCmp('openvpn_options_protocol').enable();
                                        Ext.getCmp('openvpn_options_cipher').enable();
                                        Ext.getCmp('openvpn_options_addressSpace').enable();
                                    }
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'textfield',
                        hidden: true, /* HIDDEN */
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Port'),
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
                        xtype: 'textfield',
                        hidden: true, /* HIDDEN */
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Protocol'),
                        name: 'Protocol',
                        value: this.getSettings().protocol,
                        id: 'openvpn_options_protocol',
                        allowBlank: false,
                        vtype: "port",
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().protocol = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'textfield',
                        hidden: true, /* HIDDEN */
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Cipher'),
                        name: 'Cipher',
                        value: this.getSettings().cipher,
                        id: 'openvpn_options_cipher',
                        allowBlank: false,
                        vtype: "port",
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().cipher = newValue;
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'textfield',
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Address Space'),
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
                        xtype: 'checkbox',
                        hidden: true, /* HIDDEN */
                        labelWidth: 160,
                        name: "NAT OpenVPN Traffic",
                        fieldLabel: this.i18n._("NAT All OpenVPN Traffic"),
                        checked: this.getSettings().natOpenVpnTraffic,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.getSettings().natOpenVpnTraffic = newValue;
                                }, this)
                            }
                        }
                    }]
                }]
            });
        },

        // validation function
        validate: function() {
            return  this.validateGeneralOptions() && this.validateGroups() && this.validateVpnClients();
        },

        //validate OpenVPN GeneralOptions settings
        validateGeneralOptions: function() {
            //validate site name
            var siteCmp = Ext.getCmp("openvpn_options_siteName");
            if(!siteCmp.validate()) {
                Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must enter a site name."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelGeneralOptions);
                        siteCmp.focus(true);
                    }, this)
                );
                return false;
            }
            return true;
        },

        validateGroups: function() {
            var i;
            var groups=this.gridGroups.getPageList(false, true);

            // verify that there is at least one group
            if(groups.length <= 0 ) {
                Ext.MessageBox.alert(this.i18n._('Failed'), this.i18n._("You must create at least one group."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelGroups);
                    }, this)
                );
                return false;
            }

            // removed groups should not be referenced
            var removedGroups = this.gridGroups.getDeletedList();
            if(removedGroups.length>0) {
                var clientList = this.gridRemoteClients.getPageList();
                for( i=0; i<removedGroups.length;i++) {
                    for(var j=0; j<clientList.length;j++) {
                        if (removedGroups[i].groupId == clientList[j].groupId) {
                            Ext.MessageBox.alert(this.i18n._('Failed'),
                                Ext.String.format(this.i18n._("The group: \"{0}\" cannot be deleted because it is being used by the client: {1} in the Client To Site List."), removedGroups[i].name, clientList[j].name),
                                Ext.bind(function () {
                                    this.tabs.setActiveTab(this.panelGroups);
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
                    Ext.MessageBox.alert(this.i18n._('Failed'), Ext.String.format(this.i18n._("The group name: \"{0}\" in row: {1} already exists."), group.name, i+1),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelGroups);
                        }, this));
                    return false;
                }

                // Save the group name
                groupNames[groupName] = true;
            }

            return true;
        },

        validateVpnClients: function() {
            var clientList=this.gridRemoteClients.getPageList(false, true);
            var clientNames = {};

            for(var i=0;i<clientList.length;i++) {
                var client = clientList[i];
                var clientName = client.name.toLowerCase();

                if(client.internalId>=0 && client.name!=client.originalName) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Changing name is not allowed. Create a new user."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelClients);
                        }, this)
                    );
                    return false;
                }
                
                if ( clientNames[clientName] != null ) {
                    Ext.MessageBox.alert(this.i18n._('Failed'),
                                         Ext.String.format(this.i18n._("The client name: \"{0}\" in row: {1} already exists."), clientName, i),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelRemoteClients);
                                         }, this)
                                        );
                    return false;
                }
                clientNames[clientName] = true;
            }
            return true;
        },

        validateExports: function(exportList) {
            return true;
        },

        save: function(isApply) {
            this.getSettings().groups.list = this.gridGroups.getPageList();
            this.getSettings().exports.list = this.gridExports.getPageList();
            this.getSettings().remoteClients.list = this.gridRemoteClients.getPageList();
            this.getSettings().remoteServers.list = this.gridRemoteServers.getPageList();

            this.getRpcNode().setSettings(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.afterSave(isApply);
            }, this), this.getSettings());
        },

        afterSave: function(isApply) {
            Ext.MessageBox.hide();
            if (!isApply) {
                this.closeWindow();
            } else {
                Ext.MessageBox.wait(i18n._("Reloading..."), i18n._("Please wait"));
                this.getSettings(function() {
                    // Assume the config state hasn't changed
                    this.getGroupsStore(true);
                    this.getDefaultGroupId(true);
                    this.gridRemoteClients.emptyRow.groupId = this.getDefaultGroupId();

                    this.gridExports.reload({data: this.getSettings().exports.list });

                    Ext.getCmp( "openvpn_options_siteName" ).setValue( this.getSettings().siteName );
                    Ext.getCmp( "openvpn_options_port" ).setValue( this.getSettings().port );
                    Ext.getCmp( "openvpn_options_protocol" ).setValue( this.getSettings().protocol );
                    Ext.getCmp( "openvpn_options_cipher" ).setValue( this.getSettings().cipher );
                    Ext.getCmp( "openvpn_options_addressSpace" ).setValue( this.getSettings().addressSpace );

                    this.clearDirty();
                    Ext.MessageBox.hide();
                });
            }
        }
    });
}
//@ sourceURL=openvpn-settings.js
