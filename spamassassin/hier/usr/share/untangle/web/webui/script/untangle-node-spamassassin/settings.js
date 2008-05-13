if (!Ung.hasResource["Ung.SpamAssassin"]) {
    Ung.hasResource["Ung.SpamAssassin"] = true;
    Ung.Settings.registerClassName('untangle-node-spamassassin', 'Ung.SpamAssassin');

    Ung.SpamAssassin = Ext.extend(Ung.Settings, {
        strengths : null,
        strengthsValues : null,
        actions : null,
        actionsValues : null,
        emailPanel : null,
        gridEventLog : null,
        gridRBLEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            this.strengths = [this.i18n._('Low'), this.i18n._('Medium'), this.i18n._('High'), this.i18n._('Very High'),
                    this.i18n._('Extreme'), this.i18n._('Custom')];
            this.strengthsValues = [50, 43, 35, 33, 30, 20];
            this.actions = [this.i18n._('Quarantine'), this.i18n._('Block'), this.i18n._('Mark'), this.i18n._('Pass')];
            this.actionsValues = ['Quarantine', 'Block', 'Mark', 'Pass'],
            // call superclass renderer first
            Ung.SpamAssassin.superclass.onRender.call(this, container, position);
            // builds the tabs
            this.buildEmail();
            this.buildEventLog();
            this.buildRBLEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.gridEventLog, this.gridRBLEventLog]);
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
            this.emailPanel = new Ext.Panel({
                title : this.i18n._('Email'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._('SMTP'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    defaultType : 'textfield',
                    items : [new Ext.form.Checkbox({
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
                    }), new Ext.form.ComboBox({
                        store : this.strengths,
                        fieldLabel : this.i18n._('Strength'),
                        displayField : 'select',
                        valueField : 'smtpStrengthValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Medium'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().smtpConfig.strength, this.strengths, this.strengthsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.strength = this
                                            .lookup(newValue, this.strengths, this.strengthsValues);
                                }.createDelegate(this)
                            }
                        }
                    }), new Ext.form.ComboBox({
                        store : this.actions,
                        fieldLabel : this.i18n._('Action'),
                        displayField : 'String',
                        valueField : 'smtpActionValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Quarantine'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().smtpConfig.zMsgAction, this.actions, this.actionsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.zMsgAction = this.lookup(newValue, this.actions, this.actionsValues);
                                }.createDelegate(this)
                            }
                        }
                    })]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('POP3'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    defaultType : 'textfield',
                    items : [new Ext.form.Checkbox({
                        boxLabel : 'Scan POP3',
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
                    }), new Ext.form.ComboBox({
                        store : this.strengths,
                        fieldLabel : this.i18n._('Strength'),
                        displayField : 'String',
                        valueField : 'pop3StrengthValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Medium'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().popConfig.strength, this.strengths, this.strengthsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.strength = this.lookup(newValue, this.strengths, this.strengthsValues);
                                }.createDelegate(this)
                            }
                        }
                    }), new Ext.form.ComboBox({
                        store : this.actions,
                        fieldLabel : this.i18n._('Action'),
                        displayField : 'String',
                        valueField : 'pop3ActionValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Mark'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().popConfig.zMsgAction, this.actions, this.actionsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.zMsgAction = this.lookup(newValue, this.actions, this.actionsValues);
                                }.createDelegate(this)
                            }
                        }
                    })]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('IMAP'),
                    autoHeight : true,
                    defaults : {
                        width : 210
                    },
                    defaultType : 'textfield',
                    items : [new Ext.form.Checkbox({
                        boxLabel : 'Scan IMAP',
                        name : 'imapScan',
                        hideLabel : true,
                        checked : this.getBaseSettings().imapConfig.scan
                    }), new Ext.form.ComboBox({
                        store : this.strengths,
                        fieldLabel : this.i18n._('Strength'),
                        displayField : 'String',
                        valueField : 'imapStrengthValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Medium'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().imapConfig.strength, this.strengths, this.strengthsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.strength = this
                                            .lookup(newValue, this.strengths, this.strengthsValues);
                                }.createDelegate(this)
                            }
                        }
                    }), new Ext.form.ComboBox({
                        store : this.actions,
                        fieldLabel : this.i18n._('Action'),
                        displayField : 'String',
                        valueField : 'imapActionValue',
                        typeAhead : false,
                        emptyText : this.i18n._('Mark'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        value : this.lookup(this.getBaseSettings().imapConfig.zMsgAction, this.actions, this.actionsValues),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.zMsgAction = this.lookup(newValue, this.actions, this.actionsValues);
                                }.createDelegate(this)
                            }
                        }
                    })]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    html : this.i18n._('Spam blocker was last updated')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.getBaseSettings().lastUpdate != null ? i18n
                                    .timestampFormat(this.getBaseSettings().lastUpdate) : i18n._("unknown"))
                }]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
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
                }, {
                    name : 'score'
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
                        return value === null || value.pipelineEndpoints==null ? "" : value.pipelineEndpoints.CClientAddr.hostAddress + ":" + value.pipelineEndpoints.CClientPort;
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
                    header : this.i18n._("SPAM score"),
                    width : 90,
                    sortable : true,
                    dataIndex : 'score'
                }, {
                    header : i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'messageInfo',
                    renderer : function(value) {
                        return value === null || value.pipelineEndpoints==null ? "" : value.pipelineEndpoints.SServerAddr.hostAddress + ":" + value.pipelineEndpoints.SServerPort;
                    }
                }]
            });
        },
        // RBL Event Log
        buildRBLEventLog : function() {
            this.gridRBLEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                eventManagerFn : this.getRpcNode().getRBLEventManager(),
                title : this.i18n._("DNSBL Event Log"),
                // the list of fields
                fields : [{
                    name : 'createDate'
                }, {
                    name : 'skipped'
                }, {
                    name : 'IPAddr'
                }, {
                    name : 'hostname'
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
                    width : 120,
                    sortable : true,
                    dataIndex : 'skipped',
                    renderer :function(value) {
                            return value?this.i18n._("skipped"):this.i18n._("blocked");
                        }.createDelegate(this)
                }, {
                    header : this.i18n._("sender"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'IPAddr',
                    renderer :function(value) {
                            return value==null?"":value.hostAddress;
                        }.createDelegate(this)
                }, {
                    header : this.i18n._("dnsbl server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'hostname',
                    renderer : function(value) {
                        return value.hostName;
                    }
                }]
            });
        },
        // save function
        save : function() {
            // disable tabs during save
            this.tabs.disable();
            this.getRpcNode().setBaseSettings(function(result, exception) {
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
