<script type="text/javascript">
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
            //alert("Delete row:"+index+"\n"+record.data);
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="removeRow">&nbsp;</div>';
    }
};

Ext.untangle.ProtocolControlSettings = Ext.extend(Ext.untangle.Settings, {
    storePL: null,
    gridPL: null,
    gridEventLog: null,
    rowEditPL: null,
    onRender: function(container, position) {
    	//alert("Render protocol control");
    	var el= document.createElement("div");
	    container.dom.insertBefore(el, position);
        this.el = Ext.get(el);
    	
	    // create the data store
	    this.storePL = new Ext.data.JsonStore({
	        url: Ext.untangle.ProtocolControlSettings.nodeUrl,
	        disableCaching: true,
	        baseParams: {'action':'loadProtocolList','nodeName':this.name,'nodeId':this.tid},
	        root: 'data',
	        fields: [
	           {name: 'category'},
	           {name: 'protocol'},
	           {name: 'blocked'},
	           {name: 'log'},
	           {name: 'description'},
	           {name: 'signature'},
	        ]
	    });
	    
	    // the column model has information about grid columns
	    // dataIndex maps the column to the specific data field in
	    // the data store (created below)
	    
	    var blockedColumn = new Ext.grid.CheckColumn({
	       header: "<b>block</b>", width: 40, dataIndex: 'blocked', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>log</b>", width: 35, dataIndex: 'log', fixed:true
	    });
	    var editColumn=new Ext.grid.EditColumn({
	    	header: "Edit", width: 35, fixed:true, dataIndex: null
	    });
	    var removeColumn=new Ext.grid.RemoveColumn({
	    	header: "Delete", width: 40, fixed:true, dataIndex: null
	    });
	    var cmPL = new Ext.grid.ColumnModel([
	          {id:'category',header: "category", width: 140,  dataIndex: 'category',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'protocol',header: "protocol", width: 100, dataIndex: 'protocol',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          blockedColumn,
	          logColumn,
	          {id:'description',header: "description", width: 120, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'signature',header: "signature", width: 120, dataIndex: 'signature',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          editColumn,
	          removeColumn
		]);
	
	    // by default columns are sortable
	    cmPL.defaultSortable = false;
		
		var editButtonsTemplate=new Ext.Template(
		'test',
		'<div class="editHelpButton" id="editHelpButton_{grid}">aaa</div>',
		'<div class="editCancelButton" id="editCancelButton_{grid}">bbb</div>',
		'<div class="editUpdateButton" id="editUpdateButton_{grid}">ccc</div>'
		);
		
    	
	    this.rowEditPLWin=new Ext.Window({
               layout:'fit',
               modal:true,
               title:'Edit',
               closeAction:'hide',
               autoCreate:true,                
               width:700,
               height:300,
               draggable:false,
               resizable:false,
               items: [{
		       		html:editButtonsTemplate.applyTemplate({'grid':"PL"+this.tid}),
		       		border: false
		       }]
        });

        this.rowEditPLWin.on('render', function() {
        		//alert(document.getElementById("editCancelButton_PL36"));
		        /*
				new Ext.Button({
					'parentId':this.id,
			        'iconCls': 'cancelIcon',
					'renderTo':'editCancelButton_PL36',
			        'text': 'Cancel',
			        'handler': function() {Ext.getCmp(this.parentId).rowEditor.hide()}
		        });
				new Ext.Button({
					'parentId':this.id,
			        'iconCls': 'saveIcon',
					'renderTo':'editUpdateButton_PL'+this.tid,
			        'text': 'Update',
			        'handler': function() {Ext.getCmp(this.parentId).rowEditor.hide()}
		        });
		        */
        	},this);
        //alert(document.getElementById("editCancelButton_PL36").innerHTML());
		// create the Grid
	    this.gridPL = new Ext.grid.EditorGridPanel({
	        store: this.storePL,
	        cm: cmPL,
	        tbar:[{
		            text:'Add',
		            tooltip:'Add New Row',
		            iconCls:'add',
		            parentId:this.id,
		            handler: function() {
		            	var cmp=Ext.getCmp(this.parentId);
		            	var rec=new Ext.data.Record({"category":"","protocol":"","blocked":false,"log":false,"description":"","signature":""});
						cmp.gridPL.stopEditing();
						cmp.gridPL.getStore().insert(0, [rec]);
						cmp.gridPL.startEditing(0, 0);		            	
		            }
		        }],
	        stripeRows: true,
	        plugins:[blockedColumn,logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit:1,
	        rowEditor:this.rowEditPLWin,
	        title:'Protocol List'
	    });
		
		//var myEl=new Ext.Element('<div id="containerReposytoryDescEventLog_'+this.tid+'">aaa</div>');
		//var repositoryDescsEventLogTB= new Ext.Toolbar.Item(myEl)
		//bbarEventLog.addItem(repositoryDescsEventLogTB);
    	//var bbarEventLog=new Ext.Toolbar(repositoryDescsEventLogTB);
    	this.gridEventLog = new Ext.grid.GridPanel({
			store: new Ext.data.JsonStore({
		        url: Ext.untangle.ProtocolControlSettings.nodeUrl,
		        disableCaching: true,
		        autoLoad: false,
		        baseParams: {'action':'loadEvents','nodeName':this.name,'nodeId':this.tid},
		        root: 'data',
		        fields: [
		           {name: 'timestamp'},
		           {name: 'action'},
		           {name: 'client'},
		           {name: 'request'},
		           {name: 'reason'},
		           {name: 'server'}
		        ]
	    	}),
			columns: [
			    {header: "timestamp", width: 120, sortable: true, dataIndex: 'timestamp'},
			    {header: "action", width: 70, sortable: true, dataIndex: 'action'},
			    {header: "client", width: 120, sortable: true, dataIndex: 'client'},
			    {header: "request", width: 120, sortable: true, dataIndex: 'request'},
			    {header: "reason for action", width: 120, sortable: true, dataIndex: 'reason'},
			    {header: "server", width: 120, sortable: true, dataIndex: 'server'}
			],
			//sm: new Ext.grid.RowSelectionModel({singleSelect:true}),
			title: 'Event Log',
			bbar: //bbarEventLog
				[{xtype:'tbtext', text:'<span id="boxReposytoryDescEventLog_'+this.tid+'></span>'},
				/*{xtype:'combo', id:'boxReposytoryDescEventLog_'+this.tid,
					displayField:'name',
					mode: 'local',
					store: new Ext.data.JsonStore({
				        url: Ext.untangle.ProtocolControlSettings.nodeUrl,
				        disableCaching: true,
				        baseParams: {'action':'loadRepositoryDescs','nodeName':this.name,'nodeId':this.tid},
				        root: 'data',
				        autoLoad: true,
				        fields: [
				           {name: 'name'},
				           {name: 'displayName'}
				        ]
			    	}),		
				 },*/
				 {xtype:'tbbutton',
		            text:'Refresh',
		            tooltip:'Refresh',
		            iconCls:'iconRefresh',
		            parentId:this.id,
		            handler: function() {
		            	Ext.getCmp(this.parentId).refreshEventLog();	            	
		            }
		        }]
		});
    	this.gridEventLog.on("render", function() {
			Ext.Ajax.request({
		        url: Ext.untangle.ProtocolControlSettings.nodeUrl,
		        params: {'action':'loadRepositoryDescs','nodeName':this.name,'nodeId':this.tid},
				disableCaching: true,
				nodeId: this.tid,
				success: function ( result, request) {
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.MessageBox.alert('Failed', jsonResult.msg); 
					} else {
						var out=[];
						out.push('<select id="selectReposytoryDescEventLog_'+request.nodeId+'">');
						for(var i=0;i<jsonResult.data.length;i++) {
							var selOpt=(i==0)?"selected":"";
							
							out.push('<option value="'+jsonResult.data[i].name+'" '+selOpt+'>'+jsonResult.data[i].displayName+'</option>');
						}
						out.push('</select>');
			    		
			    		var boxReposytoryDescEventLog=document.getElementById('boxReposytoryDescEventLog_'+request.nodeId);
			    		boxReposytoryDescEventLog.innerHTML=out.join();
					}
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date);
				} 
			});	
			
    	},this);
    	
    	var tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width: 690,
	        height: 580,
	        activeTab: 0,
	        frame: true,
	        deferredRender: false,
	        items: [
	            this.gridPL,
	            this.gridEventLog
	        ]
	    });
	    
	    ;
    },
    
    getSelectedEventLogRepository: function () {
    	var selObj=document.getElementById('selectReposytoryDescEventLog_'+this.tid);
    	var result=null;
    	if(selObj!=null && selObj.selectedIndex>=0) {
    		result = selObj.options[selObj.selectedIndex].value;
    	}
		return result;
    },
    
    loadPL: function() {
    	this.gridPL.getStore().load();
    	
    },
    
    refreshEventLog: function() {
    	var selRepository=this.getSelectedEventLogRepository();
    	if(selRepository!=null) {
    		this.gridEventLog.getStore().load({params:{'repositoryName':selRepository}});
    	}
    },
    
    savePL: function() {
    	this.gridPL.disable();
    	this.gridPL.getStore().commitChanges();
    	var recordsPL=this.gridPL.getStore().getRange();
    	var saveData=[];
    	for(var i=0;i<recordsPL.length;i++) {
    		saveData.push(recordsPL[i].data);
    	}
    	var saveDataJson=Ext.util.JSON.encode(saveData);
		Ext.Ajax.request({
	        url: Ext.untangle.ProtocolControlSettings.nodeUrl,
	        params: {'action':'saveProtocolList','nodeName':this.name,'nodeId':this.tid,'data':saveDataJson},
			method: 'POST',
			parentId:this.id,
			success: function ( result, request) {
				var cmp=Ext.getCmp(request.parentId);
				cmp.gridPL.enable();
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else {
					//alert("Succes Save Protocol List");
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date);
				var cmp=Ext.getCmp(request.parentId);
				cmp.gridPL.enable(); 
			} 
		});	
    },
    
	loadData: function() {
		this.loadPL();
	},
	
	save: function() {
		this.savePL();
	}
});
Ext.untangle.ProtocolControlSettings.nodeUrl="/protofilter/node.do";
Ext.untangle.Settings.registerClassName('untangle-node-protofilter','Ext.untangle.ProtocolControlSettings')
Ext.reg('untangleProtocolControlSettings', Ext.untangle.ProtocolControlSettings);
}
</script>