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
    
    buildProtocolList: function() {
	    var blockedColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", width: 40, dataIndex: 'blocked', sortable: true, fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", width: 35, dataIndex: 'log', sortable: true, fixed:true
	    });
	    var columns = [
	          {id:'id', dataIndex: 'id', hidden: true },
	          {id:'category',header: this.i18n._("category"), width: 140, sortable: true,  dataIndex: 'category',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'protocol',header: this.i18n._("protocol"), width: 100, sortable: true, dataIndex: 'protocol',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          blockedColumn,
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 120, sortable: true, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'definition',header: this.i18n._("signature"), width: 120, sortable: true, dataIndex: 'definition',
		          editor: new Ext.form.TextField({allowBlank: false})
	          }
		];
	    
		
    	this.gridProtocolList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().patternsLength,
    		emptyRow: {"category":"","protocol":"","blocked":false,"log":false,"description":"","definition":""},
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
			sortField: 'category',
			columns: columns,
			defaultSortable:true,
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
    
    buildEventLog: function() {
		// Event Log grid
		this.gridEventLog=new Ung.GridEventLog({
			settingsCmp: this,
			store: new Ext.data.JsonStore({
		        fields: [
		           {name: 'timeStamp'},
		           {name: 'blocked'},
		           {name: 'pipelineEndpoints'},
		           {name: 'protocol'},
		           {name: 'blocked'},
		           {name: 'server'}
		        ]
	    	}),
			columns: [
			    {header: this.i18n._("timestamp"), width: 120, sortable: true, dataIndex: 'timeStamp', renderer: function(value) {
			    	return i18n.timestampFormat(value);
			    }},
			    {header: this.i18n._("action"), width: 70, sortable: true, dataIndex: 'blocked', renderer: function(value) {
			    		return value?this.i18n._("blocked"):this.i18n._("passed");
			    	}.createDelegate(this)
			    },
			    {header: this.i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
			    {header: this.i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
			    {header: this.i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {
			    		return value?this.i18n._("blocked in block list"):this.i18n._("not blocked in block list");
			    	}.createDelegate(this)
			    },
			    {header: this.i18n._("server"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.SServerAddr.hostAddress+":"+value.SServerPort;}}
			]
		});
    },
    
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