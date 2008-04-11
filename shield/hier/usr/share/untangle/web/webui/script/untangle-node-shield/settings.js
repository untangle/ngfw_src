if(!Ung.hasResource["Ung.Shield"]) {
Ung.hasResource["Ung.Shield"]=true;
Ung.Settings.registerClassName('untangle-node-shield','Ung.Shield');

Ung.Shield = Ext.extend(Ung.Settings, {
    gridExceptions: null,
    gridEventLog: null,
     //called when the component is rendered
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.Shield.superclass.onRender.call(this,container, position);
		//builds the 2 tabs
		this.buildExceptions();
		this.buildEventLog();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.gridExceptions,this.gridEventLog]);
    },
    // Exceptions grid
    buildExceptions: function() {
    	//enable is a check column
	    var enableColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("enable"), dataIndex: 'live', fixed:true
	    });
	    
		var deviderData = [[5,5+' '+this.i18n._("users")],[25,25+' '+this.i18n._("users")],[40,50+' '+this.i18n._("users")],[75,100+' '+this.i18n._("users")],[-1,this.i18n._("unlimited")]];
	    
    	this.gridExceptions=new Ung.EditorGrid({
    		settingsCmp: this,
    		//the total records is set from the base settings shieldNodeRulesLength field
    		totalRecords: this.getBaseSettings().shieldNodeRulesLength,
//    		emptyRow: {"enable":true,"address":"1.2.3.4","divider":5,"description":i18n._("[no description]")},
    		title: this.i18n._("Exceptions"),
    		//the column is autoexpanded if the grid width permits
    		autoExpandColumn: 'description',
    		recordJavaClass: "com.untangle.node.shield.ShieldNodeRule",
    		//this is the function used by Ung.RpcProxy to retrive data from the server
    		proxyRpcFn: this.getRpcNode().getShieldNodeRules,
    		
			//the list of fields
			fields: [
	           {name: 'id'},
	           {name: 'live'},
	           {name: 'address'},
	           {name: 'divider'},
	           //this field is internationalized so a converter was added
	           {name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)},
			],
			//the list of columns for the column model
			columns: [
				{id:'id', dataIndex: 'id', hidden: true },
				enableColumn,
				{id:'address',header: this.i18n._("address"), width: 140, dataIndex: 'address',
					//this is a simple text editor
					editor: new Ext.form.TextField({
						allowBlank: false, 
						validator: function (fieldValue) {
							return /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(fieldValue);
						}})
				},
				{id:'divider',header: this.i18n._("user")+"<br>"+this.i18n._("count"), width: 100, dataIndex: 'divider',
		           editor: new Ext.form.ComboBox({
					    store: new Ext.data.SimpleStore({
							fields:['dividerValue', 'dividerName'],
							data: deviderData
						}),
						displayField: 'dividerName',
						valueField: 'dividerValue',
					    typeAhead: true,
					    mode: 'local',
					    triggerAction: 'all',
					    listClass: 'x-combo-list-small',
					    selectOnFocus:true}),
					renderer: function(value) {
						for (var i=0;i<deviderData.length;i++){
							if (deviderData[i][0]==value) {
								return deviderData[i][1];
							}
						}
						return value;
					}
				},
				{id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
					editor: new Ext.form.TextField({allowBlank: false})}
			],
			//sortField: 'address',
			columnsDefaultSortable: true,
			plugins: [enableColumn],
			// the row input lines used by the row editor window
			rowEditorInputLines: [
				{name:"enable", label: this.i18n._("Enable"), type:"checkbox"},
				{name:"address", label: this.i18n._("Address"), type:"text", style:"width:200px;"},
				{name:"divider", label: this.i18n._("User Count"), type:"text", style:"width:200px;"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
			]
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