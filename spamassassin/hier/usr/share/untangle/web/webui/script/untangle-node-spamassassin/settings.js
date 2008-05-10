	if(!Ung.hasResource["Ung.SpamAssassin"]) {
	    Ung.hasResource["Ung.SpamAssassin"]=true;
	    Ung.Settings.registerClassName('untangle-node-spamassassin','Ung.SpamAssassin');
	    
	    Ung.SpamAssassin = Ext.extend(Ung.Settings, {
		strengths: null,
                strengthsValues: null,
		actions: null,
                actionsValues: null,
		emailPanel: null,
		gridEventLog: null,
		gridRBLEventLog: null,
		//called when the component is rendered
		onRender: function(container, position) {
                    this.strengths = [ this.i18n._('Low'), this.i18n._('Medium'), this.i18n._('High'), this.i18n._('Very High'), this.i18n._('Extreme'), this.i18n._('Custom') ];
                    this.strengthsValues = [ 50, 43, 35, 33, 30, 20 ];
		    this.actions = [ this.i18n._('Quarantine'), this.i18n._('Block'), this.i18n._('Mark'), this.i18n._('Pass') ];
		    this.actionsValues = [ 'Quarantine', 'Block', 'Mark', 'Pass' ],
    		    //call superclass renderer first
    		    Ung.SpamAssassin.superclass.onRender.call(this,container, position);
		    //builds the 2 tabs
		    this.buildEmail();
		    this.buildEventLog();
		    //builds the tab panel with the tabs
		    this.buildTabPanel([this.emailPanel, this.gridEventLog, this.gridRBLEventLog]);
		},
                lookup: function(needle, haystack1, haystack2) {
                    for ( var  i = 0; i < haystack1.length; i++ ) {
                        if ( haystack1[i] != undefined && haystack2[i] != undefined ) {
			    if ( needle == haystack1[i] ) { return haystack2[i]; }
                            if ( needle == haystack2[i] ) { return haystack1[i]; }
                        }
	            }
                },
		// Email Config Panel
		buildEmail: function() {
		    this.emailPanel = new Ext.Panel({
			title: this.i18n._('Email'),
			layout: "form",
			bodyStyle:'padding:5px 5px 0px 5px;',
			items: [{
			    xtype:'fieldset',
			    title: this.i18n._('SMTP'),
			    autoHeight:true,
			    defaults: {width: 210},
			    defaultType: 'textfield',
			    items :[ new Ext.form.Checkbox({boxLabel: this.i18n._('Scan SMTP'), name: 'smtpScan', hideLabel: true, checked: this.getBaseSettings().smtpConfig.bScan, 
							    listeners: {
								"change": {
								    fn: function(elem, newValue) {
									this.getBaseSettings().smtpConfig.bScan=newValue;
								    }.createDelegate(this)
								}
							    }}),
				     new Ext.form.ComboBox({
                                         store: this.strengths,
                                         fieldLabel: this.i18n._('Strength'),
                                         displayField: 'select',
                                         valueField: 'smtpStrengthValue',
                                         typeAhead: false,
                                         emptyText: this.i18n._('Medium'),
                                         mode: 'local',
                                         triggerAction: 'all',
                                         listClass: 'x-combo-list-small',
                                         selectOnFocus:true,
                                         value: this.lookup( this.getBaseSettings().smtpConfig.strength, this.strengths, this.strengthsValues ),
					 listeners: {
					     "change": {
						 fn: function(elem, newValue) {
						     this.getBaseSettings().smtpConfig.strength=this.lookup( newValue, this.strengths, this.strengthsValues );
						 }.createDelegate(this)
					     }
					 }}),
				     new Ext.form.ComboBox({
                                         store: this.actions,
                                         fieldLabel: this.i18n._('Action'),
                                         displayField: 'String',
                                         valueField: 'smtpActionValue',
                                         typeAhead: false,
                                         emptyText: this.i18n._('Quarantine'),
                                         mode: 'local',
                                         triggerAction: 'all',
                                         listClass: 'x-combo-list-small',
                                         selectOnFocus:true,
                                         value: this.lookup( this.getBaseSettings().smtpConfig.zMsgAction, this.actions, this.actionsValues ),
					 listeners: {
					     "change": {
						 fn: function(elem, newValue) {
						     this.getBaseSettings().smtpConfig.zMsgAction=this.lookup( newValue, this.actions, this.actionsValues );
						 }.createDelegate(this)
					     }
					 }})
				   ]},{
				       xtype:'fieldset',
				       title: this.i18n._('POP3'),
				       autoHeight:true,
				       defaults: {width: 210},
				       defaultType: 'textfield',
				       items :[ new Ext.form.Checkbox({boxLabel: 'Scan POP3', name: 'pop3Scan', hideLabel: true, checked: this.getBaseSettings().popConfig.bScan,
							    listeners: {
								"change": {
								    fn: function(elem, newValue) {
									this.getBaseSettings().popConfig.bScan=newValue;
								    }.createDelegate(this)
								}
							    }}),
						new Ext.form.ComboBox({
						    store: this.strengths,
						    fieldLabel: this.i18n._('Strength'),
						    displayField: 'String',
						    valueField: 'pop3StrengthValue',
						    typeAhead: false,
						    emptyText: this.i18n._('Medium'),
						    mode: 'local',
						    triggerAction: 'all',
						    listClass: 'x-combo-list-small',
						    selectOnFocus:true,
                                                    value: this.lookup( this.getBaseSettings().popConfig.strength, this.strengths, this.strengthsValues ),
						    listeners: {
							"change": {
							    fn: function(elem, newValue) {
								this.getBaseSettings().popConfig.strength=this.lookup( newValue, this.strengths, this.strengthsValues );
							    }.createDelegate(this)
							}
						    }}),
						new Ext.form.ComboBox({
						    store: this.actions,
						    fieldLabel: this.i18n._('Action'),
						    displayField: 'String',
						    valueField: 'pop3ActionValue',
						    typeAhead: false,
						    emptyText: this.i18n._('Mark'),
						    mode: 'local',
						    triggerAction: 'all',
						    listClass: 'x-combo-list-small',
						    selectOnFocus:true,
						    value: this.lookup( this.getBaseSettings().popConfig.zMsgAction, this.actions, this.actionsValues ),
						    listeners: {
							"change": {
							    fn: function(elem, newValue) {
								this.getBaseSettings().popConfig.zMsgAction=this.lookup( newValue, this.actions, this.actionsValues );
							    }.createDelegate(this)
							}
						    }})
					      ]},{
						  xtype:'fieldset',
						  title: this.i18n._('IMAP'),
						  autoHeight:true,
						  defaults: {width: 210},
						  defaultType: 'textfield',
						  items :[ new Ext.form.Checkbox({boxLabel: 'Scan IMAP', name: 'imapScan', hideLabel: true, checked: this.getBaseSettings().imapConfig.bScan }),
							   new Ext.form.ComboBox({
							       store: this.strengths,
							       fieldLabel: this.i18n._('Strength'),
							       displayField: 'String',
							       valueField: 'imapStrengthValue',
							       typeAhead: false,
							       emptyText: this.i18n._('Medium'),
							       mode: 'local',
							       triggerAction: 'all',
							       listClass: 'x-combo-list-small',
							       selectOnFocus:true,
							       value: this.lookup( this.getBaseSettings().imapConfig.strength, this.strengths, this.strengthsValues ),
							       listeners: {
								   "change": {
								       fn: function(elem, newValue) {
									   this.getBaseSettings().imapConfig.strength=this.lookup( newValue, this.strengths, this.strengthsValues );
								       }.createDelegate(this)
								   }
							       }}),
							   new Ext.form.ComboBox({
							       store: this.actions,
							       fieldLabel: this.i18n._('Action'),
							       displayField: 'String',
							       valueField: 'imapActionValue',
							       typeAhead: false,
							       emptyText: this.i18n._('Mark'),
							       mode: 'local',
							       triggerAction: 'all',
							       listClass: 'x-combo-list-small',
							       selectOnFocus:true,
							       value: this.lookup( this.getBaseSettings().imapConfig.zMsgAction, this.actions, this.actionsValues ),
							       listeners: {
								   "change": {
								       fn: function(elem, newValue) {
									   this.getBaseSettings().imapConfig.zMsgAction=this.lookup( newValue, this.actions, this.actionsValues );
								       }.createDelegate(this)
								   }
							       }})
							 ]},
				{
				    xtype:'fieldset',
				    title: this.i18n._('Note'),
				    autoHeight:true,
				    html: this.i18n._('Spam blocker was last updated ') + this.getBaseSettings().signatureVersion
				}]
		    });
		},
		// Event Log
		buildEventLog: function() {
		    this.gridEventLog=new Ung.GridEventLog({
			settingsCmp: this,
			hasRepositories: false,
			eventDepth: 1000,
			
			//the list of fields
			fields:[
			    {name: 'createDate'},
			    {name: 'client'},
			    {name: 'clientIntf'},
			    {name: 'reputation'},
			    {name: 'limited'},
			    {name: 'dropped'},
			    {name: 'rejected'}
			],
			//the list of columns
			columns: [
			    {header: this.i18n._("timestamp"), width: 120, sortable: true, dataIndex: 'createDate', renderer: function(value) {
				return i18n.timestampFormat(value);
			    }},
			    {header: this.i18n._("source"), width: 120, sortable: true, dataIndex: 'client'},
			    {header: this.i18n._("source")+"<br>"+this.i18n._("interface"), width: 120, sortable: true, dataIndex: 'clientIntf'},
			    {header: this.i18n._("reputation"), width: 120, sortable: true, dataIndex: 'reputation', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("limited"), width: 120, sortable: true, dataIndex: 'limited', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("dropped"), width: 120, sortable: true, dataIndex: 'dropped', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("reject"), width: 120, sortable: true, dataIndex: 'rejected', renderer: function(value) {
				return i18n.numberFormat(value);
			    }}
			],
			refreshList: function() {
			    this.settingsCmp.node.nodeContext.rpcNode.getLogs(this.refreshCallback.createDelegate(this), this.eventDepth);
			}
			
			
			
		    });
		    this.gridRBLEventLog=new Ung.GridEventLog({
			settingsCmp: this,
			hasRepositories: false,
			eventDepth: 1000,
			
			//the list of fields
			fields:[
			    {name: 'createDate'},
			    {name: 'client'},
			    {name: 'clientIntf'},
			    {name: 'reputation'},
			    {name: 'limited'},
			    {name: 'dropped'},
			    {name: 'rejected'}
			],
			//the list of columns
			columns: [
			    {header: this.i18n._("timestamp"), width: 120, sortable: true, dataIndex: 'createDate', renderer: function(value) {
				return i18n.timestampFormat(value);
			    }},
			    {header: this.i18n._("source"), width: 120, sortable: true, dataIndex: 'client'},
			    {header: this.i18n._("source")+"<br>"+this.i18n._("interface"), width: 120, sortable: true, dataIndex: 'clientIntf'},
			    {header: this.i18n._("reputation"), width: 120, sortable: true, dataIndex: 'reputation', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("limited"), width: 120, sortable: true, dataIndex: 'limited', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("dropped"), width: 120, sortable: true, dataIndex: 'dropped', renderer: function(value) {
				return i18n.numberFormat(value);
			    }},
			    {header: this.i18n._("reject"), width: 120, sortable: true, dataIndex: 'rejected', renderer: function(value) {
				return i18n.numberFormat(value);
			    }}
			],
			refreshList: function() {
			    this.settingsCmp.node.nodeContext.rpcNode.getLogs(this.refreshCallback.createDelegate(this), this.eventDepth);
			},
			eventManagerFn: function () {
			    if(this.settingsCmp.node.nodeContext.rpcNode.rblEventManager===undefined) {
				this.settingsCmp.node.nodeContext.rpcNode.rblEventManager=this.settingsCmp.node.nodeContext.rpcNode.getRBLEventManager();
			    }
			    return this.settingsCmp.node.nodeContext.rpcNode.rblEventManager;
			}
		    });
		},
		// save function
//		save: function() {
		    //disable tabs during save
// 		    this.tabs.disable();
// 		    this.getRpcNode().(function (result, exception) {
// 			//re-enable tabs
// 			this.tabs.enable();
// 			if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
// 			//exit settings screen
// 			this.node.onCancelClick();
// 		    }.createDelegate(this),);
//		}
	    });
	}
