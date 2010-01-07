/* XXXX This logic should be moved outside of this file and into the
 * loading code, having a conditional around an entire file is a
 * little ridiculous. Especially a 2000 line file. XXX */
if (!Ung.hasResource["Ung.OpenVPN"]) {
    Ung.hasResource["Ung.OpenVPN"] = true;
    Ung.NodeWin.registerClassName('untangle-node-openvpn', "Ung.OpenVPN");

    Ext.namespace('Ung');
    Ext.namespace('Ung.Node');
    Ext.namespace('Ung.Node.OpenVPN');
    
    Ung.Node.OpenVPN.DistributeClient = Ext.extend( Object, {
        constructor : function( config )
        {
            this.i18n = config.i18n;
            this.node = config.node;
            this.config = config;
        },

        sendEmail : function()
        {
            /* Determine the email address */
            var emailAddress = this.panel.find( "name", "emailAddress" )[0].getValue();

            if ( emailAddress == null || emailAddress.length == 0 ) {
                Ext.MessageBox.alert( this.i18n._("Failure"),
                                      this.i18n._("You must specify an email address to send the key to."));
                return;
            }

            this.record.data.distributionEmail = emailAddress;

            Ext.MessageBox.wait( this.i18n._( "Distributing digital key..." ), this.i18n._( "Please wait" ));
            
            this.node.distributeClientConfig(this.completeSendEmail.createDelegate(this), this.record.data);
        },

        completeSendEmail : function( result, exception )
        {
            if (exception) {
                Ext.MessageBox.alert( this.i18n._("Error saving/sending key"),
                                      this.i18n._("OpenVPN was not able to send your digital key via email.  Please try again." ));
                return;
            }

            // go to next step
            Ext.MessageBox.alert( this.i18n._("Success"),
                                  this.i18n._("OpenVPN successfully sent your digital key via email."),
                                  this.hide.createDelegate( this ));
        },

        populate : function( record )
        {
            this.record = record;
        },

        windowsInstaller : function()
        {
            return this.startDownload( "SETUP_EXE" );
        },

        configurationFiles : function()
        {
            return this.startDownload( "ZIP" );
        },

        startDownload : function( format )
        {
            var name = this.record.data.name;

            return this.node.getAdminDownloadLink( name, format );
        },

        show : function()
        {
            var download = null;
            if ( this.config.isVpnSite == true ) {
                download = new Ext.form.FieldSet({
                    title : this.i18n._('Download Key'),
                    autoHeight : true,
                    labelWidth: 150,
                    items: [{
                        html :  "<a href=\"" + this.configurationFiles() + "\" target=\"_blank\">" + this.i18n._('Download VPN Site configuration.') + "</a>",
                        border: false,
                        cls : "description"
                    }]
                });
            } else {
                download = new Ext.form.FieldSet({
                    title : this.i18n._('Download Key'),
                    autoHeight : true,
                    labelWidth: 150,
                    items: [{
                        html :  "<a href=\"" + this.windowsInstaller() + "\" target=\"_blank\">" + this.i18n._('Click here to download a key for Windows clients.') + "</a>",
                        border: false,
                        cls : "description"
                    },{
                        html : "<a href=\"" + this.configurationFiles() + "\" target=\"_blank\">" + this.i18n._('Click here to download a key for all other clients.') + "</a>",
                        bodyStyle : 'paddingTop:10px',
                        border: false,
                        cls : "description"
                    }]
                });
            }

            this.panel = new Ext.FormPanel({
                cls: 'ung-panel',
                items : [{
                    cls : 'u-form-panel',
                    xtype : 'fieldset',
                    title : this.i18n._('Distribute via Email'),
                    autoHeight : true,
                    labelWidth: 150,
                    items: [{
                        html : this.i18n._('Click "Send Email" to send an email to "Email Address" with information to retrieve the OpenVPN Client.'),
                        border: false,
                        cls : "description"
                    },{
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Email Address'),
                        labelWidth: 150,
                        width: 200,
                        name : 'emailAddress'
                    },{
                        xtype : 'button',
                        text : this.i18n._( "Send Email" ),
                        name : 'sendEmail',
                        handler : this.sendEmail.createDelegate( this )
                }]}, download
                ]
            });

            this.window = new Ung.Window({
                title : this.i18n._('Distribute VPN Client'),
                bbar : [
                    '->',
                {
                    name : 'close',
                    iconCls : 'cancel-icon',
                    text : this.i18n._('Close'),
                    handler : this.hide.createDelegate( this )
                }],
                items : [
                    this.panel
               ]
            });

            Ext.MessageBox.hide();
            this.window.show();
        },

        hide : function()
        {
            this.window.hide();
        }
    });

    /* Namespace for the openvpn client configuration cards. */
    Ext.namespace('Ung.Node.OpenVPN.ClientWizard');
    Ung.Node.OpenVPN.ClientWizard.Welcome = Ext.extend( Object, {
        constructor : function( config )
        {
            this.i18n = config.i18n;
            this.node = config.node;

            this.title = this.i18n._("Welcome"),
            this.panel = new Ext.FormPanel({
                items : [{
                    xtype : 'label',
                    html : '<h2 class="wizard-title">'+this.i18n._("Welcome to the OpenVPN Setup Wizard!")+'</h2>'
                },{
                    html : this.i18n._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Client.'),
                    cls: 'description',
                    bodyStyle : 'padding-bottom:10px',
                    border : false
                },{
                    html : this.i18n._('Warning: Completing this wizard will overwrite any previous OpenVPN settings with new settings. All previous settings will be lost!'),

                    cls: 'description warning',
                    border : false
                }]
            })
        }
    });

    Ung.Node.OpenVPN.ClientWizard.LoadConfig = Ext.extend( Object, {
        constructor : function( config )
        {
            this.i18n = config.i18n;
            this.node = config.node;

            this.title = this.i18n._( "Install Configuration" );

            this.panel = new Ext.FormPanel({
                fileUpload : true,
                items : [{
                    xtype : 'label',
                    html : '<h2 class="wizard-title">'+this.i18n._("Download Configuration")+'</h2>'
                },{
                    cls: 'description',
                    border : false,
                    html : this.i18n
                        ._('Please specify where your VPN Client configuration should come from.')
                },{
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left',
                    labelWidth: 150,
                    labelAlign: 'right',
                    items : [{
                        xtype : "radio",
                        boxLabel : this.i18n._('Download from Server'),
                        hideLabel : true,
                        name : 'downloadClientConfiguration',
                        inputValue : "server",
                        checked : true,
                        listeners : {
                            "check" : {
                                fn : this.onChangeConfigType.createDelegate(this)
                            }
                        }
                    },{
                        xtype : "textfield",
                        name : "serverAddress",
                        fieldLabel : this.i18n._("Server IP Address"),
                        allowBlank : false,
                        width : 200,
                        validator : this.validateServerAddress.createDelegate( this )
                    },{
                        xtype : "textfield",
                        name : "password",
                        fieldLabel : this.i18n._("Password"),
                        width : 200,
                        allowBlank : false,
                        inputType : 'password'
                    },{
                        xtype : "radio",
                        boxLabel : this.i18n._('Upload Configuration'),
                        hideLabel : true,
                        name : 'downloadClientConfiguration',
                        inputValue : "upload",
                        checked : false
                    },{
                        xtype : 'textfield',
                        inputType : 'file',
                        name : 'siteConfiguration',
                        fieldLabel : 'Configuration File',
                        allowBlank : false,
                        width : 300
                    }]
                }]
            });

            this.onNext = this.retrieveClient.createDelegate( this );
            this.onValidate = this.validateSettings.createDelegate( this );

            this.onChangeConfigType( this.panel.find( "name", "downloadClientConfiguration" )[0], true );
        },

        onChangeConfigType : function( elem, checked )
        {
            /* Checked means that download client is selected */
            var serverAddress = this.panel.find( 'name', 'serverAddress' )[0];
            var password = this.panel.find( 'name', 'password' )[0];
            var siteConfiguration = this.panel.find( 'name', 'siteConfiguration' )[0];
            serverAddress.setDisabled( !checked );
            password.setDisabled( !checked );
            siteConfiguration.setDisabled( checked );

            if ( checked ) {
                _invalidate( siteConfiguration );
            } else {
                _invalidate( [ serverAddress, password ] );
            }
        },

        validateSettings : function()
        {
            var method = this.panel.find( "name", "downloadClientConfiguration" )[0].getGroupValue();

            var rv = true;

            switch ( method ) {
            case "upload":
                if ( !_validate( this.panel.find( 'name', 'siteConfiguration' ))) rv = false;
                break;

            case "server":
                if ( !_validate( this.panel.find( 'name', 'serverAddress' ))) rv = false;
                if ( !_validate( this.panel.find( 'name', 'password' ))) rv = false;
                break;
            default:
                rv = false;
            }

            return rv;
        },

        validateServerAddress : function( value )
        {
            var sarr = value.split( ":" );
            if ( sarr.length > 2 ) return this.i18n._( "Invalid Server Address" );

            /* Could check if the address is of the form <ip>|<hostname>:[<port>] */
            return true;
        },

        retrieveClient : function( handler )
        {
            var method = this.panel.find( "name", "downloadClientConfiguration" )[0].getGroupValue();

            switch ( method ) {
            case "upload": return this.uploadClient( handler );
            case "server": return this.fetchClient( handler );
            default:
                Ext.MessageBox.alert(this.i18n._( "Select a value" ), this.i18n._( "Please choose 'Download from Server' or 'Upload Configuration'." ));
                return;
            }
        },

        fetchClient : function( handler )
        {
            Ext.MessageBox.wait(this.i18n._("Downloading Configuration... (This may take up to one minute)"),
                                this.i18n._("Please wait"));

            var serverAddress = this.panel.find( 'name', 'serverAddress' )[0].getValue();
            var serverPort = "443";
            var password = this.panel.find( 'name', 'password' )[0].getValue();

            var sarr = serverAddress.split( ":" );
            if ( sarr.length == 2 ) serverPort = sarr[1];

            this.node.downloadConfig( this.completeFetchClient.createDelegate( this , [ handler ], true ),
                                      serverAddress, serverPort, password );
        },

        completeFetchClient : function( result, exception, foo, handler )
        {
            if( Ung.Util.handleException(exception,function() {
                Ext.MessageBox.alert(this.i18n._("Error downloading configuration from server."),
                                     this.i18n._("Your VPN Client configuration could not be downloaded from the server.  Please try again."));
            })) {
                return;
            }

            this.node.completeConfig( this.c_completeConfig.createDelegate( this, [ handler ], true ));
        },

        uploadClient : function( handler )
        {
            Ext.MessageBox.wait(this.i18n._("Uploading Configuration... (This may take up to one minute)"),
                                this.i18n._("Please wait"));

            this.node.getAdminClientUploadLink( this.completeGetAdminClientUploadLink.createDelegate( this, [handler], true ));
        },

        completeGetAdminClientUploadLink : function( result, exception, foo, handler )
        {
            if(Ung.Util.handleException(exception,function() {
                Ext.MessageBox.alert( this.i18n._("Failed"), this.i18n._( "Unable to load VPN Client" ));
            }.createDelegate(this),"noAlert")) return;

            var file = this.panel.find( "name", "siteConfiguration" )[0].getValue();

            /* This should have already happened in onValidate */
            if ( file == null || file.length == 0 ) {
                Ext.MessageBox.alert(this.i18n._( "Select File" ), this.i18n._( "Please choose a file to upload." ));
                return;
            }

            this.panel.getForm().submit({
                url : result,
                success : this.completeUploadClient.createDelegate( this, [ handler ], true ),
                failure : this.failUploadClient.createDelegate( this ),
                clientValidation : false
            });
        },

        /** action.result["success"] = true will trigger success in extjs for a form submit. */
        completeUploadClient : function( form, action, handler )
        {
            this.node.completeConfig( this.c_completeConfig.createDelegate( this, [ handler ], true ));
        },

        /** action.result["success"] = false will trigger failure in extjs. */
        failUploadClient : function( form, action )
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

        c_completeConfig : function( result, exception, foo, handler )
        {
            if(Ung.Util.handleException(exception,function() {
                Ext.MessageBox.alert( this.i18n._("Failed"), this.i18n._( "Unable to load VPN Client" ));
            }.createDelegate(this),"noAlert")) return;

            // go to next step
            Ext.MessageBox.alert( this.i18n._("Success"),
                                  this.i18n._("Installed OpenVPN client."));

            handler();
        }
    });

    Ung.Node.OpenVPN.ClientWizard.Congratulations = Ext.extend( Object, {
        constructor : function( config )
        {
            this.i18n = config.i18n;
            this.node = config.node;
            this.gui = config.gui;

            this.title = this.i18n._( "Finished!" );
            this.panel = new Ext.FormPanel({
                items : [{
                        xtype : 'label',
                        html : '<h2 class="wizard-title">'+this.i18n._("Finished!")+'</h2>'
                    }, {
                        cls: 'description',
                        border : false,
                        html : this.i18n._('Congratulations! OpenVPN is configured as a VPN Client.')
                    }]
            });

            this.onNext = this.completeWizard.createDelegate( this );
        },

        completeWizard : function( handler )
        {
            this.gui.node.start();
            this.gui.clientSetup.endAction();
        }
    });

    Ung.OpenVPN = Ext.extend(Ung.NodeWin, {
        configState : null,
        groupsStore : null,
        panelStatus : null,
        panelClients : null,
        gridClients : null,
        gridSites : null,
        gridExports : null,
        gridGroups : null,
        panelAdvanced : null,
        gridEventLog : null,
        initComponent : function(container, position) {
            this.configState=this.getRpcNode().getConfigState();
            this.buildStatus();
            var tabs = [this.panelStatus];

            /* Register the VTypes, need i18n to be initialized for the text */
            if(Ext.form.VTypes["openvpnClientNameVal"]==null) {
                Ext.form.VTypes["openvpnClientNameVal"] = /^[A-Za-z0-9]([-_.0-9A-Za-z]*[0-9A-Za-z])?$/;
                Ext.form.VTypes["openvpnClientName"] = function(v){
                    return Ext.form.VTypes["openvpnClientNameVal"].test(v);
                }
                Ext.form.VTypes["openvpnClientNameMask"] = /[-_.0-9A-Za-z]*/;
                Ext.form.VTypes["openvpnClientNameText"] = this.i18n._( "A client name should only contains numbers, letters, dashes and periods.  Spaces are not allowed." );
            }

            if (this.configState == "SERVER_ROUTE") {
                // keep initial settings
                this.initialVpnSettings = Ung.Util.clone(this.getVpnSettings());
                
                this.buildClients();
                this.buildExports();
                this.buildAdvanced();
                this.buildEventLog();
                tabs.push(this.panelClients);
                tabs.push(this.gridExports);
                tabs.push(this.panelAdvanced);
                tabs.push(this.gridEventLog);
            } else if (this.configState == "CLIENT") {
                this.buildEventLog();
                tabs.push(this.gridEventLog);
            }
            this.buildTabPanel(tabs);
            Ung.OpenVPN.superclass.initComponent.call(this);
        },
        getVpnSettings : function(forceReload) {
            if (forceReload || this.rpc.vpnSettings === undefined) {
                this.rpc.vpnSettings = this.getRpcNode().getVpnSettings();
            }
            return this.rpc.vpnSettings;
        },

        getVpnServerAddress : function(forceReload) {
            if (forceReload || this.rpc.vpnServerAddress === undefined) {
                this.rpc.vpnServerAddress = this.getRpcNode().getVpnServerAddress();
            }
            return this.rpc.vpnServerAddress;
        },
        getOpenVpnValidator : function(forceReload) {
            if (forceReload || this.rpc.openVpnValidator === undefined) {
                this.rpc.openVpnValidator = this.getRpcNode().getValidator();
            }
            return this.rpc.openVpnValidator;
        },
        getExportedAddressList : function(forceReload) {
            if (forceReload || this.rpc.exportedAddressList === undefined) {
                this.rpc.exportedAddressList = this.getRpcNode().getExportedAddressList();
            }
            return this.rpc.exportedAddressList;
        },
        getRegistrationInfo : function(forceReload) {
            if (forceReload || this.rpc.registrationInfo === undefined) {
                this.rpc.registrationInfo = rpc.adminManager.getRegistrationInfo();
            }
            return this.rpc.registrationInfo;
        },

        getGroupsStore : function(force) {
            if (this.groupsStore == null ) {
                this.groupsStore = new Ext.data.JsonStore({
                    fields : ['id', 'name','javaClass'],
                    data : this.getVpnSettings().groupList.list
                });
                force = false;
            }

            if(force) {
                this.groupsStore.loadData( this.getVpnSettings().groupList.list );
            }

            return this.groupsStore;
        },

        // Block lists panel
        buildStatus : function() {
            var statusLabel = "";
            var serverButtonDisabled = false;
            var clientButtonDisabled = false;
            if (this.configState == "UNCONFIGURED") {
                statusLabel = this.i18n._("Unconfigured: Use buttons below.");
            } else if (this.configState == "CLIENT") {
                statusLabel = String.format(this.i18n._("VPN Client: Connected to {0}"), this.getVpnServerAddress());
                clientButtonDisabled = true;
                serverButtonDisabled = true;
            } else if (this.configState == "SERVER_ROUTE") {
                statusLabel = this.i18n._("VPN Server");
                clientButtonDisabled = true;
                serverButtonDisabled = true;
            } else {
                clientButtonDisabled = true;
                serverButtonDisabled = true;
            }
            this.panelStatus = new Ext.Panel({
                name : 'Status',
                helpSource : 'status',
                title : this.i18n._("Status"),
                parentId : this.getId(),

                layout : "form",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Current Mode'),
                    items : [{
                        xtype : "textfield",
                        name : "Status",
                        readOnly : true,
                        fieldLabel : this.i18n._("Status"),
                        width : 250,
                        value : statusLabel
                    }]
                }, {
                    title : this.i18n._('Wizard'),
                    layout : 'table',
                    layoutConfig : {
                        columns : 2
                    },
                    items : [{
                        xtype : "button",
                        name : 'Configure as VPN Server',
                        text : this.i18n._("Configure as VPN Server"),
                        iconCls : "action-icon",
                        disabled : serverButtonDisabled,
                        handler : function() {
                            this.getRpcNode().startConfig(function(result, exception) {
                                if(Ung.Util.handleException(exception)) return;
                                this.configureVPNServer();
                            }.createDelegate(this), "SERVER_ROUTE")

                        }.createDelegate(this)
                    }, {
                        html : this.i18n
                                ._("This configures OpenVPN so remote users and networks can connect and access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        cls: 'description',
                        border : false
                    }, {
                        xtype : "button",
                        name : 'Configure as VPN Client',
                        text : this.i18n._("Configure as VPN Client"),
                        iconCls : "action-icon",
                        disabled : clientButtonDisabled,
                        handler : function() {
                            this.getRpcNode().startConfig(function(result, exception) {
                                if(Ung.Util.handleException(exception)) return;
                                this.configureVPNClient();
                            }.createDelegate(this), "CLIENT")
                        }.createDelegate(this)
                    }, {
                        html : this.i18n
                                ._("This configures OpenVPN so it connects to a remote OpenVPN Server and can access exported hosts and networks."),
                        bodyStyle : 'padding:10px 10px 10px 10px;',
                        cls: 'description',
                        border : false
                    }]
                }]
            });
        },

        // overwrite default event manager
        getEventManager : function() {
            if (this.node.nodeContext.rpcNode.eventManager === undefined) {
                this.node.nodeContext.rpcNode.eventManager = this.node.nodeContext.rpcNode.getClientConnectEventManager();
            }
            return this.node.nodeContext.rpcNode.eventManager;
        },

        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                fields : [{
                    name : 'start',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'end',
                    sortType : Ung.SortTypes.asTimestamp,
                    convert : function(val) {
                        var date = val;
                        if (date == null) {
                            date={time:0};
                        }
                        return date;
                    }
                }, {
                    name : 'clientName'
                }, {
                    name : 'address',
                    convert : function(val, rec) {
                        return val == null ? "" : val + ":" + rec.port;
                    }
                }, {
                    name : 'bytesTx',
                    convert : function(val) {
                        return parseFloat(val) / 1024;
                    }
                }, {
                    name : 'bytesRx',
                    convert : function(val) {
                        return parseFloat(val) / 1024;
                    }
                }],
                columns : [{
                    header : this.i18n._("start time"),
                    width : 130,
                    sortable : true,
                    dataIndex : 'start',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }.createDelegate( this )
                }, {
                    header : this.i18n._("end time"),
                    width : 130,
                    sortable : true,
                    dataIndex : 'end',
                    renderer : function(value) {
                        if (( value == null ) || ( value.time == 0 )) {
                            return this.i18n._( "active connection" );
                        }
                        return i18n.timestampFormat(value);
                    }.createDelegate( this )
                }, {
                    id: 'client_name',
                    header : this.i18n._("client name"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'clientName'
                }, {
                    header : this.i18n._("client address"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'address'
                }, {
                    header : this.i18n._("KB sent"),
                    width : 80,
                    sortable : true,
                    dataIndex : 'bytesTx',
                    renderer : function( value ) {
                        return Math.round(( value + 0.0 ) * 10 ) / 10;
                    }.createDelegate( this )
                }, {
                    header : this.i18n._("KB received"),
                    width : 80,
                    sortable : true,
                    dataIndex : 'bytesRx',
                    renderer : function( value ) {
                        return Math.round(( value + 0.0 ) * 10 ) / 10;
                    }.createDelegate( this )
                }],
                autoExpandColumn : 'client_name'

            });
        },
        getDistributeColumn : function() {
            return new Ext.grid.ButtonColumn({
                width : 110,
                header : this.i18n._("distribute"),
                dataIndex : null,
                i18n : this.i18n,
                handle : function(record) {
                    Ext.MessageBox.wait(
                        this.i18n._( "Updating OpenVPN Client..." ),
                        this.i18n._( "Please Wait" )
                    );
                    this.grid.distributeWindow.populate(record);
                    this.grid.distributeWindow.show();
                },
                renderer : function(value, metadata, record) {
                    var out= '';
                    if(record.data.id>=0) {
                        out= '<div class="ung-button button-column" style="text-align:center;" >' + this.i18n._("Distribute Client") + '</div>';
                    }
                    return out;
                }.createDelegate( this )
            });
        },
        getGroupsColumn : function() {
            return {
                id : 'groupId',
                header : this.i18n._("address pool"),
                width : 160,
                dataIndex : 'groupId',
                renderer : function(value, metadata, record) {
                    var result = ""
                    var store = this.getGroupsStore();
                    if (store) {
                        var index = store.findBy(function(record, id) {
                            if (record.data.id == value) {
                                return true;
                            } else {
                                return false;
                            }
                        });
                        if (index >= 0) {
                            result = store.getAt(index).get("name");
                            record.data.group = store.getAt(index).data;
                        } else {
                            record.data.group = null;
                        }

                    }
                    return result;
                }.createDelegate(this),
                editor : new Ext.form.ComboBox({
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small'
                })
            }
        },
        generateGridClients : function() {
            // live is a check column
            var clientList=this.getVpnSettings().clientList.list;

            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var distributeColumn = this.getDistributeColumn();
            var groupsColumn=this.getGroupsColumn();
            var defaultGroup= this.getGroupsStore().getCount()>0?this.getGroupsStore().getAt(0).data:null;
            var gridClients = new Ung.EditorGrid({
                initComponent : function() {
                    this.distributeWindow = new Ung.Node.OpenVPN.DistributeClient({
                        i18n : this.settingsCmp.i18n,
                        node : this.settingsCmp.getRpcNode()
                    });
                    this.subCmps.push(this.distributeWindow.window);
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp : this,
                name : 'VPN Clients',
                // the total records is set from the base settings
                paginated : false,
                anchor : "100% 50%",
                height : 250,
                //style: "margin-bottom:10px;",
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "groupId" : defaultGroup!=null?defaultGroup.id:null,
                    "group" : defaultGroup,
                    "address" : null
                },
                title : this.i18n._("VPN Clients"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnClient",
                data : clientList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'originalName',
                    mapping: 'name'
                }, {
                    name : 'group'
                }, {
                    name : 'groupId',
                    mapping: 'group',
                    convert : function(val, rec) {
                        return val==null?null:val.id;
                    }
                }, {
                    name : 'address'
                }],
                autoExpandColumn: 'name',
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("client name"),
                    width : 130,
                    dataIndex : 'name',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'openvpnClientName'
                    })
                }, groupsColumn, distributeColumn, {
                    id : 'address',
                    header : this.i18n._("virtual address"),
                    width : 100,
                    dataIndex : 'address',
                    renderer : function(value, metadata, record) {
                        return value==null ? this.i18n._("unassigned") : value;
                    }.createDelegate(this)
                }],
                // sortField : 'name',
                // columnsDefaultSortable : true,
                plugins : [liveColumn, distributeColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Client name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Client name"),
                    allowBlank : false,
                    vtype : 'openvpnClientName',
                    width : 200
                }, {
                    xtype : "combo",
                    name : "Address pool",
                    dataIndex : "groupId",
                    fieldLabel : this.i18n._("Address pool"),
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                }]
            });
            return gridClients;
        },
        generateGridSites : function() {
            // live is a check column
            var siteList=this.getVpnSettings().siteList.list;

            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var distributeColumn = this.getDistributeColumn();
            var groupsColumn=this.getGroupsColumn();
            var defaultGroup= this.getGroupsStore().getCount()>0?this.getGroupsStore().getAt(0).data:null

            var gridSites = new Ung.EditorGrid({
                initComponent : function() {
                    this.distributeWindow = new Ung.Node.OpenVPN.DistributeClient({
                        i18n : this.settingsCmp.i18n,
                        node : this.settingsCmp.getRpcNode(),
                        isVpnSite : true
                    });
                    this.subCmps.push(this.distributeWindow.window);
                    Ung.EditorGrid.prototype.initComponent.call(this);
                },
                settingsCmp : this,
                name : 'VPN Sites',
                // the total records is set from the base settings
                paginated : false,
                anchor : "100% 50%",
                height : 250,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "groupId" : defaultGroup!=null?defaultGroup.id:null,
                    "group" : defaultGroup,
                    "network" : "1.2.3.4",
                    "netmask" : "255.255.255.0",
                    "exportedAddressList" : {
                        javaClass: "java.util.ArrayList",
                        list:[{
                            javaClass:"com.untangle.node.openvpn.ClientSiteNetwork",
                            "network" : "1.2.3.4",
                            "netmask" : "255.255.255.0"
                        }]
                    }
                },
                title : this.i18n._("VPN Sites"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnSite",
                data : siteList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name: 'exportedAddressList'
                }, {
                    name : 'network',
                    mapping : 'exportedAddressList',
                    convert : function(val, rec) {
                        return (val!=null && val.list!=null && val.list.length>0)?val.list[0].network:null;
                    }
                }, {
                    name : 'netmask',
                    mapping : 'exportedAddressList',
                    convert : function(val, rec) {
                        return (val!=null && val.list!=null && val.list.length>0)?val.list[0].netmask:null;
                    }
                }, {
                    name : 'group'
                }, {
                    name : 'groupId',
                    mapping: 'group',
                    convert : function(val, rec) {
                        return val==null?null:val.id;
                    }
                }],
                autoExpandColumn: 'name',
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("site name"),
                    width : 130,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'openvpnClientName'
                    })
                }, groupsColumn, {
                    id : 'network',
                    header : this.i18n._("network address"),
                    width : 100,
                    dataIndex : 'network',
                    renderer : function(value, metadata, record) {
                        record.data.exportedAddressList.list[0].network=value;
                        return value;
                    }.createDelegate( this ),
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("network mask"),
                    width : 100,
                    dataIndex : 'netmask',
                    renderer : function(value, metadata, record) {
                        record.data.exportedAddressList.list[0].netmask=value;
                        return value;
                    }.createDelegate( this ),
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, distributeColumn],
                // sortField : 'name',
                // columnsDefaultSortable : true,
                plugins : [liveColumn, distributeColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "site name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Site name"),
                    allowBlank : false,
                    vtype : 'openvpnClientName',
                    width : 200
                }, {
                    xtype : "combo",
                    name : "Address pool",
                    dataIndex : "groupId",
                    fieldLabel : this.i18n._("Address pool"),
                    store : this.getGroupsStore(),
                    displayField : 'name',
                    valueField : 'id',
                    editable : false,
                    mode : 'local',
                    triggerAction : 'all',
                    listClass : 'x-combo-list-small',
                    width : 200
                }, new Ung.form.TextField({
                    xtype : "textfield",
                    name : "Network address",
                    dataIndex : "network",
                    fieldLabel : this.i18n._("Network address"),
                    boxLabel : "(Internal address of remote site)",
                    allowBlank : false,
                    width : 200,
                    vtype : 'ipAddress'
                }), {
                    xtype : "textfield",
                    name : "Network mask",
                    dataIndex : "netmask",
                    fieldLabel : this.i18n._("Network mask"),
                    allowBlank : false,
                    width : 200,
                    vtype : 'ipAddress'
                }]
            });
            return gridSites;
        },
        buildClients : function() {
            this.gridClients = this.generateGridClients();
            this.gridSites = this.generateGridSites();
            this.panelClients = new Ext.Panel({
                // private fields
                name : 'Clients',
                helpSource : 'clients',
                parentId : this.getId(),
                title : this.i18n._('Clients'),
                layout : "form",
                autoScroll : true,
                items : [this.gridClients, this.gridSites]
            });
        },
        generateGridExports : function(inWizard) {
            // live is a check column
            var exportedAddressList=[];
            if(inWizard) {
                if (this.getExportedAddressList()!=null) {
                    exportedAddressList=this.getExportedAddressList().exportList.list;
                }
            } else {
                exportedAddressList=this.getVpnSettings().exportedAddressList.list;
            }
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });

            var gridExports = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Exported Hosts and Networks',
                helpSource : 'exported_hosts_and_network',
                // the total records is set from the base settings
                paginated : false,
                height : inWizard?220:null,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "network" : "192.168.1.0",
                    "netmask" : "255.255.255.0"
                },
                title : this.i18n._("Exported Hosts and Networks"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.ServerSiteNetwork",
                data : exportedAddressList,
                // the list of fields
                autoGenerateId: true,
                fields : [{
                    name : 'live'
                }, {
                    name : 'name'
                }, {
                    name : 'network'
                }, {
                    name : 'netmask'
                }],
                autoExpandMin: 250,
                autoExpandColumn: 'name',
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("host/network name"),
                    width : 250,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({})
                }, {
                    id : 'network',
                    header : this.i18n._("IP address"),
                    width : inWizard?100:130,
                    dataIndex : 'network',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("netmask"),
                    width : 130,
                    dataIndex : 'netmask',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }],
                // sortField : 'name',
                // columnsDefaultSortable : true,
                plugins : [liveColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Host/network name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Host/network name"),
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "IP address",
                    dataIndex : "network",
                    allowBlank : false,
                    fieldLabel : this.i18n._("IP address"),
                    vtype : 'ipAddress',
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "Netmask",
                    dataIndex : "netmask",
                    fieldLabel : this.i18n._("Netmask"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }]
            });
            return gridExports;
        },
        buildExports : function() {
            this.gridExports = this.generateGridExports();
        },
        generateGridGroups : function() {
            // live is a check column
            var liveColumn = new Ext.grid.CheckColumn({
                id : "live",
                header : this.i18n._("enabled"),
                dataIndex : 'live',
                width : 80,
                fixed : true
            });
            var exportDNSColumn = new Ext.grid.CheckColumn({
                id : "useDNS",
                header : this.i18n._("export DNS"),
                dataIndex : 'useDNS',
                width : 90,
                fixed : true
            });

            var gridGroups = new Ung.EditorGrid({
                settingsCmp : this,
                name : 'Address Pools',
                // the total records is set from the base settings
                anchor :"100% 100%",
                paginated : false,
                height : 250,
                autoScroll : true,
                emptyRow : {
                    "live" : true,
                    "name" : this.i18n._("[no name]"),
                    "group" : null,
                    "address" : "172.16.16.0",
                    "netmask": "255.255.255.0"
                },
                title : this.i18n._("Address Pools"),
                // the column is autoexpanded if the grid width permits
                // autoExpandColumn : 'name',
                recordJavaClass : "com.untangle.node.openvpn.VpnGroup",
                data : this.getVpnSettings().groupList,
                dataRoot : 'list',
                // the list of fields
                fields : [{
                    name : 'id'
                },{
                    name : 'live'
                },{
                    name : 'name'
                },{
                    name : 'address'
                },{
                    name : 'netmask'
                },{
                    name : 'useDNS'
                }],
                autoExpandColumn: 'name',
                // the list of columns for the column model
                columns : [liveColumn, {
                    id : 'name',
                    header : this.i18n._("pool name"),
                    width : 160,
                    dataIndex : 'name',
                    // this is a simple text editor
                    editor : new Ext.form.TextField({
                        allowBlank : false
                    })
                }, {
                    id : 'address',
                    header : this.i18n._("IP address"),
                    width : 130,
                    dataIndex : 'address',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, {
                    id : 'netmask',
                    header : this.i18n._("netmask"),
                    width : 130,
                    dataIndex : 'netmask',
                    editor : new Ext.form.TextField({
                        allowBlank : false,
                        vtype : 'ipAddress'
                    })
                }, exportDNSColumn],
                // sortField : 'name',
                // columnsDefaultSortable : true,
                plugins : [liveColumn, exportDNSColumn],
                // the row input lines used by the row editor window
                rowEditorInputLines : [{
                    xtype : 'checkbox',
                    name : "Enabled",
                    dataIndex : "live",
                    fieldLabel : this.i18n._("Enabled")
                }, {
                    xtype : "textfield",
                    name : "Pool name",
                    dataIndex : "name",
                    fieldLabel : this.i18n._("Pool name"),
                    allowBlank : false,
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "IP address",
                    dataIndex : "address",
                    fieldLabel : this.i18n._("IP address"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }, {
                    xtype : "textfield",
                    name : "Netmask",
                    dataIndex : 'netmask',
                    fieldLabel : this.i18n._("Netmask"),
                    allowBlank : false,
                    vtype : 'ipAddress',
                    width : 200
                }, {
                    xtype : 'checkbox',
                    name : "export DNS",
                    dataIndex : "useDNS",
                    fieldLabel : this.i18n._("export DNS")
                }]
            });
            return gridGroups;
        },
        buildAdvanced : function() {
            this.gridGroups = this.generateGridGroups();
            this.panelAdvanced = new Ext.Panel({
                name : 'Advanced',
                helpSource : 'advanced',
                title : this.i18n._("Advanced"),
                parentId : this.getId(),

                layout : "anchor",
                cls: 'ung-panel',
                autoScroll : true,
                defaults : {
                    anchor:"98%",
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    xtype : 'form',
                    border: false,
                    bodyStyle : 'padding-bottom:20px;',
                    items : this.gridGroups
                }, {
                    xtype : 'fieldset',
                    labelWidth: 160,
                    items : [{
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Server Port (UDP)'),
                        width : 80,
                        name : 'Server Port (UDP)',
                        id : 'openvpn_advanced_publicPort',
                        value : this.getVpnSettings().publicPort,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._("You must provide a valid port."),
                        vtype : 'port',
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getVpnSettings().publicPort = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        fieldLabel : this.i18n._('Site Name'),
                        name : 'Site Name',
                        value : this.getVpnSettings().siteName,
                        id : 'openvpn_advanced_siteName',
                        allowBlank : false,
                        blankText : this.i18n._("You must enter a site name."),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getVpnSettings().siteName = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radiogroup',
                        fieldLabel : this.i18n._('DNS Override'),
                        columns : 1,
                        name : 'Site Name',
                        items : [{
                            boxLabel : this.i18n._('Enabled'),
                            name : 'DNSOverride',
                            inputValue : true,
                            checked : this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = checked;
                                        if (checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();

                                        } else {
                                            Ext.getCmp("openvpn_advanced_dns1").disable();
                                            Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            boxLabel : this.i18n._('Disabled'),
                            name : 'DNSOverride',
                            inputValue : false,
                            checked : !this.getVpnSettings().isDnsOverrideEnabled,
                            listeners : {
                                "check" : {
                                    fn : function(elem, checked) {
                                        this.getVpnSettings().isDnsOverrideEnabled = !checked;
                                        if (!checked) {
                                            Ext.getCmp("openvpn_advanced_dns1").enable();
                                            Ext.getCmp("openvpn_advanced_dns2").enable();

                                        } else {
                                            Ext.getCmp("openvpn_advanced_dns1").disable();
                                            Ext.getCmp("openvpn_advanced_dns2").disable();
                                        }
                                    }.createDelegate(this)
                                }
                            }

                        }]
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        style : 'margin:0px 0px 0px 160px;',
                        labelWidth: 160,
                        border : false,
                        items : [{
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Primary IP'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns1',
                            value : this.getVpnSettings().dns1,
                            disabled : !this.getVpnSettings().isDnsOverrideEnabled,
                            allowBlank : false,
                            blankText : this.i18n._('A Valid Primary IP Address must be specified.'),
                            vtype : 'ipAddress'
                        }, {
                            xtype : 'textfield',
                            fieldLabel : this.i18n._('Secondary IP (optional)'),
                            name : 'outsideNetwork',
                            id : 'openvpn_advanced_dns2',
                            disabled : !this.getVpnSettings().isDnsOverrideEnabled,
                            value : this.getVpnSettings().dns2,
                            vtype : 'ipAddress'
                        }]
                    }]
                }]
            });
        },

        // validation function
        validateClient : function() {
            return  this.validateAdvanced() && this.validateGroups() &&
                this.validateVpnClients() && this.validateVpnSites();
        },

        //validate OpenVPN Advanced settings
        validateAdvanced : function() {
            //validate port
            var portCmp = Ext.getCmp("openvpn_advanced_publicPort");
            if(!portCmp.validate()) {
                Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must provide a valid port."),
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                        portCmp.focus(true);
                    }.createDelegate(this)
                );
                return false;
            };

            //validate site name
            var siteCmp = Ext.getCmp("openvpn_advanced_siteName");
            if(!siteCmp.validate()) {
                Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("You must enter a site name."),
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                        siteCmp.focus(true);
                    }.createDelegate(this)
                );
                return false;
            };

            if (this.getVpnSettings().isDnsOverrideEnabled) {
                var dns1Cmp = Ext.getCmp("openvpn_advanced_dns1");
                if(!dns1Cmp.validate()) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("A valid Primary IP Address must be specified."),
                        function () {
                            this.tabs.activate(this.panelAdvanced);
                            dns1Cmp.focus(true);
                        }.createDelegate(this)
                    );
                    return false;
                };

                var dns2Cmp = Ext.getCmp("openvpn_advanced_dns2");
                if(!dns2Cmp.validate()) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), this.i18n._("A valid Secondary IP Address must be specified."),
                        function () {
                            this.tabs.activate(this.panelAdvanced);
                            dns2Cmp.focus(true);
                        }.createDelegate(this)
                    );
                    return false;
                };

                //prepare for save
                this.getVpnSettings().dns1 = dns1Cmp.getValue();
                this.getVpnSettings().dns2 = dns2Cmp.getValue() == "" ? null : dns2Cmp.getValue();
            }  else {
                this.getVpnSettings().dns1 = null;
                this.getVpnSettings().dns2 = null;
            }

            return true;
        },

        validateGroups : function() {
            var groupList=this.gridGroups.getFullSaveList();

            // verify that there is at least one group
            if(groupList.length <= 0 ){
                Ext.MessageBox.alert(this.i18n._('Failed'), this.i18n._("You must create at least one group."),
                    function () {
                        this.tabs.activate(this.panelAdvanced);
                    }.createDelegate(this)
                );
                return false;
            }

            // removed groups should not be referenced
            var removedGroups = this.gridGroups.getDeletedList();
            for(var i=0; i<removedGroups.length;i++) {
                var clientList = this.gridClients.getFullSaveList();
                for(var j=0; j<clientList.length;j++) {
                    if (removedGroups[i].id == clientList[j].groupId) {
                        Ext.MessageBox.alert(this.i18n._('Failed'),
                            String.format(this.i18n._("The group: \"{0}\" cannot be deleted because it is being used by the client: {1} in the Client To Site List."), removedGroups[i].name, clientList[j].name),
                            function () {
                                this.tabs.activate(this.panelAdvanced);
                            }.createDelegate(this)
                        );
                        return false;
                    }
                }
                var siteList=this.gridSites.getFullSaveList();
                for(var j=0; j<siteList.length;j++) {
                    if (removedGroups[i].id == siteList[j].groupId) {
                        Ext.MessageBox.alert(this.i18n._('Failed'),
                            String.format(this.i18n._("The group: \"{0}\" cannot be deleted because it is being used by the site: {1} in the Site To Site List."), removedGroups[i].name, siteList[j].name),
                            function () {
                                this.tabs.activate(this.panelAdvanced);
                            }.createDelegate(this)
                        );
                        return false;
                    }
                }
            }

            // Group names must all be unique
            for(var i=0;i<groupList.length;i++) {
                for(var j=i+1; j<groupList.length;j++) {
                    if (groupList[i].name.toLowerCase() == groupList[j].name.toLowerCase()) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), String.format(this.i18n._("The group name: \"{0}\" in row: {1} already exists."), groupList[j].name.toLowerCase(), j+1),
                            function () {
                                this.tabs.activate(this.panelAdvanced);
                            }.createDelegate(this)
                        );
                        return false;
                    }
                }
            }

            return true;
        },

        validateVpnClients : function() {
            var clientList=this.gridClients.getFullSaveList();
            for(var i=0;i<clientList.length;i++) {
                if(clientList[i].id>=0 && clientList[i].name!=clientList[i].originalName) {
                    Ext.MessageBox.alert(i18n._("Failed"), String.format(this.i18n._("Unable to change name once you've saved. Create a new user. Client name should be {0}."), clientList[i].originalName),
                        function () {
                            this.tabs.activate(this.panelClients);
                        }.createDelegate(this)
                    );
                    return false;
                }

                // Client names must all be unique
                for(var j=i+1; j<clientList.length;j++) {
                    if (clientList[i].name == clientList[j].name) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), String.format(this.i18n._("The client name: \"{0}\" in row: {1} already exists."), clientList[j].name, j+1),
                            function () {
                                this.tabs.activate(this.panelClients);
                            }.createDelegate(this)
                        );
                        return false;
                    }
                }
            }
            return true;
        },

        validateVpnSites : function() {
            var siteList=this.gridSites.getFullSaveList();
            // Site names must all be unique
            for(var i=0;i<siteList.length;i++) {
                for(var j=i+1; j<siteList.length;j++) {
                    if (siteList[i].name == siteList[j].name) {
                        Ext.MessageBox.alert(this.i18n._('Failed'), String.format(this.i18n._("The site name: \"{0}\" in row: {1} already exists."), siteList[j].name, j+1),
                            function () {
                                this.tabs.activate(this.panelClients);
                            }.createDelegate(this)
                        );
                        return false;
                    }
                }
            }
            return true;
        },

        validateServer : function() {
            var validateData = {
                map : {},
                javaClass : "java.util.HashMap"
            };
            var groupList=this.gridGroups.getFullSaveList();
            if (groupList.length > 0) {
                validateData.map["GROUP_LIST"] = {"javaClass" : "java.util.ArrayList", list : groupList};
            }
            var siteList=this.gridSites.getFullSaveList();
            if (siteList.length > 0) {
                validateData.map["SITE_LIST"] = {"javaClass" : "java.util.ArrayList", list : siteList};
            }
            var exportList=this.gridExports.getFullSaveList();
            if (exportList.length > 0) {
                validateData.map["EXPORT_LIST"] = {"javaClass" : "java.util.ArrayList", list : exportList};
            }

            // now let the server validate
            if (Ung.Util.hasData(validateData.map)) {
                try {
                    var result=null;
                    try {
                        result = this.getValidator().validate(validateData);
                    } catch (e) {
                        Ung.Util.rpcExHandler(e);
                    }

                    if (!result.valid) {
                        var errorMsg = "";
                        var tabToActivate = null;
                        switch (result.errorCode) {
                        case 'ERR_GROUP_LIST_OVERLAP' :
                            errorMsg = String.format(this.i18n._("The two networks: {0} and {1} cannot overlap"),result.cause[0],result.cause[1]);
                            tabToActivate = this.panelAdvanced;
                            break;
                        case 'ERR_SITE_LIST_OVERLAP' :
                            errorMsg = String.format(this.i18n._("The two networks: {0} and {1} cannot overlap"),result.cause[0],result.cause[1]);
                            tabToActivate = this.panelClients;
                            break;
                        case 'ERR_EXPORT_LIST_OVERLAP' :
                            errorMsg = String.format(this.i18n._("The two networks: {0} and {1} cannot overlap"),result.cause[0],result.cause[1]);
                            tabToActivate = this.gridExports;
                            break;
                        default :
                            errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                        }
                        Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg,
                                             function() {
                                                 this.settingsCmp.tabs.activate(this.tabToActivate);
                                             }.createDelegate({settingsCmp:this,tabToActivate:tabToActivate} )
                                            );
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), e.message);
                    return false;
                }
            }

            return true;
        },

        validateExports : function(exportList) {
            if (exportList.length > 0) {
                var validateData = {
                    map : {},
                    javaClass : "java.util.HashMap"
                };
                validateData.map["EXPORT_LIST"] = {"javaClass" : "java.util.ArrayList", list : exportList};

                // now let the server validate
                try {
                    var result = this.getValidator().validate(validateData);
                    if (!result.valid) {
                        var errorMsg = "";
                        switch (result.errorCode) {
                            case 'ERR_EXPORT_LIST_OVERLAP' :
                                errorMsg = String.format(this.i18n._("The two networks: {0} and {1} cannot overlap"),result.cause[0],result.cause[1]);
                            break;
                            default :
                                errorMsg = this.i18n._(result.errorCode) + ": " + result.cause;
                        }
                        Ext.MessageBox.alert(this.i18n._("Validation failed"), errorMsg);
                        return false;
                    }
                } catch (e) {
                    Ext.MessageBox.alert(this.i18n._("Failed"), e.message);
                    return false;
                }
            }
            return true;
        },

        saveAction : function()
        {
            this.commitSettings(this.completeSaveAction.createDelegate( this ));
        },
        
        completeSaveAction : function()
        {
            Ext.MessageBox.hide();
            this.closeWindow();
        },

        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },

        commitSettings : function(callback)
        {            
            // validate first
            if(this.configState == "SERVER_ROUTE") {
                if (this.validate()) {
                    Ext.MessageBox.wait(this.i18n._("Saving..."), this.i18n._("Please wait"));

                    var vpnSettings = this.getVpnSettings();
                    vpnSettings.groupList.list = this.gridGroups.getFullSaveList();
                    vpnSettings.exportedAddressList.list = this.gridExports.getFullSaveList();
                    vpnSettings.clientList.list = this.gridClients.getFullSaveList();
                    vpnSettings.siteList.list = this.gridSites.getFullSaveList();
                    
                    this.getRpcNode().setVpnSettings(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;                        
                        callback();
                    }.createDelegate(this), vpnSettings);
                }
            } else {
                callback();
            }
        },

        reloadSettings : function()
        {
            this.getRpcNode().getVpnSettings(this.completeReloadSettings.createDelegate( this ));
        },
        
        completeReloadSettings : function( result, exception )
        {
            if(Ung.Util.handleException(exception)) {
                return;
            }
            
            this.rpc.vpnSettings = result;
            this.initialVpnSettings = Ung.Util.clone(this.rpc.vpnSettings);
            /* Assume the config state hasn't changed */
            if (this.configState == "SERVER_ROUTE") {
                var groupStore = this.getGroupsStore(true);

                var defaultGroup  = groupStore.getCount()>0 ?groupStore.getAt(0).data:null;

                this.gridSites.emptyRow.groupId = defaultGroup==null? null : defaultGroup.id;
                this.gridSites.emptyRow.group = defaultGroup;
                
                this.gridClients.emptyRow.groupId =  defaultGroup.id;
                this.gridClients.emptyRow.group =  defaultGroup;   

                this.gridClients.clearChangedData();
                this.gridClients.store.loadData( this.rpc.vpnSettings.clientList.list );                                                
                this.gridSites.clearChangedData();
                this.gridSites.store.loadData( this.rpc.vpnSettings.siteList.list );
                this.gridGroups.clearChangedData();
                this.gridGroups.store.loadData( this.rpc.vpnSettings.groupList );
                this.gridExports.clearChangedData();
                this.gridExports.store.loadData( this.rpc.vpnSettings.exportedAddressList.list );
                
                Ext.getCmp( "openvpn_advanced_publicPort" ).setValue( this.rpc.vpnSettings.publicPort );
                Ext.getCmp( "openvpn_advanced_siteName" ).setValue( this.rpc.vpnSettings.siteName );
                
                /* Assuming radio box is intact */
                Ext.getCmp("openvpn_advanced_dns1").setValue( this.rpc.dns1 );
                Ext.getCmp("openvpn_advanced_dns2").setValue( this.rpc.dns2 );
            } else {
                /* do nothing */
            }

            Ext.MessageBox.hide();
        },
        
        isDirty : function() {
            if(this.configState == "SERVER_ROUTE") {
                return !Ung.Util.equals(this.getVpnSettings(), this.initialVpnSettings)
                    || this.gridGroups.isDirty()
                    || this.gridExports.isDirty()
                    || this.gridClients.isDirty()
                    || this.gridSites.isDirty();
            } else {
            	return false;
            }
        },
        configureVPNClient : function() {
            if (this.clientSetup) {
                Ext.destroy(this.clientSetup)
            }

            welcomeCard = new Ung.Node.OpenVPN.ClientWizard.Welcome({
                i18n : this.i18n,
                node : this.getRpcNode()
            });

            downloadCard = new Ung.Node.OpenVPN.ClientWizard.LoadConfig({
                i18n : this.i18n,
                node : this.getRpcNode()
            });

            congratulationsCard = new Ung.Node.OpenVPN.ClientWizard.Congratulations({
                i18n : this.i18n,
                node : this.getRpcNode(),
                gui : this
            });

            var clientWizard = new Ung.Wizard({
                height : 450,
                width : 'auto',
                modalFinish: true,
                hasCancel: true,
                cardDefaults : {
                    labelWidth : 150,
                    cls : 'untangle-form-panel'
                },
                cards : [welcomeCard, downloadCard, congratulationsCard]
            });

            this.clientSetup = new Ung.Window({
                title : this.i18n._("OpenVPN Client Setup Wizard"),
                closeAction : "cancelAction",
                defaults: {},
                wizard: clientWizard,
                items : [{
                    region : "center",
                    items : [clientWizard],
                    border : false,
                    autoScroll : true,
                    cls : 'window-background',
                    bodyStyle : 'background-color: transparent;'
                }],
                    endAction : function() {
                    this.clientSetup.hide();
                    Ext.destroy(this.clientSetup);
                    if(this.mustRefresh || true) {
                        var nodeWidget=this.node;
                        nodeWidget.settingsWin.closeWindow();
                        nodeWidget.onSettingsAction();
                    }
                }.createDelegate(this),
                cancelAction: function() {
                    this.clientSetup.wizard.cancelAction();
                }.createDelegate(this)
            });

            clientWizard.cancelAction=function() {
                if(!this.clientSetup.wizard.finished) {
                    Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring OpenVPN. Please run the Setup Wizard again."), function () {
                        this.clientSetup.endAction();
                    }.createDelegate(this));
                } else {
                    this.clientSetup.endAction();
                }
            }.createDelegate(this);

            this.clientSetup.show();
            clientWizard.goToPage(0);
        },

        completeServerWizard : function( handler )
        {
            this.node.start();
            this.serverSetup.endAction();
        },

        //
        configureVPNServer : function() {
            if (this.serverSetup) {
                Ext.destroy(this.serverSetup)
            }

            var welcomeCard = {
                title : this.i18n._("Welcome"),
                panel : {
                    xtype : 'form',
                    items : [{
                        xtype : 'label',
                        html : '<h2 class="wizard-title">'+this.i18n._("Welcome to the OpenVPN Setup Wizard!")+'</h2>'
                    }, {
                        html : this.i18n._('This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Routing Server.'),
                        bodyStyle : 'padding-bottom:20px;',
                        cls: 'description',
                        border : false
                    }, {
                        html : this.i18n
                                ._('Warning: Completing this wizard will overwrite any previous OpenVPN settings with new settings. All previous settings will be lost!'),
                        cls: 'description warning',
                        border : false
                    }]
                },
                onNext : function(handler) {
                    handler();
                }
            };
            var registrationInfo=this.getRegistrationInfo();
            var country="US";
            var companyName="";
            var state="";
            var city="";

            if(registrationInfo!=null) {
                if(registrationInfo.misc!=null && registrationInfo.misc.map!=null && registrationInfo.misc.map.country!=null) {
                    country=Ung.Country.getCountryCode(registrationInfo.misc.map.country);
                    if (country == null) {
                        country="US";
                    }
                }
                if( registrationInfo.companyName!=null) {
                    companyName=registrationInfo.companyName;
                }
                if( registrationInfo.state!=null) {
                    state=registrationInfo.state;
                }
                if( registrationInfo.city!=null) {
                    city=registrationInfo.city;
                }

            }
            var certificateCard = {
                title : this.i18n._("Step 1 - Certificate"),
                panel : {
                    xtype : 'form',
                    items : [{
                        xtype : 'label',
                        html : '<h2 class="wizard-title">'+this.i18n._("Step 1 - Certificate")+'</h2>'
                    }, {
                        html : this.i18n
                                ._('Please specify some information about your location. This information will be used to generate a secure digital certificate.'),
                        cls: 'description',
                        border : false
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        buttonAlign : 'left',
                        title : this.i18n._('This information is required.'),
                        items : [{
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_organization',
                            name : "Organization",
                            fieldLabel : this.i18n._("Organization"),
                            allowBlank : false,
                            value: companyName,
                            width : 200
                        }, {
                            fieldLabel : this.i18n._('Country'),
                            id : 'openvpn_server_wizard_country',
                            name : "Country",
                            xtype : 'combo',
                            width : 200,
                            listWidth : 205,
                            store : Ung.Country.getCountryStore(this.i18n),
                            mode : 'local',
                            triggerAction : 'all',
                            listClass : 'x-combo-list-small',
                            value : country
                        }, {
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_state',
                            name : "State/Province",
                            fieldLabel : this.i18n._("State/Province"),
                            allowBlank : false,
                            value: state,
                            width : 200
                        }, {
                            xtype : "textfield",
                            id : 'openvpn_server_wizard_locality',
                            name : "City",
                            fieldLabel : this.i18n._("City"),
                            allowBlank : false,
                            value: city,
                            width : 200
                        }]
                    }]
                },
                onNext : function(handler) {
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
                    this.getRpcNode().generateCertificate(function(result, exception) {
                        Ext.MessageBox.hide();
                        if(Ung.Util.handleException(exception)) return;
                        this.handler();
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), certificateParameters)
                }.createDelegate(this)
            };
            var gridExports = this.generateGridExports(true);
            var exportsCard = {
                title : this.i18n._("Step 2 - Exports"),
                panel : {
                    xtype : 'form',
                    items : [{
                        xtype : 'label',
                        html : '<h2 class="wizard-title">'+this.i18n._("Step 2 - Exports")+'</h2>'
                    }, {
                        html : this.i18n._('Please complete the list of exports. This is a list of hosts and networks which remote VPN users and networks will be able to contact.'),
                        cls: 'description',
                        bodyStyle: 'padding-bottom:10px',
                        border : false
                    }, {
                        html : "<i>" + this.i18n._('By default the entire internal network is exported.') + "</i>",
                        cls: 'description',
                        border : false
                    }, gridExports]
                },
                onNext : function(handler) {
                    var gridExports=Ext.getCmp(this.serverSetup.gridExportsId);
                    var saveList=gridExports.getFullSaveList();

                    if (!this.validateExports(saveList)) {
                        return;
                    }

                    this.getExportedAddressList().exportList.list = saveList;

                    Ext.MessageBox.wait(this.i18n._("Adding Exports..."), this.i18n._("Please wait"));
                    this.getRpcNode().setExportedAddressList(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.settingsCmp.getRpcNode().completeConfig(function(result, exception) { //complete server configuration
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            // go to next step
                            this.handler();
                        }.createDelegate(this))
                    }.createDelegate({
                            handler : handler,
                            settingsCmp : this
                        }), this.getExportedAddressList())
                }.createDelegate(this)
            };
            var congratulationsCard = {
                title : this.i18n._("Finished!"),
                panel : {
                    xtype : 'form',
                    items : [{
                        xtype : 'label',
                        html : '<h2 class="wizard-title">'+this.i18n._("Finished!")+'</h2>'
                    }, {
                        html : this.i18n._('Congratulations!'),
                        cls: 'description',
                        border : false
                    }, {
                        html : this.i18n._('You are now ready to begin adding remote clients and sites you wish to have access to your VPN.'),
                        cls: 'description',
                        border : false
                    }, {
                        html : this.i18n
                                ._('To add remote users, click on the Clients tab and add to the VPN Clients table.<br/>To add remote networks, click on the Clients tab and add to the VPN Sites table.'),
                        cls: 'description',
                        border : false
                    }]
                },
                onNext : this.completeServerWizard.createDelegate(this)
            };
            var serverWizard = new Ung.Wizard({
                height : 450,
                width : 'auto',
                modalFinish: true,
                hasCancel: true,
                cardDefaults : {
                    labelWidth : 150,
                    cls : 'untangle-form-panel'
                },
                cards : [welcomeCard, certificateCard, exportsCard, congratulationsCard]
            });

            this.serverSetup = new Ung.Window({
                gridExportsId: gridExports.getId(),
                title : this.i18n._("OpenVPN Server Setup Wizard"),
                closeAction : "cancelAction",
                defaults: {},
                wizard: serverWizard,
                items : [{
                    region : "center",
                    items : [serverWizard],
                    border : false,
                    autoScroll : true,
                    cls : 'window-background',
                    bodyStyle : 'background-color: transparent;'
                }],
                endAction : function() {
                    this.serverSetup.hide();
                    Ext.destroy(this.serverSetup);
                    if(this.mustRefresh || true) {
                        var nodeWidget=this.node;
                        nodeWidget.settingsWin.closeWindow();
                        nodeWidget.onSettingsAction();
                    }
                }.createDelegate(this),
                cancelAction: function() {
                    this.serverSetup.wizard.cancelAction();
                }.createDelegate(this)
            });
            serverWizard.cancelAction=function() {
                if(!this.serverSetup.wizard.finished) {
                    Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring OpenVPN. Please run the Setup Wizard again."), function () {
                        this.serverSetup.endAction();
                    }.createDelegate(this));
                } else {
                    this.serverSetup.endAction();
                }
            }.createDelegate(this);
            this.serverSetup.show();
            serverWizard.goToPage(0);
        }
    });
}
