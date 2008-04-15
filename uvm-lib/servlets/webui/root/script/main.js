Ext.namespace('Ung');
//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
//Global Variables
//the main object instance
var main=null; 
//the main internationalization object
var i18n=null;
//the main json rpc object
var rpc=null;


//Main object class
Ung.Main=function() {
}
Ung.Main.prototype = {
	leftTabs: null,
	appsSemaphore: null,
	apps: null,
	libraryApps: null,
	myApps: null,
	config: null,
	nodes: null,
	//the Ext.Viewport object for the application 
	viewport: null,
	initSemaphore: null,
	policySemaphore: null,
	//the application build version
	version: null,
	networkingWin: null,
	iframeWin: null,
	//init function
	init: function() {
		this.initSemaphore=6;
		rpc = {};
		//get JSONRpcClient
		rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
		//get node manager
		rpc.jsonrpc.RemoteUvmContext.nodeManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			rpc.nodeManager=result;
			this.postinit();
		}.createDelegate(this));
		// get policy manager
		rpc.jsonrpc.RemoteUvmContext.policyManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			rpc.policyManager=result;
			this.postinit();
		}.createDelegate(this));
		//get toolbox manager
		rpc.jsonrpc.RemoteUvmContext.toolboxManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			rpc.toolboxManager=result;
			this.postinit();
		}.createDelegate(this));
		// get admin manager
		rpc.jsonrpc.RemoteUvmContext.adminManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			rpc.adminManager=result;
			this.postinit();
		}.createDelegate(this));
		//get version
		rpc.jsonrpc.RemoteUvmContext.version(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			rpc.version=result;
			this.postinit();
		}.createDelegate(this));
		//get i18n
		Ext.Ajax.request({
	        url: "i18n",
			method: 'GET',
			success: function ( result, request) {
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				i18n =new Ung.I18N({"map":jsonResult});
				main.postinit();
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert(i18n._("Failed"), i18n._('Failed loading I18N translations for main rack')); 
			} 
		});
	},
	postinit: function() {
		this.initSemaphore--;
		if(this.initSemaphore!==0) {
			return;
		}
		// initialize viewport object
		var contentLeftArr=[
			'<div id="contentleft">',
				'<div id="logo"><img src="images/logo.png"/></div>',
				'<div id="leftHorizontalRuler"></div>',
				'<div id="leftTabs"></div>',
		 		'<div id="help"></div>',
			'</div>'];
		var contentRightArr=[
			'<div id="contentright">',
				'<div id="racks">',
					'<div id="rack_list"></div>',
					'<div id="rack_nodes">',
						'<div id="security_nodes"></div>',
						'<div id="nodes_separator" style="display:none;"><div id="nodes_separator_text"></div></div>',
						'<div id="other_nodes"></div>',
					'</div>',
				'</div>',
			'</div>'];
		
		this.viewport = new Ext.Viewport({
            layout:'border',
            items:[
                {
                    region:'west',
                    id: 'west',
                    html: contentLeftArr.join(""),
                    width: 220,
                    border: false
                 },{
                    region:'center',
                    id: 'center',
					html: contentRightArr.join(""),                    
                    border: false,
                    cls: 'centerRegion',
                    bodyStyle: 'background-color: transparent;',
                    autoScroll: true
                }
             ]
        });
		this.buildLeftTabs();

        Ext.getCmp("west").on("resize", function() {
        	var newHeight=Math.max(this.getEl().getHeight()-250,100);
       		main.leftTabs.setHeight(newHeight);
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
	        'iconCls': 'iconHelp'
		});
		this.loadTools();
		this.loadPolicies();
	},
	//build left tabs
	buildLeftTabs: function () {
		this.leftTabs = new Ext.TabPanel({
		    renderTo: 'leftTabs',
		    activeTab: 0,
		    height: 400,
		    deferredRender:false,
		    defaults:{autoScroll: true},
		    items:[
		        {title:'Apps',html:'<div id="appsItems"></div>'},
		        {title:'Config', html:'<div id="configItems"></div>'}
		    ]
		});
	},
	/*
	loadScriptExt: function(url,callbackFn) {
		Ext.get('scripts_container').load({
			'url':url,
			'text':i18n._("loading")+"...",
			'discardUrl':true,
			'callback': callbackFn,
			'scripts':true,
			'nocache':false,
			disableCaching: false
		});
	},
	loadScriptSol1: function(sScriptSrc, oCallback) {
		var oHead = document.getElementById('head')[0];
		var oScript = document.createElement('script');
		oScript.type = 'text/javascript';
		oScript.src = sScriptSrc;
		// most browsers
		oScript.onload = oCallback;
		// IE 6 & 7
		oScript.onreadystatechange = function() {
			if (this.readyState == 'complete') {
				oCallback();
			}
		}
		oHead.appendChild(oScript);
	},*/
	//Load script file dynamically 
	loadScript: function(sScriptSrc, oCallback) {
		var error=null;
		try {
			if(window.XMLHttpRequest)
				var req = new XMLHttpRequest();
			else
				var req = new ActiveXObject("Microsoft.XMLHTTP");
			req.open("GET",sScriptSrc,false);
			req.send(null);
			if( window.execScript)
				window.execScript(req.responseText);
			else
				window.eval(req.responseText);
		} catch (e) {
			error=e;	
		}
		if(oCallback) {
			oCallback.call(this);
		}
		return error;
	},
	//get help link
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
		this.loadApps();
		this.loadConfig();
	},
	buildApps: function() {
		this.appsSemaphore--;
		if(this.appsSemaphore!==0) {
			return;
		}
		this.apps=this.libraryApps.concat(this.myApps);
		var appsCmps=[];
		for(var i=0;i<this.apps.length;i++) {
			var item=this.apps[i];
  			 appsCmps.push(new Ung.AppItem({
				item: item,
				renderTo:'appsItems'
	        }));
		}
		Ung.MessageClientThread.run();
	},
	loadApps: function() {
		this.appsSemaphore=2;
		if(main.apps!=null) {
			for(var i=0; i<main.apps.length; i++) {
				var appItemCmp=Ext.getCmp('appItem_'+main.apps[i].name);
				if(appItemCmp!=null) {
					Ext.destroy(appItemCmp);
				}
			}
		}
		rpc.toolboxManager.uninstalled(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			var uninstalledMD=result;
			this.libraryApps=[];
			for(var i=0;i<uninstalledMD.length;i++) {
				var md=uninstalledMD[i];
				if(md.type=="NODE" && md.viewPosition>=0) {
					this.libraryApps.push(md);
				}
			}
			this.buildApps();
		}.createDelegate(this));
		
		rpc.toolboxManager.installedVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			var installedVisibleMD=result;
			this.myApps=[];
			for(var i=0;i<installedVisibleMD.length;i++) {
				var md=installedVisibleMD[i];
				if(md.type=="NODE" && (md.core || md.security) && md.name!="untangle-node-router") {
					this.myApps.push(md);
				}
			}
			this.buildApps();
			this.updateMyAppsButtons();
		}.createDelegate(this));
	},
	
	loadConfig: function() {
		this.config = 
			[{"name":"networking","displayName":i18n._("Networking"),"iconCls":"iconConfigNetwork"},
			{"name":"remoteAdmin","displayName":i18n._("Remote Admin"),"iconCls":"iconConfigAdmin"},
			{"name":"email","displayName":i18n._("Email"),"iconCls":"iconConfigEmail"},
			{"name":"userDirectory","displayName":i18n._("User Directory"),"iconCls":"iconConfigDirectory"},
			{"name":"backupRestore","displayName":i18n._("Backup/Restore"),"iconCls":"iconConfigBackup"},
			{"name":"support","displayName":i18n._("Support"),"iconCls":"iconConfigSupport"},
			{"name":"upgrade","displayName":i18n._("Upgrade"),"iconCls":"iconConfigUpgrade"},
			{"name":"setupInfo","displayName":i18n._("Setup Info"),"iconCls":"iconConfigSetup"}];		
		this.buildConfig();	
	},
	//load policies list
	loadPolicies: function() {
		rpc.policyManager.getPolicies( function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return; }
			rpc.policies=result;
			this.buildPolicies();
		}.createDelegate(this));
	},
	getNodeMackageDesc: function(Tid) {
		var i;
		if(this.myApps!==null) {
			for(i=0;i<this.myApps.length;i++) {
				if(this.myApps[i].name==Tid.nodeName) {
					return this.myApps[i];
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
		node.blingers=eval([{'type':'ActivityBlinger','bars':['ACTIVITY 1','ACTIVITY 2','ACTIVITY 3','ACTIVITY 4']},{'type':'SystemBlinger'}]);
		return node;
	},
	//load the list of nodes for the current policy
	loadNodes: function() {
		this.policySemaphore=2;
		rpc.nodeManager.nodeInstancesVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
				return;
			}
			rpc.policyTids=result.list;
			this.loadNodesCallback();
		}.createDelegate(this), rpc.currentPolicy);
		rpc.nodeManager.nodeInstancesVisible(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
				return;
			}
			rpc.commonTids=result.list;
			this.loadNodesCallback();
		}.createDelegate(this), null);
	},
	loadNodesCallback: function() {
		this.policySemaphore--;
		if(this.policySemaphore!==0) {
			return;
		}
		Ung.BlingerManager.stop();
		this.destoyNodes();
		rpc.tids=[];
		var i=null;
		for(i=0;i<rpc.policyTids.length;i++) {
			rpc.tids.push(rpc.policyTids[i]);
		}
		for(i=0;i<rpc.commonTids.length;i++) {
			rpc.tids.push(rpc.commonTids[i]);
		}
		this.nodes=[];
		for(i=0;i<rpc.tids.length;i++) {
			if(rpc.tids[i].nodeName=="untangle-node-router") {
				continue;
			}
			var node=this.createNode(rpc.tids[i]);
			this.nodes.push(node);
		}
		for(var i=0;i<this.nodes.length;i++) {
			var node=this.nodes[i];
			this.addNode(node);
		}
		this.updateSeparator();
		this.updateMyAppsButtons();
		this.loadNodesRunStates();
	},
	//load run states for all Nodes
	loadNodesRunStates: function() {
		rpc.nodeManager.allNodeStates(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message);
				return;
			}
			var allNodeStates=result;
			for(var i=0;i<this.nodes.length;i++) {
				var nodeCmp=Ung.Node.getCmp(this.nodes[i].tid);
				if(nodeCmp) {
					nodeCmp.updateRunState(allNodeStates.map[this.nodes[i].tid]);
				}
			}
			Ung.BlingerManager.start();
		}.createDelegate(this));
	},
	
	installNode: function(item) {
		if(item!==null) {
			var appItemCmp=Ext.getCmp('appItem_'+item.name);
			if(appItemCmp) {
				appItemCmp.hide();
			}
			var policy=null;
			if (!item.service && !item.util && !item.core) {
        		policy = rpc.currentPolicy;
        	}
			rpc.nodeManager.instantiate(function (result, exception) {
				if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
				var tid = result;
				rpc.tids.push(tid);
				var node=this.createNode(tid);
				this.nodes.push(node);
				this.addNode(node);
				this.updateSeparator();
			}.createDelegate(this), item.name, policy);
		}
	},
