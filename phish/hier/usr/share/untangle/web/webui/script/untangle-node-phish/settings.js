if (!Ung.hasResource["Ung.Phish"]) {
    Ung.hasResource["Ung.Phish"] = true;
    Ung.Settings.registerClassName('untangle-node-phish', 'Ung.Phish');

    Ung.Phish = Ext.extend(Ung.Settings, {
        smtpData : null,
        spamData : null,
        emailPanel : null,
        webPanel : null,
        gridWebEventLog : null,
        gridEmailEventLog : null,
        // override get base settings object
        getBaseSettings : function(forceReload) {
            if (forceReload || this.rpc.baseSettings === undefined) {
                this.rpc.baseSettings = this.getRpcNode().getPhishBaseSettings();
            }
            return this.rpc.baseSettings;
        },

        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Phish.superclass.onRender.call(this, container, position);
            // builds the tabs
            this.buildEmail();
            this.buildWeb();
            this.buildWebEventLog();
            this.buildEmailEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.webPanel, this.gridWebEventLog, this.gridEmailEventLog]);
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
        },
        // Email Config Panel
        buildEmail : function() {
            this.smtpData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')],
                    ['BLOCK', this.i18n._('Block')], ['QUARANTINE', this.i18n._('Quarantine')]];
            this.spamData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')]];
            this.emailPanel = new Ext.Panel({
                title : this.i18n._('Email'),
                name : 'Email',
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
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
                        checked : this.getBaseSettings().smtpConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.scan = newValue;
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
                        value : this.getBaseSettings().smtpConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.msgAction = newValue;
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
                        checked : this.getBaseSettings().popConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.scan = newValue;
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
                        value : this.getBaseSettings().popConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.msgAction = newValue;
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
                        checked : this.getBaseSettings().imapConfig.scan,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.scan = newValue;
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
                        value : this.getBaseSettings().imapConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.msgAction = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    html : this.i18n._('Phish Blocker email signatures were last updated')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.getBaseSettings().lastUpdate != null ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : i18n
                                    ._("unknown"))
                }]
            });
        },
        // Web Config Panel
        buildWeb : function() {
            this.webPanel = new Ext.Panel({
                title : this.i18n._('Web'),
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    autoHeight : true,
                    items : [{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Enable Phish web filtering'),
                        name : 'Enable Phish web filtering',
                        hideLabel : true,
                        checked : this.getBaseSettings().enableGooglePhishList,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    this.getBaseSettings().enableGooglePhishList = checked;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    html : this.i18n._('Phish Blocker web signatures were last updated ')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.getBaseSettings().lastUpdate != null ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : i18n
                                    ._("unknown"))
                }]
            });
        },
        // Web Event Log
        buildWebEventLog : function() {
            this.gridWebEventLog = new Ung.GridEventLog({
                name : 'Web Event Log',
                settingsCmp : this,
                title : this.i18n._("Web Event Log"),
                // the list of fields
                fields : [{
                    name : 'createDate'
                }, {
                    name : 'actionType'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'requestLine'
                }],
                // the list of columns
                columns : [{
                    header : i18n._("timestamp"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'createDate',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr.hostAddress + ":" + value.CClientPort;
                    }
                }, {
                    header : i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'actionType',
                    renderer : function(value) {
                        switch (value) {
                            case 0 : // PASSED
                                return this.i18n._("pass");
                            default :
                            case 1 : // BLOCKED
                                return this.i18n._("block");
                        }
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("request"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'requestLine',
                    renderer : function(value) {
                        return value == null || value.url == null ? "" : value.url;
                    }.createDelegate(this)
                }, {
                    header : i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr.hostAddress + ":" + value.SServerPort;
                    }
                }]
            });
        },
        // Email Event Log
        buildEmailEventLog : function() {
            this.gridEmailEventLog = new Ung.GridEventLog({
                name : 'Email Event Log',
                settingsCmp : this,
                title : this.i18n._("Email Event Log"),
                // the list of fields
                fields : [{
                    name : 'createDate'
                }, {
                    name : 'type'
                }, {
                    name : 'actionType'
                }, {
                    name : 'messageInfo'
                }, {
                    name : 'subject'
                }, {
                    name : 'receiver'
                }, {
                    name : 'sender'
                }],
                // the list of columns
                columns : [{
                    header : i18n._("timestamp"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'createDate',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : i18n._("action"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'actionType',
                    renderer : function(value, metadata, record ) {
                        switch (record.data.type) {
                            case 'POP/IMAP' :
                                switch (value) {
                                    case 0 : // PASSED
                                        return this.i18n._("pass message");
                                    default :
                                    case 1 : // MARKED
                                        return this.i18n._("mark infection");
                                }
                                break;
                            case 'SMTP' :
                                switch (value) {
                                    case 0 : // PASSED
                                        return this.i18n._("pass message");
                                    case 1 : // MARKED
                                        return this.i18n._("mark infection");
                                    case 2 : // BLOCKED
                                        return this.i18n._("block message");
                                    default :
                                    case 3 : // QUARANTINED
                                        return this.i18n._("quarantine message");
                                }
                                break;
                        }
                        return "";
                    }.createDelegate(this)
                }, {
                    header : i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'messageInfo',
                    renderer : function(value) {
                        return value === null || value.pipelineEndpoints == null ? "" : value.pipelineEndpoints.CClientAddr.hostAddress
                                + ":" + value.pipelineEndpoints.CClientPort;
                    }
                }, {
                    header : this.i18n._("subject"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'subject'
                }, {
                    header : this.i18n._("receiver"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'receiver'
                }, {
                    header : this.i18n._("sender"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'sender'
                }, {
                    header : i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'messageInfo',
                    renderer : function(value) {
                        return value === null || value.pipelineEndpoints == null ? "" : value.pipelineEndpoints.SServerAddr.hostAddress
                                + ":" + value.pipelineEndpoints.SServerPort;
                    }
                }]
            });
        },

        // save function
        save : function() {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            this.getRpcNode().setPhishBaseSettings(function(result, exception) {
                Ext.MessageBox.hide();
                if (exception) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    return;
                }
                // exit settings screen
                this.cancelAction();
            }.createDelegate(this), this.getBaseSettings());
        }
    });
}
