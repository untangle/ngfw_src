//<script type="text/javascript">
if(!Untangle._hasResource["Untangle.Protofilter"]) {
Untangle._hasResource["Untangle.Protofilter"]=true;

Untangle.Protofilter = Ext.extend(Untangle.Settings, {
    gridProtocolList: null,
    gridEventLog: null,
    onRender: function(container, position) {
    	Untangle.Protofilter.superclass.onRender.call(this,container, position);
		this.buildProtocolList();
		this.buildEventLog();
		this.buldTabPanel([this.gridProtocolList,this.gridEventLog])
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
		
		var contentTemplate=new Ext.Template(
		'<div class="inputLine"><span class="label">'+this.i18n._("Category")+':</span><span class="formw"><input type="text" id="field_category_pl_{tid}" style="width:200px;"/></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Protocol")+':</span><span class="formw"><input type="text" id="field_protocol_pl_{tid}" style="width:200px;"/></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Block")+':</span><span class="formw"><input type="checkbox" id="field_blocked_pl_{tid}" /></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Log")+':</span><span class="formw"><input type="checkbox" id="field_log_pl_{tid}" /></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Description")+':</span><span class="formw"><textarea type="text" id="field_description_pl_{tid}" style="width:200px;height:60px;"></textarea></span></div>',
		'<div class="inputLine"><span class="label">'+this.i18n._("Signature")+':</span><span class="formw"><textarea type="text" id="field_definition_pl_{tid}" style="width:200px;height:60px;"></textarea></span></div>');
		var buttonsTemplate=new Ext.Template(
		'<div class="rowEditorHelp" id="winRowEditProtocolList_help_{tid}"></div>',
		'<div class="rowEditorCancel" id="winRowEditProtocolList_cancel_{tid}"></div>',
		'<div class="rowEditorUpdate" id="winRowEditProtocolList_update_{tid}"></div>');

		var rowEditor=new Ext.Window({
			id: 'winRowEditProtocolList_'+this.tid,
			parentId: this.getId(),
			tid: this.tid,
			rowIndex: null,
			layout:'border',
			modal: true,
			title: this.i18n._('Edit'),
			closeAction: 'hide',
			autoCreate: true,                
			width: 400,
			height: 300,
			draggable: false,
			resizable: false,
			items: [{
				region:"center",
				html: contentTemplate.applyTemplate({'tid':this.tid}),
				border: false,
				autoScroll: true,
				cls: 'windowBackground',
				bodyStyle: 'background-color: transparent;'
			}, 
	    	{
		    	region: "south",
		    	html: buttonsTemplate.applyTemplate({'tid':this.tid}),
		        border: false,
		        height:40,
		        cls: 'windowBackground',
		        bodyStyle: 'background-color: transparent;'
	    	}],
			listeners: {
				'show': {
					fn: function() {
						var grid=this.gridProtocolList;
						var objPosition=grid.getPosition();
						this.gridProtocolList.rowEditor.setPosition(objPosition);
						//var objSize=grid.getSize();
						//this.gridProtocolList.rowEditor.setSize(objSize);
					},
					scope: this
				}
			},
			initContent: function() {
				var parentCmp=Ext.getCmp(this.parentId);
				var cmp=null;
				cmp=new Ext.Button({
					'renderTo':'winRowEditProtocolList_help_'+parentCmp.tid,
					'iconCls': 'helpIcon',
					'text': parentCmp.i18n._('Help'),
					'handler': function() {Ext.MessageBox.alert("TODO","Implement Help Page");}.createDelegate(this)
				});
				cmp=new Ext.Button({
					'renderTo':'winRowEditProtocolList_cancel_'+parentCmp.tid,
					'iconCls': 'cancelIcon',
					'text': parentCmp.i18n._('Cancel'),
					'handler': function() {this.hide();}.createDelegate(this)
				});
				cmp=new Ext.Button({
					'renderTo':'winRowEditProtocolList_update_'+parentCmp.tid,
					'iconCls': 'saveIcon',
					'text': parentCmp.i18n._('Update'),
					'handler': function() {this.updateData();}.createDelegate(this)
				});
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
			updateData: function() {
				if(this.rowIndex!==null) {
					var cmp=Ext.getCmp(this.parentId);
					var rec=cmp.gridProtocolList.getStore().getAt(this.rowIndex);
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
		rowEditor.render('container');
		rowEditor.initContent();
		
		// create the Protocol list Grid
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
	        rowEditor: rowEditor,
	        title: this.i18n._('Protocol List')
	    });
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
			    		return value?Untangle.Protofilter.getI18N()._("blocked"):Untangle.Protofilter.getI18N()._("passed");
			    	}
			    },
			    {header: this.i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
			    {header: this.i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
			    {header: this.i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {return value?Untangle.Protofilter.getI18N()._("blocked in block list"):Untangle.Protofilter.getI18N()._("not blocked in block list");}},
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