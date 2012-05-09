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

        initComponent : function() {
            this.getSettings();
            this.lastUpdate = this.getRpcNode().getLastUpdate();
            this.lastCheck = this.getRpcNode().getLastUpdateCheck();
            this.signatureVer = this.getRpcNode().getSignatureVersion();

            this.buildEmail();
            this.buildEventLog();
            this.buildDnsblEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.emailPanel, this.gridEventLog, this.gridDnsblEventLog]);
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
            Ext.getCmp('spamassassin_smtpStrengthValue').setVisible(this.isCustomStrength(this.settings.smtpConfig.strength));
            Ext.getCmp('spamassassin_pop3StrengthValue').setVisible(this.isCustomStrength(this.settings.popConfig.strength));
            Ext.getCmp('spamassassin_imapStrengthValue').setVisible(this.isCustomStrength(this.settings.imapConfig.strength));
            if(!this.settings.smtpConfig.scan) {
                Ext.getCmp('spamassassin_smtpStrengthValue').disable();
            }
            if(!this.settings.popConfig.scan) {
                Ext.getCmp('spamassassin_pop3StrengthValue').disable();
            }
            if(!this.settings.imapConfig.scan) {
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
                             ['DROP', this.i18n._('Drop')], ['QUARANTINE', this.i18n._('Quarantine')]];
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
                        checked : this.settings.smtpConfig.scan,
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
                                    this.settings.smtpConfig.scan = newValue;
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
                                value : this.getStrengthSelectionValue(this.settings.smtpConfig.strength),
                                listeners : {
                                    select : Ext.bind(function(elem, record,index) {
                                            var customCmp = Ext.getCmp('spamassassin_smtpStrengthValue');
                                            if (record[0].data.field1 == 0) {
                                                customCmp.setVisible(true);
                                            } else {
                                                customCmp.setVisible(false);
                                                this.settings.smtpConfig.strength = record[0].data.field1;
                                            }
                                            customCmp.setValue(this.settings.smtpConfig.strength / 10.0);
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
                                value : this.settings.smtpConfig.strength / 10.0,
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
                                            this.settings.smtpConfig.strength = Math.round(newValue * 10);
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
                        value : this.settings.smtpConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.smtpConfig.msgAction = newValue;
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
                        checked : this.settings.smtpConfig.blockSuperSpam,
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
                                    this.settings.smtpConfig.blockSuperSpam = newValue;
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
                                 value : this.settings.smtpConfig.superSpamStrength / 10.0,
                                 allowDecimals: false,
                                 allowNegative: false,
                                 minValue: 0,
                                 itemCls : 'left-indent-4 super-spam-threshold x-item-disabled',
                                 maxValue: 2147483647,
								 hideTrigger:true,
                                 listeners : {
                                     "change" : {
                                         fn : Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.superSpamStrength = Math.round(newValue * 10);
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
                                     checked : this.settings.smtpConfig.tarpit,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, checked) {
                                                 this.settings.smtpConfig.tarpit = checked;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'SMTP Add Email Headers',
                                     boxLabel : this.i18n._('Add email headers'),
                                     hideLabel : true,
                                     checked : this.settings.smtpConfig.addSpamHeaders,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.addSpamHeaders = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'SMTP Fail Closed',
                                     boxLabel : this.i18n._('Close connection on scan failure'),
                                     hideLabel : true,
                                     checked : this.settings.smtpConfig.failClosed,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.failClosed = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'checkbox',
                                     name : 'Scan outbound (WAN) SMTP',
                                     boxLabel : this.i18n._('Scan outbound (WAN) SMTP'),
                                     hideLabel : true,
                                     checked : this.settings.smtpConfig.scanWanMail,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.scanWanMail = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('CPU Load Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP CPU Load Limit',
                                     value : this.settings.smtpConfig.loadLimit,
                                     allowDecimals: true,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a float.'),
                                     minValue : 0,
                                     maxValue : 50,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.loadLimit = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('Concurrent Scan Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP Concurrent Scan Limit',
                                     value : this.settings.smtpConfig.scanLimit,
                                     allowDecimals: false,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a integer.'),
                                     minValue : 0,
                                     maxValue : 100,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.scanLimit = newValue;
                                             },this)
                                         }
                                     }
                                 },{
                                     xtype : 'numberfield',
                                     fieldLabel : this.i18n._('Message Size Limit'),
                                     labelStyle : 'width:130px',
                                     name : 'SMTP Message Size Limit',
                                     value : this.settings.smtpConfig.msgSizeLimit,
                                     allowDecimals: false,
                                     allowBlank : false,
                                     blankText : this.i18n._('Value must be a integer.'),
                                     minValue : 0,
                                     maxValue : 2147483647,
									 hideTrigger:true,
                                     listeners : {
                                         "change" : {
                                             fn : Ext.bind(function(elem, newValue) {
                                                 this.settings.smtpConfig.msgSizeLimit = newValue;
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
                        checked : this.settings.popConfig.scan,
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
                                    this.settings.popConfig.scan = newValue;
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
                                value : this.getStrengthSelectionValue(this.settings.popConfig.strength),
                                listeners : {
                                    select : Ext.bind(function(elem, record,index) {
												var customCmp = Ext.getCmp('spamassassin_pop3StrengthValue');
												if (record[0].data.field1 == 0) {
													customCmp.setVisible(true);
												} else {
													customCmp.setVisible(false);
													this.settings.popConfig.strength = record[0].data.field1;
												}
												customCmp.setValue(this.settings.popConfig.strength / 10.0);
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
                                value : this.settings.popConfig.strength / 10.0,
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
                                            this.settings.popConfig.strength = Math.round(newValue * 10);
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
                        value : this.settings.popConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.popConfig.msgAction = newValue;
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
                            checked : this.settings.popConfig.addSpamHeaders,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.settings.popConfig.addSpamHeaders = newValue;
                                    },this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Message Size Limit'),
                            labelStyle : 'width:130px',
                            name : 'POP Message Size Limit',
                            value : this.settings.popConfig.msgSizeLimit,
                            allowDecimals: false,
                            allowBlank : false,
                            blankText : this.i18n._('Value must be a integer.'),
                            minValue : 0,
                            maxValue : 2147483647,
							hideTrigger:true,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.settings.popConfig.msgSizeLimit = newValue;
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
                        checked : this.settings.imapConfig.scan,
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
                                    this.settings.imapConfig.scan = newValue;
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
                                value : this.getStrengthSelectionValue(this.settings.imapConfig.strength),
                                listeners : {
                                    select :Ext.bind(function(elem, record, index) {
                                            var customCmp = Ext.getCmp('spamassassin_imapStrengthValue');
                                            if (record[0].data.field1 == 0) {
                                                customCmp.setVisible(true);
                                            } else {
                                                customCmp.setVisible(false);
                                                this.settings.imapConfig.strength = record[0].data.field1;
                                            }
                                            customCmp.setValue(this.settings.imapConfig.strength / 10.0);
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
                                value : this.settings.imapConfig.strength / 10.0,
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
                                            this.settings.imapConfig.strength = Math.round(newValue * 10);
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
                        value : this.settings.imapConfig.msgAction,
                        listeners : {
                            "change" : {
                                fn : Ext.bind(function(elem, newValue) {
                                    this.settings.imapConfig.msgAction = newValue;
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
                            checked : this.settings.imapConfig.addSpamHeaders,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.settings.imapConfig.addSpamHeaders = newValue;
                                    },this)
                                }
                            }
                        },{
                            xtype : 'numberfield',
                            fieldLabel : this.i18n._('Message Size Limit'),
                            labelStyle : 'width:130px',
                            name : 'IMAP Message Size Limit',
                            value : this.settings.imapConfig.msgSizeLimit,
                            allowDecimals: false,
                            allowBlank : false,
                            blankText : this.i18n._('Value must be a integer.'),
                            minValue : 0,
                            maxValue : 2147483647,
							hideTrigger:true,
                            listeners : {
                                "change" : {
                                    fn : Ext.bind(function(elem, newValue) {
                                        this.settings.imapConfig.msgSizeLimit = newValue;
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
                    name : 'time_stamp',
                    sortType : Ung.SortTypes.asTimestamp
                }, {
                    name : 'vendor',
                    mapping : 'vendor'
                }, {
                    name : 'displayAction',
                    mapping :  this.getRpcNode().getVendor() + '_action',
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
                    mapping : 'c_client_addr'
                }, {
                    name : 'server',
                    mapping : 's_server_addr'
                }, {
                    name : 'subject',
                    type : 'string'
                }, {
                    name : 'addr_name',
                    type : 'string'
                }, {
                    name : 'addr',
                    type : 'string'
                }, {
                    name : 'sender',
                    type : 'string'
                }, {
                    name : 'score',
                    mapping : this.getRpcNode().getVendor() + '_score'
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
                    width : 150,
                    flex:1,
                    sortable : true,
                    dataIndex : 'subject'
                }, {
                    header : this.i18n._("Action"),
                    width : 125,
                    sortable : true,
                    dataIndex : 'displayAction'
                }, {
                    header : this.i18n._("Spam score"),
                    width : 70,
                    sortable : true,
                    dataIndex : 'score'
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
                    name : 'time_stamp',
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
                    mapping : 'ip_addr',
                    convert : function(value) {
                        return value == null ? "" : value;
                    }
                }, {
                    name : 'hostname'
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
                    header : this.i18n._("Action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'action'
                }, {
                    header : this.i18n._("Sender"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'sender'
                }, {
                    header : this.i18n._("DNSBL Server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'hostname'
                }]
            });
        },
        validate: function() {
            var cmp = null;
            var valid =
                ((cmp = Ext.getCmp('spamassassin_smtpStrengthValue')).isValid() &&
                 (cmp = Ext.getCmp('spamassassin_pop3StrengthValue')).isValid() &&
                 (cmp = Ext.getCmp('spamassassin_imapStrengthValue')).isValid());
            if (!valid) {
                Ext.MessageBox.alert(i18n._("Failed"), this.i18n._('The value of Strength Value field is invalid.'),
                                     Ext.bind(function () {
                                         this.tabs.setActiveTab(this.emailPanel);
                                         cmp.focus(true);
                                     },this)
                                    );
            }
            return valid;
        }
    });
}
//@ sourceURL=spamassassin-settingsNew.js
