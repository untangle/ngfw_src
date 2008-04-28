if(!Ung.hasResource["Ung.Phish"]) {
Ung.hasResource["Ung.Phish"]=true;
Ung.Settings.registerClassName('untangle-node-phish','Ung.Phish');

Ung.Phish = Ext.extend(Ung.Settings, {
    actions: [ 'Quarantine', 'Block', 'Mark', 'Pass' ],
    emailPanel: null,
    webPanel: null,
    gridEventLog: null,
    //called when the component is rendered
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.Phish.superclass.onRender.call(this,container, position);
		//builds the 2 tabs
		this.buildEmail();
                this.buildWeb();
		this.buildEventLog();
		//builds the tab panel with the tabs
	this.buildTabPanel([this.emailPanel, this.webPanel, this.gridEventLog]);
    },
    // Email Config Panel
    buildEmail: function() {
    	this.emailPanel = new Ext.Panel({
		    title: this.i18n._('Email'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0',
	            items: [{
                      xtype:'fieldset',
                      title: this.i18n._('SMTP'),
                      collapsible: true,
                      autoHeight:true,
                      defaults: {width: 210},
                      defaultType: 'textfield',
		      items :[ new Ext.form.Checkbox({boxLabel: 'Scan SMTP', name: 'smtpScan' }),
                                new Ext.form.ComboBox({
                                            store: this.actions,
					    fieldLabel: 'Action',
                                            displayField: 'String',
                                            valueField: 'smtpActionValue',
                                            typeAhead: false,
 				            emptyText: 'Quarantine',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
                                            selectOnFocus:true})
                      ]},{
                      xtype:'fieldset',
                      title: this.i18n._('POP3'),
                      collapsible: true,
                      autoHeight:true,
                      defaults: {width: 210},
                      defaultType: 'textfield',
                      items :[ new Ext.form.Checkbox({boxLabel: 'Scan POP3', name: 'pop3Scan' }),
                               new Ext.form.ComboBox({
                                            store: this.actions,
					    fieldLabel: 'Action',
                                            displayField: 'String',
                                            valueField: 'pop3ActionValue',
                                            typeAhead: false,
 				            emptyText: 'Mark',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
                                            selectOnFocus:true})
                      ]},{
                      xtype:'fieldset',
                      title: this.i18n._('IMAP'),
                      collapsible: true,
                      autoHeight:true,
                      defaults: {width: 210},
                      defaultType: 'textfield',
                      items :[ new Ext.form.Checkbox({boxLabel: 'Scan IMAP', name: 'imapScan' }),
                               new Ext.form.ComboBox({
                                            store: this.actions,
					    fieldLabel: 'Action',
                                            displayField: 'String',
                                            valueField: 'imapActionValue',
                                            typeAhead: false,
 				            emptyText: 'Mark',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
                                            selectOnFocus:true})
                      ]},
                      {
	              xtype:'fieldset',
	              title: this.i18n._('Note'),
	              autoHeight:true,
	              html: this.i18n._('Phish Blocker email signatures were last updated xxx.')
		      }]
    	});
    },
    // Web Config Panel
    buildWeb: function() {
    	this.webPanel = new Ext.Panel({
		    title: this.i18n._('Web'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0',
	            items: [new Ext.form.Checkbox({boxLabel: 'Enable Phish web filtering', name: 'webScan' }),
                      {
	              xtype:'fieldset',
	              title: this.i18n._('Note'),
	              autoHeight:true,
	              html: this.i18n._('Phish Blocker web signatures were last updated xxx.')
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
    },
    // save function
	save: function() {
		//disable tabs during save
		this.tabs.disable();
		this.getRpcNode().updateAll(function (result, exception) {
			//re-enable tabs
			this.tabs.enable();
			if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			//exit settings screen
			this.node.onCancelClick();
		}.createDelegate(this),this.gridExceptions.getSaveList());
	}
});
}
