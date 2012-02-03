if (!Ung.hasResource["Ung.SpamAssassin"]) {
    Ung.hasResource["Ung.SpamAssassin"] = true;
    Ung.NodeWin.registerClassName('untangle-node-spamassassin', 'Ung.SpamAssassin');

    Ext.define('Ung.SpamAssassin', {
		extend:'Ung.NodeWin',
        lastUpdate : null,
        lastCheck : null,
        signatureVersion : null,
        strengthsData : null,
        smtpData : null,
        spamData : null,
        emailPanel : null,
        gridEventLog : null,
        gridDnsblEventLog : null,
        // override get node settings object to reload the signature information.
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

            this.buildEmail();
            this.buildEventLog();
            this.buildDnsblEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.gridEventLog, this.gridDnsblEventLog]);
            //this.tabs.activate(this.emailPanel);
            Ung.SpamAssassin.superclass.initComponent.call(this);
        },
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.SpamAssassin.superclass.onRender.call(this, container, position);
            Ext.defer(this.initSubCmps,1, this);
            this.enableSuperSpam(Ext.getCmp('spamassassin_smtpAction'));
        },
        initSubCmps : function() {
            Ext.getCmp('spamassassin_smtpStrengthValue').setVisible(this.isCustomStrength(this.getNodeSettings().smtpConfig.strength));
            Ext.getCmp('spamassassin_pop3StrengthValue').setVisible(this.isCustomStrength(this.getNodeSettings().popConfig.strength));
            Ext.getCmp('spamassassin_imapStrengthValue').setVisible(this.isCustomStrength(this.getNodeSettings().imapConfig.strength));
            if(!this.getNodeSettings().smtpConfig.scan) {
                Ext.getCmp('spamassassin_smtpStrengthValue').disable();
            }
            if(!this.getNodeSettings().popConfig.scan) {
                Ext.getCmp('spamassassin_pop3StrengthValue').disable();
            }
            if(!this.getNodeSettings().imapConfig.scan) {
                Ext.getCmp('spamassassin_imapStrengthValue').disable();
            }
        },
        isCustomStrength : function(strength) {
            return !(strength == 50 || strength == 43 || strength == 35 || strength == 33 || strength == 30);
        },
        getStrengthSelectionValue : function(strength) {
            if (this.isCustomStrength(strength)) {
                return 0;
            } else {
                return strength;
            }
        },
        onBeforeCollapse : function( panel, animate )
        {
            this.isAdvanced = false;
            return true;
        },
        onBeforeExpand : function( panel, animate )
        {
            this.isAdvanced = true;
            return true;
        },
        //enable super spam if quarantine is selected from the drop down
        enableSuperSpam: function(elem){
            var dsfq = Ext.getCmp('drop-super-spam');
            var ssv = Ext.getCmp('spamassassin_smtpSuperStrengthValue');
            var newValue = elem.getValue();
            if(elem.disabled==true){
                dsfq.disable();
                ssv.disable();
            }else if(newValue == 'QUARANTINE' || newValue == 'MARK'){
                dsfq.enable();
                if(dsfq.getValue()){
                    ssv.enable();
                }else{
                    ssv.enable();
                }
            }else{
                dsfq.setValue(0);
                dsfq.disable();
                ssv.disable();
            }
            //Ext does not gray out the label of a textfield!
            if(ssv.disabled){
                ssv.fireEvent('disable');
            }else{
                ssv.fireEvent('enable');
            }
        },
        // Email Config Panel
        buildEmail : function() {
            this.smtpData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')],
                             ['BLOCK', this.i18n._('Drop')], ['QUARANTINE', this.i18n._('Quarantine')]];
            this.spamData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')]];
            this.strengthsData = [[50, this.i18n._('Low (Threshold: 5.0)')], [43, this.i18n._('Medium (Threshold: 4.3)')], [35, this.i18n._('High (Threshold: 3.5)')],
                                  [33, this.i18n._('Very High (Threshold: 3.3)')], [30, this.i18n._('Extreme (Threshold: 3.0)')], [0, this.i18n._('Custom')]];

            this.emailPanel = Ext.create('Ext.panel.Panel',{
                title : this.i18n._('Email'),
                name : 'Email',
                helpSource : 'email',
                layout : "anchor",
                defaults: {
                    anchor: '98%',
                    autoScroll: true
                },
                autoScroll : true,
                cls: 'ung-panel',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._('SMTP'),
                    autoHeight : true,
                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan SMTP',
                        boxLabel : this.i18n._('Scan SMTP'),
                        hideLabel : true,
                        checked : this.getNodeSettings().smtpConfig.scan,
                        listeners : {
                            "render":{
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('spamassassin_smtpStrength').enable();
                                        Ext.getCmp('spamassassin_smtpAction').enable();
                                    }else{
                                        Ext.getCmp('spamassassin_smtpStrength').disable();
                                        Ext.getCmp('spamassassin_smtpAction').disable();
                                    }
                                    this.enableSuperSpam(Ext.getCmp('spamassassin_smtpAction'));

                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().smtpConfig.scan = newValue;
                                    if(elem.getValue()){
                                        Ext.getCmp('spamassassin_smtpStrength').enable();
                                        Ext.getCmp('spamassassin_smtpAction').enable();
                                        if(Ext.getCmp('spamassassin_smtpStrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_smtpStrengthValue').enable();
                                        }
                                    }else{
                                        Ext.getCmp('spamassassin_smtpStrength').disable();
                                        Ext.getCmp('spamassassin_smtpAction').disable();
                                        if(Ext.getCmp('spamassassin_smtpStrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_smtpStrengthValue').disable();
                                        }
                                    }
                                    this.enableSuperSpam(Ext.getCmp('spamassassin_smtpAction'));
                                },this)
                            }
                        }
                    }, {
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
                          //  layout: 'form',
                            items: [{
                                xtype : 'combo',
                                name : 'SMTP Strength',
                                id : 'spamassassin_smtpStrength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                itemCls : 'left-indent-1',
                                width : 300,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getNodeSettings().smtpConfig.strength),
                                listeners : {
                                    select : Ext.bind(function(elem, record,index) {
                                            var customCmp = Ext.getCmp('spamassassin_smtpStrengthValue');
                                            if (record[0].data.field1 == 0) {
                                                customCmp.setVisible(true);
                                            } else {
                                                customCmp.setVisible(false);
                                                this.getNodeSettings().smtpConfig.strength = record[0].data.field1;
                                            }
                                            customCmp.setValue(this.getNodeSettings().smtpConfig.strength / 10.0);
                                        },this)
								}
                            }]
                        },{
                            border: false,
                            columnWidth:1,
//                            layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel : '&nbsp;&nbsp;&nbsp;' + this.i18n._('Strength Value'),
                                name : 'SMTP Strength Value',
                                id: 'spamassassin_smtpStrengthValue',
                                itemCls : 'left-indent-1',
                                value : this.getNodeSettings().smtpConfig.strength / 10.0,
                                width : 200,
                                allowDecimals: true,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
								hideTrigger:true,
                                listeners : {
                                    "change" : {
                                        fn : Ext.bind(function(elem, newValue) {
                                            this.getNodeSettings().smtpConfig.strength = Math.round(newValue * 10);
                                        },this)
                                    }
                                }
                            }]
                        }]
                    }, {
                        xtype : 'combo',
                        name : 'SMTP Action',
                        editable : false,
                        store : this.smtpData,
                        valueField : 'key',
                        displayField : 'name',
                        id :'spamassassin_smtpAction',
                        fieldLabel : this.i18n._('Action'),
                        itemCls : 'left-indent-1',
                        width : 300,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().smtpConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().smtpConfig.msgAction = newValue;
                                },this)
                            },
                            "render" : {
                                fn : Ext.bind(function(elem){
                                    this.enableSuperSpam(elem);
                                },this)
                            },
                            "select" :{
                                fn : Ext.bind(function(elem){
                                    this.enableSuperSpam(elem);
                                },this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'Enable Super Spam Blocking',
                        id : 'drop-super-spam',
                        boxLabel : this.i18n._('Drop Super Spam'),
                        hideLabel : true,
                        itemCls : 'left-indent-4',
                        checked : this.getNodeSettings().smtpConfig.blockSuperSpam,
                        listeners : {
                            "render" : {
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('spamassassin_smtpSuperStrengthValue').enable();
                                    }else{
                                        Ext.getCmp('spamassassin_smtpSuperStrengthValue').disable();
                                    }
                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().smtpConfig.blockSuperSpam = newValue;
                                    if(newValue){
                                        Ext.getCmp('spamassassin_smtpSuperStrengthValue').enable();
                                    }else{
                                        Ext.getCmp('spamassassin_smtpSuperStrengthValue').disable();
                                    }
                                },this)
                            }
                        }
                    },
                             {
                                 xtype : 'numberfield',
                                 labelWidth: 150,
                                 fieldLabel : this.i18n._('Super Spam Threshold'),
                                 name : 'Super Spam Level',
                                 id: 'spamassassin_smtpSuperStrengthValue',
                                 value : this.getNodeSettings().smtpConfig.superSpamStrength / 10.0,
                                 allowDecimals: false,
                                 allowNegative: false,
                                 minValue: 0,
                                 itemCls : 'left-indent-4 super-spam-threshold x-item-disabled',
                                 maxValue: 2147483647,
								 hideTrigger:true,
                                 listeners : {
                                     "change" : {
                                         fn : Ext.bind(function(elem, newValue) {
                                             this.getNodeSettings().smtpConfig.superSpamStrength = Math.round(newValue * 10);
                                         },this)
                                     },
                                     "disable" :{
                                         fn : function(elem){
                                             if(this.rendered){
                                                 this.getEl().addCls('x-item-disabled');
                                             }
                                         }
                                     },
                                     "render" :{
                                         fn : Ext.bind(function(elem){
                                             this.enableSuperSpam(Ext.getCmp('spamassassin_smtpAction'));
                                         },this)
                                     },
                                     "enable" : {
                                         fn : function(elem){
                                             if(this.rendered){
                                                 this.getEl().removeCls('x-item-disabled');
                                             }
                                         }
                                     }
                                 }
                             },{
                                 name:'advanced',
                                 xtype:'fieldset',
                                 title:i18n._("Advanced SMTP Configuration"),
                                 collapsible : true,
                                 collapsed : !this.isAdvanced,
                                 autoHeight : true,
                                 listeners : {
                                     beforecollapse : Ext.bind(this.onBeforeCollapse, this ),
                                     beforeexpand : Ext.bind(this.onBeforeExpand, this )
                                 },
                                 items : [{
                                     xtype : 'checkbox',
                                     name : 'Enable tarpitting',
                                     boxLabel : this.i18n._('Enable tarpitting'),
                                     hideLabel : true,
                                     checked : this.getNodeSettings().smtpConfig.tarpit,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, checked) {
                                                 this.getNodeSettings().smtpConfig.tarpit = checked;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'SMTP Add Email Headers',
                                     boxLabel : this.i18n._('Add email headers'),
                                     hideLabel : true,
                                     checked : this.getNodeSettings().smtpConfig.addSpamHeaders,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.addSpamHeaders = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'SMTP Fail Closed',
                                     boxLabel : this.i18n._('Close connection on scan failure'),
                                     hideLabel : true,
                                     checked : this.getNodeSettings().smtpConfig.failClosed,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.failClosed = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'Scan outbound (WAN) SMTP',
                                     boxLabel : this.i18n._('Scan outbound (WAN) SMTP'),
                                     hideLabel : true,
                                     checked : this.getNodeSettings().smtpConfig.scanWanMail,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.scanWanMail = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('CPU Load Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP CPU Load Limit',
                                     value : this.getNodeSettings().smtpConfig.loadLimit,
                                     allowDecimals: true,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a float.'),
                                     minValue : 0,
                                     maxValue : 50,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.loadLimit = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('Concurrent Scan Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP Concurrent Scan Limit',
                                     value : this.getNodeSettings().smtpConfig.scanLimit,
                                     allowDecimals: false,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a integer.'),
                                     minValue : 0,
                                     maxValue : 100,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.scanLimit = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('Message Size Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP Message Size Limit',
                                     value : this.getNodeSettings().smtpConfig.msgSizeLimit,
                                     allowDecimals: false,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a integer.'),
                                     minValue : 0,
                                     maxValue : 2147483647,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.getNodeSettings().smtpConfig.msgSizeLimit = newValue;
                                             },this)
                                         }
                                     }
                                 }]
                             }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('POP3'),
                    autoHeight : true,

                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan POP3',
                        boxLabel : this.i18n._('Scan POP3'),
                        hideLabel : true,
                        checked : this.getNodeSettings().popConfig.scan,
                        listeners : {
                            "render" : {
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('spamassassin_pop3Strength').enable();
                                        Ext.getCmp('spamassassin_pop3Action').enable();
                                    }else{
                                        Ext.getCmp('spamassassin_pop3Strength').disable();
                                        Ext.getCmp('spamassassin_pop3Action').disable();
                                    }
                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().popConfig.scan = newValue;
                                    if(newValue){
                                        Ext.getCmp('spamassassin_pop3Strength').enable();
                                        Ext.getCmp('spamassassin_pop3Action').enable();
                                        if(Ext.getCmp('spamassassin_pop3StrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_pop3StrengthValue').enable();
                                        }
                                    }else{
                                        Ext.getCmp('spamassassin_pop3Strength').disable();
                                        Ext.getCmp('spamassassin_pop3Action').disable();
                                        if(Ext.getCmp('spamassassin_pop3StrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_pop3StrengthValue').disable();
                                        }
                                    }
                                },this)
                            }
                        }
                    }, {
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
  //                          layout: 'form',
                            items: [{
                                xtype : 'combo',
                                name : 'POP3 Strength',
                                id : 'spamassassin_pop3Strength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                itemCls : 'left-indent-1',
                                width : 300,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getNodeSettings().popConfig.strength),
                                listeners : {
                                    select : Ext.bind(function(elem, record,index) {
												var customCmp = Ext.getCmp('spamassassin_pop3StrengthValue');
												if (record[0].data.field1 == 0) {
													customCmp.setVisible(true);
												} else {
													customCmp.setVisible(false);
													this.getNodeSettings().popConfig.strength = record[0].data.field1;
												}
												customCmp.setValue(this.getNodeSettings().popConfig.strength / 10.0);
											},this)
                                }
                            }]
                        },{
                            border: false,
                            columnWidth:1,
    //                        layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel :'&nbsp;&nbsp;&nbsp;' + this.i18n._('Strength Value'),
                                name : 'POP3 Strength Value',
                                id: 'spamassassin_pop3StrengthValue',
                                itemCls : 'left-indent-1',
                                value : this.getNodeSettings().popConfig.strength / 10.0,
                                width: 200,
                                allowDecimals: true,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
								hideTrigger:true,
                                listeners : {
                                    "change" : {
                                        fn : Ext.bind(function(elem, newValue) {
                                            this.getNodeSettings().popConfig.strength = Math.round(newValue * 10);
                                        },this)
                                    }
                                }
                            }]
                        }]
                    }, {
                        xtype : 'combo',
                        name : 'POP3 Action',
                        editable : false,
                        store : this.spamData,
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        id  : 'spamassassin_pop3Action',
                        width : 300,
                        mode : 'local',
                        itemCls : 'left-indent-1',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().popConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().popConfig.msgAction = newValue;
                                },this)
                            }
                        }
                    },{
                        name:'advanced',
                        xtype:'fieldset',
                        title:i18n._("Advanced POP Configuration"),
                        collapsible : true,
                        collapsed : !this.isAdvanced,
                        autoHeight : true,
                        listeners : {
                            beforecollapse : Ext.bind(this.onBeforeCollapse,this ),
                            beforeexpand : Ext.bind(this.onBeforeExpand, this )
                        },
                        items : [{
                            xtype : 'checkbox',
                            name : 'POP Add Email Headers',
                            boxLabel : this.i18n._('Add email headers'),
                            hideLabel : true,
                            checked : this.getNodeSettings().popConfig.addSpamHeaders,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.getNodeSettings().popConfig.addSpamHeaders = newValue;
                                    },this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Message Size Limit'),
                            labelStyle : 'width:130px',
                            name : 'POP Message Size Limit',
                            value : this.getNodeSettings().popConfig.msgSizeLimit,
                            allowDecimals: false,
                            allowBlank : false,
                            blankText : this.i18n._('Value must be a integer.'),
                            minValue : 0,
                            maxValue : 2147483647,
							hideTrigger:true,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.getNodeSettings().popConfig.msgSizeLimit = newValue;
                                    },this)
                                }
                            }
                        }]
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('IMAP'),
                    autoHeight : true,

                    items : [{
                        xtype : 'checkbox',
                        name : 'Scan IMAP',
                        boxLabel : this.i18n._('Scan IMAP'),
                        name : 'imapScan',
                        hideLabel : true,
                        checked : this.getNodeSettings().imapConfig.scan,
                        listeners : {
                            "render" : {
                                fn : Ext.bind(function(elem) {
                                    if(elem.getValue()){
                                        Ext.getCmp('spamassassin_imapStrength').enable();
                                        Ext.getCmp('spamassassin_imapAction').enable();
                                    }else{
                                        Ext.getCmp('spamassassin_imapStrength').disable();
                                        Ext.getCmp('spamassassin_imapAction').disable();
                                    }
                                },this)
                            },
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().imapConfig.scan = newValue;
                                    if(newValue){
                                        Ext.getCmp('spamassassin_imapStrength').enable();
                                        Ext.getCmp('spamassassin_imapAction').enable();
                                        if(Ext.getCmp('spamassassin_imapStrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_imapStrengthValue').enable();
                                        }
                                    }else{
                                        Ext.getCmp('spamassassin_imapStrength').disable();
                                        Ext.getCmp('spamassassin_imapAction').disable();
                                        if(Ext.getCmp('spamassassin_imapStrengthValue').isVisible()) {
                                            Ext.getCmp('spamassassin_imapStrengthValue').disable();
                                        }
                                    }
                                },this)
                            }
                        }
                    }, {
                        border: false,
                        layout:'column',
                        autoWidth : true,
                        items: [{
                            border: false,
      //                      layout: 'form',

                            items: [{
                                xtype : 'combo',
                                name : 'IMAP Strength',
                                id  : 'spamassassin_imapStrength',
                                editable : false,
                                store : this.strengthsData,
                                fieldLabel : this.i18n._('Strength'),
                                itemCls : 'left-indent-1',
                                width : 300,
                                mode : 'local',
                                triggerAction : 'all',
                                listClass : 'x-combo-list-small',
                                value : this.getStrengthSelectionValue(this.getNodeSettings().imapConfig.strength),
                                listeners : {
                                    select :Ext.bind(function(elem, record, index) {
                                            var customCmp = Ext.getCmp('spamassassin_imapStrengthValue');
                                            if (record[0].data.field1 == 0) {
                                                customCmp.setVisible(true);
                                            } else {
                                                customCmp.setVisible(false);
                                                this.getNodeSettings().imapConfig.strength = record[0].data.field1;
                                            }
                                            customCmp.setValue(this.getNodeSettings().imapConfig.strength / 10.0);
                                        },this)
                                }
                            }]
                        },{
                            border: false,
                            columnWidth:1,
        //                    layout: 'form',
                            items: [{
                                xtype : 'numberfield',
                                fieldLabel :'&nbsp;&nbsp;&nbsp;' + this.i18n._('Strength Value'),
                                name : 'IMAP Strength Value',
                                id: 'spamassassin_imapStrengthValue',
                                itemCls : 'left-indent-1',
                                value : this.getNodeSettings().imapConfig.strength / 10.0,
                                width: 200,
                                allowDecimals: true,
                                allowBlank : false,
                                blankText : this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                                minValue : -2147483648,
                                maxValue : 2147483647,
								hideTrigger:true,
                                listeners : {
                                    "change" : {
                                        fn : Ext.bind(function(elem, newValue) {
                                            this.getNodeSettings().imapConfig.strength = Math.round(newValue * 10);
                                        },this)
                                    }
                                }
                            }]
                        }]
                    }, {
                        xtype : 'combo',
                        name : 'IMAP Action',
                        id  : 'spamassassin_imapAction',
                        editable : false,
                        store : this.spamData,
                        valueField : 'key',
                        displayField : 'name',
                        fieldLabel : this.i18n._('Action'),
                        itemCls : 'left-indent-1',
                        width : 300,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getNodeSettings().imapConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.getNodeSettings().imapConfig.msgAction = newValue;
                                },this)
                            }
                        }
                    },{
                        name:'advanced',
                        xtype:'fieldset',
                        title:i18n._("Advanced IMAP Configuration"),
                        collapsible : true,
                        collapsed : !this.isAdvanced,
                        autoHeight : true,
                        listeners : {
                            beforecollapse : Ext.bind(this.onBeforeCollapse, this ),
                            beforeexpand : Ext.bind(this.onBeforeExpand,this )
                        },
                        items : [{
                            xtype : 'checkbox',
                            name : 'IMAP Add Email Headers',
                            boxLabel : this.i18n._('Add email headers'),
                            hideLabel : true,
                            checked : this.getNodeSettings().imapConfig.addSpamHeaders,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.getNodeSettings().imapConfig.addSpamHeaders = newValue;
                                    },this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Message Size Limit'),
                            labelStyle : 'width:130px',
                            name : 'IMAP Message Size Limit',
                            value : this.getNodeSettings().imapConfig.msgSizeLimit,
                            allowDecimals: false,
                            allowBlank : false,
                            blankText : this.i18n._('Value must be a integer.'),
                            minValue : 0,
                            maxValue : 2147483647,
							hideTrigger:true,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.getNodeSettings().imapConfig.msgSizeLimit = newValue;
                                    },this)
                                }
                            }
                        }]
                    }]
                }, {
                    xtype : 'fieldset',
                    title : this.i18n._('Note'),
                    autoHeight : true,
                    cls: 'description',
                    html : this.i18n._('Spam Blocker Lite last checked for updates') + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + (this.lastCheck != null ? i18n.timestampFormat(this.lastCheck) : i18n._("unknown"))
                        + '<br\>'
                        + this.i18n._('Spam Blocker Lite was last updated') + ":&nbsp;&nbsp;&nbsp;&nbsp;"
                        + (this.lastUpdate != null ? i18n.timestampFormat(this.lastUpdate) : i18n._("unknown"))
            }]
            });
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = Ext.create('Ung.GridEventLog',{
                helpSource : 'event_log',
                settingsCmp : this,
                // the list of fields
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'vendor',
                    mapping : 'vendor'
                }, {
                    name : 'displayAction',
                    mapping :  this.getRpcNode().getVendor() + 'Action',
                    type : 'string',
                    convert : Ext.bind( function(value, rec ) { // FIXME: make that a switch
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
                }, {
                    name : 'score',
                    mapping : this.getRpcNode().getVendor() + 'Score'
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
                    header : this.i18n._("spam score"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'score'
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
        // Dnsbl Event Log
        buildDnsblEventLog : function() {
            this.gridDnsblEventLog = Ext.create('Ung.GridEventLog',{
                settingsCmp : this,
                name : 'Tarpit Event Log',
                helpSource : 'tarpit_event_log',
                eventQueriesFn : this.getRpcNode().getTarpitEventQueries,
                title : this.i18n._("Tarpit Event Log"),
                // the list of fields
                fields : [{
                    name : 'timeStamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'action',
                    mapping : 'skipped',
                    type : 'string',
                    convert : Ext.bind(function(value) {
                        return value ? this.i18n._("skipped") : this.i18n._("blocked");
                    },this)
                }, {
                    name : 'sender',
                    mapping : 'IPAddr',
                    convert : function(value) {
                        return value == null ? "" : value;
                    }
                }, {
                    name : 'hostname'
                }],
                // the list of columns
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
                    dataIndex : 'action'
                }, {
                    header : this.i18n._("sender"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'sender'
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
                                     Ext.bind(function () {
                                         this.tabs.activate(this.emailPanel);
                                         cmp.focus(true);
                                     },this)
                                    );
            }
            return valid;
        },
        //apply function
        applyAction : function(){
            this.saveAction(true);
        },
        // save function
        saveAction : function(keepWindowOpen) {
             if (this.isDirty() === false) {
                 if (!keepWindowOpen) { this.closeWindow(); }
                 return;
             }
            if (this.validate()) {
                Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
                this.getRpcNode().setSettings(Ext.bind(function(result, exception) {
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                    // exit settings screen
                    if(!keepWindowOpen) {
                        this.closeWindow();
                    } else {
                        this.initialNodeSettings = Ung.Util.clone(this.getNodeSettings());
                    }
                },this), this.getNodeSettings());
            }
        },
        isDirty : function() {
            return !Ung.Util.equals(this.getNodeSettings(), this.initialNodeSettings);
        }

    });
}
