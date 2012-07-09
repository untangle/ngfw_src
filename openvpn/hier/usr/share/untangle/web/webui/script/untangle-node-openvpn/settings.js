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
            var panelItems = null;
            if (this.isVpnSite) {
                panelItems = {
                    xtype: 'fieldset',
                    title: this.i18n._('Download Client Config'),
                    autoHeight: true,
                    labelWidth: 150,
                    items: [{
                        html: this.i18n._('This configuration file can be used to configure the remote VPN Site.'),
                        border: false,
                        cls: "description"
                    }, {
                        name: 'downloadConfigurationFile',
                        html: " ",
                        border: false,
                        height: "25",
                        cls: "description"
                    }]
                };
            } else {
                panelItems = [{
                    xtype: 'fieldset',
                    title: this.i18n._('Download Client Config'),
                    autoHeight: true,
                    labelWidth: 150,
                    items: [{
                        html: this.i18n._('These files can be used to configure your remote VPN Clients.'),
                        border: false,
                        cls: "description"
                    }, {
                        name: 'downloadWindowsInstaller',
                        html:  " ",
                        border: false,
                        height: "25",
                        cls: "description"
                    }, {
                        name: 'downloadConfigurationFile',
                        html: " ",
                        border: false,
                        height: "25",
                        cls: "description"
                    }]
                }, {
                    xtype: 'fieldset',
                    title: this.i18n._('or Distribute Client Config via Email'),
                    autoHeight: true,
                    items: [{
                        html: this.i18n._('Click "Send Email" to send an email to "Email Address" with information to retrieve the OpenVPN Client.'),
                        border: false,
                        cls: "description",
                        labelWidth: 150,
                        labelAlign:'left'
                    }, {
                        html: this.i18n._('Note: Clients can only download the client linked in the email if HTTPS remote administration is enabled!'),
                        border: false,
                        bodyStyle: 'paddingBottom:10px',
                        cls: 'description warning',
                        labelWidth: 150,
                        labelAlign:'left'

                    }, {
                        xtype: 'textfield',
                        fieldLabel: this.i18n._('Email Address'),
                        labelWidth: 150,
                        labelAlign:'left',
                        width: 480,
                        vtype: 'email',
                        name: 'emailAddress'
                    }, {
                        xtype: 'button',
                        text: this.i18n._( "Send Email" ),
                        name: 'sendEmail',
                        labelWidth: 150,
                        labelAlign:'left',
                        handler: Ext.bind(this.sendEmail, this )
                    }]
                }]
            }
            this.items = {
                xtype: 'panel',
                cls: 'ung-panel',
                items: panelItems
            }
            this.callParent(arguments);
        },
        closeWindow: function() {
            this.hide();
        },
        sendEmail: function() {
            // Determine the email address
            if(!this.emailAddressBox) {
                this.emailAddressBox = this.items.get(0).query('textfield[name="emailAddress"]')[0];
            }

            var emailAddress = this.emailAddressBox.getValue();

            if ( emailAddress == null || emailAddress.length == 0 || !this.emailAddressBox.isValid()) {
                Ext.MessageBox.alert( this.i18n._("Failure"), this.i18n._("You must specify valid an email address to send the key to."));
                return;
            }

            this.record.data.distributionEmail = emailAddress;

            Ext.MessageBox.wait( this.i18n._( "Distributing Client Installer..." ), this.i18n._( "Please wait" ));

            this.node.distributeClientConfig(Ext.bind(function( result, exception ) {
                if (exception) {
                    Ext.MessageBox.alert( this.i18n._("Error saving/sending key"),
                                          this.i18n._("OpenVPN was not able to send your client installer via email.  Please try again." ));
                    return;
                }

                // go to next step
                Ext.MessageBox.alert( this.i18n._("Success"),
                                      this.i18n._("OpenVPN successfully sent your Client Installer via email."),
                                      Ext.bind(this.close, this ));
            }, this), this.record.data);
        },
        populate: function( record ) {
            this.record = record;
            this.show();
            if(!this.downloadConfigurationFileEl) {
                this.downloadConfigurationFileEl = this.items.get(0).query('[name="downloadConfigurationFile"]')[0].getEl();
            }
            this.downloadConfigurationFileEl.dom.innerHTML = this.i18n._('Loading...');
            if (!this.isVpnSite) {
                if(!this.downloadWindowsInstallerEl) {
                    this.downloadWindowsInstallerEl = this.items.get(0).query('[name="downloadWindowsInstaller"]')[0].getEl();
                }
                this.downloadWindowsInstallerEl.dom.innerHTML = this.i18n._('Loading...');
            }
            Ext.MessageBox.wait(this.i18n._( "Building OpenVPN Client..." ), this.i18n._( "Please Wait" ));
            // populate download links
            if (this.isVpnSite) {
                this.node.getAdminDownloadLink(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.downloadConfigurationFileEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' + this.i18n._('Download VPN Site configuration.') + '</a>';
                    Ext.MessageBox.hide();
                }, this), this.record.data.name, "ZIP" );
            } else {
                var loadSemaphore = 2;
                this.node.getAdminDownloadLink(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.downloadWindowsInstallerEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' + this.i18n._('Click here to download an installer for Windows clients.') + '</a>';
                    loadSemaphore--;
                    if(loadSemaphore == 0) {
                        Ext.MessageBox.hide();
                    }
                }, this), this.record.data.name, "SETUP_EXE" );
                this.node.getAdminDownloadLink(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.downloadConfigurationFileEl.dom.innerHTML = '<a href="' + result + '" target="_blank">' + this.i18n._('Click here to download a configuration file for all OSs.') + '</a>';
                    loadSemaphore--;
                    if(loadSemaphore == 0) {
                        Ext.MessageBox.hide();
                    }
                }, this), this.record.data.name, "ZIP" );

            }
        }
    });

    // Namespace for the openvpn client configuration cards.
    Ext.namespace('Ung.Node.OpenVPN.ClientWizard');
    Ext.define('Ung.Node.OpenVPN.ClientWizard.Welcome', {
        constructor: function( config )
        {
            this.i18n = config.i18n;
            this.node = config.node;

            this.title = this.i18n._("Welcome"),
            this.panel = Ext.create('Ext.form.Panel', {
                border: false,
                items: [{
                    xtype: 'label',
                    html: '<h2 class="wizard-title">'+this.i18n._("Welcome to the OpenVPN Setup Wizard!")+'</h2>'
                }, {
                    html: this.i18n._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Client.'),
                    cls: 'description',
                    bodyStyle: 'padding-bottom:10px',
                    border: false
                }, {
                    html: this.i18n._('Warning: Completing this wizard will overwrite any previous OpenVPN settings with new settings. All previous settings will be lost!'),

                    cls: 'description warning',
                    border: false
                }]
            });
        }
    });

    Ext.define('Ung.Node.OpenVPN.ClientWizard.LoadConfig', {
        constructor: function( config ) {
            this.i18n = config.i18n;
            this.node = config.node;

            this.title = this.i18n._( "Install Configuration" );

            this.panel = Ext.create('Ext.form.Panel', {
                border: false,
                fileUpload: true,
                items: [{
                    xtype: 'label',
                    html: '<h2 class="wizard-title">'+this.i18n._("Download Configuration")+'</h2>'
                }, {
                    cls: 'description',
                    border: false,
                    html: this.i18n._('Upload the Client Configuration file downloaded from the VPN Server.')
                }, {
                    xtype: 'fieldset',
                    autoHeight: true,
                    buttonAlign: 'left',
                    labelWidth: 150,
                    labelAlign: 'right',
                    items: [{
                        xtype: 'textfield',
                        inputType: 'file',
                        name: 'siteConfiguration',
                        fieldLabel: 'Configuration File',
                        allowBlank: false,
                        width: 500,
                        size: 50
                    }]
                }]
            });

            this.onNext = Ext.bind(this.retrieveClient, this );
            this.onValidate = Ext.bind(this.validateSettings, this );
        },

        validateSettings: function() {
            if ( !_validate( this.panel.query('textfield[name="siteConfiguration"]'))) return false;

            return true;
        },

        validateServerAddress: function( value ) {
            var sarr = value.split( ":" );
            if ( sarr.length > 2 ) return this.i18n._( "Invalid Server Address" );

            /* Could check if the address is of the form <ip>|<hostname>:[<port>] */
            return true;
        },

        retrieveClient: function( handler ) {
            return this.uploadClient( handler );
        },

        uploadClient: function( handler ) {
            Ext.MessageBox.wait(this.i18n._("Uploading Configuration... (This may take a minute)"),
                                this.i18n._("Please wait"));

            this.node.getAdminClientUploadLink( Ext.bind(this.completeGetAdminClientUploadLink, this, [handler], true ));
        },

        completeGetAdminClientUploadLink: function( result, exception, foo, handler ) {
            if(Ung.Util.handleException(exception,Ext.bind(function() {
                Ext.MessageBox.alert( this.i18n._("Failed"), this.i18n._( "Unable to load VPN Client" ));
            }, this),"noAlert")) return;

            var file = this.panel.query('textfield[name="siteConfiguration"]')[0].getValue();

            /* This should have already happened in onValidate */
            if ( file == null || file.length == 0 ) {
                Ext.MessageBox.alert(this.i18n._( "Select File" ), this.i18n._( "Please choose a file to upload." ));
                return;
            }

            this.panel.getForm().submit({
                url: result,
                success: Ext.bind(this.completeUploadClient, this, [ handler ], true ),
                failure: Ext.bind(this.failUploadClient, this ),
                clientValidation: false
            });
        },

        /** action.result["success"] = true will trigger success in extjs for a form submit. */
        completeUploadClient: function( form, action, handler ) {
            this.node.completeConfig( Ext.bind(this.c_completeConfig, this, [ handler ], true ));
        },

        /** action.result["success"] = false will trigger failure in extjs. */
        failUploadClient: function( form, action )
        {
            var response = action.result;

            var message = this.i18n._("Unable to install OpenVPN client.");
            if ( response.code == 'connect' || response.code == 'unknown' ) {
                message = this.i18n._("Unable to verify connection to server.");
            } else if ( response.code == 'invalid file' ) {
                message = this.i18n._("The selected configuration file is invalid.");
            }

            Ext.MessageBox.alert(this.i18n._( "Failed" ), message );
        },

        c_completeConfig: function( result, exception, foo, handler ) {
            if(Ung.Util.handleException(exception,Ext.bind(function() {
                Ext.MessageBox.alert( this.i18n._("Failed"), this.i18n._( "Unable to load VPN Client" ));
            }, this),"noAlert")) return;

            // go to next step
            Ext.MessageBox.alert( this.i18n._("Success"),
                                  this.i18n._("Installed OpenVPN client."));

            handler();
        }
    });

    Ext.define('Ung.Node.OpenVPN.ClientWizard.Congratulations', {
        constructor: function( config ) {
            this.i18n = config.i18n;
            this.node = config.node;
            this.gui = config.gui;

            this.title = this.i18n._( "Finished!" );
            this.panel = Ext.create('Ext.form.Panel', {
                border: false,
                items: [{
                    xtype: 'label',
                    html: '<h2 class="wizard-title">'+this.i18n._("Finished!")+'</h2>'
                }, {
                    cls: 'description',
                    border: false,
                    html: this.i18n._('Congratulations! OpenVPN is configured as a VPN Client.')
                }]
            });

            this.onNext = Ext.bind(this.completeWizard, this );
        },

        completeWizard: function( handler ) {
            this.gui.node.start();
            this.gui.clientSetup.endAction();
        }
    });

    Ext.define('Ung.OpenVPN', {
        extend:'Ung.NodeWin',
        configState: null,
        groupsStore: null,
        panelStatus: null,
        panelClients: null,
        gridClients: null,
        gridSites: null,
        gridExports: null,
        gridGroups: null,
        panelAdvanced: null,
        gridConnectionEventLog: null,
        initComponent: function(container, position) {
            this.configState=this.getRpcNode().getConfigState();
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

            if (this.configState == "SERVER") {
                this.buildClients();
                this.buildExports();
                this.buildAdvanced();
                this.buildConnectionEventLog();
                tabs.push(this.panelClients);
                tabs.push(this.gridExports);
                tabs.push(this.panelAdvanced);
                tabs.push(this.gridConnectionEventLog);
            } else if (this.configState == "CLIENT") {
                this.buildConnectionEventLog();
                tabs.push(this.gridConnectionEventLog);
            }
            this.buildTabPanel(tabs);
            this.callParent(arguments);
        },
        getVpnServerAddress: function(forceReload) {
            if (forceReload || this.rpc.vpnServerAddress === undefined) {
                this.rpc.vpnServerAddress = this.getRpcNode().getVpnServerAddress();
            }
            return this.rpc.vpnServerAddress;
        },
        getExportedAddressList: function(forceReload) {
            if (forceReload || this.rpc.exportedAddressList === undefined) {
                this.rpc.exportedAddressList = this.getRpcNode().getExportedAddressList();
            }
            return this.rpc.exportedAddressList;
        },
        getGroupsStore: function(force) {
            if (this.groupsStore == null ) {
                this.groupsStore = Ext.create('Ext.data.Store', {
                    fields: ['id', 'name','javaClass'],
                    data: this.getSettings().groupList.list
                });
                force = false;
            }

            if(force) {
                this.groupsStore.loadData( this.getSettings().groupList.list );
            }

            return this.groupsStore;
        },
        getDefaultGroupName: function(forceReload) {
            if (forceReload || this.defaultGroupName === undefined) {
                var defaultGroup = this.getGroupsStore().getCount()>0 ? this.getGroupsStore().getAt(0).data:null;
                this.defaultGroupName = defaultGroup==null ? null: defaultGroup.name;
            }
            return this.defaultGroupName;
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
                title: this.i18n._("Active Clients"),
                qtip: this.i18n._("The Active Clients list shows connected clients."),
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
                recordJavaClass: "com.untangle.node.openvpn.ClientStatusEvent",
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
                    header: this.i18n._("Rx Bytes"),
                    dataIndex:'bytesRxTotal',
                    width: 180
                }, {
                    header: this.i18n._("Tx Bytes"),
                    dataIndex:'bytesTxTotal',
                    width: 180
                }]
            });
        },

        // Status panel
        buildStatus: function() {
            var statusLabel = "";
            var serverButtonDisabled = false;
            var clientButtonDisabled = false;
            var wizardVisible = true;
            if (this.configState == "UNCONFIGURED") {
                statusLabel = this.i18n._("Unconfigured: Use buttons below.");
                wizardVisible = true;
            } else if (this.configState == "CLIENT") {
                statusLabel = Ext.String.format(this.i18n._("VPN Client: Connected to {0}"), this.getVpnServerAddress());
                clientButtonDisabled = true;
                serverButtonDisabled = true;
                wizardVisible = false;
            } else if (this.configState == "SERVER") {
                statusLabel = this.i18n._("VPN Server");
                clientButtonDisabled = true;
                serverButtonDisabled = true;
                wizardVisible = false;
            } else {
                clientButtonDisabled = true;
                serverButtonDisabled = true;
                wizardVisible = false;
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
                defaults: {
                    xtype: 'fieldset',
                },
                items: [{
                    xtype: 'fieldset',
                    cls: 'description',
                    title: this.i18n._('Current Mode'),
                    items: [{
                        xtype: "textfield",
                        name: "Mode",
                        readOnly: true,
                        fieldLabel: this.i18n._("Mode"),
                        width: 350,
                        value: statusLabel
                    }, {
                        html: "<i>" + this.i18n._("To reconfigure the current OpenVPN mode remove the application from the rack and reinstall.") + "</i>",
                        cls: 'description',
                        border: false,
                        hidden: wizardVisible
                    }]
                }, {
                    title: this.i18n._('Wizard'),
                    layout: {
                        type:'table',
                        columns:1
                    },
                    hidden: !wizardVisible,
                    items: [{
                        xtype:'fieldset',
                        items:[{
                            xtype: "button",
                            name: 'Configure as VPN Server',
                            text: this.i18n._("Configure as VPN Server"),
                            iconCls: "action-icon",
                            disabled: serverButtonDisabled,
                            handler: Ext.bind(function() {
                                this.getRpcNode().startConfig( Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    this.configureVPNServer();
                                }, this), "SERVER");
                            }, this)
                        }, {
                            html: this.i18n._("This configures OpenVPN so remote users and networks can connect and access exported hosts and networks."),
                            bodyStyle: 'padding:10px 10px 10px 10px;',
                            cls: 'description',
                            border: false
                        }]
                    }, {
                        xtype:'fieldset',
                        items:[{
                            xtype: "button",
                            name: 'Configure as VPN Client',
                            text: this.i18n._("Configure as VPN Client"),
                            iconCls: "action-icon",
                            disabled: clientButtonDisabled,
                            handler: Ext.bind(function() {
                                    this.getRpcNode().startConfig(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    this.configureVPNClient();
                                }, this), "CLIENT");
                            }, this)
                        }, {
                            html: this.i18n._("This configures OpenVPN so it connects to a remote OpenVPN Server and can access exported hosts and networks."),
                            bodyStyle: 'padding:10px 10px 10px 10px;',
                            cls: 'description',
                            border: false
                        }]
                }]
            }]});

            if (this.configState == "SERVER") {
                this.buildActiveClientsGrid();
                this.panelStatus.add( this.gridActiveClients );
            }
        },

        // Connections Event Log
        buildConnectionEventLog: function() {
            this.gridConnectionEventLog = Ext.create('Ung.GridEventLog', {
                settingsCmp: this,
                eventQueriesFn: this.getRpcNode().getStatusEventsQueries,
                name: "Connection Event Log",
                title: i18n._('Connection Event Log'),
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
                    name: 'port',
                    mapping: 'remote_port'
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
                    header: this.i18n._("Port"),
                    dataIndex: 'port'
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
                    if(record.data.id>=0) {
                        //adding the x-action-col-0 class to force the processing of click event
                        out= '<div class="x-action-col-0 ung-button button-column" style="text-align:center;">' + this.i18n._("Distribute Client") + '</div>';
                    }
                    return out;
                }, this )

            });
        },
        getGroupsColumn: function() {
            return {
                header: this.i18n._("Address Pool"),
                width: 160,
                dataIndex: 'groupName',
                editor: Ext.create('Ext.form.ComboBox', {
                    store: this.getGroupsStore(),
                    displayField: 'name',
                    valueField: 'name',
                    editable: false,
                    mode: 'local',
                    triggerAction: 'all',
                    listClass: 'x-combo-list-small'
                })
            };
        },
        buildGridClients: function() {
            this.gridClients = Ext.create('Ung.EditorGrid', {
                initComponent: function() {
                    this.distributeWindow = Ext.create('Ung.Node.OpenVPN.DistributeClient', {
                        i18n: this.settingsCmp.i18n,
                        node: this.settingsCmp.getRpcNode()
                    });
                    this.subCmps.push(this.distributeWindow);
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp: this,
                name: 'VPN Clients',
                sortable: true,
                paginated: false,
                anchor: "100% 48%",
                style: "margin-bottom:10px;",
                emptyRow: {
                    "live": true,
                    "name": this.i18n._("newClient"),
                    "groupName": this.getDefaultGroupName(),
                    "address": null
                },
                title: this.i18n._("VPN Clients"),
                recordJavaClass: "com.untangle.node.openvpn.VpnClient",
                dataProperty: "clientList",
                autoGenerateId: true,
                fields: [{
                    name: 'live'
                }, {
                    name: 'name'
                }, {
                    name: 'originalName',
                    mapping: 'name'
                }, {
                    name: 'groupName'
                }, {
                    name: 'address'
                }],
                columns: [{
                        xtype:'checkcolumn',
                        header: this.i18n._("Anabled"),
                        dataIndex: 'live',
                        width: 80,
                        fixed: true
                    }, {
                        header: this.i18n._("Client Name"),
                        width: 130,
                        dataIndex: 'name',
                        flex:1,
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            vtype: 'openvpnClientName'
                        }
                    },
                    this.getGroupsColumn(),
                    this.getDistributeColumn(),
                    {
                        header: this.i18n._("Virtual Address"),
                        width: 100,
                        dataIndex: 'address',
                        renderer: Ext.bind(function(value, metadata, record) {
                            return value==null ? this.i18n._("unassigned"): value;
                        }, this)
                    }],
                //sortField: 'name',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                    xtype: "textfield",
                    name: "Client name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Client name"),
                    allowBlank: false,
                    vtype: 'openvpnClientName',
                    width: 300
                }, {
                    xtype: "combo",
                    name: "Address pool",
                    dataIndex: "groupName",
                    fieldLabel: this.i18n._("Address pool"),
                    store: this.getGroupsStore(),
                    displayField: 'name',
                    valueField: 'name',
                    editable: false,
                    mode: 'local',
                    triggerAction: 'all',
                    listClass: 'x-combo-list-small',
                    width: 300
                }]
            });
        },
        buildGridSites: function() {
            this.gridSites = Ext.create('Ung.EditorGrid', {
                initComponent: function() {
                    this.distributeWindow = Ext.create('Ung.Node.OpenVPN.DistributeClient', {
                        i18n: this.settingsCmp.i18n,
                        node: this.settingsCmp.getRpcNode(),
                        isVpnSite: true
                    });
                    this.subCmps.push(this.distributeWindow);
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp: this,
                name: 'VPN Sites',
                // the total records is set from the base settings
                paginated: false,
                anchor: "100% 48%",
                emptyRow: {
                    "live": true,
                    "name": this.i18n._("newSite"),
                    "groupName": this.getDefaultGroupName(),
                    "network": "1.2.3.4",
                    "netmask": "255.255.255.0",
                    "exportedAddressList": {
                        javaClass: "java.util.ArrayList",
                        list:[{
                            javaClass:"com.untangle.node.openvpn.SiteNetwork",
                            "network": "1.2.3.4",
                            "netmask": "255.255.255.0"
                        }]
                    }
                },
                title: this.i18n._("VPN Sites"),
                recordJavaClass: "com.untangle.node.openvpn.VpnSite",
                dataRoot: '',
                dataFn: Ext.bind( function() {
                    var data = this.getSettings().siteList.list;
                    if( data != null ) {
                        for(var i=0; i<data.length; i++) {
                            if(data[i].exportedAddressList==null || data[i].exportedAddressList.list==null || data[i].exportedAddressList.list.length==0) {
                                data[i].exportedAddressList = {
                                    javaClass: "java.util.ArrayList",
                                    list:[{
                                        javaClass:"com.untangle.node.openvpn.SiteNetwork",
                                        "network": "1.2.3.4",
                                        "netmask": "255.255.255.0"
                                    }]
                                };
                            }
                            data[i].network = data[i].exportedAddressList.list[0].network;
                            data[i].netmask = data[i].exportedAddressList.list[0].netmask;
                        }
                    }
                    return data;
                }, this),
                getPageList: function () {
                    var data = Ung.EditorGrid.prototype.getPageList.call(this);
                    for(var i=0; i<data.length; i++) {
                        data[i].exportedAddressList.list[0].network = data[i].network;
                        data[i].exportedAddressList.list[0].netmask = data[i].netmask;
                    }
                    return data;
                },
                // the list of fields
                autoGenerateId: true,
                fields: [{
                    name: 'live'
                }, {
                    name: 'name'
                }, {
                    name: 'exportedAddressList'
                }, {
                    name: 'network'
                }, {
                    name: 'netmask'
                }, {
                    name: 'groupName'
                }],
                // the list of columns for the column model
                columns: [{
                    xtype:'checkcolumn',
                    header: this.i18n._("Enabled"),
                    dataIndex: 'live',
                    width: 80,
                    fixed: true
                }, {
                    header: this.i18n._("Site Name"),
                    width: 130,
                    dataIndex: 'name',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype: 'openvpnClientName'
                    }
                },
                this.getGroupsColumn(),
                {
                    header: this.i18n._("Network Address"),
                    width: 100,
                    dataIndex: 'network',
                    renderer: Ext.bind(function(value, metadata, record) {
                        record.data.exportedAddressList.list[0].network=value;
                        return value;
                    }, this ),
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype: 'ipAddress'
                    }
                }, {
                    header: this.i18n._("Network Mask"),
                    width: 100,
                    dataIndex: 'netmask',
                    renderer: Ext.bind(function(value, metadata, record) {
                                    record.data.exportedAddressList.list[0].netmask=value;
                                    return value;
                    }, this ),
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype: 'ipAddress'
                    }
                },
                this.getDistributeColumn()],
                // sortField: 'name',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                    xtype: "textfield",
                    name: "site name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Site Name"),
                    allowBlank: false,
                    vtype: 'openvpnClientName',
                    width: 300
                }, {
                    xtype: "combo",
                    name: "Address Pool",
                    dataIndex: "groupName",
                    fieldLabel: this.i18n._("Address Pool"),
                    allowBlank: false,
                    displayField: 'name',
                    valueField: 'name',
                    store: this.getGroupsStore(),
                    editable: false,
                    mode: 'local',
                    triggerAction: 'all',
                    width: 300
                }, {
                    cls: "description",
                    border: false,
                    html: this.i18n._("Traffic to the below network will be routed over the VPN to the remote site.") + "<br/>" +
                        this.i18n._("The remote site network IP/netmask must be separate from the local network IP/netmask.") + "<br/>"
                }, {
                    xtype: "textfield",
                    name: "Network address",
                    dataIndex: "network",
                    fieldLabel: this.i18n._("Network address"),
                    boxLabel: this.i18n._("Internal address of remote site (eg: 192.168.1.1)"),
                    allowBlank: false,
                    width: 300,
                    vtype: 'ipAddress'
                }, {
                    xtype: "textfield",
                    name: "Network mask",
                    dataIndex: "netmask",
                    fieldLabel: this.i18n._("Network mask"),
                    boxLabel: this.i18n._("Internal netmask of remote site (eg: 255.255.255.0)"),
                    allowBlank: false,
                    width: 300,
                    vtype: 'ipAddress'
                }]
            });
        },
        buildClients: function() {
            this.buildGridClients();
            this.buildGridSites();
            this.panelClients = Ext.create('Ext.panel.Panel', {
                // private fields
                name: 'Clients',
                helpSource: 'clients',
                parentId: this.getId(),
                title: this.i18n._('Clients'),
                layout: 'anchor',
                cls: 'ung-panel',
                items: [this.gridClients, this.gridSites]
            });
        },
        generateGridExports: function(inWizard) {
            // live is a check column
            var exportedAddressList=[];
            if(inWizard) {
                if (this.getExportedAddressList()!=null) {
                    exportedAddressList=this.getExportedAddressList().exportList.list;
                }
            } else {
                exportedAddressList=this.getSettings().exportedAddressList.list;
            }

            var gridExports = Ext.create('Ung.EditorGrid', {
                settingsCmp: this,
                name: 'Exported Hosts and Networks',
                helpSource: 'exported_hosts_and_network',
                // the total records is set from the base settings
                paginated: false,
                height: inWizard?220:null,
                emptyRow: {
                    "live": true,
                    "name": this.i18n._("[no name]"),
                    "network": "192.168.1.0",
                    "netmask": "255.255.255.0"
                },
                title: this.i18n._("Exported Hosts and Networks"),
                recordJavaClass: "com.untangle.node.openvpn.SiteNetwork",
                data: exportedAddressList,
                // the list of fields
                autoGenerateId: true,
                fields: [{
                    name: 'live'
                }, {
                    name: 'name'
                }, {
                    name: 'network'
                }, {
                    name: 'netmask'
                }],
                autoExpandMin: 250,
                // the list of columns for the column model
                columns: [
                    {
                        xtype:'checkcolumn',
                        header: this.i18n._("Enabled"),
                        dataIndex: 'live',
                        width: 80,
                        fixed: true
                    },
                    {
                        header: this.i18n._("Host/Network Name"),
                        width: 250,
                        dataIndex: 'name',
                        flex:1,
                        editor: { xtype:'textfield'}
                    },
                    {
                        header: this.i18n._("IP Address"),
                        width: inWizard?100:130,
                        dataIndex: 'network',
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            vtype: 'ipAddress'
                            }
                    },
                    {
                        header: this.i18n._("Netmask"),
                        width: 130,
                        dataIndex: 'netmask',
                        editor: {
                            xtype:'textfield',
                            allowBlank: false,
                            vtype: 'ipAddress'
                        }
                    }],
                // sortField: 'name',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                    xtype: "textfield",
                    name: "Host/network name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Host/network name"),
                    width: 300
                }, {
                    xtype: "textfield",
                    name: "IP address",
                    dataIndex: "network",
                    allowBlank: false,
                    fieldLabel: this.i18n._("IP address"),
                    vtype: 'ipAddress',
                    width: 300
                }, {
                    xtype: "textfield",
                    name: "Netmask",
                    dataIndex: "netmask",
                    fieldLabel: this.i18n._("Netmask"),
                    allowBlank: false,
                    vtype: 'ipAddress',
                    width: 300
                }]
            });
            return gridExports;
        },
        buildExports: function() {
            this.gridExports = this.generateGridExports();
        },
        generateGridGroups: function() {
            var gridGroups = Ext.create('Ung.EditorGrid', {
                settingsCmp: this,
                name: 'Address Pools',
                // the total records is set from the base settings
                anchor:"100% 100%",
                paginated: false,
                height: 250,
                autoScroll: true,
                emptyRow: {
                    "live": true,
                    "name": this.i18n._("[no name]"),
                    "group": null,
                    "address": "172.16.16.0",
                    "netmask": "255.255.255.0"
                },
                title: this.i18n._("Address Pools"),
                recordJavaClass: "com.untangle.node.openvpn.VpnGroup",
                dataProperty: 'groupList',
                autoGenerateId: true,
                // the list of fields
                fields: [{
                    name: 'id'
                }, {
                    name: 'live'
                }, {
                    name: 'name'
                }, {
                    name: 'originalName',
                    mapping: 'name'
                }, {
                    name: 'address'
                }, {
                    name: 'netmask'
                }, {
                    name: 'useDNS'
                }],
                // the list of columns for the column model
                columns: [
                {
                    xtype:'checkcolumn',
                    header: this.i18n._("Enabled"),
                    dataIndex: 'live',
                    width: 80,
                    fixed: true
                },
                {
                    header: this.i18n._("Pool Name"),
                    width: 160,
                    dataIndex: 'name',
                    flex:1,
                    editor: {
                        xtype:'textfield',
                        allowBlank:false
                    }
                },
                {
                    header: this.i18n._("IP Address"),
                    width: 130,
                    dataIndex: 'address',
                    editor: {
                        xtype:'textfield',
                        allowBlank:false,
                        vtype: 'ipAddress'
                    }
                },
                {
                    header: this.i18n._("Netmask"),
                    width: 130,
                    dataIndex: 'netmask',
                    editor: {
                        xtype:'textfield',
                        allowBlank: false,
                        vtype: 'ipAddress'
                    }
                },
                {
                    id: "useDNS",
                    header: this.i18n._("Export DNS"),
                    dataIndex: 'useDNS',
                    width: 90,
                    fixed: true
                }],
                // sortField: 'name',
                columnsDefaultSortable: true,
                // the row input lines used by the row editor window
                rowEditorInputLines: [{
                    xtype: 'checkbox',
                    name: "Enabled",
                    dataIndex: "live",
                    fieldLabel: this.i18n._("Enabled")
                }, {
                    xtype: "textfield",
                    name: "Pool Name",
                    dataIndex: "name",
                    fieldLabel: this.i18n._("Pool Name"),
                    allowBlank: false,
                    width: 300
                }, {
                    xtype: "textfield",
                    name: "IP address",
                    dataIndex: "address",
                    fieldLabel: this.i18n._("IP Address"),
                    allowBlank: false,
                    vtype: 'ipAddress',
                    width: 300
                }, {
                    xtype: "textfield",
                    name: "Netmask",
                    dataIndex: 'netmask',
                    fieldLabel: this.i18n._("Netmask"),
                    allowBlank: false,
                    vtype: 'ipAddress',
                    width: 300
                }, {
                    xtype: 'checkbox',
                    name: "Export DNS",
                    dataIndex: "useDNS",
                    fieldLabel: this.i18n._("Export DNS")
                }]
            });
            return gridGroups;
        },
        buildAdvanced: function() {
            this.gridGroups = this.generateGridGroups();
            this.panelAdvanced = Ext.create('Ext.panel.Panel', {
                name: 'Advanced',
                helpSource: 'advanced',
                title: this.i18n._("Advanced"),
                parentId: this.getId(),

                layout: "anchor",
                cls: 'ung-panel',
                autoScroll: true,
                defaults: {
                    anchor:"98%",
                    xtype: 'fieldset',
                    autoHeight: true,
                    buttonAlign: 'left'
                },
                items: [{
                    xtype: 'form',
                    border: false,
                    bodyStyle: 'padding-bottom:20px;',
                    items: this.gridGroups
                }, {
                    xtype: 'fieldset',
                    items: [{
                        xtype: 'textfield',
                        labelWidth: 160,
                        labelAlign:'left',
                        width:300,
                        fieldLabel: this.i18n._('Site Name'),
                        name: 'Site Name',
                        value: this.getSettings().siteName,
                        id: 'openvpn_advanced_siteName',
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
                        xtype: 'radiogroup',
                        labelWidth: 160,
                        labelAlign:'left',
                        fieldLabel: this.i18n._('DNS Override'),
                        columns: 1,
                        name: 'Site Name',
                        items: [{
                            boxLabel: this.i18n._('Enabled'),
                            name: 'DNSOverride',
                            inputValue: true,
                            checked: this.getSettings().isDnsOverrideEnabled,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getSettings().isDnsOverrideEnabled = checked;
                                        if (checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();

                                        } else {
                                            Ext.getCmp("openvpn_advanced_dns1").disable();
                                            Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }, this)
                                }
                            }
                        }, {
                            boxLabel: this.i18n._('Disabled'),
                            name: 'DNSOverride',
                            inputValue: false,
                            checked: !this.getSettings().isDnsOverrideEnabled,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, checked) {
                                        this.getSettings().isDnsOverrideEnabled = !checked;
                                        if (!checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();

                                        } else {
                                            Ext.getCmp("openvpn_advanced_dns1").disable();
                                            Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }, this)
                                }
                            }

                        }]
                    }, {
                        xtype: 'fieldset',
                        autoHeight: true,
                        style: 'margin:0px 0px 0px 160px;',
                        labelWidth: 160,
                        border: false,
                        items: [{
                            xtype: 'textfield',
                            fieldLabel: this.i18n._('Primary IP'),
                            name: 'outsideNetwork',
                            id: 'openvpn_advanced_dns1',
                            value: this.getSettings().dns1,
                            disabled: !this.getSettings().isDnsOverrideEnabled,
                            allowBlank: false,
                            blankText: this.i18n._('A Valid Primary IP Address must be specified.'),
                            vtype: 'ipAddress'
                        }, {
                            xtype: 'textfield',
                            fieldLabel: this.i18n._('Secondary IP (optional)'),
                            name: 'outsideNetwork',
                            id: 'openvpn_advanced_dns2',
                            disabled: !this.getSettings().isDnsOverrideEnabled,
                            value: this.getSettings().dns2,
                            vtype: 'ipAddress'
                        }]
                    }]
                }]
            });
        },

        // validation function
        validate: function() {
            if (this.configState == "SERVER") {
                return  this.validateAdvanced() && this.validateGroups() &&
                    this.validateVpnClients() && this.validateVpnSites();
            } else {
                return true;
            }

        },

        //validate OpenVPN Advanced settings
        validateAdvanced: function() {
            //validate site name
            var siteCmp = Ext.getCmp("openvpn_advanced_siteName");
            if(!siteCmp.validate()) {
                Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must enter a site name."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdvanced);
                        siteCmp.focus(true);
                    }, this)
                );
                return false;
            };

            if (this.getSettings().isDnsOverrideEnabled) {
                var dns1Cmp = Ext.getCmp("openvpn_advanced_dns1");
                if(!dns1Cmp.validate()) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("A valid Primary IP Address must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelAdvanced);
                            dns1Cmp.focus(true);
                        }, this)
                    );
                    return false;
                };

                var dns2Cmp = Ext.getCmp("openvpn_advanced_dns2");
                if(!dns2Cmp.validate()) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("A valid Secondary IP Address must be specified."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelAdvanced);
                            dns2Cmp.focus(true);
                        }, this)
                    );
                    return false;
                };

                //prepare for save
                this.getSettings().dns1 = dns1Cmp.getValue();
                this.getSettings().dns2 = dns2Cmp.getValue() == "" ? null: dns2Cmp.getValue();
            }  else {
                this.getSettings().dns1 = null;
                this.getSettings().dns2 = null;
            }

            return true;
        },

        validateGroups: function() {
            var i;
            var groupList=this.gridGroups.getPageList();

            // verify that there is at least one group
            if(groupList.length <= 0 ) {
                Ext.MessageBox.alert(this.i18n._('Failed'), this.i18n._("You must create at least one group."),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelAdvanced);
                    }, this)
                );
                return false;
            }

            // removed groups should not be referenced
            var removedGroups = this.gridGroups.getDeletedList();

            for( i=0; i<removedGroups.length;i++) {
                var clientList = this.gridClients.getPageList();
                for(var j=0; j<clientList.length;j++) {
                    if (removedGroups[i].name == clientList[j].groupName) {
                        Ext.MessageBox.alert(this.i18n._('Failed'),
                            Ext.String.format(this.i18n._("The group: \"{0}\" cannot be deleted because it is being used by the client: {1} in the Client To Site List."), removedGroups[i].name, clientList[j].name),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelAdvanced);
                            }, this)
                        );
                        return false;
                    }
                }

                var siteList=this.gridSites.getPageList();
                for(var j=0; j<siteList.length;j++) {
                    if (removedGroups[i].name == siteList[j].groupName) {
                        Ext.MessageBox.alert(this.i18n._('Failed'),
                            Ext.String.format(this.i18n._("The group: \"{0}\" cannot be deleted because it is being used by the site: {1} in the Site To Site List."), removedGroups[i].name, siteList[j].name),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelAdvanced);
                            }, this)
                        );
                        return false;
                    }
                }
            }

            // Group names must all be unique
            var groupNames = {};

            for( i=0;i<groupList.length;i++) {
                var groupName = groupList[i].name.toLowerCase();

                var group = groupList[i];
                if(group.id>=0 && group.name!=group.originalName) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Changing name is not allowed. Create a new group."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelClients);
                        }, this));
                    return false;
                }

                if ( groupNames[groupName] != null ) {
                    Ext.MessageBox.alert(this.i18n._('Failed'), Ext.String.format(this.i18n._("The group name: \"{0}\" in row: {1} already exists."), groupList[j].name.toLowerCase(), j+1),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelAdvanced);
                                         }, this));
                    return false;
                }

                /* Save the group name */
                groupNames[groupName] = true;
            }

            return true;
        },

        validateVpnClients: function() {
            var clientList=this.gridClients.getPageList();
            var clientNames = {};
            var client = null;

            for(var i=0;i<clientList.length;i++) {
                client = clientList[i];
                if(client.id>=0 && client.name!=client.originalName) {
                    Ext.MessageBox.alert(i18n._("Failed"), this.i18n._("Changing name is not allowed. Create a new user."),
                        Ext.bind(function () {
                            this.tabs.setActiveTab(this.panelClients);
                        }, this)
                    );
                    return false;
                }

                if ( clientNames[client.name] != null ) {
                    Ext.MessageBox.alert(this.i18n._('Failed'),
                                         Ext.String.format(this.i18n._("The client name: \"{0}\" in row: {1} already exists."), client.name, i),
                                         Ext.bind(function () {
                                             this.tabs.setActiveTab(this.panelClients);
                                         }, this)
                                        );
                    return false;
                }
                clientNames[client.name] = true;
            }
            return true;
        },

        validateVpnSites: function() {
            var siteList=this.gridSites.getPageList();
            // Site names must all be unique
            for(var i=0;i<siteList.length;i++) {
                for(var j=i+1; j<siteList.length;j++) {
                    if (siteList[i].name == siteList[j].name) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), Ext.String.format(this.i18n._("The site name: \"{0}\" in row: {1} already exists."), siteList[j].name, j+1),
                            Ext.bind(function () {
                                this.tabs.setActiveTab(this.panelClients);
                            }, this)
                        );
                        return false;
                    }
                }
            }
            return true;
        },
        validateExports: function(exportList) {
            return true;
        },
        save: function(isApply) {
            if(this.configState == "SERVER") {
                this.getSettings().groupList.list = this.gridGroups.getPageList();
                this.getSettings().exportedAddressList.list = this.gridExports.getPageList();
                this.getSettings().clientList.list = this.gridClients.getPageList();
                this.getSettings().siteList.list = this.gridSites.getPageList();


                this.getRpcNode().setSettings(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.afterSave(isApply);
                }, this), this.getSettings());
            } else {
                this.afterSave(isApply);
            }
        },
        afterSave: function(isApply) {
            Ext.MessageBox.hide();
            if (!isApply) {
                this.closeWindow();
            } else {
                Ext.MessageBox.wait(i18n._("Reloading..."), i18n._("Please wait"));
                this.getSettings(function() {
                    // Assume the config state hasn't changed
                    if (this.configState == "SERVER") {
                        this.getGroupsStore(true);
                        this.getDefaultGroupName(true);
                        this.gridSites.emptyRow.groupName = this.getDefaultGroupName();
                        this.gridClients.emptyRow.groupName = this.getDefaultGroupName();

                        this.gridExports.reload({data: this.getSettings().exportedAddressList.list });

                        Ext.getCmp( "openvpn_advanced_siteName" ).setValue( this.getSettings().siteName );

                        // Assuming radio box is intact
                        Ext.getCmp("openvpn_advanced_dns1").setValue( this.getSettings().dns1 );
                        Ext.getCmp("openvpn_advanced_dns2").setValue( this.getSettings().dns2 );
                        this.clearDirty();
                    } else {
                        // do nothing
                    }
                    Ext.MessageBox.hide();
                });
            }
        },
        configureVPNClient: function() {
            if (this.clientSetup) {
                Ext.destroy(this.clientSetup);
            }

            welcomeCard = Ext.create('Ung.Node.OpenVPN.ClientWizard.Welcome', {
                i18n: this.i18n,
                node: this.getRpcNode()
            });

            downloadCard = Ext.create('Ung.Node.OpenVPN.ClientWizard.LoadConfig', {
                i18n: this.i18n,
                node: this.getRpcNode()
            });

            congratulationsCard =Ext.create('Ung.Node.OpenVPN.ClientWizard.Congratulations', {
                i18n: this.i18n,
                node: this.getRpcNode(),
                gui: this
            });

            var clientWizard = Ext.create('Ung.Wizard', {
                height: 450,
                width: 'auto',
                modalFinish: true,
                hasCancel: true,
                cardDefaults: {
                    labelWidth: 150,
                    cls: 'untangle-form-panel'
                },
                cards: [welcomeCard, downloadCard, congratulationsCard]
            });

            this.clientSetup = Ext.create('Ung.Window', {
                title: this.i18n._("OpenVPN Client Setup Wizard"),
                closeAction: "cancelAction",
                defaults: {},
                wizard: clientWizard,
                items: [{
                    region: "center",
                    items: [clientWizard],
                    border: false,
                    autoScroll: true,
                    cls: 'window-background',
                    bodyStyle: 'background-color: transparent;'
                }],
                endAction: Ext.bind(function() {
                    this.clientSetup.hide();
                    Ext.destroy(this.clientSetup);
                    if(this.mustRefresh || true) {
                        var nodeWidget=this.node;
                        nodeWidget.settingsWin.closeWindow();
                        nodeWidget.onSettingsAction();
                    }
                }, this),
                cancelAction: Ext.bind(function() {
                    this.clientSetup.wizard.cancelAction();
                }, this)
            });

            clientWizard.cancelAction=Ext.bind(function() {
                if(!this.clientSetup.wizard.finished) {
                    Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring OpenVPN. Please run the Setup Wizard again."), Ext.bind(function () {
                        this.clientSetup.endAction();
                    }, this));
                } else {
                    this.clientSetup.endAction();
                }
            }, this);

            this.clientSetup.show();
            clientWizard.goToPage(0);
        },

        completeServerWizard: function( handler )
        {
            this.node.start();
            this.serverSetup.endAction();
        },

        //
        configureVPNServer: function() {
            if (this.serverSetup) {
                Ext.destroy(this.serverSetup);
            }

            var welcomeCard = {
                title: this.i18n._("Welcome"),
                panel: {
                    xtype: 'form',
                    items: [{
                        xtype: 'label',
                        html: '<h2 class="wizard-title">'+this.i18n._("Welcome to the OpenVPN Setup Wizard!")+'</h2>'
                    }, {
                        html: this.i18n._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Server.'),
                        bodyStyle: 'padding-bottom:20px;',
                        cls: 'description',
                        border: false
                    }, {
                        html: this.i18n
                                ._('Warning: Completing this wizard will overwrite any previous OpenVPN settings with new settings. All previous settings will be lost!'),
                        cls: 'description warning',
                        border: false
                    }]
                },
                onNext: function(handler) {
                    handler();
                }
            };
            var country="US";
            var companyName="";
            var state="";
            var city="";

            var certificateCard = {
                title: this.i18n._("Step 1 - Certificate"),
                panel: {
                    xtype: 'form',
                    items: [{
                        xtype: 'label',
                        html: '<h2 class="wizard-title">'+this.i18n._("Step 1 - Certificate")+'</h2>'
                    }, {
                        html: this.i18n
                                ._('Please specify some information about your location. This information will be used to generate a secure digital certificate.'),
                        cls: 'description',
                        border: false
                    }, {
                        xtype: 'fieldset',
                        autoHeight: true,
                        buttonAlign: 'left',
                        title: this.i18n._('This information is required.'),
                        items: [{
                            xtype: "textfield",
                            id: 'openvpn_server_wizard_organization',
                            name: "Organization",
                            fieldLabel: this.i18n._("Organization"),
                            allowBlank: false,
                            value: companyName,
                            width: 300
                        }, {
                            fieldLabel: this.i18n._('Country'),
                            id: 'openvpn_server_wizard_country',
                            name: "Country",
                            xtype: 'combo',
                            width: 300,
                            listWidth: 205,
                            store: Ung.Country.getCountryStore(this.i18n),
                            mode: 'local',
                            triggerAction: 'all',
                            listClass: 'x-combo-list-small',
                            value: country
                        }, {
                            xtype: "textfield",
                            id: 'openvpn_server_wizard_state',
                            name: "State/Province",
                            fieldLabel: this.i18n._("State/Province"),
                            allowBlank: false,
                            value: state,
                            width: 300
                        }, {
                            xtype: "textfield",
                            id: 'openvpn_server_wizard_locality',
                            name: "City",
                            fieldLabel: this.i18n._("City"),
                            allowBlank: false,
                            value: city,
                            width: 300
                        }]
                    }]
                },
                onNext: Ext.bind(function(handler) {
                    var organization=Ext.getCmp("openvpn_server_wizard_organization").getValue();
                    var country=Ext.getCmp("openvpn_server_wizard_country").getValue();
                    var state=Ext.getCmp("openvpn_server_wizard_state").getValue();
                    var locality=Ext.getCmp("openvpn_server_wizard_locality").getValue();
                    if(organization.length==0) {
                        Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must fill out the name of your organization."));
                        return;
                    }
                    if(state.length==0) {
                        Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must fill out the name of your state."));
                        return;
                    }
                    if(locality.length==0) {
                        Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must fill out the name of your locality."));
                        return;
                    }
                    Ext.MessageBox.wait(this.i18n._("Generating Certificate..."), this.i18n._("Please wait"));
                    var certificateParameters={
                        javaClass: "com.untangle.node.openvpn.CertificateParameters",
                        organization: organization,
                        domain: "",
                        country: country,
                        state: state,
                        locality: locality,
                        storeCaUsb: false
                    };
                    this.getRpcNode().generateCertificate(Ext.bind(function(result, exception) {
                        Ext.MessageBox.hide();
                        if(Ung.Util.handleException(exception)) return;
                        this.handler();
                    }, {
                        handler: handler,
                        settingsCmp: this
                    }), certificateParameters);
                }, this)
            };
            var gridExports = this.generateGridExports(true);
            var exportsCard = {
                title: this.i18n._("Step 2 - Exports"),
                panel: {
                    xtype: 'form',
                    items: [{
                        xtype: 'label',
                        html: '<h2 class="wizard-title">'+this.i18n._("Step 2 - Exports")+'</h2>'
                    }, {
                        html: this.i18n._('Please complete the list of exports. This is a list of hosts and networks which remote VPN users and networks will be able to contact.'),
                        cls: 'description',
                        bodyStyle: 'padding-bottom:10px',
                        border: false
                    }, {
                        html: "<i>" + this.i18n._('By default the entire internal network is exported.') + "</i>",
                        cls: 'description',
                        border: false
                    }, gridExports]
                },
                onNext: Ext.bind(function(handler) {
                    var gridExports=Ext.getCmp(this.serverSetup.gridExportsId);
                    var saveList=gridExports.getPageList();

                    if (!this.validateExports(saveList)) {
                        return;
                    }

                    this.getExportedAddressList().exportList.list = saveList;

                    Ext.MessageBox.wait(this.i18n._("Adding Exports..."), this.i18n._("Please wait"));
                    this.getRpcNode().setExportedAddressList(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.settingsCmp.getRpcNode().completeConfig(Ext.bind(function(result, exception) { //complete server configuration
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            // go to next step
                            this.handler();
                        }, this));
                    }, {
                        handler: handler,
                        settingsCmp: this
                    }), this.getExportedAddressList());
                }, this)
            };
            var congratulationsCard = {
                title: this.i18n._("Finished!"),
                panel: {
                    xtype: 'form',
                    items: [{
                        xtype: 'label',
                        html: '<h2 class="wizard-title">'+this.i18n._("Finished!")+'</h2>'
                    }, {
                        html: this.i18n._('Congratulations!'),
                        cls: 'description',
                        border: false
                    }, {
                        html: this.i18n._('You are now ready to begin adding remote clients and sites you wish to have access to your VPN.'),
                        cls: 'description',
                        border: false
                    }, {
                        html: this.i18n
                                ._('To add remote users, click on the Clients tab and add to the VPN Clients table.<br/>To add remote networks, click on the Clients tab and add to the VPN Sites table.'),
                        cls: 'description',
                        border: false
                    }]
                },
                onNext: Ext.bind(this.completeServerWizard, this)
            };
            var serverWizard = Ext.create('Ung.Wizard', {
                height: 450,
                width: 'auto',
                modalFinish: true,
                hasCancel: true,
                cardDefaults: {
                    labelWidth: 150,
                    cls: 'untangle-form-panel'
                },
                cards: [welcomeCard, certificateCard, exportsCard, congratulationsCard]
            });

            this.serverSetup = Ext.create('Ung.Window', {
                gridExportsId: gridExports.getId(),
                title: this.i18n._("OpenVPN Server Setup Wizard"),
                closeAction: "cancelAction",
                defaults: {},
                wizard: serverWizard,
                items: [{
                    region: "center",
                    items: [serverWizard],
                    border: false,
                    autoScroll: true,
                    cls: 'window-background',
                    bodyStyle: 'background-color: transparent;'
                }],
                endAction: Ext.bind(function() {
                    this.serverSetup.hide();
                    Ext.destroy(this.serverSetup);
                    if(this.mustRefresh || true) {
                        var nodeWidget=this.node;
                        nodeWidget.settingsWin.closeWindow();
                        nodeWidget.onSettingsAction();
                    }
                }, this),
                cancelAction: Ext.bind(function() {
                    this.serverSetup.wizard.cancelAction();
                }, this)
            });
            serverWizard.cancelAction=Ext.bind(function() {
                if(!this.serverSetup.wizard.finished) {
                    Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring OpenVPN. Please run the Setup Wizard again."), Ext.bind(function () {
                        this.serverSetup.endAction();
                    }, this));
                } else {
                    this.serverSetup.endAction();
                }
            }, this);
            this.serverSetup.show();
            serverWizard.goToPage(0);
        }
    });
}
//@ sourceURL=openvpn-settings.js
