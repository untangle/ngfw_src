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
            // workarownd to solve the problem with
            // baseSettings.popConfig.msgAction==baseSettings.imapConfig.msgAction
            var baseSettings = this.getBaseSettings();
            if (baseSettings.popConfig.msgAction == baseSettings.imapConfig.msgAction) {
                var msgAction = {};
                msgAction.javaClass = baseSettings.imapConfig.msgAction.javaClass;
                msgAction.key = baseSettings.imapConfig.msgAction.key;
                msgAction.name = baseSettings.imapConfig.msgAction.name;
                baseSettings.imapConfig.msgAction = msgAction;
            }
            // call superclass renderer first
            Ung.Phish.superclass.onRender.call(this, container, position);
            // builds the 3 tabs
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
            this.smtpData = [['mark message', this.i18n._('Mark'), 'M'], ['pass message', this.i18n._('Pass'), 'P'],
                    ['block message', this.i18n._('Block'), 'B'], ['quarantine message', this.i18n._('Quarantine'), 'Q']];
            this.spamData = [['mark message', this.i18n._('Mark'), 'M'], ['pass message', this.i18n._('Pass'), 'P']];
            this.emailPanel = new Ext.Panel({
                title : this.i18n._('Email'),
                info : 'emailPanel',
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
                        name : 'smtpScan',
                        hideLabel : true,
                        checked : this.getBaseSettings().smtpConfig.scan,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'smtpAction',
                        editable : false,
                        store : this.smtpData,
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getBaseSettings().smtpConfig.msgAction.name,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.msgAction.name = newValue;
                                    for (var i = 0; i < this.smtpData.length; i++) {
                                        if (this.smtpData[i][0] == newValue) {
                                            this.getBaseSettings().smtpConfig.msgAction.key = this.smtpData[i][2]
                                            break;
                                        }
                                    }
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
                        name : 'pop3Scan',
                        hideLabel : true,
                        checked : this.getBaseSettings().popConfig.scan,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'pop3Action',
                        editable : false,
                        store : this.spamData,
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getBaseSettings().popConfig.msgAction.name,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.msgAction.name = newValue;
                                    for (var i = 0; i < this.spamData.length; i++) {
                                        if (this.spamData[i][0] == newValue) {
                                            this.getBaseSettings().popConfig.msgAction.key = this.spamData[i][2]
                                            break;
                                        }
                                    }
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
                        name : 'imapScan',
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
                        name : 'imapAction',
                        editable : false,
                        store : this.spamData,
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getBaseSettings().imapConfig.msgAction.name,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.msgAction.name = newValue;
                                    for (var i = 0; i < this.spamData.length; i++) {
                                        if (this.spamData[i][0] == newValue) {
                                            this.getBaseSettings().imapConfig.msgAction.key = this.spamData[i][2]
                                            break;
                                        }
                                    }
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
                        name : 'enableGooglePhishList',
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
                info : 'gridWebEventLog',
                settingsCmp : this,
                title : this.i18n._("Web Event Log"),
                // the list of fields
                fields : [{
                    name : 'createDate'
                }, {
                    name : 'action'
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
                    dataIndex : 'action',
                    renderer : function(value) {
                        return value ? this.i18n._("none") : this.i18n._(value.action);
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
                info : 'gridEmailEventLog',
                settingsCmp : this,
                title : this.i18n._("Email Event Log"),
                // the list of fields
                fields : [{
                    name : 'createDate'
                }, {
                    name : 'actionName'
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
                    dataIndex : 'actionName'
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
            // disable tabs during save
            this.tabs.disable();
            this.getRpcNode().setPhishBaseSettings(function(result, exception) {
                // re-enable tabs
                this.tabs.enable();
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
