if (!Ung.hasResource["Ung.Phish"]) {
    Ung.hasResource["Ung.Phish"] = true;
    Ung.NodeWin.registerClassName('untangle-node-phish', 'Ung.Phish');

    Ung.Phish = Ext.extend(Ung.NodeWin, {
        lastUpdate : null,
        lastCheck : null,
        signatureVersion : null,
        smtpData : null,
        spamData : null,
        emailPanel : null,
        webPanel : null,
        gridWebEventLog : null,
        gridEmailEventLog : null,
        // override get base settings object
        getNodeSettings : function(forceReload) {
            if (forceReload || this.rpc.nodeSettings === undefined) {
                try {
                   this.rpc.nodeSettings = this.getRpcNode().getSettings();
                   this.lastUpdate = this.getRpcNode().getLastUpdate();
                   this.lastCheck = this.getRpcNode().getLastUpdateCheck();
                   this.signatureVer = this.getRpcNode().getSignatureVersion();
                } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
            }
            return this.rpc.nodeSettings;
        },
        initComponent : function() {
            // keep initial node settings
            this.initialNodeSettings = Ung.Util.clone(this.getNodeSettings());
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
                    ['BLOCK', this.i18n._('Drop')], ['QUARANTINE', this.i18n._('Quarantine')]];
            this.spamData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')]];
            this.emailPanel = new Ext.Panel({
                title : this.i18n._('Email'),
                name : 'Email',
                helpSource : 'email',
                layout : "form",
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
                        checked : this.getNodeSettings().smtpConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getNodeSettings().smtpConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'SMTP Action',
                        editable : false,
                        store : new Ext.data.SimpleStore({
                            fields : ['key', 'name'],
                            data : this.smtpData
                        }),
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().smtpConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getNodeSettings().smtpConfig.msgAction = newValue;
                                }.createDelegate(this)
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
                        checked : this.getNodeSettings().popConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getNodeSettings().popConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'POP3 Action',
                        editable : false,
                        store : new Ext.data.SimpleStore({
                            fields : ['key', 'name'],
                            data : this.spamData
                        }),
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().popConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getNodeSettings().popConfig.msgAction = newValue;
                                }.createDelegate(this)
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
                        checked : this.getNodeSettings().imapConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                  this.getNodeSettings().imapConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'IMAP Action',
                        editable : false,
                        store : new Ext.data.SimpleStore({
                            fields : ['key', 'name'],
                            data : this.spamData
                        }),
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().imapConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getNodeSettings().imapConfig.msgAction = newValue;
                                }.createDelegate(this)
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
            this.webPanel = new Ext.Panel({
                title : this.i18n._('Web'),
                helpSource : 'web',
                layout : "form",
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
                        checked : this.getNodeSettings().enableGooglePhishList,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getNodeSettings().enableGooglePhishList = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
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
            var asClient = function(value) {
                var pe = (value == null ? null : value.pipelineEndpoints);
                return pe === null ? "" : pe.CClientAddr + ":" + pe.CClientPort;
            };
            var asServer = function(value) {
                var pe = (value == null ? null : value.pipelineEndpoints);
                return pe === null ? "" : pe.SServerAddr + ":" + pe.SServerPort;
            };
            var asRequest = function(value) {
                return value == null || value.url == null ? "" : value.url;
            }.createDelegate(this);

            this.gridWebEventLog = new Ung.GridEventLog({
                name : 'Web Event Log',
                helpSource : 'web_event_log',
                settingsCmp : this,
                title : this.i18n._("Web Event Log"),
                eventManagerFn : this.getRpcNode().getPhishHttpEventManager(),
                // the list of fields
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'displayAction',
                    mapping : 'actionType',
                    type : 'string',
                    convert : function(value) {
                        switch (value) {
                            case 0 : // PASSED
                                return this.i18n._("pass");
                            case 1 : // BLOCKED
                            default :
                                return this.i18n._("block");
                        }
                    }.createDelegate(this)
                }, {
                    name : 'client',
                    mapping : 'requestLine',
                    sortType : asClient
                }, {
                    name : 'server',
                    mapping : 'requestLine',
                    sortType : asServer
                }, {
                    name : 'request',
                    mapping : 'requestLine',
                    sortType : asRequest
                }],
                // the list of columns
                autoExpandColumn : 'request',
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'displayAction'
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client',
                    renderer : asClient
                }, {
                    id : 'request',
                    header : this.i18n._("request"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'request',
                    renderer : asRequest
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server',
                    renderer : asServer
                }]
            });
        },
        // Email Event Log
        buildEmailEventLog : function() {
            this.gridEmailEventLog = new Ung.GridEventLog({
                name : 'Email Event Log',
                helpSource : 'email_event_log',
                settingsCmp : this,
                title : this.i18n._("Email Event Log"),
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'vendor',
                    mapping : 'vendor'
                }, {
                    name : 'displayAction',
                    mapping : 'phishAction',
                    type : 'string',
                    convert : function(value, rec ) { // FIXME: make that a switch
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
                    }.createDelegate(this)
                }, {
                    name : 'client',
                    mapping : 'CClientAddr'
                }, {
                    name : 'server',
                    mapping : 'SServerAddr'
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
                autoExpandColumn : 'subject',
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : Ung.Util.timestampFieldWidth,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("receiver"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'addr'
                }, {
                    header : this.i18n._("sender"),
                    width : Ung.Util.emailFieldWidth,
                    sortable : true,
                    dataIndex : 'sender'
                }, {
                    id : 'subject',
                    header : this.i18n._("subject"),
                    width : 150,
                    sortable : true,
                    dataIndex : 'subject'
                }, {
                    header : this.i18n._("action"),
                    width : 125,
                    sortable : true,
                    dataIndex : 'displayAction'
                }, {
                    header : this.i18n._("client"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'client'
                }, {
                    header : this.i18n._("server"),
                    width : Ung.Util.ipFieldWidth,
                    sortable : true,
                    dataIndex : 'server'
                }]
            });
        },

        // apply action
        applyAction : function()
        {
            this.commitSettings(this.reloadSettings.createDelegate(this));
        },

        reloadSettings : function()
        {
            this.initialNodeSettings = Ung.Util.clone(this.getNodeSettings(true));

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
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.getRpcNode().setSettings(function(result, exception) {

                if(Ung.Util.handleException(exception)) return;
                callback();
            }.createDelegate(this), this.getNodeSettings());
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getNodeSettings(), this.initialNodeSettings);
        }
    });
}
