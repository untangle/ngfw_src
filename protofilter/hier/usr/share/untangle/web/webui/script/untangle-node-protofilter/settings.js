//<script type="text/javascript">
if(!Untangle._hasResource["Untangle.Protofilter"]) {
Untangle._hasResource["Untangle.Protofilter"]=true;

Untangle.Protofilter = Ext.extend(Untangle.Settings, {
    i18n: null,
    node:null,
    tabs: null,
    storePL: null,
    gridPL: null,
    gridEventLog: null,
    rowEditPL: null,
    rpc: null,
    onRender: function(container, position) {
    	var el= document.createElement("div");
	    container.dom.insertBefore(el, position);
        this.el = Ext.get(el);
    	this.i18n=Untangle.i18nNodeInstances[this.name];
    	this.rpc={};
    	this.rpc.repository={};
    	Untangle.Protofilter.instanceId=this.getId();    	
    	if(this.node.nodeContext.node.eventManager===undefined) {
			this.node.nodeContext.node.eventManager=this.node.nodeContext.node.getEventManager();
		}
		this.rpc.node = this.node.nodeContext.node;
		this.rpc.eventManager=this.node.nodeContext.node.eventManager;
		
	    // create the data store
	    this.storePL = new Ext.data.JsonStore({
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
	    // dataIndex maps the column to the specific data field in
	    // the data store (created below)
	    
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
	    var cmPL = new Ext.grid.ColumnModel([
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
	    cmPL.defaultSortable = false;
		
		var editPLTemplate=new Ext.Template(
		'<div class="inputLine"><span class="label">'+this.i18n._("Category")+':</span><span class="formw"><input type="text" id="field_category_pl_{tid}" style="width:200px;"/></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Protocol")+':</span><span class="formw"><input type="text" id="field_protocol_pl_{tid}" style="width:200px;"/></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Block")+':</span><span class="formw"><input type="checkbox" id="field_blocked_pl_{tid}" /></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Log")+':</span><span class="formw"><input type="checkbox" id="field_log_pl_{tid}" /></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Description")+':</span><span class="formw"><textarea type="text" id="field_description_pl_{tid}" style="width:200px;height:60px;"></textarea></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Signature")+':</span><span class="formw"><textarea type="text" id="field_definition_pl_{tid}" style="width:200px;height:60px;"></textarea></span></div>');
		var winHTML=editPLTemplate.applyTemplate({'tid':this.tid});
		this.rowEditPLWin=new Ext.Window({
			id: 'rowEditPLWin_'+this.tid,
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
				'parentId':this.getId(),
				'iconCls': 'helpIcon',
				'text': this.i18n._('Help'),
				'handler': function() {Ext.MessageBox.alert("TODO","Implement Help Page");}
			},
			{
				'parentId':this.getId(),
				'iconCls': 'saveIcon',
				'text': this.i18n._('Update'),
				'handler': function() {Ext.getCmp(this.parentId).rowEditPLWin.saveData();}
			},
			{
			'parentId':this.getId(),
			'iconCls': 'cancelIcon',
			'text': this.i18n._('Cancel'),
			'handler': function() {Ext.getCmp(this.parentId).rowEditPLWin.hide();}
			}
			],
			listeners: {
				'show': {
					fn: function() {
						var gridPL=Ext.getCmp(this.parentId).gridPL;
						var objPosition=gridPL.getPosition();
						this.setPosition(objPosition);
						//var objSize=gridPL.getSize();
						//this.setSize(objSize);
					},
					scope: this.rowEditPLWin
				}
			},
			initContent: function() {
					
			},
			populate: function(record,rowIndex) {
				this.rowIndex=rowIndex;
				document.getElementById("field_category_pl_"+this.tid).value=record.data.category;
				document.getElementById("field_protocol_pl_"+this.tid).value=record.data.protocol;
				document.getElementById("field_blocked_pl_"+this.tid).checked=record.data.blocked;
				document.getElementById("field_log_pl_"+this.tid).checked=record.data.log;
				document.getElementById("field_description_pl_"+this.tid).value=record.data.description;
				document.getElementById("field_definition_pl_"+this.tid).value=record.data.definition;
			},
			saveData: function() {
				if(this.rowIndex!==null) {
					var cmp=Ext.getCmp(this.parentId);
					var rec=cmp.gridPL.getStore().getAt(this.rowIndex);
					rec.set("category", document.getElementById("field_category_pl_"+this.tid).value);
					rec.set("protocol", document.getElementById("field_protocol_pl_"+this.tid).value);
					rec.set("blocked", document.getElementById("field_blocked_pl_"+this.tid).checked);
					rec.set("log", document.getElementById("field_log_pl_"+this.tid).checked);
					rec.set("description", document.getElementById("field_description_pl_"+this.tid).value);
					rec.set("definition", document.getElementById("field_definition_pl_"+this.tid).value);
				}
				this.hide();
			}
		});
		this.rowEditPLWin.render('container');
		this.rowEditPLWin.initContent();
		
		// create the Protocol list Grid
	    this.gridPL = new Ext.grid.EditorGridPanel({
	        store: this.storePL,
	        cm: cmPL,
	        tbar:[{
		            text: this.i18n._('Add'),
		            tooltip:this.i18n._('Add New Row'),
		            iconCls:'add',
		            parentId:this.getId(),
		            handler: function() {
		            	var cmp=Ext.getCmp(this.parentId);
		            	var rec=new Ext.data.Record({"category":"","protocol":"","blocked":false,"log":false,"description":"","definition":""});
						cmp.gridPL.getStore().insert(0, [rec]);
						cmp.gridPL.rowEditor.populate(rec,0);
           				cmp.gridPL.rowEditor.show();		            	
		            }
		        }],
	        stripeRows: true,
	        plugins:[blockedColumn,logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit: 1,
	        rowEditor: this.rowEditPLWin,
	        title: this.i18n._('Protocol List')
	    });
		
		
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
			    		return value?Untangle.Protofilter.getI18N()._("blocked"):Untangle.Protofilter.getI18N()._("passed");
			    	}
			    },
			    {header: this.i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
			    {header: this.i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
			    {header: this.i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {return value?Untangle.Protofilter.getI18N()._("blocked in block list"):Untangle.Protofilter.getI18N()._("not blocked in block list");}},
			    {header: this.i18n._("server"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.SServerAddr.hostAddress+":"+value.SServerPort;}}
			]
		})
    	this.tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width: 690,
	        height: 400,
	        activeTab: 0,
	        frame: true,
	        deferredRender: false,
	        items: [
	            this.gridPL,
	            this.gridEventLog
	        ]
	    });
    },
    
    
    loadPL: function() {
    	this.gridPL.getStore().loadData(this.rpc.settings.patterns.list);
    },
    
    
    savePL: function() {
    	this.tabs.disable();
    	this.gridPL.getStore().commitChanges();
    	var recordsPL=this.gridPL.getStore().getRange();
    	var patternsList=[];
    	for(var i=0;i<recordsPL.length;i++) {
    		var pattern=recordsPL[i].data;
    		pattern.javaClass="com.untangle.node.protofilter.ProtoFilterPattern";
    		patternsList.push(pattern);
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
				this.loadPL();
			}
		}.createDelegate(this));
		
	},
	
	save: function() {
		this.savePL();
	}
});
Untangle.Protofilter.instanceId=null;
Untangle.Protofilter.getInstanceCmp =function() {
	var cmp=null;
	if(Untangle.Protofilter.instanceId!==null) {
		cmp=Ext.getCmp(Untangle.Protofilter.instanceId);
	}
	return cmp;
};
Untangle.Protofilter.getI18N= function() {
	return Untangle.i18nNodeInstances['untangle-node-protofilter'];
}
Untangle.Settings.registerClassName('untangle-node-protofilter','Untangle.Protofilter');
Ext.reg('untangleProtofilter', Untangle.Protofilter);
}
//</script>