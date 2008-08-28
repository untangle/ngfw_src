if (!Ung.hasResource["Ung.SpamAssassin"]) {
    Ung.hasResource["Ung.SpamAssassin"] = true;
    Ung.Settings.registerClassName('untangle-node-spamassassin', 'Ung.SpamAssassin');

    Ung.SpamAssassin = Ext.extend(Ung.Settings, {
        strengthsData : null,
        smtpData : null,
        spamData : null,
        emailPanel : null,
        gridEventLog : null,
        gridRBLEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
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
        isCustomStrength : function(strength) {
            return !(strength == 50 || strength == 43 || strength == 35 || strength == 33 || strength == 30)
        },
        getStrengthSelectionValue : function(strength) {
            if (this.isCustomStrength(strength)) {
                return 0;
            } else {
                return strength;
            }
        },
        // Email Config Panel
        buildEmail : function() {
            this.smtpData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')],
                    ['BLOCK', this.i18n._('Block')], ['QUARANTINE', this.i18n._('Quarantine')]];
            this.spamData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')]];
            this.strengthsData = [[50, this.i18n._('Low')], [43, this.i18n._('Medium')], [35, this.i18n._('High')],
                    [33, this.i18n._('Very High')], [30, this.i18n._('Extreme')], [0, this.i18n._('Custom')],];
                    
            var strengthValidator = function(fieldValue) {
                if( 30 <= fieldValue && fieldValue <= 100) {
                    return true;
                } else {
                    return this.i18n._('Strength Value must be a number in range 100-30. Smaller value is higher strength.'); 
                }
            }.createDelegate(this);
                        
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
                        width : 210,
                        labelStyle: "width:160px;"
                    },
                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan SMTP',
                        boxLabel : this.i18n._('Scan SMTP'),
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
                        xtype : 'checkbox',
                        name : 'Enable SMTP greylisting',
                        boxLabel : this.i18n._('Enable SMTP greylisting'),
                        hideLabel : true,
                        checked : this.getBaseSettings().smtpConfig.throttle,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.throttle = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'SMTP Strength',
                        editable : false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().smtpConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var customCmp = Ext.getCmp('spamassassin_smtpStrengthValue');
                                    if (newValue == 0) {
                                        customCmp.enable();
                                    } else {
                                        customCmp.disable();
                                        this.getBaseSettings().smtpConfig.strength = newValue;
                                    }
                                    customCmp.setValue(this.getBaseSettings().smtpConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Strength Value (100-30)'),
                        name : 'SMTP Strength Value',
                        id: 'spamassassin_smtpStrengthValue',
                        value : this.getBaseSettings().smtpConfig.strength,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._('Strength Value must be a number in range 100-30. Smaller value is higher strength.'),
                        disabled : !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                        validator : strengthValidator,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.strength = newValue;
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
                        width : 210,
                        labelStyle: "width:160px;"
                    },
                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan POP3',
                        boxLabel : this.i18n._('Scan POP3'),
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
                        name : 'POP3 Strength',
                        editable : false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().popConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var customCmp = Ext.getCmp('spamassassin_pop3StrengthValue');
                                    if (newValue == 0) {
                                        customCmp.enable();
                                    } else {
                                        customCmp.disable();
                                        this.getBaseSettings().popConfig.strength = newValue;
                                    }
                                    customCmp.setValue(this.getBaseSettings().popConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Strength Value (100-30)'),
                        name : 'POP3 Strength Value',
                        id: 'spamassassin_pop3StrengthValue',
                        value : this.getBaseSettings().popConfig.strength,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._('Strength Value must be a number in range 100-30. Smaller value is higher strength.'),
                        disabled : !this.isCustomStrength(this.getBaseSettings().popConfig.strength),
                        validator : strengthValidator,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.strength = newValue;
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
                        width : 210,
                        labelStyle: "width:160px;"
                    },
                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan IMAP',
                        boxLabel : this.i18n._('Scan IMAP'),
                        name : 'imapScan',
                        hideLabel : true,
                        checked : this.getBaseSettings().imapConfig.scan,
                        listeners : {
                            "check" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        name : 'IMAP Strength',
                        editable : false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().imapConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var customCmp = Ext.getCmp('spamassassin_imapStrengthValue');
                                    if (newValue == 0) {
                                        customCmp.enable();
                                    } else {
                                        customCmp.disable();
                                        this.getBaseSettings().imapConfig.strength = newValue;
                                    }
                                    customCmp.setValue(this.getBaseSettings().imapConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'numberfield',
                        fieldLabel : this.i18n._('Strength Value (100-30)'),
                        name : 'IMAP Strength Value',
                        id: 'spamassassin_imapStrengthValue',
                        value : this.getBaseSettings().imapConfig.strength,
                        width: 50,
                        allowDecimals: false,
                        allowNegative: false,
                        allowBlank : false,
                        blankText : this.i18n._('Strength Value must be a number in range 100-30. Smaller value is higher strength.'),
                        disabled : !this.isCustomStrength(this.getBaseSettings().imapConfig.strength),
                        validator : strengthValidator,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.strength = newValue;
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
                    html : this.i18n._('Spam blocker was last updated')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.getBaseSettings().lastUpdate != null ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : i18n
                                    ._("unknown"))
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
                        return value === null || value.pipelineEndpoints == null ? "" : value.pipelineEndpoints.SServerAddr.hostAddress
                                + ":" + value.pipelineEndpoints.SServerPort;
                    }
                }]
            });
        },
        // RBL Event Log
        buildRBLEventLog : function() {
            this.gridRBLEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                name : 'Tarpit Event Log',
                eventManagerFn : this.getRpcNode().getRBLEventManager(),
                title : this.i18n._("Tarpit Event Log"),
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
                    renderer : function(value) {
                        return value ? this.i18n._("skipped") : this.i18n._("blocked");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("sender"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'IPAddr',
                    renderer : function(value) {
                        return value == null ? "" : value.hostAddress;
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
        validateClient : function() {
        	var cmp = null;
            var valid = 
                ((cmp = Ext.getCmp('spamassassin_smtpStrengthValue')).isValid() && 
                (cmp = Ext.getCmp('spamassassin_pop3StrengthValue')).isValid() &&
                (cmp = Ext.getCmp('spamassassin_imapStrengthValue')).isValid());
            if (!valid) {
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._('Strength Value must be a number in range 100-30. Smaller value is higher strength.'),
                    function () {
                        this.tabs.activate(this.emailPanel);
                        cmp.focus(true);
                    }.createDelegate(this) 
                );
            }
            return valid;
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
            Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().setBaseSettings(function(result, exception) {
                    Ext.MessageBox.hide();
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    // exit settings screen
                    this.cancelAction();
                }.createDelegate(this), this.getBaseSettings());
            }
        }
    });
}
