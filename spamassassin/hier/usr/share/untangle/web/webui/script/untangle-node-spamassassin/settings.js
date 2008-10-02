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
        initComponent : function() {
            this.buildEmail();
            this.buildEventLog();
            this.buildRBLEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.gridEventLog, this.gridRBLEventLog]);
            this.tabs.activate(this.emailPanel);
            Ung.SpamAssassin.superclass.initComponent.call(this);
        },
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.SpamAssassin.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
        },
        initSubCmps : function() {
            Ext.getCmp('spamassassin_smtpStrengthValue').setContainerVisible(this.isCustomStrength(this.getBaseSettings().smtpConfig.strength));
            Ext.getCmp('spamassassin_pop3StrengthValue').setContainerVisible(this.isCustomStrength(this.getBaseSettings().popConfig.strength));
            Ext.getCmp('spamassassin_imapStrengthValue').setContainerVisible(this.isCustomStrength(this.getBaseSettings().imapConfig.strength));
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
                    [33, this.i18n._('Very High')], [30, this.i18n._('Extreme')], [0, this.i18n._('Custom')]];
                    
            this.emailPanel = new Ext.Panel({
                title : this.i18n._('Email'),
                name : 'Email',
                helpSource : 'email',
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px;',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._('SMTP'),
                    autoHeight : true,
                    labelWidth: 150,
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
                        name : 'Enable SMTP tarpitting',
                        boxLabel : this.i18n._('Enable SMTP tarpitting'),
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
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            labelWidth: 150,
                            items: [{
                                xtype : 'combo',
                                name : 'SMTP Strength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                width : 200,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getBaseSettings().smtpConfig.strength),
                                listeners : {
                                    "select" : {
                                        fn : function(elem, record) {
                                            var customCmp = Ext.getCmp('spamassassin_smtpStrengthValue');
                                            if (record.data.value == 0) {
                                                customCmp.showContainer();
                                            } else {
                                                customCmp.hideContainer();
                                                this.getBaseSettings().smtpConfig.strength = record.data.value;
                                            }
                                            customCmp.setValue(this.getBaseSettings().smtpConfig.strength);
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        },{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel : this.i18n._('Strength Value'),
                                name : 'SMTP Strength Value',
                                id: 'spamassassin_smtpStrengthValue',
                                value : this.getBaseSettings().smtpConfig.strength,
                                width : 100,
                                allowDecimals: false,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
                                listeners : {
                                    "change" : {
                                        fn : function(elem, newValue) {
                                            this.getBaseSettings().smtpConfig.strength = newValue;
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        }]
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
                        width : 200,
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
                    labelWidth: 150,
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
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            labelWidth: 150,
                            items: [{
                                xtype : 'combo',
                                name : 'POP3 Strength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                width : 200,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getBaseSettings().popConfig.strength),
                                listeners : {
                                    "select" : {
                                        fn : function(elem, record) {
                                            var customCmp = Ext.getCmp('spamassassin_pop3StrengthValue');
                                            if (record.data.value == 0) {
                                                customCmp.showContainer();
                                            } else {
                                                customCmp.hideContainer();
                                                this.getBaseSettings().popConfig.strength = record.data.value;
                                            }
                                            customCmp.setValue(this.getBaseSettings().popConfig.strength);
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        },{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel : this.i18n._('Strength Value'),
                                name : 'POP3 Strength Value',
                                id: 'spamassassin_pop3StrengthValue',
                                value : this.getBaseSettings().popConfig.strength,
                                width: 100,
                                allowDecimals: false,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
                                listeners : {
                                    "change" : {
                                        fn : function(elem, newValue) {
                                            this.getBaseSettings().popConfig.strength = newValue;
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        }]
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
                        width : 200,
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
                    labelWidth: 150,
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
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            labelWidth: 150,
                            items: [{
                                xtype : 'combo',
                                name : 'IMAP Strength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                width : 200,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getBaseSettings().imapConfig.strength),
                                listeners : {
                                    "select" : {
                                        fn : function(elem, record) {
                                            var customCmp = Ext.getCmp('spamassassin_imapStrengthValue');
                                            if (record.data.value == 0) {
                                                customCmp.showContainer();
                                            } else {
                                                customCmp.hideContainer();
                                                this.getBaseSettings().imapConfig.strength = record.data.value;
                                            }
                                            customCmp.setValue(this.getBaseSettings().imapConfig.strength);
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        },{
                            border: false,
                            columnWidth:.5,
                            layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel : this.i18n._('Strength Value'),
                                name : 'IMAP Strength Value',
                                id: 'spamassassin_imapStrengthValue',
                                value : this.getBaseSettings().imapConfig.strength,
                                width: 100,
                                allowDecimals: false,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
                                listeners : {
                                    "change" : {
                                        fn : function(elem, newValue) {
                                            this.getBaseSettings().imapConfig.strength = newValue;
                                        }.createDelegate(this)
                                    }
                                }
                            }]
                        }]
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
                        width : 200,
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
                    html : this.i18n._('Spam Blocker was last updated')
                            + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                            + (this.getBaseSettings().lastUpdate != null ? i18n.timestampFormat(this.getBaseSettings().lastUpdate) : i18n
                                    ._("unknown"))
                }]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                helpSource : 'event_log',
                settingsCmp : this,
                // the list of fields
                fields : [{
                    name : 'timeStamp'
                }, {
                    name : 'type'
                }, {
                    name : 'actionType'
                }, {
                    name : 'clientAddr'
                }, {
                    name : 'clientPort'
                }, {
                    name : 'serverAddr'
                }, {
                    name : 'serverPort'
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
                    dataIndex : 'timeStamp',
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
                    dataIndex : 'clientAddr',
                    renderer : function(value, metadata, record ) {
                        return value === null ? "" : record.data.clientAddr + ":" + record.data.clientPort;
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
                    dataIndex : 'serverAddr',
                    renderer : function(value, metadata, record ) {
                        return value === null ? "" : record.data.serverAddr + ":" + record.data.serverPort;
                    }
                }]
            });
        },
        // RBL Event Log
        buildRBLEventLog : function() {
            this.gridRBLEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                name : 'Tarpit Event Log',
                helpSource : 'tarpit_event_log',
                eventManagerFn : this.getRpcNode().getRBLEventManager(),
                title : this.i18n._("Tarpit Event Log"),
                // the list of fields
                fields : [{
                    name : 'timeStamp'
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
                    dataIndex : 'timeStamp',
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
                        return value == null ? "" : value;
                    }
                }, {
                    header : this.i18n._("DNSBL server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'hostname'
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
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._('The value of Strength Value field is invalid.'),
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
