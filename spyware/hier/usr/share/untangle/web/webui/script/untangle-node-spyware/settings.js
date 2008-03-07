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
'<div> Web</div><hr/><div><input type="checkbox" id="web_checkbox_{key}" /> Block Spyware & Ad URLs</div>',
'<div> Cookies</div><hr/><div><input type="checkbox" id="cookies_checkbox_{key}" /> Block Tracking & Ad Cookies <span id="cookies_button_{key}"></span></div>',
'<div> ActiveX</div><hr/><div><input type="checkbox" id="activex_checkbox_{key}" /> Block Malware ActiveX Installs <span id="activex_button_{key}"></span></div>',
'<div> Traffic</div><hr/><div><input type="checkbox" id="traffic_checkbox_{key}" /> Monitor Suspicious Traffic <span id="traffic_button_{key}"></span></div>',
'<div> Spyware Blocker signatures were last updated: <span id="last_update_signatures_{key}"></span></div>');
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
	    var proxy = new Untangle.RpcProxy(this.rpc.node.getCookieRules);
	    var store = new Ext.data.Store({
	        proxy: proxy,
	        reader: new Ext.data.JsonReader({
	        	root: 'list',
	            //id: 'string',
		        fields: [
		           {name: 'string'},
		           {name: 'category'},
		           {name: 'log'},
		           {name: 'description'}
		        ]
			}),
			remoteSort: true
        });

	    // the column model has information about grid columns
	    // dataIndex maps the column to the specific data field in the data store (created below)
	    
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", width: 35, dataIndex: 'log', fixed:true
	    });
	    var editColumn=new Ext.grid.EditColumn({
	    	header: this.i18n._("Edit"), width: 35, fixed:true, sortable: false, dataIndex: null
	    });
	    var removeColumn=new Ext.grid.RemoveColumn({
	    	header: this.i18n._("Delete"), width: 40, fixed:true, sortable: false, dataIndex: null
	    });
	    var columnModel = new Ext.grid.ColumnModel([
	          {id:'string',header: this.i18n._("string"), width: 140,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'category',header: this.i18n._("category"), width: 140,  dataIndex: 'category',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          editColumn,
	          removeColumn
		]);
	
	    columnModel.defaultSortable = true;
		
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
		            	var rec=new Ext.data.Record({"string":"","category":"","log":false,"description":""});
						cmp.gridPassList.getStore().insert(0, [rec]);
						cmp.gridPassList.rowEditor.populate(rec,0);
           				cmp.gridPassList.rowEditor.show();		            	
		            }
			}],
			bbar: new Ext.PagingToolbar({
	            pageSize: 20,
	            store: store,
	            displayInfo: true,
	            displayMsg: 'Displaying topics {0} - {1} of {2}',
	            emptyMsg: "No topics to display"
	        }),    
	        stripeRows: true,
	        plugins:[logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'string',
	        clicksToEdit: 1,
	        title: this.i18n._('Pass List'),
	        enableHdMenu: false
	    });
	    // create the row editor
	    this.gridPassList.rowEditor=new Untangle.RowEditorWindow({
			width: 400,
			height: 300,
			key: "protocolList",
			settingsCmp: this,
			grid: this.gridPassList,
			inputLines: [
				{name:"string", label: this.i18n._("String"), type:"text", style:"width:200px;"},
				{name:"category", label: this.i18n._("Category"), type:"text", style:"width:200px;"},
				{name:"log", label: this.i18n._("Log"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"},
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
    	this.gridPassList.getStore().load(this.rpc.passList);
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
			this.gridPassList.getStore().proxy.setTotalRecords(this.rpc.settings.cookieRulesLength);
			/*
			this.rpc.node.getCookieRules(function (result, exception) {
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
				result.totalCount=100;
				this.rpc.passList=result;
				
				this.loadPassList();
				
			}.createDelegate(this),0,18,[]);
			*/
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