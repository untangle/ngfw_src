if (!Ung.hasResource["Ung.SpamAssassin"]) {
    Ung.hasResource["Ung.SpamAssassin"] = true;
    Ung.Settings.registerClassName('untangle-node-spamassassin', 'Ung.SpamAssassin');

    Ung.SpamAssassin = Ext.extend(Ung.Settings, {
        strengthsData : null,
        smtpData: null,
        spamData: null,
        emailPanel : null,
        gridEventLog : null,
        gridRBLEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            //workarownd to solve the problem with baseSettings.popConfig.msgAction==baseSettings.imapConfig.msgAction
            var baseSettings=this.getBaseSettings();
            if(baseSettings.popConfig.msgAction==baseSettings.imapConfig.msgAction) {
                var msgAction={};
                msgAction.javaClass=baseSettings.imapConfig.msgAction.javaClass;
                msgAction.key=baseSettings.imapConfig.msgAction.key;
                msgAction.name=baseSettings.imapConfig.msgAction.name;
                baseSettings.imapConfig.msgAction=msgAction;
            }
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
        isCustomStrength: function (strength) {
           return !(strength==50 || strength==43 || strength==35 ||strength==33 ||strength==30)
        },
        getStrengthSelectionValue: function (strength) {
        	if(this.isCustomStrength(strength)) {
        		return 0;
        	} else {
        		return strength;
        	}
        },
        // Email Config Panel
        buildEmail : function() {
            this.smtpData= [
                ['mark message',this.i18n._('Mark'),'M'],
                ['pass message',this.i18n._('Pass'),'P'],
                ['block message',this.i18n._('Block'),'B'],
                ['quarantine message',this.i18n._('Quarantine'),'Q']
            ];
            this.spamData= [
                ['mark message',this.i18n._('Mark'),'M'],
                ['pass message',this.i18n._('Pass'),'P']
            ];
        	this.strengthsData= [
        	   [50, this.i18n._('Low')],
               [43, this.i18n._('Medium')],
               [35, this.i18n._('High')],
               [33, this.i18n._('Very High')],
               [30, this.i18n._('Extreme')],
               [0, this.i18n._('Custom')],
        	];
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
                    items : [{
                    	xtype : 'checkbox',
                        boxLabel : this.i18n._('Scan SMTP'),
                        hideLabel : true,
                        checked : this.getBaseSettings().smtpConfig.scan,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.scan = newValue;
                                }.createDelegate(this)
                            }
                        }
                    },{
                        xtype : 'checkbox',
                        boxLabel : this.i18n._('Enable SMTP greylisting'),
                        hideLabel : true,
                        checked : this.getBaseSettings().smtpConfig.throttle,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.throttle = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'combo',
                        editable: false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().smtpConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                	var customCmp=this.emailPanel.items.get(0).items.get(3);
                                	if(newValue==0) {
                                		customCmp.enable();
                                	} else {
                                		customCmp.disable();
                                		this.getBaseSettings().smtpConfig.strength=newValue;
                                	}
                                	customCmp.setValue(this.getBaseSettings().smtpConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        fieldLabel: this.i18n._('Custom Strength'),
                        allowBlank:false,
                        value: this.getBaseSettings().smtpConfig.strength,
                        disabled:  !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().smtpConfig.strength=newValue;
                                }.createDelegate(this)
                            }
                        }
                    },  {
                        xtype:'combo',
                        editable: false,
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
                                    for(var i=0;i<this.smtpData.length;i++) {
                                       if(this.smtpData[i][0]==newValue) {
                                           this.getBaseSettings().smtpConfig.msgAction.key=this.smtpData[i][2]
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
                        boxLabel : 'Scan POP3',
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
                        editable: false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().popConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var customCmp=this.emailPanel.items.get(1).items.get(2);
                                    if(newValue==0) {
                                        customCmp.enable();
                                    } else {
                                        customCmp.disable();
                                        this.getBaseSettings().popConfig.strength=newValue;
                                    }
                                    customCmp.setValue(this.getBaseSettings().popConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        fieldLabel: this.i18n._('Custom Strength'),
                        allowBlank:false,
                        value: this.getBaseSettings().popConfig.strength,
                        disabled:  !this.isCustomStrength(this.getBaseSettings().popConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.strength=newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype: 'combo',
                        editable: false,
                        store : this.spamData,
                        fieldLabel : this.i18n._('Action'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value :this.getBaseSettings().popConfig.msgAction.name,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().popConfig.msgAction.name = newValue;
                                    for(var i=0;i<this.spamData.length;i++) {
                                       if(this.spamData[i][0]==newValue) {
                                           this.getBaseSettings().popConfig.msgAction.key=this.spamData[i][2]
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
                        boxLabel : 'Scan IMAP',
                        name : 'imapScan',
                        hideLabel : true,
                        checked : this.getBaseSettings().imapConfig.scan
                    }, {
                        xtype : 'combo',
                        editable: false,
                        store : this.strengthsData,
                        fieldLabel : this.i18n._('Strength'),
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getStrengthSelectionValue(this.getBaseSettings().imapConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    var customCmp=this.emailPanel.items.get(2).items.get(2);
                                    if(newValue==0) {
                                        customCmp.enable();
                                    } else {
                                        customCmp.disable();
                                        this.getBaseSettings().imapConfig.strength=newValue;
                                    }
                                    customCmp.setValue(this.getBaseSettings().imapConfig.strength);
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        fieldLabel: this.i18n._('Custom Strength'),
                        allowBlank:false,
                        value: this.getBaseSettings().imapConfig.strength,
                        disabled:  !this.isCustomStrength(this.getBaseSettings().imapConfig.strength),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getBaseSettings().imapConfig.strength=newValue;
                                }.createDelegate(this)
                            }
                        }
                    },  {
                        xtype : 'combo',
                        editable: false,
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
                                    for(var i=0;i<this.spamData.length;i++) {
                                       if(this.spamData[i][0]==newValue) {
                                           this.getBaseSettings().imapConfig.msgAction.key=this.spamData[i][2]
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
