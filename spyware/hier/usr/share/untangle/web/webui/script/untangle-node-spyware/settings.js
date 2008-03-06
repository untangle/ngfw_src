//<script type="text/javascript">
if(!Untangle._hasResource["Untangle.Spyware"]) {
Untangle._hasResource["Untangle.Spyware"]=true;

Untangle.Spyware = Ext.extend(Untangle.Settings, {
	panelBlockLists: null,
    gridPassList: null,
    gridEventLog: null,
    onRender: function(container, position) {
    	Untangle.Spyware.superclass.onRender.call(this,container, position);
		this.builBlockLists();
		this.buildPassList();
		this.buildEventLog();
		this.buldTabPanel([this.panelBlockLists,this.gridPassList,this.gridEventLog]);
    },
    builBlockLists: function () {
		var template=new Ext.Template(
'<div>Web</div><hr/><div><input type="checkbox" id="web_checkbox_{key}" /> Block Spyware & Ad URLs</div>',
'<div>Cookies</div><hr/><div><input type="checkbox" id="cookies_checkbox_{key}" /> Block Tracking & Ad Cookies <div id="cookies_button_{key}"></div></div>',
'<div>ActiveX</div><hr/><div><input type="checkbox" id="activex_checkbox_{key}" /> Block Malware ActiveX Installs <div id="activex_button_{key}"></div></div>',
'<div>Traffic</div><hr/><div><input type="checkbox" id="traffic_checkbox_{key}" /> Monitor Suspicious Traffic <div id="traffic_button_{key}"></div></div>',
'<div>Spyware Blocker signatures were last updated: <div id="last_update_signatures_{key}"></div></div>');
		this.panelBlockLists=new Ext.Panel({
			title: this.i18n._("Block Lists"),
			key: "blockLists_"+this.getId(),
			parentId: this.getId(),
			html: template.applyTemplate({key: "blockLists_"+this.getId()}),
			afterRender: function() {
				Ext.Panel.prototype.afterRender.call(this);
				var settingsCmp= Ext.getCmp(this.parentId);
				var cmp=null;
				cmp=new Ext.Button({
					'renderTo':'cookies_button_'+this.key,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageCookiesList();}.createDelegate(settingsCmp)
				});
				cmp=new Ext.Button({
					'renderTo':'activex_button_'+this.key,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageActiveXList();}.createDelegate(settingsCmp)
				});
				cmp=new Ext.Button({
					'renderTo':'traffic_button_'+this.key,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageTrafficList();}.createDelegate(settingsCmp)
				});
			},
		});
    
    },
    onManageCookiesList: function () {
    	alert("TODO");
    },
    onManageActiveXList: function () {
    	alert("TODO");
    },
    onManageTrafficList: function () {
    	alert("TODO");
    },
    
    buildPassList: function() {
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
	    var editColumn=new Ext.grid.EditColumn({
	    	header: this.i18n._("Edit"), width: 35, fixed:true, dataIndex: null
	    });
	    var removeColumn=new Ext.grid.RemoveColumn({
	    	header: this.i18n._("Delete"), width: 40, fixed:true, dataIndex: null
	    });
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
	    this.gridPassList = new Ext.grid.EditorGridPanel({
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
						cmp.gridPassList.getStore().insert(0, [rec]);
						cmp.gridPassList.rowEditor.populate(rec,0);
           				cmp.gridPassList.rowEditor.show();		            	
		            }
		        }],
		        /*
			bbar: new Ext.PagingToolbar({
	            pageSize: 25,
	            store: store,
	            displayInfo: true,
	            displayMsg: 'Displaying topics {0} - {1} of {2}',
	            emptyMsg: "No topics to display",
	            items:[
	                '-', {
	                pressed: true,
	                enableToggle:true,
	                text: 'Show Preview',
	                cls: 'x-btn-text-icon details',
	                toggleHandler: toggleDetails
	            }]
	        }),*/     
	        stripeRows: true,
	        plugins:[blockedColumn,logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit: 1,
	        title: this.i18n._('Pass List'),
	    });
	    // create the row editor
	    this.gridPassList.rowEditor=new Untangle.RowEditorWindow({
			width: 400,
			height: 300,
			key: "protocolList",
			settingsCmp: this,
			grid: this.gridPassList,
			inputLines: [
				{name:"category", label: this.i18n._("Category"), type:"text", style:"width:200px;"},
				{name:"protocol", label: this.i18n._("Protocol"), type:"text", style:"width:200px;"},
				{name:"blocked", label: this.i18n._("Block"), type:"checkbox"},
				{name:"log", label: this.i18n._("Log"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
				{name:"definition", label: this.i18n._("Signature"), type:"textarea", style:"width:200px;height:60px;"}
			]
		});
		this.gridPassList.rowEditor.render('container');
		this.gridPassList.rowEditor.initContent(); // TODO: do this on render.
    },
    
    buildEventLog: function() {
		// Event Log grid
		this.gridEventLog=new Untangle.GridEventLog({
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
    loadPassList: function() {
    	this.gridPassList.getStore().loadData(this.rpc.passList);
    },
    
    
    savePassList: function() {
    	this.tabs.disable();
    	this.gridPassList.getStore().commitChanges();
    	var records=this.gridPassList.getStore().getRange();
    	var list=[];
    	for(var i=0;i<records.length;i++) {
    		var pattern=records[i].data;
    		pattern.javaClass="com.untangle.node.spyware.ProtoFilterPattern";
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
		this.rpc.node.getBaseSettings(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			this.rpc.settings=result;
			this.loadPassList();
			this.rpc.node.getDomainWhitelist(function (result, exception) {
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
				this.rpc.passList=result;
				this.loadPassList();
				
			}.createDelegate(this),0,100,[]);
		}.createDelegate(this));
	},
	
	save: function() {
		this.savePassList();
	}
});
Untangle.Settings.registerClassName('untangle-node-spyware','Untangle.Spyware');
Ext.reg('untangleSpyware', Untangle.Spyware);
}
//</script>