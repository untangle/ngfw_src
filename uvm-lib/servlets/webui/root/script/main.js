Ext.namespace('Ung');
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
//Global Variables
var main=null; 
var i18n=null;
var rpc=null;

Ung.Main=function() {
}
Ung.Main.prototype = {
	tabs: null,
	library: null,
	myApps: null,
	config: null,
	nodes: null,
	viewport: null,
	initSemaphore: null,
	policySemaphore: null,
	version: null,
	networkingWin: null,
	init: function() {
		main.initSemaphore=6;
		rpc = new Ung.RPC();
		rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
		rpc.jsonrpc.RemoteUvmContext.nodeManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.nodeManager=result;
			main.postinit();
		});
		rpc.jsonrpc.RemoteUvmContext.policyManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.policyManager=result;
			main.postinit();
		});
		rpc.jsonrpc.RemoteUvmContext.toolboxManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.toolboxManager=result;
			main.postinit();
		});
		rpc.jsonrpc.RemoteUvmContext.adminManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.adminManager=result;
			main.postinit();
		});
		rpc.jsonrpc.RemoteUvmContext.version(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.version=result;
			main.postinit();
		});
			
		Ext.Ajax.request({
	        url: "i18n",
			method: 'GET',
			success: function ( result, request) {
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				i18n =new Ung.I18N({"map":jsonResult});
				main.postinit();
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert("Failed", 'Failed loading I18N translations for main rack'); 
			} 
		});
	},
	postinit: function() {
		main.initSemaphore--;
		if(main.initSemaphore!==0) {
			return;
		}
		main.buildTabs();
		main.viewport = new Ext.Viewport({
            layout:'border',
            items:[
                {
                    region:'west',
                    id: 'west',
                    contentEl: 'contentleft',
                    width: 222,
                    border: false
                 },{
                    region:'center',
                    id: 'center',
					contentEl: 'contentright',                    
                    border: false,
                    cls: 'contentright',
                    bodyStyle: 'background-color: transparent;',
                    autoScroll: true
                }
             ]
        });
        Ext.getCmp("west").on("resize", function() {
        	var newSize=Math.max(this.getEl().getHeight()-250,100);
       		main.tabs.setHeight(newSize);
        });
        Ext.getCmp("west").fireEvent("resize");
		var buttonCmp=new Ung.Button({
			'height': '46px',
			'width': '86px',
			'renderTo': 'help',
	        'text': i18n._('Help'),
	        'handler': function() {	  
				var rackBaseHelpLink = main.getHelpLink("rack");
				window.open(rackBaseHelpLink);
				},
	        'imageSrc': 'images/IconHelp36x36.png'
		});
		main.loadTools();
		main.loadPolicies();
	},
	
	loadScript: function(url,callbackFn) {
		Ext.get('scripts_container').load({
			'url':url,
			'text':"loading...",
			'discardUrl':true,
			'callback': callbackFn,
			'scripts':true,
			'nocache':false,
			disableCaching: false
		});
	},
	getHelpLink: function(source,focus) {
		var baseLink="http://www.untangle.com/docs/get.php?";
		if(source) {
			source=source.toLowerCase().replace(" ","_");
		}
		var helpLink=baseLink+"version="+rpc.version+"&source="+source;
		if(focus) {
			focus=focus.toLowerCase().replace(" ","_");
			helpLink+="&focus="+focus;
		}
		return helpLink;
	},
	loadTools: function() {
		this.loadLibrary();
		this.loadMyApps();
		this.loadConfig();
	},
	
	loadLibrary: function() {
		rpc.toolboxManager.uninstalled(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			var uninstalledMD=result;
			if(uninstalledMD===null) {
				main.library=null;
			} else {
				main.library=[];
				for(var i=0;i<uninstalledMD.length;i++) {
					var md=uninstalledMD[i];
					if(md.type=="LIB_ITEM" && md.viewPosition>=0) {
						main.library.push(md);
					}
				}
				main.buildLibrary();
			}
		});
	},
	
	loadMyApps: function() {
		if(main.myApps!==null) {
			for(var i=0;i<main.myApps.length;i++) {
				var cmp=Ext.getCmp('myAppButton_'+this.myApps[i].name);
				if(cmp!==null) {
					cmp.destroy();
					cmp=null;
				}
			}
			main.myApps=null;
		}
		rpc.toolboxManager.installedVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			var installedVisibleMD=result;
			main.myApps=[];
			for(var i=0;i<installedVisibleMD.length;i++) {
				var md=installedVisibleMD[i];
				if(md.type=="NODE" && (md.core || md.security) && md.name!="untangle-node-router") {
					main.myApps.push(md);
				}
			}
			main.buildMyApps();
			main.updateMyAppsButtons();
		});
	},
	
	loadConfig: function() {
		main.config = 
			[{"name":"networking","displayName":"Networking","image":"images/tools/config/IconConfigNetwork36x36.png"},
			{"name":"remoteAdmin","displayName":"Remote Admin","image":"images/tools/config/IconConfigAdmin36x36.png"},
			{"name":"email","displayName":"Email","image":"images/tools/config/IconConfigEmail36x36.png"},
			{"name":"userDirectory","displayName":"User Directory","image":"images/tools/config/IconConfigDirectory36x36.png"},
			{"name":"backupRestore","displayName":"Backup/Restore","image":"images/tools/config/IconConfigBackup36x36.png"},
			{"name":"support","displayName":"Support","image":"images/tools/config/IconConfigSupport36x36.png"},
			{"name":"upgrade","displayName":"Upgrade","image":"images/tools/config/IconConfigUpgrade36x36.png"},
			{"name":"setupInfo","displayName":"Setup Info","image":"images/tools/config/IconConfigSetup36x36.png"}];		
		main.buildConfig();	
	},
	
	loadPolicies: function() {
		rpc.policyManager.getPolicies( function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return; }
			rpc.policies=result;
			main.buildPolicies();
		});
	},
	getNodeMackageDesc: function(Tid) {
		var i;
		if(main.myApps!==null) {
			for(i=0;i<main.myApps.length;i++) {
				if(main.myApps[i].name==Tid.nodeName) {
					return main.myApps[i];
				}
			}
		}
		return null;
	},
	createNode: function (Tid) {
		var node={};
		node.id=node.tid=Tid.id;
		node.Tid=Tid;
		var md=this.getNodeMackageDesc(Tid);
		if(md!==null) {
			node.md=md;
			
			node.name=md.name;
			node.displayName=md.displayName;
			node.viewPosition=md.viewPosition;
			node.rackType=md.rackType;
			node.isService=md.service;
			node.isUtil=md.util;
			node.isSecurity=md.security;
			node.isCore=md.core;
			node.image='image?name='+node.name;
		}
		node.blingers=eval([{'type':'ActivityBlinger','bars':['ACT 1','ACT 2','ACT 3','ACT 4']},{'type':'SystemBlinger'}]);
		return node;
	},
	loadNodes: function() {
		main.policySemaphore=2;
		rpc.nodeManager.nodeInstancesVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message);
				return;
			}
			rpc.policyTids=result.list;
			main.loadNodesCallback();
		}, rpc.currentPolicy);
		rpc.nodeManager.nodeInstancesVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message);
				return;
			}
			rpc.commonTids=result.list;
			main.loadNodesCallback();
		}, null);
	},
	loadNodesCallback: function() {
		main.policySemaphore--;
		if(main.policySemaphore!==0) {
			return;
		}
		Ung.BlingerManager.stop();
		main.destoyNodes();
		rpc.tids=[];
		var i=null;
		for(i=0;i<rpc.policyTids.length;i++) {
			rpc.tids.push(rpc.policyTids[i]);
		}
		for(i=0;i<rpc.commonTids.length;i++) {
			rpc.tids.push(rpc.commonTids[i]);
		}
		main.nodes=[];
		for(i=0;i<rpc.tids.length;i++) {
			if(rpc.tids[i].nodeName=="untangle-node-router") {
				continue;
			}
			var node=this.createNode(rpc.tids[i]);
			main.nodes.push(node);
		}
		for(var i=0;i<main.nodes.length;i++) {
			var node=main.nodes[i];
			this.addNode(node);
		}
		this.updateSeparator();
		this.updateMyAppsButtons();
		this.loadNodesRunStates();
		
	},
	loadNodesRunStates: function() {
		rpc.nodeManager.allNodeStates(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message);
				return;
			}
			var allNodeStates=result;
			for(var i=0;i<main.nodes.length;i++) {
				var nodeCmp=Ung.Node.getCmp(main.nodes[i].tid);
				if(nodeCmp) {
					nodeCmp.updateRunState(allNodeStates.map[main.nodes[i].tid]);
				}
			}
			Ung.BlingerManager.start();
		});
	},
	buildTabs: function () {
		this.tabs = new Ext.TabPanel({
		    renderTo: 'tabs',
		    'activeTab': 0,
		    'height':400,
		    'defaults':{autoScroll: true},
		    'items':[
		        {'contentEl':'tabLibrary', 'title':'Library'},
		        {'contentEl':'tabMyApps', 'title':'My Apps'},
		        {'contentEl':'tabConfig', 'title':'Config'}
		    ]
		});
	},
	
	clickMyApps: function(item) {
		if(item!==null) {
			Ext.getCmp('myAppButton_'+item.name).disable();
			var policy=null;
			if (!item.service && !item.util && !item.core) {
        		policy = rpc.currentPolicy;
        	}
			rpc.nodeManager.instantiate(function (result, exception) {
				if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
				var tid = result;
				rpc.tids.push(tid);
				var node=main.createNode(tid);
				main.nodes.push(node);
				main.addNode(node);
				main.updateSeparator();
			}, item.name, policy);
		}
	},

	clickLibrary: function(item) {
		if(item!==null) {
			Ext.getCmp('libraryButton_'+item.name).disable();
			rpc.toolboxManager.install(function (result, exception) {
				if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
				main.loadMyApps();
				Ext.MessageBox.alert("TODO","Purchase: add to myApps buttons, remove from library");

			}, item.name);
		}
	},
	
	clickConfig: function(item) {
		switch(item.name){
			case "networking":
				rpc.adminManager.generateAuthNonce(function (result, exception) {
					if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
					var alpacaUrl = "/alpaca/?" + result;
					//window.open(url);
					if(main.networkingWin==null) {
					    main.networkingWin=new Ext.Window({
			                id: 'networkingWin',
			                layout:'border',
			                modal:true,
			                title:'Networking',
			                closeAction:'hide',
			                autoCreate:true,          
			                width:740,
			                height:690,
			                draggable:false,
			                resizable:false,
				            items: [{
						        region:"center",
						        html: '<iframe id="networkingWin_iframe" name ="networkingWin_iframe" width="100%" height="100%">',
						        border: false,
						        //autoScroll: true,
						        //cls: 'windowBackground',
						        //bodyStyle: 'background-color: transparent;'
						    	}
						    ]
			            });
						main.networkingWin.render('container');
					};
		        	main.networkingWin.show();
		        	main.networkingWin.setPosition(222,0);
		        	var objSize=main.viewport.getSize();
		        	objSize.width=objSize.width-222;
		        	main.networkingWin.setSize(objSize);
		        	//document.getElementById("networkingWin_iframe").src=alpacaUrl;
		        	window.frames["networkingWin_iframe"].location.href=alpacaUrl;
				});
				break;
			default:
				Ext.MessageBox.alert("Failed","TODO: implement config "+item.name);
				break;
		}
	
		/*
		if(item!==null && item.action!==null) {
			Ext.MessageBox.alert("Failed","TODO: implement config "+item.name);
			var action=item.action;
			if(item.action.url!==null) {
				window.open(item.action.url);
			} else if(item.action.method!==null) {
				eval(item.action.method);
			}
		}
		*/
	},
	
	todo: function() {
		Ext.MessageBox.alert("Failed","TODO: implement this.");
	},
	
	buildLibrary: function() {
  		var out=[];
  		if(this.library!==null) {
	  		for(var i=0;i<this.library.length;i++) {
	  			var item=this.library[i];
	  			var buttonCmp=new Ung.Button({
	  				'id':'libraryButton_'+item.name,
					'libraryIndex':i,
					'height':'50px',
					'renderTo':'toolsLibrary',
					'cls':'toolboxButton',
			        'text': item.displayName,
			        'handler': function() {main.clickLibrary(main.library[this.libraryIndex]);},
			        'imageSrc': 'image?name='+ item.name
		        });
	  		}
	  	}
	},

	buildMyApps: function() {
  		var out=[];
  		for(var i=0;i<this.myApps.length;i++) {
  			var item=this.myApps[i];
  			var buttonCmp=new Ung.Button({
  				'id':'myAppButton_'+item.name,
				'myAppIndex':i,
				'height':'50px',
				'renderTo':'toolsMyApps',
				'cls':'toolboxButton',
		        'text': item.displayName,
		        'handler': function() {main.clickMyApps(main.myApps[this.myAppIndex]);},
		        'imageSrc': 'image?name='+ item.name,
		        'disabled':true
	        });
  		}
	},
	
	buildConfig: function() {
  		var out=[];
  		for(var i=0;i<this.config.length;i++) {
  			var item=this.config[i];
  			var buttonCmp=new Ung.Button({
				'configIndex':i,
				'height':'42px',
				'renderTo':'toolsConfig',
				'cls':'toolboxButton',
		        'text': item.displayName,
		        'handler': function() {main.clickConfig(main.config[this.configIndex]);},
		        'imageSrc': item.image
	        });
  		}
	},
	
	destoyNodes: function () {
		if(this.nodes!==null) {
			for(var i=0;i<this.nodes.length;i++) {
				var node=this.nodes[i];
				var cmp=Ext.getCmp(this.nodes[i].id);
				if(cmp) {
					cmp.destroy();
					cmp=null;
				}
			}
		}
	},
	
	getNodePosition: function(place, viewPosition) {
		var placeEl=document.getElementById(place);
		var position=0;
		if(placeEl.hasChildNodes()) {
			for(var i=0;i<placeEl.childNodes.length;i++) {
				if(placeEl.childNodes[i].getAttribute('viewPosition')-viewPosition<0) {
					position=i+1;
				} else {
					break;
				}
			}
		}
		return position;
	},
	
	addNode: function (node) {
		var nodeWidget=new Ung.Node(node);
		var place=node.isSecurity?'security_nodes':'other_nodes';
		var position=this.getNodePosition(place,node.viewPosition);
		nodeWidget.render(place,position);
		var cmp=Ext.getCmp('myAppButton_'+node.name);
		if(cmp!==null) {
			Ext.getCmp('myAppButton_'+node.name).disable();
		}
	},
	
	updateSeparator: function() {
		var hasUtilOrService=false;
		var hasCore=false;
		for(var i=0;i<this.nodes.length;i++) {
			if(this.nodes[i].isUtil || this.nodes[i].isService) {
				hasUtilOrService=true;
			} else if(this.nodes[i].isCore) {
				hasCore=true;
			}
		}
		document.getElementById("nodes_separator_text").innerHTML=hasUtilOrService?"Services & Utilities":hasCore?"Services":"";
		document.getElementById("nodes_separator").style.display=hasUtilOrService || hasCore?"":"none";
	},
	
	updateMyAppsButtons: function() {
		if(this.myApps!==null && this.nodes!==null) {
			var i=null;
			for(i=0;i<this.myApps.length;i++) {
				Ext.getCmp('myAppButton_'+this.myApps[i].name).enable();
			}
			for(i=0;i<this.nodes.length;i++) {
				Ext.getCmp('myAppButton_'+this.nodes[i].name).disable();
			}
		}
	},
	
	buildPolicies: function () {
		var out=[];
		out.push('<select id="rack_select" onchange="main.changePolicy()">');
		for(var i=0;i<rpc.policies.length;i++) {
			var selVirtualRack=rpc.policies[i]["default"]===true?"selected":"";
			
			if(rpc.policies[i]["default"]===true) {
				rpc.currentPolicy=rpc.policies[i];
			}
			out.push('<option value="'+rpc.policies[i].id+'" '+selVirtualRack+'>'+rpc.policies[i].name+'<\/option>');
		}
		out.push('<\/select>');
		out.push('<div id="rack_policy_button" style="position:absolute;top:15px;left:500px;"><\/div>');
		document.getElementById('rack_list').innerHTML=out.join('');
		var buttonCmp = new Ext.Button({
			'renderTo':'rack_policy_button',
	        'text': 'Show Policy Manager',
	        'handler': function() {Ext.MessageBox.alert("Failed","TODO:Show Policy Manager");}
        });
		this.loadNodes();
	},
	
	changePolicy: function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
			Ext.MessageBox.alert("TODO","Change Virtual Rack");
			this.loadNodes();
		}
	}
};	