/*
	clickLibrary: function(item) {
		if(item!==null) {
			Ext.getCmp('libraryButton_'+item.name).disable();
			rpc.toolboxManager.install(function (result, exception) {
				if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
				this.loadMyApps();
				Ext.MessageBox.alert("TODO","Purchase: add to myApps buttons, remove from library");

			}.createDelegate(this), item.name);
		}
	},
	*/
	getIframeWin: function() {
		if(this.iframeWin==null) {
			this.iframeWin=new Ung.Window({
                id: 'iframeWin',
                title:'',
                layout: 'fit',
	            items: {
			        html: '<iframe id="iframeWin_iframe" name="iframeWin_iframe" width="100%" height="100%" />'
		    	}
				
			});
			window.frames["iframeWin_iframe"].location.href="about:blank";
			this.iframeWin.render();
		}
		return this.iframeWin;
	},
	clickConfig: function(item) {
		switch(item.name){
			case "networking":
				rpc.adminManager.generateAuthNonce(function (result, exception) {
					if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
					var alpacaUrl = "/alpaca/?" + result;
					//window.open(url);
					if(this.networkingWin==null) {
					    this.networkingWin=new Ext.Window({
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
						        border: false
						    	}
						    ]
			            });
						this.networkingWin.render('container');
					};
		        	this.networkingWin.show();
		        	this.networkingWin.setPosition(220,0);
		        	var objSize=this.viewport.getSize();
		        	objSize.width=objSize.width-220;
		        	this.networkingWin.setSize(objSize);
		        	//document.getElementById("networkingWin_iframe").src=alpacaUrl;
		        	window.frames["networkingWin_iframe"].location.href=alpacaUrl;
				}.createDelegate(this));
				break;
			default:
				Ext.MessageBox.alert(i18n._("Failed"),"TODO: implement config "+item.name);
				break;
		}
	},
	
	todo: function() {
		Ext.MessageBox.alert(i18n._("Failed"),"TODO: implement this.");
	},
	/*
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
			        'iconSrc': 'image?name='+ item.name
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
		        'text': i18n._(item.displayName),
		        'handler': function() {main.clickMyApps(main.myApps[this.myAppIndex]);},
		        'iconSrc': 'image?name='+ item.name,
		        'disabled':true
	        });
  		}
	},
	*/
	buildConfig: function() {
  		var out=[];
  		for(var i=0;i<this.config.length;i++) {
  			var item=this.config[i];
  			var buttonCmp=new Ung.Button({
				configIndex: i,
				height: '42px',
				renderTo: 'configItems',
				cls:'toolboxButton',
		        text: item.displayName,
		        handler: function() {main.clickConfig(main.config[this.configIndex]);},
		        iconCls: item.iconCls
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
		var cmp=Ext.getCmp('appItem_'+node.name);
		if(cmp!=null) {
			cmp.hide();
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
		document.getElementById("nodes_separator_text").innerHTML=hasUtilOrService?i18n._("Services & Utilities"):hasCore?i18n._("Services"):"";
		document.getElementById("nodes_separator").style.display=hasUtilOrService || hasCore?"":"none";
		if(hasUtilOrService || hasCore) {
			document.getElementById("racks").style.backgroundPosition="0px 100px";
		} else {
			document.getElementById("racks").style.backgroundPosition="0px 50px";
			
		}
	},
	
	updateMyAppsButtons: function() {
		if(this.myApps!==null && this.nodes!==null) {
			var i=null;
			for(i=0;i<this.myApps.length;i++) {
				Ext.getCmp('appItem_'+this.myApps[i].name).show();
			}
			for(i=0;i<this.nodes.length;i++) {
				Ext.getCmp('appItem_'+this.nodes[i].name).hide();
			}
		}
	},
	//build policies select box
	buildPolicies: function () {
		var out=[];
		out.push('<select id="rack_select" onchange="main.changePolicy()">');
		for(var i=0;i<rpc.policies.length;i++) {
			var selVirtualRack=rpc.policies[i]["default"]===true?"selected":"";
			
			if(rpc.policies[i]["default"]===true) {
				rpc.currentPolicy=rpc.policies[i];
			}
			out.push('<option value="'+rpc.policies[i].id+'" '+selVirtualRack+'>'+i18n._(rpc.policies[i].name)+'<\/option>');
		}
		out.push('<\/select>');
		out.push('<div id="rack_policy_button" style="position:absolute;top:15px;left:500px;"><\/div>');
		document.getElementById('rack_list').innerHTML=out.join('');
		var buttonCmp = new Ext.Button({
			'renderTo':'rack_policy_button',
	        'text': i18n._('Show Policy Manager'),
	        'handler': function() {Ext.MessageBox.alert(i18n._("Failed"),"TODO:Show Policy Manager");}
        });
		this.loadNodes();
	},
	// change current policy
	changePolicy: function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
			Ext.MessageBox.alert("TODO","Change Virtual Rack");
			this.loadNodes();
		}
	}
};	
