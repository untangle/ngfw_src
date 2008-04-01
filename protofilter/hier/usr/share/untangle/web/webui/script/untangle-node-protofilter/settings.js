if(!Ung.hasResource["Ung.Protofilter"]) {
Ung.hasResource["Ung.Protofilter"]=true;
Ung.Settings.registerClassName('untangle-node-protofilter','Ung.Protofilter');

Ung.Protofilter = Ext.extend(Ung.Settings, {
    gridProtocolList: null,
    gridEventLog: null,
     //called when the component is rendered
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.Protofilter.superclass.onRender.call(this,container, position);
		//builds the 2 tabs
		this.buildProtocolList();
		this.buildEventLog();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.gridProtocolList,this.gridEventLog]);
    },
    // Protocol list grid
    buildProtocolList: function() {
    	//blocked is a check column
	    var blockedColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", dataIndex: 'blocked', fixed:true
	    });
	    //log is a check column
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", dataIndex: 'log', fixed:true
	    });
		
    	this.gridProtocolList=new Ung.EditorGrid({
    		settingsCmp: this,
    		//the total records is set from the base settings patternsLength field
    		totalRecords: this.getBaseSettings().patternsLength,
    		emptyRow: {"category":i18n._("[no category]"),"protocol":i18n._("[no protocol]"),"blocked":false,"log":false,"description":i18n._("[no description]"),"definition":"[no signature]"},
    		title: this.i18n._("Protocol List"),
    		//the column is autoexpanded if the grid width permits
    		autoExpandColumn: 'category',
    		recordJavaClass: "com.untangle.node.protofilter.ProtoFilterPattern",
    		//this is the function used by Ung.RpcProxy to retrive data from the server
    		proxyRpcFn: this.getRpcNode().getPatterns,
			//the list of fields
			fields: [
	           {name: 'id'},
	           //this field is internationalized so a converter was added
	           {name: 'category', convert: function(v){ return this.i18n._(v)}.createDelegate(this)},
	           {name: 'protocol'},
	           {name: 'blocked'},
	           {name: 'log'},
	           //this field is internationalized so a converter was added
	           {name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)},
	           {name: 'definition'}
			],
			//the list of columns for the column model
			columns: [
				{id:'id', dataIndex: 'id', hidden: true },
				{id:'category',header: this.i18n._("category"), width: 140, dataIndex: 'category',
					//this is a simple text editor
					editor: new Ext.form.TextField({allowBlank: false})},
				{id:'protocol',header: this.i18n._("protocol"), width: 100, dataIndex: 'protocol',
					editor: new Ext.form.TextField({allowBlank: false})},
				blockedColumn,
				logColumn,
				{id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
					editor: new Ext.form.TextField({allowBlank: false})},
				{id:'definition',header: this.i18n._("signature"), width: 120, dataIndex: 'definition',
					editor: new Ext.form.TextField({allowBlank: false})}
			],
			sortField: 'category',
			columnsDefaultSortable: true,
			plugins: [blockedColumn,logColumn],
			// the row input lines used by the row editor window
			rowEditorInputLines: [
				{name:"category", label: this.i18n._("Category"), type:"text", style:"width:200px;"},
				{name:"protocol", label: this.i18n._("Protocol"), type:"text", style:"width:200px;"},
				{name:"blocked", label: this.i18n._("Block"), type:"checkbox"},
				{name:"log", label: this.i18n._("Log"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
				{name:"definition", label: this.i18n._("Signature"), type:"textarea", style:"width:200px;height:60px;"}
			]
    	});
    },
    // Event Log
    buildEventLog: function() {
		this.gridEventLog=new Ung.GridEventLog({
			settingsCmp: this,
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
		}.createDelegate(this),this.gridProtocolList.getSaveList());
	}
});
}