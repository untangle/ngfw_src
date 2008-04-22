if(!Ung.hasResource["Ung.WebFilter"]) {
Ung.hasResource["Ung.WebFilter"]=true;
Ung.Settings.registerClassName('untangle-node-webfilter','Ung.WebFilter');

Ung.WebFilter = Ext.extend(Ung.Settings, {
    gridExceptions: null,
    gridEventLog: null,
     //called when the component is rendered
    onRender: function(container, position) {
    	//call superclass renderer first
    	Ung.WebFilter.superclass.onRender.call(this,container, position);
		//builds the 3 tabs
		this.buildPassLists();
		this.buildEventLog();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.panelPassLists, this.gridEventLog]);
    },
    // Pass Lists Panel
    buildPassLists: function() {
    	this.panelPassLists = new Ext.Panel({
    		//private fields
			winPassedUrls: null,
			winPassedClients: null,
			parentId: this.getId(),
    		
		    title: this.i18n._('Pass Lists'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0',
			items: [{
	            xtype:'fieldset',
	            title: this.i18n._('Sites'),
	            autoHeight:true,
	            buttonAlign: 'left',
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelPassLists.onManagePassedUrls();}.createDelegate(this)
	                }
	            ]
			}, {
	            xtype:'fieldset',
	            title: this.i18n._('IP addresses'),
	            autoHeight:true,
	            buttonAlign: 'left',
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelPassLists.onManagePassedClients();}.createDelegate(this)
	                }
	            ]
			}],
			
		    onManagePassedUrls: function () {
		    	if(!this.winPassedUrls) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildPassedUrls();
		    		this.winPassedUrls=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridPassedUrls
		    		});
		    	}
		    	this.winPassedUrls.show();
		    },
		    onManagePassedClients: function () {
		    	if(!this.winPassedClients) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildPassedClients();
		    		this.winPassedClients=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridPassedClients
		    		});
		    	}
		    	this.winPassedClients.show();
		    },
			beforeDestroy : function(){
		        Ext.destroy(
		            this.winPassedUrls,
		            this.winPassedClients
		        );
		        Ext.Panel.prototype.beforeDestroy.call(this);
		    }
    	});
    },
    // Passed Sites
    buildPassedUrls: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("pass"), dataIndex: 'live', fixed:true
	    });

    	this.gridPassedUrls=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().passedUrlsLength,
    		emptyRow: {"string":"","live":true,"description":this.i18n._("[no description]")},
    		title: this.i18n._("Sites"),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getPassedUrls,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
				{name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'string',header: this.i18n._("site"), width: 200,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          {id:'description',header: this.i18n._("description"), width: 200,  dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'string',
			columnsDefaultSortable: true,
			autoExpandColumn: 'description',
			plugins: [liveColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "string",
					label: this.i18n._("Site"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "live",
					label: this.i18n._("Pass")
				}),
				new Ext.form.TextArea({
					name: "description",
					label: this.i18n._("Description"),
					width: 200,
					height: 60
				})
			]
    	});
    },    
    // Passed IP Addresses
    buildPassedClients: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("pass"), dataIndex: 'live', fixed:true
	    });

    	this.gridPassedClients=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().passedClientsLength,
    		emptyRow: {"ipMaddr":"0.0.0.0/32","live":true,"description":this.i18n._("[no description]")},
    		title: this.i18n._("IP addresses"),
    		recordJavaClass: "com.untangle.uvm.node.IPMaddrRule",
    		proxyRpcFn: this.getRpcNode().getPassedClients,
			fields: [
				{name: 'id'},
				{name: 'ipMaddr'},
				{name: 'live'},
				{name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'ipMaddr',header: this.i18n._("IP address/range"), width: 200,  dataIndex: 'ipMaddr',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          {id:'description',header: this.i18n._("description"), width: 200,  dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'ipMaddr',
			columnsDefaultSortable: true,
			autoExpandColumn: 'description',
			plugins: [liveColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "ipMaddr",
					label: this.i18n._("IP address/range"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "live",
					label: this.i18n._("Pass")
				}),
				new Ext.form.TextArea({
					name: "description",
					label: this.i18n._("Description"),
					width: 200,
					height: 60
				})
			]
    	});
    },    
    // Event Log
    buildEventLog: function() {
		this.gridEventLog=new Ung.GridEventLog({
			settingsCmp: this,
			//This is a predefined event log, so there is no need to specify the fields and columns
			predefinedType: "TYPE1"
		});
    },
    // save function
	save: function() {
		//disable tabs during save
		this.tabs.disable();
		this.getRpcNode().updateAll(function (result, exception) {
			//re-enable tabs
			this.tabs.enable();
			if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			//exit settings screen
			this.node.onCancelClick();
		}.createDelegate(this),
				this.getBaseSettings(),
				this.gridPassedClients?this.gridPassedClients.getSaveList():null,
				this.gridPassedUrls?this.gridPassedUrls.getSaveList():null,
				this.gridBlockedUrls?this.gridBlockedUrls.getSaveList():null,
				this.gridBlockedMimeTypes?this.gridBlockedMimeTypes.getSaveList():null,
				this.gridBlockedExtensions?this.gridBlockedExtensions.getSaveList():null,
				this.gridBlacklistCategories?this.gridBlacklistCategories.getSaveList():null);
	}
});
}
