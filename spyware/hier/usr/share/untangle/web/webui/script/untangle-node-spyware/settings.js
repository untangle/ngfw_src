//<script type="text/javascript">
if(!Untangle._hasResource["Untangle.Spyware"]) {
Untangle._hasResource["Untangle.Spyware"]=true;

Untangle.Spyware = Ext.extend(Untangle.Settings, {
    i18n: null,
    node:null,
    tabs: null,
    storePassList: null,
    gridPassList: null,
    gridEventLog: null,
    rowEditPassList: null,
    rpc: null,
    onRender: function(container, position) {
    	var el= document.createElement("div");
	    container.dom.insertBefore(el, position);
        this.el = Ext.get(el);
    	this.i18n=Untangle.i18nNodeInstances[this.name];
    	this.rpc={};
    	this.rpc.repository={};
    	Untangle.Spyware.instanceId=this.getId();    	
    	if(this.node.nodeContext.node.eventManager===undefined) {
			this.node.nodeContext.node.eventManager=this.node.nodeContext.node.getEventManager();
		}
		this.rpc.node = this.node.nodeContext.node;
		this.rpc.eventManager=this.node.nodeContext.node.eventManager;
		
		
		//--- Block Lists -------------------
		var panelBlockListsTemplate=new Ext.Template(
'<div>Web</div><hr/><div><input type="checkbox" id="web_checkbox_{id}" /> Block Spyware & Ad URLs<div/>',
'<div>Cookies</div><hr/><div><input type="checkbox" id="cookies_checkbox_{id}" /> Block Tracking & Ad Cookies <div id="cookies_button_{id}"></div><div/>',
'<div>ActiveX</div><hr/><div><input type="checkbox" id="activex_checkbox_{id}" /> Block Malware ActiveX Installs <div id="activex_button_{id}"></div><div/>',
'<div>Traffic</div><hr/><div><input type="checkbox" id="traffic_checkbox_{id}" /> Monitor Suspicious Traffic <div id="traffic_button_{id}"></div><div/>',
'<div>Spyware Blocker signatures were last updated: <div id="last_update_signatures_{id}"></div></div>');
		this.panelBlockLists=new Ext.Panel({
			title:this.i18n._("Block Lists"),
			html: panelBlockListsTemplate.applyTemplate({id:this.getId()})
		});
		var cmp=null;
		/*
		cmp=new Ext.Button({
			'renderTo':'cookies_button_'+this.getId(),
	        'text': i18n._('manage list'),
	        'handler': function() {this.onManageCookiesList();}.createDelegate(this)
        });
		cmp=new Ext.Button({
			'renderTo':'activex_button_'+this.getId(),
	        'text': i18n._('manage list'),
	        'handler': function() {this.onManageActiveXList();}.createDelegate(this)
        });
		cmp=new Ext.Button({
			'renderTo':'traffic_button_'+this.getId(),
	        'text': i18n._('manage list'),
	        'handler': function() {this.onManageTrafficList();}.createDelegate(this)
        });
		*/
		
		
		//--- Pass List ----------------------
	    // create the data store
	    this.storePassList = new Ext.data.JsonStore({
	        fields: [
	           {name: 'site'},
	           {name: 'pass'},
	           {name: 'description'},
	        ]
	    });
	    // the column model has information about grid columns
	    // dataIndex maps the column to the specific data field in
	    // the data store (created below)
	    
	    var passColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("pass")+"</b>", width: 40, dataIndex: 'pass', fixed:true
	    });
	    var editColumn=new Ext.grid.EditColumn({
	    	header: this.i18n._("Edit"), width: 35, fixed:true, dataIndex: null
	    });
	    var removeColumn=new Ext.grid.RemoveColumn({
	    	header: this.i18n._("Delete"), width: 40, fixed:true, dataIndex: null
	    });
	    var cmPassList = new Ext.grid.ColumnModel([
          {id:'site',header: this.i18n._("site"), width: 140,  dataIndex: 'site',
	          editor: new Ext.form.TextField({allowBlank: false})
          },
          passColumn,
          {id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
	          editor: new Ext.form.TextField({allowBlank: false})
          },
          editColumn,
          removeColumn
		]);
	
	    // by default columns are sortable
	    cmPassList.defaultSortable = false;
		//--Editor for Pass List
		var editPassListTemplate=new Ext.Template(
		'<div class="inputLine"><span class="label">'+this.i18n._("Site")+':</span><span class="formw"><input type="text" id="field_site_passlist_{tid}" style="width:200px;"/></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Pass")+':</span><span class="formw"><input type="checkbox" id="field_pass_passlist_{tid}" /></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Description")+':</span><span class="formw"><textarea type="text" id="field_description_passlist_{tid}" style="width:200px;height:60px;"></textarea></span></div>');
		var winHTML=editPassListTemplate.applyTemplate({'tid':this.tid});
		this.rowEditPassListWin=new Ext.Window({
			id: 'rowEditPassListWin_'+this.tid,
			parentId: this.getId(),
			tid: this.tid,
			rowIndex: null,
			layout: 'fit',
			modal: true,
			title: this.i18n._('Edit'),
			closeAction: 'hide',
			autoCreate: true,                
			width: 400,
			height: 300,
			draggable: false,
			resizable: false,
			items: {
				html: winHTML,
				border: false,
				deferredRender:false,
				cls: 'windowBackground',
				bodyStyle: 'background-color: transparent;'
			},
			buttons: [
			{
				'iconCls': 'helpIcon',
				'text': this.i18n._('Help'),
				'handler': function() {Ext.MessageBox.alert("TODO","Implement Help Page");}.createDelegate(this)
			},
			{
				'iconCls': 'cancelIcon',
				'text': this.i18n._('Cancel'),
				'handler': function() {this.rowEditPassListWin.hide();}.createDelegate(this)
			},
			{
				'iconCls': 'saveIcon',
				'text': this.i18n._('Update'),
				'handler': function() {this.rowEditPassListWin.saveData();}.createDelegate(this)
			}
			],
			listeners: {
				'show': {
					fn: function() {
						var grid=Ext.getCmp(this.parentId).gridPassList;
						var objPosition=grid.getPosition();
						this.setPosition(objPosition);
						//var objSize=grid.getSize();
						//this.setSize(objSize);
					},
					scope: this.rowEditPassListWin
				}
			},
			initContent: function() {
					
			},
			populate: function(record,rowIndex) {
				this.rowIndex=rowIndex;
				document.getElementById("field_site_passlist_"+this.tid).value=record.data.site;
				document.getElementById("field_pass_passlist_"+this.tid).checked=record.data.pass;
				document.getElementById("field_description_passlist_"+this.tid).value=record.data.description;
			},
			saveData: function() {
				if(this.rowIndex!==null) {
					var cmp=Ext.getCmp(this.parentId);
					var rec=cmp.gridPassList.getStore().getAt(this.rowIndex);
					rec.set("site", document.getElementById("field_site_passlist_"+this.tid).value);
					rec.set("pass", document.getElementById("field_pass_passlist_"+this.tid).checked);
					rec.set("description", document.getElementById("field_description_passlist_"+this.tid).value);
				}
				this.hide();
			}
		});
		this.rowEditPassListWin.render('container');
		this.rowEditPassListWin.initContent();
		
		// create Pass List grid object
	    this.gridPassList = new Ext.grid.EditorGridPanel({
	        store: this.storePassList,
	        cm: cmPassList,
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
	        stripeRows: true,
	        plugins:[passColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit: 1,
	        rowEditor: this.rowEditPassListWin,
	        title: this.i18n._('Pass List')
	    });
		
		
		//--- Event Log Grid --------------
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
			    		return value?Untangle.Spyware.getI18N()._("blocked"):Untangle.Spyware.getI18N()._("passed");
			    }},
			    {header: this.i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
			    {header: this.i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
			    {header: this.i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {return value?Untangle.Spyware.getI18N()._("blocked in block list"):Untangle.Spyware.getI18N()._("not blocked in block list");}},
			    {header: this.i18n._("server"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.SServerAddr.hostAddress+":"+value.SServerPort;}}
			]
		})
		
		//---- Main tab panel -----------------
    	this.tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width: 690,
	        height: 400,
	        activeTab: 0,
	        frame: true,
	        deferredRender: false,
	        items: [
	            //this.panelBlockLists,
	            //this.gridPassList,
	            this.gridEventLog
	        ]
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
    loadPassList: function() {
    	this.gridPassList.getStore().loadData(this.rpc.settings.patterns.list);
    },
    
    savePassList: function() {
    	this.tabs.disable();
    	this.gridPassList.getStore().commitChanges();
    	var records=this.gridPassList.getStore().getRange();
    	var list=[];
    	for(var i=0;i<records.length;i++) {
    		var rec=records[i].data;
    		rec.javaClass="com.untangle.node.spyware.SpywarePattern";
    		list.push(rec);
    	}
    	this.rpc.settings.patterns.list=patternsList;
    	this.rpc.settings.patterns.javaClass="java.util.ArrayList";
    	this.rpc.node.setProtoFilterSettings(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			if(this!==null) {
				this.tabs.enable();
			}
		}.createDelegate(this), this.rpc.settings);
    },
    
	loadData: function() {
		/*
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
				this.loadPassList();
			}
		}.createDelegate(this));
		*/
	},
	
	save: function() {
		this.savePassList();
	}
});
Untangle.Spyware.instanceId=null;
Untangle.Spyware.getInstanceCmp =function() {
	var cmp=null;
	if(Untangle.Spyware.instanceId!==null) {
		cmp=Ext.getCmp(Untangle.Spyware.instanceId);
	}
	return cmp;
};
Untangle.Spyware.getI18N= function() {
	return Untangle.i18nNodeInstances['untangle-node-spyware'];
}
Untangle.Settings.registerClassName('untangle-node-spyware','Untangle.Spyware');
Ext.reg('untangleSpyware', Untangle.Spyware);
}
//</script>