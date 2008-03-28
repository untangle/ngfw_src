//<script type="text/javascript">
if(!Ung.hasResource["Ung.Protofilter"]) {
Ung.hasResource["Ung.Protofilter"]=true;
Ung.Settings.registerClassName('untangle-node-protofilter','Ung.Protofilter');

Ung.Protofilter = Ext.extend(Ung.Settings, {
    gridProtocolList: null,
    gridEventLog: null,
    onRender: function(container, position) {
    	Ung.Protofilter.superclass.onRender.call(this,container, position);
		this.buildProtocolList();
		this.buildEventLog();
		this.buildTabPanel([this.gridProtocolList,this.gridEventLog]);
    },
    // Protocol list grid
    buildProtocolList: function() {
	    var blockedColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", dataIndex: 'blocked', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", dataIndex: 'log', fixed:true
	    });
		
    	this.gridProtocolList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().patternsLength,
    		emptyRow: {"category":"[no category]","protocol":"[no protocol]","blocked":false,"log":false,"description":"[no description]","definition":"[no signature]"},
    		title: this.i18n._('Protocol List'),
    		autoExpandColumn: 'category',
    		recordJavaClass: "com.untangle.node.protofilter.ProtoFilterPattern",
    		proxyRpcFn: this.getRpcNode().getPatterns,
			fields: [
	           {name: 'id'},
	           {name: 'category'},
	           {name: 'protocol'},
	           {name: 'blocked'},
	           {name: 'log'},
	           {name: 'description'},
	           {name: 'definition'}
			],
			columns: [
				{id:'id', dataIndex: 'id', hidden: true },
				{id:'category',header: this.i18n._("category"), width: 140, dataIndex: 'category',
				 editor: new Ext.form.TextField({allowBlank: false})
				},
				{id:'protocol',header: this.i18n._("protocol"), width: 100, dataIndex: 'protocol',
				 editor: new Ext.form.TextField({allowBlank: false})
				},
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
			predefinedType: "TYPE1"
		});
    },
    // save function
	save: function() {
		this.tabs.disable();
		this.getRpcNode().updateAll(function (result, exception) {
			this.tabs.enable();
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			this.node.onCancelClick();
		}.createDelegate(this),this.gridProtocolList.getSaveList());
	}
});
}
//</script>