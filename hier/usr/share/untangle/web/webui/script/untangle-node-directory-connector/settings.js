Ext.define('Webui.untangle-node-directory-connector.settings', {
    extend:'Ung.NodeWin',
    panelUserNotificationApi: null,
    panelActiveDirectoryConnector: null,
    panelRadius: null,
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._("Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.");
    },

    initComponent: function(container, position) {
        this.buildRefreshTask();
        
        this.buildUserNotificationApi();
        this.buildActiveDirectoryConnector();
        this.buildRadius();
        this.buildGoogle();
        this.buildFacebook();

        this.buildTabPanel([this.panelUserNotificationApi, this.panelActiveDirectoryConnector, this.panelRadius, this.panelGoogle, this.panelFacebook]);
        this.callParent(arguments);

    },
    beforeDestroy: function() {
        this.refreshGoogleTask.stop();
        this.callParent(arguments);
    },

    buildRefreshTask: function() {
        var me = this;
        this.refreshGoogleTask = {
            // update interval in millisecond
            updateFrequency: 3000,
            count:0,
            maxTries: 40,
            started: false,
            intervalId: null,
            start: function() {
                this.stop();
                this.count=0;
                this.intervalId = window.setInterval(this.run, this.updateFrequency);
                this.started = true;
            },
            stop:function() {
                if (this.intervalId !== null) {
                    window.clearInterval(this.intervalId);
                    this.intervalId = null;
                }
                this.started = false;
            },
            run: Ext.bind(function () {
                if(!this || !this.rendered) {
                    return;
                }
                this.refreshGoogleTask.count++;

                if ( this.refreshGoogleTask.count > this.refreshGoogleTask.maxTries ) {
                    this.refreshGoogleTask.stop();
                    return;
                }

                this.getGoogleManager().isGoogleDriveConnected(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.googleDriveConnected = result;
                    var googleDriveStatus = this.panelGoogle.down('component[name=googleDriveStatus]');
                    googleDriveStatus.setHtml((this.googleDriveConnected ? i18n._("The Google Connector is configured.") : i18n._("The Google Connector is unconfigured.")));
                    googleDriveStatus.setStyle((this.googleDriveConnected ? {color:'green'} : {color:'red'}));
                    var disableButton = this.panelGoogle.down('button[name=disconnect_google_drive_button]');
                    disableButton.setDisabled( !this.googleDriveConnected );

                    if ( this.googleDriveConnected ) {
                        this.refreshGoogleTask.stop();
                        return;
                    }
                }, this));

            },this)
        };
    },
    
    getActiveDirectorySettings: function() {
        return this.getSettings().activeDirectorySettings;
    },
    getRadiusSettings: function() {
        return this.getSettings().radiusSettings;
    },
    getGoogleSettings: function() {
        return this.getSettings().googleSettings;
    },
    getFacebookSettings: function() {
        return this.getSettings().facebookSettings;
    },
    getActiveDirectoryManager: function(forceReload) {
        if (forceReload || this.rpc.activeDirectoryManager === undefined) {
            try {
                this.rpc.activeDirectoryManager = this.getRpcNode().getActiveDirectoryManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.activeDirectoryManager;
    },
    getGoogleManager: function(forceReload) {
        if (forceReload || this.rpc.googleManager === undefined) {
            try {
                this.rpc.googleManager = this.getRpcNode().getGoogleManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.googleManager;
    },
    getFacebookManager: function(forceReload) {
        if (forceReload || this.rpc.facebookManager === undefined) {
            try {
                this.rpc.facebookManager = this.getRpcNode().getFacebookManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.facebookManager;
    },
    getRadiusManager: function(forceReload) {
        if (forceReload || this.rpc.radiusManager === undefined) {
            try {
                this.rpc.radiusManager = this.getRpcNode().getRadiusManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.radiusManager;
    },
    
    openUserGroupMap: function() {
        if(!this.winUserGroupMap) {
            this.winUserGroupMap = Ext.create('Ung.Window', {
                title: i18n._('User map window'),
                bbar: ['->', {
                    name: "Cancel",
                    iconCls: 'cancel-icon',
                    text: i18n._('Cancel'),
                    handler: function() {
                        this.up('window').cancelAction();
                    }
                }," "],
                items: Ext.create('Ung.grid.Panel',{
                    name: 'usersGrid',
                    hasEdit: false,
                    hasDelete: false,
                    hasAdd: false,
                    sortField: 'name',
                    reserveScrollbar: true,
                    dataFn: Ext.bind(function(handler) {
                        this.getActiveDirectoryManager().getUserGroupMap( Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            var users = [];
                            for ( var k in result.map) {
                                users.push({name: k, groups: result.map[k]});
                            }
                            handler({list: users});
                        },this));
                    }, this),
                    plugins:['gridfilters'],
                    columnMenuDisabled: false,
                    fields:[{
                        name: 'name'
                    },{
                        name:'groups'
                    }],
                    columns: [{
                        header: i18n._('name'),
                        dataIndex:'name',
                        sortable: true,
                        width: 180,
                        filter: {
                            type: 'string'
                        }
                    },{
                        header: i18n._('groups'),
                        dataIndex: 'groups',
                        sortable: true,
                        flex: 1,
                        filter: {
                            type: 'string'
                        }
                    }]
                })
            });
            this.subCmps.push(this.winUserGroupMap);
        }
        this.winUserGroupMap.show();
        this.winUserGroupMap.down('[name=usersGrid]').reload();
    },
    buildUserNotificationApi: function() {
        this.panelUserNotificationApi = Ext.create('Ext.panel.Panel',{
            name: 'User Notification API',
            helpSource: 'directory_connector_user_notification_api',
            title: i18n._('User Notification API'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                title: i18n._('User Notification API'),
                name: 'User Notification API',
                items: [{
                    xtype: 'component',
                    html: Ext.String.format(i18n._('The User Notification API provides a web-based app/API to allow scripts and agents to update the server\'s username-to-IP address mapping in order to properly identify users for Policy Manager, Reports, and other applications.'),'<b>','</b>')
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Disabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableNotification',
                    checked: !this.getSettings().apiEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getSettings().apiEnabled = !checked;
                                this.panelUserNotificationApi.down('textfield[name=secretKey]').setDisabled(checked);
                                this.panelUserNotificationApi.down('button[name=downloadLoginScript]').setDisabled(checked);
                            }, this)
                        }
                    }
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Enabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableNotification',
                    checked: this.getSettings().apiEnabled
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype:'textfield',
                        name: 'secretKey',
                        fieldLabel: i18n._('Secret Key'),
                        labelWidth: 250,
                        labelAlign: 'right',
                        width: 450,
                        value: this.getSettings().apiSecretKey,
                        disabled: (!this.getSettings().apiEnabled),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getSettings().apiSecretKey = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'label',
                        html: i18n._('(blank means no secret key is required)'),
                        cls: 'boxlabel'
                    }]
                }, {
                    xtype: 'component',
                    html: "<hr />",
                    style: {marginRight: '20px'}
                }, {
                    xtype: 'button',
                    margin: '10 0 0 0',
                    text: i18n._('Download User Notification Login Script'),
                    name: 'downloadLoginScript',
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        var downloadUrl = "../userapi/";
                        window.open(downloadUrl);
                    }, this)
                }]
            }]
        });
    },

    buildActiveDirectoryConnector: function() {
        this.panelActiveDirectoryConnector = Ext.create('Ext.panel.Panel',{
            name: 'Active Directory',
            helpSource: 'directory_connector_active_directory_connector',
            title: i18n._('Active Directory'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                title: i18n._('Active Directory Connector'),
                name: 'Active Directory Connector',
                xtype: 'fieldset',
                items: [{
                    xtype: 'component',
                    html: Ext.String.format(i18n._('This allows the server to connect to an {0}Active Directory Server{1} for authentication for Captive Portal.'),'<b>','</b>')
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Disabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableAD',
                    checked: !this.getActiveDirectorySettings().enabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getActiveDirectorySettings().enabled = !checked;
                                var panel = this.panelActiveDirectoryConnector;
                                var components = [
                                    panel.down("textfield[name=LDAPHost]"),
                                    panel.down("checkbox[name=LDAPSecure]"),
                                    panel.down("textfield[name=LDAPPort]"),
                                    panel.down("textfield[name=superuser]"),
                                    panel.down("textfield[name=superuserPass]"),
                                    panel.down("textfield[name=domain]"),
                                    panel.down("textfield[name=OUFilter]"),
                                    panel.down("button[name=ActiveDirectoryTest]"),
                                    panel.down("button[name=ActiveDirectoryUsers]"),
                                    panel.down("textarea[name=ADUsersTextArea]"),
                                    panel.down("button[name=UserGroupMap]"),
                                    panel.down("button[name=RefreshGroupCache]")
                                ];

                                for(var i=0; i<components.length; i++) {
                                    components[i].setDisabled(checked);
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Enabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableAD',
                    checked: this.getActiveDirectorySettings().enabled
                }, {
                    xtype: 'textfield',
                    name: 'LDAPHost',
                    fieldLabel: i18n._('AD Server IP or Hostname'),
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getActiveDirectorySettings().LDAPHost,
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getActiveDirectorySettings().LDAPHost = newValue;
                        }, this )
                    }
                }, {
                    xtype: 'checkbox',
                    name: 'LDAPSecure',
                    fieldLabel: i18n._('Secure'),
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getActiveDirectorySettings().LDAPSecure,
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getActiveDirectorySettings().LDAPSecure = newValue;
                            var ldapPort = this.panelActiveDirectoryConnector.down("textfield[name=LDAPPort]");
                            if( newValue == true ){
                                if( ldapPort.getValue() == "389"){
                                    ldapPort.setValue("636");
                                }
                            }else{
                                if( ldapPort.getValue() == "636"){
                                    ldapPort.setValue("389");
                                }

                            }
                        }, this )
                    }
                }, {
                    xtype: 'textfield',
                    name: 'LDAPPort',
                    fieldLabel: i18n._('Port'),
                    allowBlank: false,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getActiveDirectorySettings().LDAPPort,
                    vtype: "port",
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getActiveDirectorySettings().LDAPPort = newValue;
                        }, this )
                    }
                }, {
                    xtype: 'textfield',
                    name: 'superuser',
                    fieldLabel: i18n._('Authentication Login'),
                    labelWidth: 250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getActiveDirectorySettings().superuser,
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getActiveDirectorySettings().superuser = newValue;
                        }, this )
                    }
                }, {
                    xtype: 'textfield',
                    inputType: 'password',
                    name: 'superuserPass',
                    fieldLabel: i18n._('Authentication Password'),
                    labelWidth: 250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getActiveDirectorySettings().superuserPass,
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getActiveDirectorySettings().superuserPass = newValue;
                        }, this )
                    }
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype:'textfield',
                        name: 'domain',
                        fieldLabel: i18n._('Active Directory Domain'),
                        labelWidth: 250,
                        labelAlign: 'right',
                        width: 450,
                        value: this.getActiveDirectorySettings().domain,
                        disabled: (!this.getActiveDirectorySettings().enabled),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getActiveDirectorySettings().domain = newValue;
                            }, this )
                        }
                    },{
                        xtype: 'label',
                        html: i18n._('(FQDN such as mycompany.local)'),
                        cls: 'boxlabel'
                    }]
                }, {
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'textfield',
                        name: 'OUFilter',
                        fieldLabel: i18n._('Active Directory Organization'),
                        labelWidth:250,
                        labelAlign:'right',
                        width: 450,
                        value: this.getActiveDirectorySettings().OUFilter,
                        disabled: (!this.getActiveDirectorySettings().enabled),
                        listeners: {
                            "change": Ext.bind(function( elem, newValue ) {
                                this.getActiveDirectorySettings().OUFilter = newValue;
                            }, this )
                        }
                    }, {
                        xtype: 'label',
                        html: i18n._('(optional)'),
                        cls: 'boxlabel'
                    }]
                }, {
                    xtype: 'component',
                    html: "<hr />",
                    style: {marginRight: '20px'}
                }, {
                    xtype: 'component',
                    html: Ext.String.format(i18n._('The {0}Active Directory Test{1} verifies that the server can connect to the Active Directory Server.'),'<b>','</b>')
                }, {
                    xtype: 'button',
                    margin: '10 0 5 0',
                    text: i18n._('Active Directory Test'),
                    iconCls: 'test-icon',
                    name: 'ActiveDirectoryTest',
                    disabled: (!this.getActiveDirectorySettings().enabled),
                    handler: Ext.bind(function() {
                        this.panelActiveDirectoryConnector.onADTestClick();
                    }, this)
                }, {
                    xtype: 'container',
                    html: "<hr />",
                    style: {marginRight: '20px'}
                }, {
                    xtype: 'container',
                    layout:'column',
                    items: [ {
                        xtype: 'panel',
                        border: false,
                        flex: 0,
                        width: 255,
                        buttonAlign: 'right',
                        bodyStyle: "padding-top: 90px",
                        buttons: [{
                            xtype: 'button',
                            text: i18n._('Active Directory Users'),
                            name: 'ActiveDirectoryUsers',
                            disabled: (!this.getActiveDirectorySettings().enabled),
                            handler: Ext.bind(function() {
                                this.panelActiveDirectoryConnector.onADUsersClick();
                            }, this)
                        }]
                    }, {
                        border: false,
                        flex: 1,
                        items: [ {
                            xtype: 'textarea',
                            name: 'ADUsersTextArea',
                            hideLabel: true,
                            readOnly: true,
                            width: 300,
                            height: 200,
                            disabled: (!this.getActiveDirectorySettings().enabled),
                            isDirty: function(){
                                return false;
                            }
                        }, {
                            xtype: 'button',
                            name: 'UserGroupMap',
                            text: i18n._('User Group Map'),
                            iconCls: 'test-icon',
                            disabled: (!this.getActiveDirectorySettings().enabled),
                            handler: Ext.bind(function() {
                                this.openUserGroupMap();
                            }, this)
                        }, {
                            xtype: 'button',
                            margin: '0 0 0 10',
                            text: i18n._('Refresh group cache'),
                            name: 'RefreshGroupCache',
                            iconCls: 'test-icon',
                            disabled: (!this.getActiveDirectorySettings().enabled),
                            handler: Ext.bind(function() {
                                this.getRpcNode().refreshGroupCache(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                }, this));
                            }, this)
                        }]
                    }]
                }]
            }],

            onADTestClick: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Testing..."), i18n._("Active Directory Test"));
                var message = this.getActiveDirectoryManager().getActiveDirectoryStatusForSettings( Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var message = i18n._(result);
                    Ext.MessageBox.alert(i18n._("Active Directory Test"), message);
                }, this), this.getSettings());
            }, this),

            onADUsersClick: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Obtaining users..."), i18n._("Active Directory Users"));
                this.getActiveDirectoryManager().getActiveDirectoryUserEntries( Ext.bind(function( result, exception ) {
                    if(Ung.Util.handleException(exception)) return;
                    var userEntries = result.list;
                    var usersList = "";
                    var usersArray = [];
                    var i;
                    for(i=0; i<userEntries.length; i++) {
                        if( userEntries[i] == null ) {
                            continue;
                        }
                        var uid = userEntries[i].uid != null ? userEntries[i].uid: i18n._('[any]');
                        usersArray.push(( uid + "\r\n"));
                    }
                    usersArray.sort(function(a,b) {
                        a = String(a).toLowerCase();
                        b = String(b).toLowerCase();
                        try {
                            if(a < b) {
                                return -1;
                            }
                            if (a > b) {
                                return 1;
                            }
                            return 0;
                        } catch(e) {
                            return 0;
                        }
                    });

                    usersList += i18n._('[any]') +"\r\n";
                    for (i = 0 ; i < usersArray.length ; i++) {
                        usersList += usersArray[i];
                    }

                    this.panelActiveDirectoryConnector.down("textarea[name=ADUsersTextArea]").setValue(usersList);
                    Ext.MessageBox.close();
                }, this));
            }, this)
        });
    },

    buildRadius: function() {
        var enableRadius = this.getSettings().radiusSettings.enabled;
        this.panelRadius = Ext.create('Ext.panel.Panel',{
            name: 'RADIUS',
            helpSource: 'directory_connector_radius_connector',
            title: i18n._('RADIUS'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                title: i18n._('RADIUS Connector'),
                name: 'RADIUS Connector',
                xtype: 'fieldset',
                labelWidth: 250,
                items: [{
                    xtype: 'container',
                    html: Ext.String.format(i18n._('This allows your server to connect to a {0}RADIUS Server{1} in order to identify users for use by Captive Portal and L2TP/IPsec.'),'<b>','</b>')
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Disabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableRadius',
                    checked: (!this.getRadiusSettings().enabled),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getRadiusSettings().enabled = !checked;

                                var components = this.query('textfield[requiresRadius="true"]'), c;
                                for ( c = 0 ; c < components.length ; c++ ) {
                                    if ( checked ) {
                                        components[c].disable();
                                    } else {
                                        components[c].enable();
                                    }
                                }

                                this.panelRadius.down('button[name=radius_test]').setDisabled( checked );
                                this.panelRadius.down('textfield[name=radius_test_username]').setDisabled( checked );
                                this.panelRadius.down('textfield[name=radius_test_password]').setDisabled( checked );
                            }, this)
                        }
                    }
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Enabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableRadius',
                    checked: this.getRadiusSettings().enabled
                }, {
                    xtype: 'textfield',
                    name: 'Radius Server IP or Hostname',
                    fieldLabel: i18n._('RADIUS Server IP or Hostname'),
                    requiresRadius: true,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getRadiusSettings().server,
                    disabled: (!this.getRadiusSettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getRadiusSettings().server = newValue;
                        }, this )
                    }
                }, {
                    xtype: 'textfield',
                    name: 'authPort',
                    fieldLabel: i18n._('Authentication Port'),
                    requiresRadius: true,
                    allowBlank: false,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getRadiusSettings().authPort,
                    vtype: "port",
                    disabled: (!this.getRadiusSettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getRadiusSettings().authPort = newValue;
                        }, this )
                    }
                }, {
                    xtype: 'textfield',
                    name: 'acctPort',
                    fieldLabel: i18n._('Accounting Port'),
                    requiresRadius: true,
                    allowBlank: false,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    value: this.getRadiusSettings().acctPort,
                    vtype: "port",
                    disabled: (!this.getRadiusSettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getRadiusSettings().acctPort = newValue;
                        }, this )
                    }
                },{
                    xtype: 'textfield',
                    name: 'Shared Secret',
                    fieldLabel: i18n._('Shared Secret'),
                    requiresRadius: true,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    maxLength: 48,
                    enforceMaxLength: true,
                    value: this.getRadiusSettings().sharedSecret,
                    disabled: (!this.getRadiusSettings().enabled),
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getRadiusSettings().sharedSecret = newValue;
                        }, this )
                    }
                },{
                    xtype: "combo",
                    name: "authenticationMethod",
                    value: this.getRadiusSettings().authenticationMethod,
                    store: [
                        [ "MSCHAPV2", i18n._( "MS-CHAP v2" )],
                        [ "MSCHAPV1", i18n._( "MS-CHAP v1" )],
                        [ "CHAP", i18n._( "CHAP" )],
                        [ "PAP", i18n._( "PAP" )]
                        // Doesn't work: [ "CLEARTEXT", i18n._( "Cleartext" )],
                    ],
                    queryMode: 'local',
                    disabled: (!this.getRadiusSettings().enabled),
                    fieldLabel: i18n._( "Authentication Method" ),
                    requiresRadius: true,
                    labelWidth:250,
                    labelAlign:'right',
                    width: 450,
                    editable: false,
                    listeners: {
                        "change": Ext.bind(function( elem, newValue ) {
                            this.getRadiusSettings().authenticationMethod = newValue;
                        }, this )
                    }
                },{
                    xtype: 'container',
                    html: '<br/><br/><b>' + i18n._('IMPORTANT') + ':</b>&nbsp;&nbsp' + i18n._('When using Windows as a RADIUS server, the best security and compatibility is achieved by selecting MS-CHAP v2.  Please also make sure the MS-CHAP v2 protocol is enabled for RADIUS clients in the Windows Network Policy Server.')
                },{
                    xtype: 'component',
                    html: "<br/><hr/><br/>",
                    style: {marginRight: '20px'}
                },{
                    xtype: 'component',
                    html: Ext.String.format(i18n._('The {0}RADIUS Test{1} verifies that the server can authenticate the provided username/password.'),'<b>','</b>')
                },
                {
                    xtype:'textfield',
                    style: {marginTop: '10px'},
                    name: 'radius_test_username',
                    fieldLabel: i18n._('Username'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getRadiusSettings().enabled)
                },
                {
                    xtype:'textfield',
                    name: 'radius_test_password',
                    fieldLabel: i18n._('Password'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getRadiusSettings().enabled)
                },
                {
                    xtype: 'button',
                    text: i18n._('RADIUS Test'),
                    iconCls: 'test-icon',
                    name: 'radius_test',
                    disabled: (!this.getRadiusSettings().enabled),
                    handler: Ext.bind(function() {
                        this.panelRadius.onRadiusTestClick();
                    }, this)
                }]
            }],

            onRadiusTestClick: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Testing..."), i18n._("RADIUS Test"));
                var userCmp = this.panelRadius.down('textfield[name=radius_test_username]').getValue();
                var passwordCmp = this.panelRadius.down('textfield[name=radius_test_password]').getValue();

                var message = this.getRadiusManager().getRadiusStatusForSettings( Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var message = i18n._(result);
                    Ext.MessageBox.alert(i18n._("RADIUS Test"), message);
                }, this), this.getSettings(), userCmp, passwordCmp);
            }, this)
        });
    },

    buildGoogle: function() {
        this.authorizationUrl = this.getGoogleManager().getAuthorizationUrl(window.location.protocol, window.location.host);
        this.googleDriveConnected = this.getGoogleManager().isGoogleDriveConnected();
        this.panelGoogle = Ext.create('Ext.panel.Panel',{
            name: 'Google',
            helpSource: 'directory_connector_google_connector',
            title: i18n._('Google'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                name: 'Google Drive',
                title: i18n._('Google Drive'),
                xtype: 'fieldset',
                items: [{
                    xtype: 'container',
                    margin: '5 0 15 0',
                    html: i18n._('This allows your server to connect to Google Drive.')
                }, {
                    xtype: 'component',
                    name: 'googleDriveStatus',
                    html: (this.googleDriveConnected ? i18n._("The Google Drive is configured.") : i18n._("The Google Drive is unconfigured.")),
                    style: (this.googleDriveConnected ? {color:'green'} : {color:'red'}),
                    cls: (this.googleDriveConnected ? null : 'warning')
                }, {
                    xtype: "button",
                    margin: '10 10 0 0',
                    name: 'configure_google_drive_button',
                    text: (this.googleDriveConnected ? i18n._("Reconfigure Google Drive") : i18n._("Configure Google Drive")),
                    iconCls: "action-icon",
                    handler: Ext.bind(function() {
                        this.refreshGoogleTask.start();
                        window.open(this.authorizationUrl);
                    }, this)
                }, {
                    xtype: "button",
                    margin: '10 0 0 0',
                    name: 'disconnect_google_drive_button',
                    text: i18n._("Disconnect Google Drive"),
                    disabled: !this.googleDriveConnected,
                    iconCls: "action-icon",
                    handler: Ext.bind(function() {
                        this.getGoogleManager().disconnectGoogleDrive();
                        this.googleDriveConnected = false;

                        var googleDriveStatus = this.panelGoogle.down('component[name=googleDriveStatus]');
                        googleDriveStatus.setHtml((this.googleDriveConnected ? i18n._("The Google Connector is configured.") : i18n._("The Google Connector is unconfigured.")));
                        googleDriveStatus.setStyle((this.googleDriveConnected ? {color:'green'} : {color:'red'}));
                        var disableButton = this.panelGoogle.down('button[name=disconnect_google_drive_button]');
                        disableButton.setDisabled( !this.googleDriveConnected );
                    }, this)
                }]
            },{
                name: 'Google Authentication',
                title: i18n._('Google Authentication'),
                xtype: 'fieldset',
                items: [{
                    xtype: 'container',
                    html: Ext.String.format(i18n._('This allows your server to connect to {0}Google{1} in order to identify users for use by Captive Portal.'),'<b>','</b>')
                }, {
                    xtype: 'component',
                    html: i18n._('WARNING: Google Authentication is experimental and uses an unofficial API. Read the documentation for details.'),
                    cls: 'warning',
                    margin: '0 0 10 10'
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Disabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableGoogle',
                    checked: (!this.getGoogleSettings().authenticationEnabled),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getGoogleSettings().authenticationEnabled = !checked;

                                var components = this.query('textfield[requiresGoogle="true"]'), c;
                                for ( c = 0 ; c < components.length ; c++ ) {
                                    if ( checked ) {
                                        components[c].disable();
                                    } else {
                                        components[c].enable();
                                    }
                                }

                                this.panelGoogle.down('button[name=google_test]').setDisabled( checked );
                                this.panelGoogle.down('textfield[name=google_test_username]').setDisabled( checked );
                                this.panelGoogle.down('textfield[name=google_test_password]').setDisabled( checked );
                            }, this)
                        }
                    }
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Enabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableGoogle',
                    checked: this.getGoogleSettings().authenticationEnabled
                },{
                    xtype: 'component',
                    html: "<BR><HR><BR>",
                    style: {marginRight: '20px'}
                },{
                    xtype: 'component',
                    html: Ext.String.format(i18n._('The {0}Google Authentication Test{1} verifies that the server can authenticate the provided username/password.'),'<b>','</b>')
                },{
                    xtype:'textfield',
                    style: {marginTop: '10px'},
                    name: 'google_test_username',
                    fieldLabel: i18n._('Username'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getGoogleSettings().authenticationEnabled)
                },{
                    xtype:'textfield',
                    name: 'google_test_password',
                    fieldLabel: i18n._('Password'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getGoogleSettings().authenticationEnabled)
                },{
                    xtype: 'button',
                    text: i18n._('Google Authentication Test'),
                    iconCls: 'test-icon',
                    name: 'google_test',
                    disabled: (!this.getGoogleSettings().authenticationEnabled),
                    handler: Ext.bind(function() {
                        this.panelGoogle.onGoogleTestClick();
                    }, this)
                }]
            }],
            onGoogleTestClick: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Testing..."), i18n._("Google Authentication Test"));
                var userCmp = this.panelGoogle.down('textfield[name=google_test_username]').getValue();
                var passwordCmp = this.panelGoogle.down('textfield[name=google_test_password]').getValue();

                var message = this.getGoogleManager().authenticate( Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var message;
                    if ( result == true ) 
                        message = i18n._('Login successful.');
                    else
                        message = i18n._('Login failed.');
                    Ext.MessageBox.alert(i18n._("Google Authentication Test"), message);
                }, this), userCmp, passwordCmp);
            }, this)
        });
    },

    buildFacebook: function() {
        this.panelFacebook = Ext.create('Ext.panel.Panel',{
            name: 'Facebook',
            helpSource: 'directory_connector_facebook_connector',
            title: i18n._('Facebook'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                name: 'Facebook Authentication',
                title: i18n._('Facebook Authentication'),
                xtype: 'fieldset',
                items: [{
                    xtype: 'container',
                    html: Ext.String.format(i18n._('This allows your server to connect to {0}Facebook{1} in order to identify users for use by Captive Portal.'),'<b>','</b>')
                }, {
                    xtype: 'component',
                    html: i18n._('WARNING: Facebook Authentication is experimental and uses an unofficial API. Read the documentation for details.'),
                    cls: 'warning',
                    margin: '0 0 10 10'
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Disabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableFacebook',
                    checked: (!this.getFacebookSettings().authenticationEnabled),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                this.getFacebookSettings().authenticationEnabled = !checked;

                                var components = this.query('textfield[requiresFacebook="true"]'), c;
                                for ( c = 0 ; c < components.length ; c++ ) {
                                    if ( checked ) {
                                        components[c].disable();
                                    } else {
                                        components[c].enable();
                                    }
                                }

                                this.panelFacebook.down('button[name=facebook_test]').setDisabled( checked );
                                this.panelFacebook.down('textfield[name=facebook_test_username]').setDisabled( checked );
                                this.panelFacebook.down('textfield[name=facebook_test_password]').setDisabled( checked );
                            }, this)
                        }
                    }
                }, {
                    xtype: 'radio',
                    boxLabel: '<b>'+i18n._('Enabled')+'</b>',
                    style: {marginLeft: '50px'},
                    hideLabel: true,
                    name: 'enableFacebook',
                    checked: this.getFacebookSettings().authenticationEnabled
                },{
                    xtype: 'component',
                    html: "<BR><HR><BR>",
                    style: {marginRight: '20px'}
                },{
                    xtype: 'component',
                    html: Ext.String.format(i18n._('The {0}Facebook Authentication Test{1} verifies that the server can authenticate the provided username/password.'),'<b>','</b>')
                },{
                    xtype:'textfield',
                    style: {marginTop: '10px'},
                    name: 'facebook_test_username',
                    fieldLabel: i18n._('Username'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getFacebookSettings().authenticationEnabled)
                },{
                    xtype:'textfield',
                    name: 'facebook_test_password',
                    fieldLabel: i18n._('Password'),
                    labelWidth:150,
                    labelAlign:'left',
                    width: 350,
                    disabled: (!this.getFacebookSettings().authenticationEnabled)
                },{
                    xtype: 'button',
                    text: i18n._('Facebook Authentication Test'),
                    iconCls: 'test-icon',
                    name: 'facebook_test',
                    disabled: (!this.getFacebookSettings().authenticationEnabled),
                    handler: Ext.bind(function() {
                        this.panelFacebook.onFacebookTestClick();
                    }, this)
                }]
            }],
            onFacebookTestClick: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Testing..."), i18n._("Facebook Authentication Test"));
                var userCmp = this.panelFacebook.down('textfield[name=facebook_test_username]').getValue();
                var passwordCmp = this.panelFacebook.down('textfield[name=facebook_test_password]').getValue();

                var message = this.getFacebookManager().authenticate( Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var message;
                    if ( result == true ) 
                        message = i18n._('Login successful.');
                    else
                        message = i18n._('Login failed.');
                    Ext.MessageBox.alert(i18n._("Facebook Authentication Test"), message);
                }, this), userCmp, passwordCmp);
            }, this)
        });
    },
    
    //validate directory connector settings
    validate: function() {
        if(this.getSettings().activeDirectorySettings.enabled) {
            var hostCmp = this.panelActiveDirectoryConnector.down("textfield[name=LDAPHost]");
            var portCmp = this.panelActiveDirectoryConnector.down("textfield[name=LDAPPort]");
            var loginCmp = this.panelActiveDirectoryConnector.down("textfield[name=superuser]");
            var passwordCmp = this.panelActiveDirectoryConnector.down("textfield[name=superuserPass]");
            var domainCmp =this.panelActiveDirectoryConnector.down("textfield[name=domain]");
            var orgCmp = this.panelActiveDirectoryConnector.down("textfield[name=OUFilter]");

            //validate port
            if (!portCmp.isValid()) {
            Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        portCmp.focus(true);
                    }, this)
                );
                return false;
            }

            // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length == 0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Password must be specified if a Login is specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        passwordCmp.focus(true);
                    }, this)
                );
                return false;
            } else if(loginCmp.getValue().length == 0 && passwordCmp.getValue().length > 0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Login must be specified if a Password is specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        loginCmp.focus(true);
                    }, this)
                );
                return false;
            }

            // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
            if (loginCmp.getValue().length > 0 && passwordCmp.getValue().length > 0 && hostCmp.getValue().length == 0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Hostname must be specified if Login or Password are specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        hostCmp.focus(true);
                    }, this)
                );
                return false;
            }

            // CHECK THAT A DOMAIN IS SUPPLIED
            if (domainCmp.getValue().length == 0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Search Base must be specified.'),
                    Ext.bind(function () {
                        this.tabs.setActiveTab(this.panelActiveDirectoryConnector);
                        domainCmp.focus(true);
                    }, this)
                );
                return false;
            }
        }
        return true;
    }
});
//# sourceURL=directory-connector-settings.js
