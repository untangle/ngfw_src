//<script type="text/javascript">
if(!Ung.hasResource["Ung.Spyware"]) {
Ung.hasResource["Ung.Spyware"]=true;
Ung.Settings.registerClassName('untangle-node-spyware',"Ung.Spyware");

Ung.Spyware = Ext.extend(Ung.Settings, {
	gridActiveXList: null,
	gridCookiesList: null,
	gridSubnetList: null,
	panelBlockLists: null,
    gridPassList: null,
    gridEventLog: null,
    onRender: function(container, position) {
    	Ung.Spyware.superclass.onRender.call(this,container, position);
		this.buildBlockLists();
		this.buildPassList();
		this.buildEventLog();
		this.buildTabPanel([this.panelBlockLists,this.gridPassList,this.gridEventLog]);
    },
    
    buildBlockLists: function () {
		this.panelBlockLists=new Ext.Panel({
			winCookiesList: null,
			winActiveXList: null,
			winSubnetList: null,
			subCmps: [],
			title: this.i18n._("Block Lists"),
			parentId: this.getId(),
			initComponent: function() {
				var template=new Ext.Template(
'<div style="margin:20px 5px 0px 5px;">Web<hr/><div style="clear: both; margin-top: 10px; height:25px;"><input type="checkbox" id="web_checkbox_{id}" onchange="Ext.getCmp(\''+this.getId()+'\').onChangeSpyware(this)"/> Block Spyware & Ad URLs</div></div>',
'<div style="margin:20px 5px 0px 5px;">Cookies<hr/><div style="clear: both; margin-top: 10px; width:400px;height:25px;"><div style="float:left;width:300px;"><input type="checkbox" id="cookies_checkbox_{id}" onchange="Ext.getCmp(\''+this.getId()+'\').onChangeCookies(this)"/>&nbsp;&nbsp;&nbsp;Block Tracking & Ad Cookies</div><div id="cookies_button_{id}" style="float:right;"></div></div></div>',
'<div style="margin:20px 5px 0px 5px;">ActiveX<hr/><div style="clear: both; margin-top: 10px; width:400px;height:25px;"><div style="float:left;width:300px;"><input type="checkbox" id="activex_checkbox_{id}" onchange="Ext.getCmp(\''+this.getId()+'\').onChangeActiveX(this)"/>&nbsp;&nbsp;&nbsp;Block Malware ActiveX Installs</div><div id="activex_button_{id}" style="float:right;"></div></div></div>',
'<div style="margin:20px 5px 0px 5px;">Traffic<hr/><div style="clear: both; margin-top: 10px; width:400px;height:25px;"><div style="float:left;width:300px;"><input type="checkbox" id="subnet_checkbox_{id}" onchange="Ext.getCmp(\''+this.getId()+'\').onChangeSubnet(this)"/>&nbsp;&nbsp;&nbsp;Monitor Suspicious Traffic</div><div id="subnet_button_{id}" style="float:right;"></div></div></div>',
'<div style="margin:20px 5px 0px 5px; font-size:smaller;">Spyware Blocker signatures were last updated: <span id="last_update_signatures_{id}"></span></div>');
				this.html=template.applyTemplate({id: this.getId()}),
				Ext.Panel.prototype.initComponent.call(this);
			},
			
			afterRender: function() {
				Ext.Panel.prototype.afterRender.call(this);
				
				this.subCmps.push(new Ext.Button({
					'renderTo':'cookies_button_'+this.id,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageCookiesList();}.createDelegate(this)
				}));
				this.subCmps.push(new Ext.Button({
					'renderTo':'activex_button_'+this.id,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageActiveXList();}.createDelegate(this)
				}));
				this.subCmps.push(new Ext.Button({
					'renderTo':'subnet_button_'+this.id,
					'text': i18n._('manage list'),
					'handler': function() {this.onManageSubnetList();}.createDelegate(this)
				}));
				var baseSettings=Ext.getCmp(this.parentId).getBaseSettings();
				document.getElementById("last_update_signatures_"+this.getId()).innerHTML="TODO: need api value";
				document.getElementById("web_checkbox_"+this.getId()).checked=baseSettings.spywareEnabled;
				document.getElementById("cookies_checkbox_"+this.getId()).checked=baseSettings.cookieBlockerEnabled;
				document.getElementById("activex_checkbox_"+this.getId()).checked=baseSettings.blockAllActiveX;
				document.getElementById("subnet_checkbox_"+this.getId()).checked=baseSettings.activeXEnabled;
			},
			onChangeSpyware: function(checkObj) {
				Ext.getCmp(this.parentId).getBaseSettings().spywareEnabled=checkObj.checked;
			},
			onChangeCookies: function(checkObj) {
				Ext.getCmp(this.parentId).getBaseSettings().cookieBlockerEnabled=checkObj.checked;
			},
			onChangeActiveX: function(checkObj) {
				Ext.getCmp(this.parentId).getBaseSettings().blockAllActiveX=checkObj.checked;
			},
			onChangeSubnet: function(checkObj) {
				Ext.getCmp(this.parentId).getBaseSettings().activeXEnabled=checkObj.checked;
			},
		    onManageCookiesList: function () {
		    	var settingsCmp= Ext.getCmp(this.parentId);
		    	if(!this.winCookiesList) {
		    		this.winCookiesList=new Ung.UpdateWindow({
		    			parentId: this.getId(),
		    			listeners: {
		    				'show':{
						        fn: function() {
						        	var panelCmp= Ext.getCmp(this.parentId);
						        	var settingsCmp=Ext.getCmp(panelCmp.parentId);
						        	this.initialChangedData=Ext.encode(settingsCmp.gridCookiesList.changedData);
						        	settingsCmp.gridCookiesList.setHeight(this.getContentHeight());
						        },
						        delay:1
						    }
		    			},
		    			cancelAction: function () {
		    				var panelCmp= Ext.getCmp(this.parentId);
						    var settingsCmp=Ext.getCmp(panelCmp.parentId);
		    				settingsCmp.gridCookiesList.changedData=Ext.decode(this.initialChangedData);
		    				settingsCmp.gridCookiesList.getView().refresh();
		    				this.hide();
		    			},
		    			updateAction: function () {
		    				this.hide();
		    			}
		    		});
			    	if(!settingsCmp.gridCookiesList) {
			    		settingsCmp.buildCookiesList();
			    	}
		    	}
		    	this.winCookiesList.show();
		    },
		    onManageActiveXList: function () {
		    	var settingsCmp= Ext.getCmp(this.parentId);
		    	if(!this.winActiveXList) {
		    		this.winActiveXList=new Ung.UpdateWindow({
		    			parentId: this.getId(),
		    			listeners: {
		    				'show':{
						        fn: function() {
						        	var panelCmp= Ext.getCmp(this.parentId);
						        	var settingsCmp=Ext.getCmp(panelCmp.parentId);
						        	this.initialChangedData=Ext.encode(settingsCmp.gridActiveXList.changedData);
						        	settingsCmp.gridActiveXList.setHeight(this.getContentHeight());
						        },
						        delay:1
						    }
		    			},
		    			cancelAction: function () {
		    				var panelCmp= Ext.getCmp(this.parentId);
						    var settingsCmp=Ext.getCmp(panelCmp.parentId);
		    				settingsCmp.gridActiveXList.changedData=Ext.decode(this.initialChangedData);
		    				settingsCmp.gridActiveXList.getView().refresh();
		    				this.hide();
		    			},
		    			updateAction: function () {
		    				this.hide();
		    			}
		    		});
			    	if(!settingsCmp.gridActiveXList) {
			    		settingsCmp.buildActiveXList();
			    	}
		    	}
		    	this.winActiveXList.show();
		    },
		    onManageSubnetList: function () {
		    	var settingsCmp= Ext.getCmp(this.parentId);
		    	if(!this.winSubnetList) {
		    		this.winSubnetList=new Ung.UpdateWindow({
		    			parentId: this.getId(),
		    			listeners: {
		    				'show':{
						        fn: function() {
						        	var panelCmp= Ext.getCmp(this.parentId);
						        	var settingsCmp=Ext.getCmp(panelCmp.parentId);
						        	this.initialChangedData=Ext.encode(settingsCmp.gridSubnetList.changedData);
						        	settingsCmp.gridSubnetList.setHeight(this.getContentHeight());
						        },
						        delay:1
						    }
		    			},
		    			cancelAction: function () {
		    				var panelCmp= Ext.getCmp(this.parentId);
						    var settingsCmp=Ext.getCmp(panelCmp.parentId);
		    				settingsCmp.gridSubnetList.changedData=Ext.decode(this.initialChangedData);
		    				settingsCmp.gridSubnetList.getView().refresh();
		    				this.hide();
		    			},
		    			updateAction: function () {
		    				this.hide();
		    			}
		    		});
			    	if(!settingsCmp.gridSubnetList) {
			    		settingsCmp.buildSubnetList();
			    	}
		    	}
		    	this.winSubnetList.show();
		    },
			
			beforeDestroy : function(){
		        Ext.destroy(
		            this.winCookiesList,
		            this.winActiveXList,
		            this.winSubnetList
		        );
	        	Ext.each(this.subCmps,Ext.destroy);
		        Ext.Panel.prototype.beforeDestroy.call(this);
		    }
		});
    
    },
    // Cookies List
    buildCookiesList: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", width: 35, dataIndex: 'live', fixed:true
	    });
	    var columns = [
	          {id:'string',header: this.i18n._("identification"), width: 140,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          liveColumn,
		];
	    //columnModel.defaultSortable = true;

    	this.gridCookiesList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().cookieRulesLength,
    		emptyRow: {"string":"","live":true},
    		title: this.i18n._('Cookies List'),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getCookieRules,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
			],
			columns: columns,
			autoExpandColumn: 'string',
			plugins: [liveColumn],
			rowEditorInputLines: [
				{name:"string", label: this.i18n._("Identification"), type:"text", style:"width:200px;"},
				{name:"live", label: this.i18n._("Block"), type:"checkbox"}
			]
    	});
    	this.gridCookiesList.render(this.panelBlockLists.winCookiesList.getContentEl());
    },
    // ActiveX List
    buildActiveXList: function() {
	    var liveColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("block")+"</b>", width: 35, dataIndex: 'live', fixed:true
	    });
	    var columns = [
	          {id:'string',header: this.i18n._("identification"), width: 140,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          liveColumn,
		];
	    //columnModel.defaultSortable = true;

    	this.gridActiveXList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().activeXRulesLength,
    		emptyRow: {"string":"","live":true},
    		title: this.i18n._('ActiveX List'),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getActiveXRules,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
			],
			columns: columns,
			autoExpandColumn: 'string',
			plugins: [liveColumn],
			rowEditorInputLines: [
				{name:"string", label: this.i18n._("Identification"), type:"text", style:"width:200px;"},
				{name:"live", label: this.i18n._("Block"), type:"checkbox"}
			]
    	});
    	this.gridActiveXList.render(this.panelBlockLists.winActiveXList.getContentEl());
    },
    // Subnet List
    buildSubnetList: function() {
	    var logColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("log")+"</b>", width: 35, dataIndex: 'log', fixed:true
	    });
	    var columns = [
	          {id:'ipMaddr',header: this.i18n._("ipMaddr"), width: 140,  dataIndex: 'ipMaddr',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          {id:'category',header: this.i18n._("category"), width: 140,  dataIndex: 'category',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          logColumn,
	          {id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          }
		];
	    //columnModel.defaultSortable = true;

    	this.gridSubnetList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords: this.getBaseSettings().subnetRulesLength,
    		emptyRow: {"ipMaddr":"","category":"","log":false,"description":""},
    		title: this.i18n._('Subnet List'),
    		recordJavaClass: "com.untangle.uvm.node.StringRule",
    		proxyRpcFn: this.getRpcNode().getSubnetRules,
			fields: [
				{name: 'id'},
				{name: 'ipMaddr'},
				{name: 'category'},
				{name: 'log'},
				{name: 'description'}
			],
			columns: columns,
			plugins: [logColumn],
			rowEditorInputLines: [
				{name:"ipMaddr", label: this.i18n._("ipMaddr"), type:"text", style:"width:200px;"},
				{name:"category", label: this.i18n._("Category"), type:"text", style:"width:200px;"},
				{name:"log", label: this.i18n._("Log"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"}
			]
    	});
    	this.gridSubnetList.render(this.panelBlockLists.winSubnetList.getContentEl());
    },
    // Pass List
    buildPassList: function() {
	    var passColumn = new Ext.grid.CheckColumn({
	       header: "<b>"+this.i18n._("pass")+"</b>", width: 35, dataIndex: 'live', fixed:true
	    });
	    var columns = [
	          {id:'site',header: this.i18n._("site"), width: 140,  dataIndex: 'string',
		          editor: new Ext.form.TextField({allowBlank: false})
	          },
	          passColumn,
	          {id:'description',header: this.i18n._("description"), width: 120, dataIndex: 'description',
		          editor: new Ext.form.TextField({allowBlank: false})
	          }
		];
	    //columnModel.defaultSortable = true;

    	this.gridPassList=new Ung.EditorGrid({
    		settingsCmp: this,
    		totalRecords:this.getBaseSettings().domainWhitelistLength,
    		emptyRow: {"string":"","live":false,"description":""},
    		title: this.i18n._('Pass List'),
    		proxyRpcFn: this.getRpcNode().getDomainWhitelist,
			fields: [
				{name: 'id'},
				{name: 'string'},
				{name: 'live'},
				{name: 'description'}
			],
			columns: columns,
			plugins: [passColumn],
			rowEditorInputLines: [
				{name:"string", label: this.i18n._("Site"), type:"text", style:"width:200px;"},
				{name:"live", label: this.i18n._("Pass"), type:"checkbox"},
				{name:"description", label: this.i18n._("Description"), type:"textarea", style:"width:200px;height:60px;"}
			]
    	});
    },
    // Event Log
    buildEventLog: function() {
		// Event Log grid
		this.gridEventLog=new Ung.GridEventLog({
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
	save: function() {
		this.tabs.disable();
		this.getRpcNode().updateAll(function (result, exception) {
			this.tabs.enable();
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			this.node.onCancelClick();
			
		}.createDelegate(this),
			this.getBaseSettings(),
			this.gridCookiesList?this.gridCookiesList.getSaveList():null,
			this.gridActiveXList?this.gridActiveXList.getSaveList():null,
			this.gridSubnetList?this.gridSubnetList.getSaveList():null,
			this.gridPassList.getSaveList() );
	}

});
}
//</script>