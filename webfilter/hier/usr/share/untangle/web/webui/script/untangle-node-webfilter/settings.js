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
		this.buildBlockLists();
		this.buildPassLists();
		this.buildEventLog();
		//builds the tab panel with the tabs
		this.buildTabPanel([this.panelBlockLists, this.panelPassLists, this.gridEventLog]);
    },
    // Block Lists Panel
    buildBlockLists: function() {
    	this.panelBlockLists = new Ext.Panel({
    		//private fields
			winBlacklistCategories: null,
			winBlockedUrls: null,
			winBlockedExtensions: null,
			winBlockedMimeTypes: null,
			parentId: this.getId(),
    		
		    title: this.i18n._('Block Lists'),
		    layout: "form",
		    bodyStyle:'padding:5px 5px 0',
		    defaults: {
	            xtype:'fieldset',
	            autoHeight:true,
	            buttonAlign: 'left'
		    },
			items: [{
	            title: this.i18n._('Categories'),
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelBlockLists.onManageBlacklistCategories();}.createDelegate(this)
	                }
	            ]
			}, {
	            title: this.i18n._('Sites'),
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelBlockLists.onManageBlockedUrls();}.createDelegate(this)
	                }
	            ]
			}, {
	            title: this.i18n._('File Types'),
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelBlockLists.onManageBlockedExtensions();}.createDelegate(this)
	                }
	            ]
			}, {
	            title: this.i18n._('MIME Types'),
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelBlockLists.onManageBlockedMimeTypes();}.createDelegate(this)
	                }
	            ]
			}],
			
		    onManageBlacklistCategories: function () {
		    	if(!this.winBlacklistCategories) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildBlacklistCategories();
		    		this.winBlacklistCategories=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridBlacklistCategories
		    		});
		    	}
		    	this.winBlacklistCategories.show();
		    },
		    onManageBlockedUrls: function () {
		    	if(!this.winBlockedUrls) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildBlockedUrls();
		    		this.winBlockedUrls=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridBlockedUrls
		    		});
		    	}
		    	this.winBlockedUrls.show();
		    },
		    onManageBlockedExtensions: function () {
		    	if(!this.winBlockedExtensions) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildBlockedExtensions();
		    		this.winBlockedExtensions=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridBlockedExtensions
		    		});
		    	}
		    	this.winBlockedExtensions.show();
		    },
		    onManageBlockedMimeTypes: function () {
		    	if(!this.winBlockedMimeTypes) {
			    	var settingsCmp= Ext.getCmp(this.parentId);
		    		settingsCmp.buildBlockedMimeTypes();
		    		this.winBlockedMimeTypes=new Ung.ManageListWindow({
		    			grid: settingsCmp.gridBlockedMimeTypes
		    		});
		    	}
		    	this.winBlockedMimeTypes.show();
		    },
			beforeDestroy : function(){
		        Ext.destroy(
					this.winBlacklistCategories,
					this.winBlockedUrls,
					this.winBlockedExtensions,
		            this.winBlockedMimeTypes
		        );
		        Ext.Panel.prototype.beforeDestroy.call(this);
		    }
    	});
    },
    // Block Categories
    buildBlacklistCategories: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("block"), dataIndex: 'blockDomains', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("log"), dataIndex: 'logOnly', fixed:true
	    });

    	this.gridBlacklistCategories=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().blacklistCategoriesLength,
    		hasAdd: false,
    		hasDelete: false,
    		title: this.i18n._("Categories"),
    		recordJavaClass: "com.untangle.node.webfilter.BlacklistCategory",
    		proxyRpcFn: this.getRpcNode().getBlacklistCategories,
			fields: [
				{name: 'id'},
				{name: 'displayName'},
				{name: 'blockDomains'},
				{name: 'logOnly'},
				{name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'displayName',header: this.i18n._("category"), width: 200,  dataIndex: 'displayName',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 200,  dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'displayName',
			columnsDefaultSortable: true,
			autoExpandColumn: 'description',
			plugins: [liveColumn,logColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "displayName",
					label: this.i18n._("Category"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "blockDomains",
					label: this.i18n._("Block")
				}),
				new Ext.form.Checkbox({
					name: "logOnly",
					label: this.i18n._("Log")
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
    // Block Sites
    buildBlockedUrls: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("block"), dataIndex: 'live', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("log"), dataIndex: 'log', fixed:true
	    });

    	this.gridBlockedUrls=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().blockedUrlsLength,
    		emptyRow: {"string":"","live":true,"log":true,"description":this.i18n._("[no description]")},
    		title: this.i18n._("Sites"),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getBlockedUrls,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
				{name: 'log'},
				{name: 'description', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'string',header: this.i18n._("site"), width: 200,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 200,  dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'string',
			columnsDefaultSortable: true,
			autoExpandColumn: 'description',
			plugins: [liveColumn,logColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "string",
					label: this.i18n._("Site"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "live",
					label: this.i18n._("Block")
				}),
				new Ext.form.Checkbox({
					name: "log",
					label: this.i18n._("Log")
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
    // Block File Types
    buildBlockedExtensions: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("block"), dataIndex: 'live', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("log"), dataIndex: 'log', fixed:true
	    });

    	this.gridBlockedExtensions=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().blockedExtensionsLength,
    		emptyRow: {"string":"[no extension]","live":true,"log":true,"name":this.i18n._("[no description]")},
    		title: this.i18n._("File Types"),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getBlockedExtensions,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
				{name: 'log'},
				{name: 'name', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'string',header: this.i18n._("file type"), width: 200,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          logColumn,
	          {id:'name',header: this.i18n._("description"), width: 200,  dataIndex: 'name',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'string',
			columnsDefaultSortable: true,
			autoExpandColumn: 'name',
			plugins: [liveColumn,logColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "string",
					label: this.i18n._("File Type"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "live",
					label: this.i18n._("Block")
				}),
				new Ext.form.Checkbox({
					name: "log",
					label: this.i18n._("Log")
				}),
				new Ext.form.TextArea({
					name: "name",
					label: this.i18n._("Description"),
					width: 200,
					height: 60
				})
			]
    	});
    },        
    // Block MIME Types
    buildBlockedMimeTypes: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("block"), dataIndex: 'live', fixed:true
	    });
	    var logColumn = new Ext.grid.CheckColumn({
	       header: this.i18n._("log"), dataIndex: 'log', fixed:true
	    });

    	this.gridBlockedMimeTypes=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().blockedMimeTypesLength,
    		emptyRow: {"mimeType":"[no mime type]","live":true,"log":true,"name":this.i18n._("[no description]")},
    		title: this.i18n._("MIME Types"),
    		recordJavaClass: "com.untangle.uvm.node.MimeTypeRule",
    		proxyRpcFn: this.getRpcNode().getBlockedMimeTypes,
			fields: [
				{name: 'id'},
				{name: 'mimeType'},
				{name: 'live'},
				{name: 'log'},
				{name: 'name', convert: function(v){ return this.i18n._(v)}.createDelegate(this)}
			],
			columns: [
	          {id:'mimeType',header: this.i18n._("MIME type"), width: 200,  dataIndex: 'mimeType',
		          editor: new Ext.form.TextField({allowBlank: false})},
	          liveColumn,
	          logColumn,
	          {id:'name',header: this.i18n._("description"), width: 200,  dataIndex: 'name',
		          editor: new Ext.form.TextField({allowBlank: false})},	          
			],
			sortField: 'mimeType',
			columnsDefaultSortable: true,
			autoExpandColumn: 'name',
			plugins: [liveColumn,logColumn],
			rowEditorInputLines: [
				new Ext.form.TextField({
					name: "mimeType",
					label: this.i18n._("MIME Type"),
					allowBlank: false, 
					width: 200
				}),
				new Ext.form.Checkbox({
					name: "live",
					label: this.i18n._("Block")
				}),
				new Ext.form.Checkbox({
					name: "log",
					label: this.i18n._("Log")
				}),
				new Ext.form.TextArea({
					name: "name",
					label: this.i18n._("Description"),
					width: 200,
					height: 60
				})
			]
    	});
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
		    defaults: {
	            xtype:'fieldset',
	            autoHeight:true,
	            buttonAlign: 'left'
		    },
			items: [{
	            title: this.i18n._('Sites'),
	            buttons :[
	            	{
						text: this.i18n._("manage list"),
						handler: function() {this.panelPassLists.onManagePassedUrls();}.createDelegate(this)
	                }
	            ]
			}, {
	            title: this.i18n._('IP addresses'),
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
    //validation function
    validate: function(callback) {
    	//ipMaddr list must be validated server side
		var ipMaddrList=[];
		var passedClientsSaveList=this.gridPassedClients?this.gridPassedClients.getSaveList():null;
		if(passedClientsSaveList!=null) {
			//added
			for(var i=0;i<passedClientsSaveList[0].list.length;i++) {
				ipMaddrList.push(passedClientsSaveList[0].list[i]["ipMaddr"]);
			}
			//modified
			for(var i=0;i<passedClientsSaveList[2].list.length;i++) {
				ipMaddrList.push(passedClientsSaveList[2].list[i]["ipMaddr"]);
			}
			if(ipMaddrList.length>0) {
				this.getValidator().validate(function (result, exception) {
					if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
					if(!result.valid) {
						this.panelPassLists.onManagePassedClients();
						this.gridPassedClients.focusFirstChangedDataByFieldValue("ipMaddr",result.cause);
						Ext.MessageBox.alert(this.i18n._("Validation failed"),this.i18n._(result.message)+": "+result.cause);
						return;
					} else {
						callback.call(this);
					}
					
				}.createDelegate(this),{list: ipMaddrList,"javaClass":"java.util.ArrayList"});
			}
		}
		if(ipMaddrList.length==0) {
			callback.call(this);
		}
    },
    // save function
	save: function() {
		//validate first
		this.validate( function() {
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
		});
	}
});
}