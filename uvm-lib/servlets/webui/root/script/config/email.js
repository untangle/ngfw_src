if (!Ung.hasResource["Ung.Email"]) {
    Ung.hasResource["Ung.Email"] = true;

    Ung.Email = Ext.extend(Ung.ConfigWin, {
        panelOutgoingServer : null,
        panelFromSafeList : null,
        panelQuarantine : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('Email')
            }];
            Ung.Email.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Email.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 5 tabs
        },
        initSubCmps : function() {
            this.buildOutgoingServer();
            this.buildFromSafeList();
            this.buildQuarantine();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelOutgoingServer, this.panelFromSafeList, this.panelQuarantine]);
            this.tabs.activate(this.panelOutgoingServer);
        },
        // get languange settings object
        getLanguageSettings : function(forceReload) {
            if (forceReload || this.rpc.languageSettings === undefined) {
                this.rpc.languageSettings = rpc.languageManager.getLanguageSettings();
            }
            return this.rpc.languageSettings;
        },
        buildOutgoingServer : function() {
            this.panelOutgoingServer = new Ext.Panel({
                // private fields
                name : 'panelOutgoingServer',
                parentId : this.getId(),
                title : this.i18n._('Outgoing Server'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                onEmailTest: function() {
                	Ung.Util.todo();
                },

                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Outgoing Email Server (SMTP)'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n
                                ._("The Outgoing Email Server settings determine how the Untangle Server sends emails such as reports, quarantine digests, etc. In most cases the default setting should work, but if not, you should specify an SMTP server that will relay mail for the  Untangle Server.")
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('Send Email Directly'),
                        hideLabel : true,
                        name : 'httpEnabled',
                        // checked : this.getHttpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    // this.getHttpSettings().enabled = checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'radio',
                        boxLabel : this.i18n._('Send Email using the specified SMTP Server'),
                        hideLabel : true,
                        name : 'httpEnabled',
                        // checked : !this.getHttpSettings().enabled,
                        listeners : {
                            "check" : {
                                fn : function(elem, checked) {
                                    // this.getHttpSettings().enabled =
                                    // !checked;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'fieldset',
                        autoHeight : true,
                        items : [{
                            xtype : 'textfield',
                            name : 'smtpStrengthValue',
                            fieldLabel : this.i18n._('Server Address or Hostname'),
                            allowBlank : false,
                            // value :
                            // this.getBaseSettings().smtpConfig.strength,
                            // disabled :
                            // !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                            // regex : /^(100|[3-9][0-9])$/,
                            // regexText : this.i18n._('Strength Value must be a
                            // number in range 100-30. Smaller value is higher
                            // strength.'),
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        // this.getBaseSettings().smtpConfig.strength
                                        // = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'textfield',
                            name : 'smtpStrengthValue',
                            fieldLabel : this.i18n._('Server Port'),
                            allowBlank : false,
                            // value :
                            // this.getBaseSettings().smtpConfig.strength,
                            // disabled :
                            // !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                            // regex : /^(100|[3-9][0-9])$/,
                            // regexText : this.i18n._('Strength Value must be a
                            // number in range 100-30. Smaller value is higher
                            // strength.'),
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        // this.getBaseSettings().smtpConfig.strength
                                        // = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }, {
                            xtype : 'checkbox',
                            name : 'useAuthentication',
                            boxLabel : this.i18n._('Use Authentication'),
                            hideLabel : true,
                            // checked :
                            // this.getAccessSettings().isSupportEnabled,
                            listeners : {
                                "change" : {
                                    fn : function(elem, newValue) {
                                        // this.getAccessSettings().isSupportEnabled
                                        // = newValue;
                                    }.createDelegate(this)
                                }
                            }
                        }]
                    }]
                }, {
                    title : this.i18n._('Email From Address'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                    	html : this.i18n._("The Untangle Server will send email from this address.")
                    }, {
                        xtype : 'textfield',
                        name : 'emailFromAddress',
                        hideLabel : true,
                        allowBlank : false,
                        // value :
                        // this.getBaseSettings().smtpConfig.strength,
                        // disabled :
                        // !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                        // regex : /^(100|[3-9][0-9])$/,
                        // regexText : this.i18n._('Strength Value must be a
                        // number in range 100-30. Smaller value is higher
                        // strength.'),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    // this.getBaseSettings().smtpConfig.strength
                                    // = newValue;
                                }.createDelegate(this)
                            }
                        }

                    }]
                }, {
                    title : this.i18n._('Email Test'),
                    items : [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('The Email Test will send an email to a specified address with the current configuration. If the test email is not received your settings may be incorrect.')
                    }], 
                    buttons : [{
                        text : this.i18n._("Email Test"),
                        name: "emailTestButton",
                        handler : function() {
                            this.panelOutgoingServer.onEmailTest();
                        }.createDelegate(this)
                    }]
                }]
            });
        },
        buildFromSafeList : function() {
            this.panelFromSafeList = new Ext.Panel({
                // private fields
                name : 'panelOutgoingServer',
                parentId : this.getId(),
                title : this.i18n._('From-Safe List'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Global')
                },{
                	title : this.i18n._('Per User')
                }]
            });
        },
        buildQuarantine : function() {
            this.panelQuarantine = new Ext.Panel({
                // private fields
                name : 'panelQuarantine',
                parentId : this.getId(),
                title : this.i18n._('Quarantine'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                	items: [{
                        xtype : 'textfield',
                        name : 'smtpStrengthValue',
                        fieldLabel : this.i18n._('Maximum Holding Time (days) (max 36)'),
                        allowBlank : false,
                        // value :
                        // this.getBaseSettings().smtpConfig.strength,
                        // disabled :
                        // !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                        regex : /^([0-9]|[0-2][0-9]|3[0-6])$/,
                        regexText : this.i18n._('Maximum Holding Time must be a number in range 0-36'),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    // this.getBaseSettings().smtpConfig.strength
                                    // = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'textfield',
                        name : 'smtpStrengthValue',
                        fieldLabel : this.i18n._('Digest Sending Time'),
                        allowBlank : false,
                        // value :
                        // this.getBaseSettings().smtpConfig.strength,
                        // disabled :
                        // !this.isCustomStrength(this.getBaseSettings().smtpConfig.strength),
                        //regex : /^([0-9]|[0-2][0-9]|3[0-6])$/,
                        //regexText : this.i18n._('Maximum Holding Time must be a number in range 0-36'),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    // this.getBaseSettings().smtpConfig.strength
                                    // = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                	title : this.i18n._('User Quarantines'),
                	items: [{
                	}]
                }, {
                	title : this.i18n._('Quarantinable Addresses'),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('This list is a list of all addresses that will have quarantines automatically created. An email address that does not have a quarantinable address will have their mail marked instead.')
                    },{
                    }]
                }, {
                    title : this.i18n._('Quarantine Forwards'),
                    items: [{
                        bodyStyle : 'padding:0px 0px 5px 5px;',
                        border : false,
                        html : this.i18n._('This is a list of email addresses whose quarantine digest get forward to another account. This is common for distribution lists where the whole list should not receive the digest.')
                    },{
                    }]
                }]
            });
        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.saveSemaphore = 1;
                // save language settings
                // rpc.languageManager.setLanguageSettings(function(result,
                // exception) {
                // if (exception) {
                // Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                // return;
                // }
                this.afterSave();
                // }.createDelegate(this), this.getLanguageSettings());
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                Ext.MessageBox.hide();
                this.cancelAction();
            }
        }

    });

}