//<script type="text/javascript">
if(!Ung._hasResource["Ung.Protofilter"]) {
Ung._hasResource["Ung.Protofilter"]=true;

Ung.Protofilter = Ext.extend(Ung.Settings, {
    gridProtocolList: null,
    gridEventLog: null,
    onRender: function(container, position) {
    	Ung.Protofilter.superclass.onRender.call(this,container, position);
		this.buildProtocolList();
		this.buildEventLog();
		this.bulidTabPanel([this.gridProtocolList,this.gridEventLog]);
    },
    
    buildProtocolList: function() {
	    // create the data store
	    var store = new Ext.data.JsonStore({
	        fields: [
	           {name: 'category'},
	           {name: 'protocol'},
	           {name: 'blocked'},
	           {name: 'log'},
	           {name: 'description'},
	           {name: 'definition'}
	        ]
	    });
	    // the column model has information about grid columns
	    // dataIndex maps the column to the specific data field in the data store (created below)
	    
	    var blockedColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", width: 40, dataIndex: 'blocked', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", width: 35, dataIndex: 'log', fixed:true
	    });
	    var editColumn=new Ext.grid.EditColumn();
	    var removeColumn=new Ext.grid.RemoveColumn();
	    var columnModel = new Ext.grid.ColumnModel([
	          {id:'category',header: this.i18n._("category"), width: 140,  dataIndex: 'category',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'protocol',header: this.i18n._("protocol"), width: 100, dataIndex: 'protocol',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          blockedColumn,
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'definition',header: this.i18n._("signature"), width: 120, dataIndex: 'definition',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          editColumn,
	          removeColumn
		]);
	
	    // by default columns are sortable
	    columnModel.defaultSortable = false;
		
		// create the Grid
	    this.gridProtocolList = new Ext.grid.EditorGridPanel({
	        store: store,
	        cm: columnModel,
	        tbar:[{
		            text: this.i18n._('Add'),
		            tooltip:this.i18n._('Add New Row'),
		            iconCls:'add',
		            parentId:this.getId(),
		            handler: function() {
		            	var cmp=Ext.getCmp(this.parentId);
		            	var rec=new Ext.data.Record({"category":"","protocol":"","blocked":false,"log":false,"description":"","definition":""});
						cmp.gridProtocolList.getStore().insert(0, [rec]);
						cmp.gridProtocolList.rowEditor.populate(rec,0);
           				cmp.gridProtocolList.rowEditor.show();		            	
		            }
		        }],
	        stripeRows: true,
	        plugins:[blockedColumn,logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit: 1,
	        title: this.i18n._('Protocol List'),
	    });
	    // create the row editor
	    this.gridProtocolList.rowEditor=new Ung.RowEditorWindow({
			width: 400,
			height: 300,
			key: "protocolList",
			settingsCmp: this,
			grid: this.gridProtocolList,
			inputLines: [
				{name:"category", label: this.i18n._("Category"), type:"text", style:"width:200px;"},
				{name:"protocol", label: this.i18n._("Protocol"), type:"text", style:"width:200px;"},
				{name:"blocked", label: this.i18n._("Block"), type:"checkbox"},
				{name:"log", label: this.i18n._("Log"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
				{name:"definition", label: this.i18n._("Signature"), type:"textarea", style:"width:200px;height:60px;"}
			]
		});
		this.gridProtocolList.rowEditor.render('container');
		this.gridProtocolList.rowEditor.initContent(); // TODO: do this on render.
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
    loadProtocolList: function() {
    	this.gridProtocolList.getStore().loadData(this.rpc.settings.patterns.list);
    },
    
    
    saveProtocolList: function() {
    	this.tabs.disable();
    	this.gridProtocolList.getStore().commitChanges();
    	var records=this.gridProtocolList.getStore().getRange();
    	var list=[];
    	for(var i=0;i<records.length;i++) {
    		var pattern=records[i].data;
    		pattern.javaClass="com.untangle.node.protofilter.ProtoFilterPattern";
    		list.push(pattern);
    	}
    	this.rpc.settings.patterns.list=list;
    	this.rpc.settings.patterns.javaClass="java.util.ArrayList";
    	this.rpc.node.setProtoFilterSettings(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			if(this!==null) {
				this.tabs.enable();
			}
		}.createDelegate(this), this.rpc.settings);
    },
    
	loadData: function() {
		this.rpc.node.getProtoFilterSettings(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			if(this!==null) {
				this.rpc.settings=result;
		    	var pl=this.rpc.settings.patterns.list;
		    	for(var i=0;i<pl.length;i++) {
		    		var pattern=pl[i];
		    		pattern["category"]=this.i18n._(pattern["category"]);
		    		pattern["description"]=this.i18n._(pattern["description"]);
		    	}
				this.loadProtocolList();
			}
		}.createDelegate(this));
		
	},
	
	save: function() {
		this.saveProtocolList();
	}
});
Ung.Settings.registerClassName('untangle-node-protofilter','Ung.Protofilter');
Ext.reg('utgProtofilter', Ung.Protofilter);
}
//</script>