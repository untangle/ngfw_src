//<script type="text/javascript">
if(!Ext.untangle._hasResource["Ext.untangle.ProtocolControlSettings"]) {
Ext.untangle._hasResource["Ext.untangle.ProtocolControlSettings"]=true;

Ext.grid.CheckColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            record.set(this.dataIndex, !record.data[this.dataIndex]);
        }
    },

    renderer: function(value, metadata, record){
        metadata.css += ' x-grid3-check-col-td'; 
        return '<div class="x-grid3-check-col'+(value?'-on':'')+' x-grid3-cc-'+this.id+'">&#160;</div>';
    }
};
Ext.grid.EditColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.EditColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('editRow') != -1){
            e.stopEvent();
           var index = this.grid.getView().findRowIndex(t);
           var record = this.grid.store.getAt(index);
            //populate row editor
           this.grid.rowEditor.populate(record,index);
           this.grid.rowEditor.show();
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="editRow">&nbsp;</div>';
    }
};
Ext.grid.RemoveColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.RemoveColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('removeRow') != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.grid.store.remove(record);
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="removeRow">&nbsp;</div>';
    }
};

Ext.untangle.ProtocolControlSettings = Ext.extend(Ext.untangle.Settings, {
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
    	this.i18n=node_i18n_instances[this.name];
    	this.rpc={};
    	this.rpc.repository={};
    	Ext.untangle.ProtocolControlSettings.instanceId=this.getId();    	
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
    	this.gridEventLog = new Ext.grid.GridPanel({
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
			    		return value?Ext.untangle.ProtocolControlSettings.getI18N()._("blocked"):Ext.untangle.ProtocolControlSettings.getI18N()._("passed");
			    	}
			    },
			    {header: this.i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
			    {header: this.i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
			    {header: this.i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {return value?Ext.untangle.ProtocolControlSettings.getI18N()._("blocked in block list"):Ext.untangle.ProtocolControlSettings.getI18N()._("not blocked in block list");}},
			    {header: this.i18n._("server"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.SServerAddr.hostAddress+":"+value.SServerPort;}}
			],
			//sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
			title: this.i18n._('Event Log'),
			bbar: 
				[{ xtype:'tbtext',
				   text:'<span id="boxReposytoryDescEventLog_'+this.tid+'"></span>'},
				 {xtype:'tbbutton',
		            text:this.i18n._('Refresh'),
		            tooltip:this.i18n._('Refresh'),
		            iconCls:'iconRefresh',
		            parentId:this.getId(),
		            handler: function() {
		            	Ext.getCmp(this.parentId).refreshEventLog();	            	
		            }
		        }],
		    listeners: {
	       		'render': {
	       			fn: function() {
	       				this.rpc.eventManager.getRepositoryDescs(function (result, exception) {
							if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
							var cmp=Ext.untangle.ProtocolControlSettings.getInstanceCmp();
							if(cmp!==null) {
								cmp.rpc.repositoryDescs=result;
								var out=[];
								out.push('<select id="selectReposytoryDescEventLog_'+cmp.tid+'">');
								var repList=cmp.rpc.repositoryDescs.list;
								for(var i=0;i<repList.length;i++) {
									var repDesc=repList[i];
									var selOpt=(i===0)?"selected":"";
									out.push('<option value="'+repDesc.name+'" '+selOpt+'>'+cmp.i18n._(repDesc.name)+'</option>');
								}
								out.push('</select>');
					    		
					    		var boxReposytoryDescEventLog=document.getElementById('boxReposytoryDescEventLog_'+cmp.tid);
					    		boxReposytoryDescEventLog.innerHTML=out.join("");
							}
						});
				    },
				    scope: this
	        	}
	       }
		});
    	
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
    
    getSelectedEventLogRepository: function () {
    	var selObj=document.getElementById('selectReposytoryDescEventLog_'+this.tid);
    	var result=null;
    	if(selObj!==null && selObj.selectedIndex>=0) {
    		result = selObj.options[selObj.selectedIndex].value;
    	}
		return result;
    },
    
    loadPL: function() {
    	this.gridPL.getStore().loadData(this.rpc.settings.patterns.list);
    },
    
    refreshEventLog: function() {
    	var selRepository=this.getSelectedEventLogRepository();
    	if(selRepository!==null) {
    		if(this.rpc.repository[selRepository] === undefined) {
    			this.rpc.repository[selRepository]=this.rpc.eventManager.getRepository(selRepository);
    		}
    		this.rpc.repository[selRepository].getEvents(function (result, exception) {
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
				var events = result;
				var cmp=Ext.untangle.ProtocolControlSettings.getInstanceCmp();
				if(cmp!==null) {
					cmp.gridEventLog.getStore().loadData(events.list);
				}
			});
    	}
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
			var cmp=Ext.untangle.ProtocolControlSettings.getInstanceCmp();
			if(cmp!==null) {
				cmp.tabs.enable();
			}
		}, this.rpc.settings);
    },
    
	loadData: function() {
		this.rpc.node.getProtoFilterSettings(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			var cmp=Ext.untangle.ProtocolControlSettings.getInstanceCmp();
			if(cmp!==null) {
				cmp.rpc.settings=result;
		    	var pl=cmp.rpc.settings.patterns.list;
		    	for(var i=0;i<pl.length;i++) {
		    		var pattern=pl[i];
		    		pattern["category"]=cmp.i18n._(pattern["category"]);
		    		pattern["description"]=cmp.i18n._(pattern["description"]);
		    	}
				
				cmp.loadPL();
			}
		});
		
	},
	
	save: function() {
		this.savePL();
	}
});
Ext.untangle.ProtocolControlSettings.instanceId=null;
Ext.untangle.ProtocolControlSettings.getInstanceCmp =function() {
	var cmp=null;
	if(Ext.untangle.ProtocolControlSettings.instanceId!==null) {
		cmp=Ext.getCmp(Ext.untangle.ProtocolControlSettings.instanceId);
	}
	return cmp;
};
Ext.untangle.ProtocolControlSettings.getI18N= function() {
	return node_i18n_instances['untangle-node-protofilter'];
}
Ext.untangle.Settings.registerClassName('untangle-node-protofilter','Ext.untangle.ProtocolControlSettings');
Ext.reg('untangleProtocolControlSettings', Ext.untangle.ProtocolControlSettings);
}
//</script>