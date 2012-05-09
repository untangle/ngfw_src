if (!Ung.hasResource["Ung.Phish"]) {
    Ung.hasResource["Ung.Phish"] = true;
    Ung.NodeWin.registerClassName('untangle-node-phish', 'Ung.Phish');

    Ext.define('Ung.Phish',{
		extend:'Ung.NodeWin',
        lastUpdate : null,
        lastCheck : null,
        signatureVersion : null,
        smtpData : null,
        spamData : null,
        emailPanel : null,
        webPanel : null,
        gridWebEventLog : null,
        gridEmailEventLog : null,

        initComponent : function() {
            this.getSettings();
            this.lastUpdate = this.getRpcNode().getLastUpdate();
            this.lastCheck = this.getRpcNode().getLastUpdateCheck();
            this.signatureVer = this.getRpcNode().getSignatureVersion();
            // build tabs
            this.buildEmail();
            this.buildWeb();
            this.buildEmailEventLog();
            this.buildWebEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.webPanel, this.gridEmailEventLog, this.gridWebEventLog]);
            Ung.Phish.superclass.initComponent.call(this);
        },
        lookup : function(needle, haystack1, haystack2) {
            for (var i = 0; i < haystack1.length; i++) {
                if (haystack1[i] != undefined && haystack2[i] != undefined) {
                    if (needle == haystack1[i]) {
                        return haystack2[i];
                    }
                    if (needle == haystack2[i]) {
                        return haystack1[i];
                    }
                }
            }
            return null;
        },
        // Email Config Panel
        buildEmail : function() {
            this.smtpData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')],
                    ['DROP', this.i18n._('Drop')], ['QUARANTINE', this.i18n._('Quarantine')]];
            this.spamData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')]];
            this.emailPanel = Ext.create('Ext.panel.Panel',{
                title : this.i18n._('Email'),
                name : 'Email',
                helpSource : 'email',
                autoScroll : true,
                cls: 'ung-panel',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._('SMTP'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan SMTP'),
                        name : 'Scan SMTP',
                        hideLabel : true,
                        checked : this.settings.smtpConfig.scan,
                        handler : Ext.bind(function(elem, newValue) {
                                    this.settings.smtpConfig.scan = newValue;
                                },this)
						}
                     , {
                        xtype : 'combo',
                        name : 'SMTP Action',
                        editable : false,
						store :this.smtpData,
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.settings.smtpConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.smtpConfig.msgAction = newValue;
                                },this)
                            }
                        }
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('POP3'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan POP3'),
                        name : 'Scan POP3',
                        hideLabel : true,
                        checked : this.settings.popConfig.scan,
                        handler : Ext.bind(function(elem, newValue) {
                                    this.settings.popConfig.scan = newValue;
                                },this)
					}, 
					{
                        xtype : 'combo',
                        name : 'POP3 Action',
                        editable : false,
                        store :this.spamData,
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.settings.popConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.popConfig.msgAction = newValue;
                                },this)
                            }
                        }
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('IMAP'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan IMAP'),
                        name : 'Scan IMAP',
                        hideLabel : true,
                        checked : this.settings.imapConfig.scan,
                        handler : Ext.bind(function(elem, newValue) {
                                  this.settings.imapConfig.scan = newValue;
                                },this)
                        }
                     , {
                        xtype : 'combo',
                        name : 'IMAP Action',
                        editable : false,
                        store : this.spamData,
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.settings.imapConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.imapConfig.msgAction = newValue;
                                },this)
                            }
                        }
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    cls: 'description',
                    html : //this.i18n._('Phish Blocker last checked for updates') + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                           //+ (this.lastUpdateCheck != null ? i18n.timestampFormat(this.lastUpdateCheck) : i18n._("unknown"))
                           //+ '<br\>'
                           this.i18n._('Phish Blocker email signatures were last updated') + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                           + (this.lastUpdate != null ? i18n.timestampFormat(this.lastUpdate) : i18n._("unknown"))
                    //+ '<br\>'
                    //+ this.i18n._('Current Version:') + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                    //+ (this.signatureVersion != null ? this.signatureVersion : i18n._("unknown"))
                }]
            });
        },
        // Web Config Panel
        buildWeb : function() {
            this.webPanel = Ext.create('Ext.panel.Panel',{
                title : this.i18n._('Web'),
                helpSource : 'web',
                autoScroll : true,
                cls: 'ung-panel',
                items : [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Enable Phish web filtering'),
                        name : 'Enable Phish web filtering',
                        hideLabel : true,
                        checked : this.settings.enableGooglePhishList,
						handler:Ext.bind(function(elem, checked) {
                                    this.settings.enableGooglePhishList = checked;
                                },this)
                        }]
                    }
                  , {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    cls: 'description',
                    html : this.i18n._('Phish Blocker web signatures were last updated ')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.lastUpdate != null ? i18n.timestampFormat(this.lastUpdate) : i18n
                                    ._("unknown"))
                }]
            });
        },
        // Web Event Log
        buildWebEventLog : function() {
            this.gridWebEventLog = Ext.create('Ung.GridEventLog',{
                name : 'Web Event Log',
                helpSource : 'web_event_log',
                settingsCmp : this,
                title : this.i18n._("Web Event Log"),
                eventQueriesFn : this.getRpcNode().getHttpEventQueries,
                // the list of fields
                fields : [{
                    name : 'time_stamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'displayAction',
                    mapping : 'phish_action',
                    type : 'string',
                    convert : Ext.bind(function(value) {
                        switch (value) {
                            case 0 : // PASSED
                                return this.i18n._("pass");
                            case 1 : // BLOCKED
                            default :
                                return this.i18n._("block");
                        }
                    },this)
				}, {
                    name : 'client',
                    mapping : 'c_client_addr'
                }, {
                    name : 'uid'
                }, {
                    name : 'server',
                    mapping : 'c_server_addr'
                }, {
                    name : 'host',
                    mapping : 'host'
                }, {
                    name : 'uri',
                    mapping : 'uri'
                }],
                // the list of columns
                columns : [{
                    header : this.i18n._("Timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'time_stamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("Client"),
                    width : Ung.Util.ipFieldWidth,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("Username"),
                    width : Ung.Util.usernameFieldWidth,
                    dataIndex : 'uid'
                }, {
                    header : this.i18n._("Uost"),
                    width : Ung.Util.hostnameFieldWidth,
                    dataIndex : 'host'
                }, {
                    header : this.i18n._("Uri"),
                    flex:1,
                    width : Ung.Util.uriFieldWidth,
                    dataIndex : 'uri'
                }, {
                    header : this.i18n._("Action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'displayAction'
                }]
            });
        },
        // Email Event Log
        buildEmailEventLog : function() {
            this.gridEmailEventLog = Ext.create('Ung.GridEventLog',{
                name : 'Email Event Log',
                helpSource : 'email_event_log',
                settingsCmp : this,
                title : this.i18n._("Email Event Log"),
                fields : [{
                    name : 'time_stamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'vendor',
                    mapping : 'vendor'
                }, {
                    name : 'displayAction',
                    mapping : 'phish_action',
                    type : 'string',
                    convert : Ext.bind(function(value, rec ) { // FIXME: make that a switch
                            if (value == 'P') { // PASSED
                                return this.i18n._("pass message");
                            } else if (value == 'M') { // MARKED
                                return this.i18n._("mark message");
                            } else if (value == 'B') { // DROP
                                return this.i18n._("drop message");
                            } else if (value == 'Q') { // QUARANTINED
                                return this.i18n._("quarantine message");
                            } else if (value == 'S') { // SAFELISTED
                                return this.i18n._("pass safelist message");
                            } else if (value == 'Z') { // OVERSIZE
                                return this.i18n._("pass oversize message");
                            } else if (value == 'O') { // OUTBOUND
                                return this.i18n._("pass outbound message");
                            } else {
                                return this.i18n._("unknown action");
                            }
                        return "";
                    },this)
                }, {
                    name : 'client',
                    mapping : 'c_client_addr'
                }, {
                    name : 'server',
                    mapping : 's_server_addr'
                }, {
                    name : 'subject',
                    type : 'string'
                }, {
                    name : 'addrName',
                    type : 'string'
                }, {
                    name : 'addr',
                    type : 'string'
                }, {
                    name : 'sender',
                    type : 'string'
                }],
                // the list of columns
                columns : [{
                    header : this.i18n._("Timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'time_stamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("Receiver"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'addr'
                }, {
                    header : this.i18n._("Sender"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'sender'
                }, {
                    header : this.i18n._("Subject"),
                    flex:1,
                    width : 150,
                    sortable : true,
                    dataIndex : 'subject'
                }, {
                    header : this.i18n._("Action"),
                    width : 125,
                    sortable : true,
                    dataIndex : 'displayAction'
                }, {
                    header : this.i18n._("Client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("Server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },
        
        afterSave : function()  {
            this.lastUpdate = this.getRpcNode().getLastUpdate();
            this.lastCheck = this.getRpcNode().getLastUpdateCheck();
            this.signatureVer = this.getRpcNode().getSignatureVersion();
        }

    });
}
//@ sourceURL=phish-settingsNew.js
