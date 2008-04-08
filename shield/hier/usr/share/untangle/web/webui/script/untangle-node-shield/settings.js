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
    	//blocked is a check column
    	
	    var enableColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("enable"), dataIndex: 'live', fixed:true
	    });
	    
    	this.gridExceptions=new Ung.EditorGrid({
    		settingsCmp: this,
    		//the total records is set from the base settings shieldNodeRulesLength field
    		totalRecords: this.getBaseSettings().shieldNodeRulesLength,
//    		emptyRow: {"enable":true,"address":"1.2.3.4","blocked":false,"log":false,"description":i18n._("[no description]"),"definition":"[no signature]"},
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
	           {name: 'block'},
	           //this field is internationalized so a converter was added
	           {name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)},
			],
			//the list of columns for the column model
			columns: [
				{id:'id', dataIndex: 'id', hidden: true },
				enableColumn,
				{id:'address',header: this.i18n._("address"), width: 140, dataIndex: 'address',
					//this is a simple text editor
					editor: new Ext.form.TextField({allowBlank: false})},
				{id:'block',header: this.i18n._("block"), width: 100, dataIndex: 'block',
					editor: new Ext.form.TextField({allowBlank: false})},
				{id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
					editor: new Ext.form.TextField({allowBlank: false})}
			],
//			sortField: 'id',
//			columnsDefaultSortable: true,
//			plugins: [blockedColumn,logColumn],
			plugins: [enableColumn],
			// the row input lines used by the row editor window
			rowEditorInputLines: [
				{name:"enable", label: this.i18n._("Enable"), type:"checkbox"},
				{name:"address", label: this.i18n._("Sddress"), type:"text", style:"width:200px;"},
				{name:"block", label: this.i18n._("Block"), type:"text", style:"width:200px;"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
				{name:"definition", label: this.i18n._("Signature"), type:"textarea", style:"width:200px;height:60px;"}
			]
    	});
    },
    // Event Log
    buildEventLog: function() {
		this.gridEventLog=new Ung.GridEventLog({
			settingsCmp: this,
			hasRepositories: false,
			//This is a predefined event log, so there is no need to specify the fields and columns
			predefinedType: "TYPE1"
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
		}.createDelegate(this),this.gridExtensions.getSaveList());
	}
});
}