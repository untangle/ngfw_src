if(!Ung.hasResource["Ung.SpamAssassin"]) {
Ung.hasResource["Ung.SpamAssassin"]=true;
Ung.Settings.registerClassName('untangle-node-spamassassin','Ung.SpamAssassin');

Ung.SpamAssassin = Ext.extend(Ung.Settings, {
    strengths: [ 'Low', 'Medium', 'High', 'Very High', 'Extreme', 'Custom' ],
    actions: [ 'Quarantine', 'Block', 'Mark', 'Pass' ],
    emailPanel: null,
    gridEventLog: null,
    //called when the component is rendered
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.SpamAssassin.superclass.onRender.call(this,container, position);
		//builds the 2 tabs
		this.buildEmail();
		this.buildEventLog();
		//builds the tab panel with the tabs
	this.buildTabPanel([this.emailPanel, this.gridEventLog]);
    },
    // get spam settings object
    getSpamSettings: function(forceReload) {
        if(forceReload || this.rpc.spamSettings===undefined) {
            this.rpc.spamSettings=this.getRpcNode().getSpamSettings();
        }
        return this.rpc.spamSettings;
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
		      items :[ new Ext.form.Checkbox({boxLabel: 'Scan SMTP', name: 'smtpScan', hideLabel: true }),
                               new Ext.form.ComboBox({
                                            store: this.strengths,
					    fieldLabel: 'Strength',
                                            displayField: 'String',
                                            valueField: 'smtpStrengthValue',
                                            typeAhead: false,
 				            emptyText: 'Medium',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
                                            selectOnFocus:true}),
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
                      autoHeight:true,
                      defaults: {width: 210},
                      defaultType: 'textfield',
                      items :[ new Ext.form.Checkbox({boxLabel: 'Scan POP3', name: 'pop3Scan', hideLabel: true }),
                               new Ext.form.ComboBox({
                                            store: this.strengths,
					    fieldLabel: 'Strength',
                                            displayField: 'String',
                                            valueField: 'pop3StrengthValue',
                                            typeAhead: false,
 				            emptyText: 'Medium',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
					    selectOnFocus:true}),
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
                      autoHeight:true,
                      defaults: {width: 210},
                      defaultType: 'textfield',
                      items :[ new Ext.form.Checkbox({boxLabel: 'Scan IMAP', name: 'imapScan', hideLabel: true }),
                               new Ext.form.ComboBox({
                                            store: this.strengths,
					    fieldLabel: 'Strength',
                                            displayField: 'String',
                                            valueField: 'imapStrengthValue',
                                            typeAhead: false,
 				            emptyText: 'Medium',
                                            mode: 'local',
                                            triggerAction: 'all',
                                            listClass: 'x-combo-list-small',
					    selectOnFocus:true}),
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
