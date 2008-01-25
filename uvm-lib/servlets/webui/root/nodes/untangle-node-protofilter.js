<script type="text/javascript">
Ext.grid.CheckColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype ={
    init : function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown : function(e, t){
        if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            record.set(this.dataIndex, !record.data[this.dataIndex]);
        }
    },

    renderer : function(value, metadata, record){
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
    init : function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown : function(e, t){
        if(t.className && t.className.indexOf('editRow') != -1){
            e.stopEvent();
	        alert("Edit");
            
            //var index = this.grid.getView().findRowIndex(t);
            //var record = this.grid.store.getAt(index);
            //record.set(this.dataIndex, !record.data[this.dataIndex]);
        }
    },

    renderer : function(value, metadata, record){
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
    init : function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown : function(e, t){
        if(t.className && t.className.indexOf('removeRow') != -1){
            e.stopEvent();
            alert("Delete row");
            //var index = this.grid.getView().findRowIndex(t);
            //var record = this.grid.store.getAt(index);
        }
    },

    renderer : function(value, metadata, record){
        return '<div class="removeRow">&nbsp;</div>';
    }
};

Ext.untangle.ProtocolControlSettings = Ext.extend(Ext.untangle.Settings, {
    storePL:null,
    onRender : function(container, position) {
    	//alert("Render protocol control");
    	//Ext.untangle.ProtocolControlSettings.superclass.onRender.call(this, container, position);
    	var el= document.createElement("div");
	    container.dom.insertBefore(el, position);
        this.el = Ext.get(el);
    	//alert(this.getEl().id);
	    var dataPL = [
			["a1","b1", true, true,"c1","d1"],
			["a2","b2", true, true,"c2","d2"]
	    ];
	    // create the data store
	    this.storePL = new Ext.data.JsonStore({
	        //url:'requests/test.js',
	        url:'protofilter.htm',
	        disableCaching:true,
	        baseParams:{'action':'loadProtocolList','nodeName':this.name,'nodeId':this.tid},
	        root:'data',
	        //data:dataPL,
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
	       header: "<b>block</b>", width: 75, dataIndex: 'blocked', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>log</b>", width: 75, dataIndex: 'log', fixed:true
	    });
	    var editColumn=new Ext.grid.EditColumn({
	    	header: "Edit", width: 60, fixed:true, dataIndex: 'log'
	    });
	    var removeColumn=new Ext.grid.RemoveColumn({
	    	header: "Delete", width: 60, fixed:true, dataIndex: 'log'
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
		
		    // create the Grid
	    var gridPL = new Ext.grid.EditorGridPanel({
	        store: this.storePL,
	        cm: cmPL,
	        tbar:[{
		            text:'Add',
		            tooltip:'Add New Row',
		            iconCls:'add'
		        }],
	        stripeRows: true,
	        plugins:[blockedColumn,logColumn,editColumn,removeColumn],
	        autoExpandColumn: 'category',
	        clicksToEdit:1,
	        title:'Protocol List'
	    });
    	
    	
    	var tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width:690,
	        height:580,
	        activeTab: 0,
	        frame:true,
	        items:[
	            gridPL
	        ]
	    });
    },
    loadPL:function(){
    	this.storePL.load();
    },
    savePL:function() {
    	this.storePL.commitChanges();
    	var recordsPL=this.storePL.getRange();
    	var saveData=[];
    	for(var i=0;i<recordsPL.length;i++) {
    		saveData.push(recordsPL[i].data);
    	}
    	var saveDataJson=Ext.util.JSON.encode(saveData);
    	
		Ext.Ajax.request({
	        url:'protofilter.htm',
	        params:{'action':'saveProtocolList','nodeName':this.name,'nodeId':this.tid,'data':saveDataJson},
			method: 'POST',
			success: function ( result, request) {
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else {
					alert("Succes Save Protocol List");
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});	
    },
	loadData:function() {
		this.loadPL();
	},
	save:function() {
		this.savePL();
	}
});

Ext.untangle.Settings.registerClassName('untangle-node-protofilter','Ext.untangle.ProtocolControlSettings')
Ext.reg('untangleProtocolControlSettings', Ext.untangle.ProtocolControlSettings);
</script>