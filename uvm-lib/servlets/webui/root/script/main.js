Ext.namespace('Ung');
// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// Global Variables
// the main object instance
var main=null; 
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc=null;


// Main object class
Ung.Main=function() {
}
Ung.Main.prototype = {
	disableThreads: false, // in development environment is useful to disable
                            // threads.
	leftTabs: null,
	appsSemaphore: null,
	apps: null,
	libraryApps: null,
	myApps: null,
	config: null,
	nodes: null,
	// the Ext.Viewport object for the application
	viewport: null,
	initSemaphore: null,
	policySemaphore: null,
	contentLeftWidth: 220,
	// the application build version
	version: null,
	iframeWin: null,
	// init function
	init: function() {
		this.initSemaphore=8;
		rpc = {};
		// get JSONRpcClient
		rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");

		// get skin manager
		rpc.jsonrpc.RemoteUvmContext.skinManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.skinManager=result;
			// Load Current Skin
			rpc.skinManager.getSkinSettings(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
				var skinSettings=result;
				this.loadCss("skins/"+skinSettings.administrationClientSkin+"/css/ext-skin.css");
				this.loadCss("skins/"+skinSettings.administrationClientSkin+"/css/skin.css");
				this.postinit();// 1
			}.createDelegate(this));
		}.createDelegate(this));
        // get language manager
        rpc.jsonrpc.RemoteUvmContext.languageManager(function (result, exception) { 
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.languageManager=result;
            // get translations for main module
            rpc.languageManager.getTranslations(function (result, exception) {
                if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
                    i18n =new Ung.I18N({"map":result.map});
                    this.postinit();// 2
                }.createDelegate(this),"main");
        }.createDelegate(this));

		// get node manager
		rpc.jsonrpc.RemoteUvmContext.nodeManager(function (result, exception) { 
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.nodeManager=result;
			this.postinit();// 3
		}.createDelegate(this));
		// get policy manager
		rpc.jsonrpc.RemoteUvmContext.policyManager(function (result, exception) { 
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.policyManager=result;
			this.postinit();// 4
		}.createDelegate(this));
		// get toolbox manager
		rpc.jsonrpc.RemoteUvmContext.toolboxManager(function (result, exception) { 
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.toolboxManager=result;
			this.postinit();// 5
		}.createDelegate(this));
		// get admin manager
		rpc.jsonrpc.RemoteUvmContext.adminManager(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.adminManager=result;
			this.postinit();// 6
		}.createDelegate(this));
		// get version
		rpc.jsonrpc.RemoteUvmContext.version(function (result, exception) {
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
			rpc.version=result;
			this.postinit();// 7
		}.createDelegate(this));
        // get network manager
        rpc.jsonrpc.RemoteUvmContext.networkManager(function (result, exception) {
            if(exception) { Ext.MessageBox.alert("Failed",exception.message); return;}
            rpc.networkManager=result;
            this.postinit();// 8
        }.createDelegate(this));
		
	},
	postinit: function() {
		this.initSemaphore--;
		if(this.initSemaphore!==0) {
			return;
		}
		
		this.initExtI18n();
		
		// initialize viewport object
		var contentLeftArr=[
			'<div id="contentleft">',
				'<div id="logo"><img src="/images/BrandingLogo.gif" border="0"/></div>',
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
                    width: this.contentLeftWidth,
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
		Ext.QuickTips.init();
        
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
	initExtI18n: function(){
		Ext.form.Field.prototype.invalidText=i18n._('The value in this field is invalid');
		Ext.form.TextField.prototype.blankText=i18n._('This field is required');
	},
	unactivateNode: function(mackageDesc) {
		rpc.nodeManager.nodeInstances(function (result, exception) {
				if(exception) { 
					Ext.MessageBox.alert(i18n._("Failed"),exception.message);
					return;
				}
				var tids=result;
				if(tids.length>0) {
					Ext.MessageBox.alert(this.name+" "+i18n._("Warning"), 
					i18n.sprintf(i18n._("$s cannot be removed from the toolbox because it is being used by the following policy rack:<br><b>%s</b><br><br>You must remove the product from all policy racks first."), this.displayName,tids[0]. policy.name));
					return;
				} else {
					Ung.AppItem.updateStateForNode(this.name,"unactivating");
					rpc.toolboxManager.uninstall(function (result, exception) {
						if(exception) {
							Ext.MessageBox.alert(i18n._("Failed"),exception.message);
							return;
						}
						rpc.toolboxManager.unregister(function (result, exception) {
							if(exception) {
								Ext.MessageBox.alert(i18n._("Failed"),exception.message);
								return;
							}
							main.loadApps();
						}.createDelegate(this), this.name);
					}.createDelegate(this), this.name);
				}
		}.createDelegate(mackageDesc), mackageDesc.name);
	},
	// build left tabs
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
	// Load css file Dynamically
	loadCss: function(filename) {
		var fileref=document.createElement("link");
		fileref.setAttribute("rel", "stylesheet");
		fileref.setAttribute("type", "text/css");
		fileref.setAttribute("href", filename);
		document.getElementsByTagName("head")[0].appendChild(fileref);
	},
	// Load script file Dynamically
	loadScript: function(sScriptSrc, fnCallback) {
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
		if(fnCallback) {
			fnCallback.call(this);
		}
		return error;
	},
	// Load a resource if not loaded and execute a callback function
	loadResourceAndExecute: function(resource,sScriptSrc, fnCallback) {
		if(Ung.hasResource[resource]) {
			fnCallback.call(this);
		} else {
			this.loadScript(sScriptSrc, fnCallback);
		}
	},
	// get help link
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
	// build apps
	buildApps: function() {
		this.appsSemaphore--;
		if(this.appsSemaphore!==0) {
			return;
		}
		var apps={};
		// put sore items
		for(var i=0;i<this.libraryApps.length;i++) {
			var item=this.libraryApps[i];
			apps[item.name]={item:item};
		}
		for(var i=0;i<this.myApps.length;i++) {
			var item=this.myApps[i];
  			// if trial item asociate with store item button as trialItem
  			if(item.extraName!=null && item.extraName.indexOf("Trial")!=-1) {
  				var storeLibitemName=item.name.replace("-node-","-libitem-");
  				if(apps[storeLibitemName]) {
  					apps[storeLibitemName].trialItem=item;
  				} else {
  					apps[item.name]={item:item};
  				}
  			} else { // if not traial put separate button
  				apps[item.name]={item:item};
  			}
		}
		
		this.apps=[];
		for(var appItemName in apps) {
			this.apps.push(new Ung.AppItem(apps[appItemName]));
		}
		if(!main.disableThreads) {
			Ung.MessageClientThread.start();
		}
	},
	// load Apps
	loadApps: function() {
		this.appsSemaphore=2;
		Ung.MessageClientThread.stop();
		// destoy current apps components
		if(main.apps!=null) {
			for(var i=0; i<main.apps.length; i++) {
				Ext.destroy(main.apps[i]);
			}
			this.apps=null;
		}
		// get unactivated items (store items)
		rpc.toolboxManager.uninstalled(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			var uninstalledMD=result;
			this.libraryApps=[];
			for(var i=0;i<uninstalledMD.length;i++) {
				var md=uninstalledMD[i];
				if(md.type=="LIB_ITEM" && md.viewPosition>=0 && md.name!="untangle-libitem-router") {
					this.libraryApps.push(md);
				}
			}
			this.buildApps();
		}.createDelegate(this));
		
		// get activated and installed items ( installedVisible)
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
			Ung.AppItem.updateStatesForCurrentPolicy();
			
		}.createDelegate(this));
	},
	
	// load policies list
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
	createNode: function (Tid, md) {
		var node={};
		node.id=node.tid=Tid.id;
		node.Tid=Tid;
		node.md=md;
		node.name=md.name;
		node.displayName=md.displayName;
		node.image='image?name='+node.name;
		node.blingers=eval([{'type':'ActivityBlinger','bars':['ACTIVITY 1','ACTIVITY 2','ACTIVITY 3','ACTIVITY 4']},{'type':'SystemBlinger'}]);
		return node;
	},
	// load the list of nodes for the current policy
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
			var md=this.getNodeMackageDesc(rpc.tids[i]);
			if(md!=null) {
				var node=this.createNode(rpc.tids[i], md);
				this.nodes.push(node);
			}
		}
		for(var i=0;i<this.nodes.length;i++) {
			var node=this.nodes[i];
			this.addNode(node);
		}
		this.updateSeparator();
		Ung.AppItem.updateStatesForCurrentPolicy();
		this.loadNodesRunStates();
	},
	// load run states for all Nodes
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
			if(!main.disableThreads) {
				Ung.BlingerManager.start();
			}
		}.createDelegate(this));
	},
	
	installNode: function(mackageDesc, targetPolicy) {
		if(mackageDesc!==null) {
			Ung.AppItem.updateStateForNode(mackageDesc.name, "installing")
			var policy=null;
			if (!mackageDesc.service && !mackageDesc.util && !mackageDesc.core) {
        		if(targetPolicy==null) {
        			policy = rpc.currentPolicy;
        		} else {
        			policy = targetPolicy;
        		}
        	}
			rpc.nodeManager.instantiate(function (result, exception) {
				if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
				var tid = result;
				rpc.tids.push(tid);
				var md=this.getNodeMackageDesc(tid);
				if(md==null) {
					return;
				}
				var node=this.createNode(tid, md);
				this.nodes.push(node);
				this.addNode(node);
				this.updateSeparator();
			}.createDelegate(this), mackageDesc.name, policy);
		}
	},
	getIframeWin: function() {
		if(this.iframeWin==null) {
			this.iframeWin=new Ung.Window({
                id: 'iframeWin',
                title:'',
                layout: 'fit',
	            items: {
			        html: '<iframe id="iframeWin_iframe" name="iframeWin_iframe" width="100%" height="100%" />'
		    	},
		    	closeAction:'closeActionFn',
		    	closeActionFn: function() {
					this.hide();
					window.frames["iframeWin_iframe"].location.href="about:blank";
				}
				
			});
			this.iframeWin.render();
		}
		return this.iframeWin;
	},
	// load Config
	loadConfig: function() {
		this.config = 
			[{"name":"networking","displayName":i18n._("Networking"),"iconClass":"iconConfigNetwork"},
			{"name":"administration","displayName":i18n._("Administration"),"iconClass":"iconConfigAdmin"},
			{"name":"racks","displayName":i18n._("Racks"),"iconClass":"iconConfigAdmin"},
			{"name":"email","displayName":i18n._("Email"),"iconClass":"iconConfigEmail"},
			{"name":"userDirectory","displayName":i18n._("User Directory"),"iconClass":"iconConfigDirectory"},
			{"name":"upgrade","displayName":i18n._("Upgrade"),"iconClass":"iconConfigUpgrade"},
			{"name":"system","displayName":i18n._("System"),"iconClass":"iconConfigSetup"},
			{"name":"systemInfo","displayName":i18n._("System Info"),"iconClass":"iconConfigSupport"}];		
		this.buildConfig();	
	},
	// build config buttons
	buildConfig: function() {
  		var out=[];
  		for(var i=0;i<this.config.length;i++) {
  			var item=this.config[i];
  			var buttonCmp=new Ung.Button({
  				id: "configItem_"+item.name,
  				info: "configItem_"+item.name,
				configIndex: i,
				height: '42px',
				renderTo: 'configItems',
				cls:'toolboxButton',
		        text: item.displayName,
		        handler: function() {main.clickConfig(main.config[this.configIndex]);},
		        iconCls: item.iconClass
	        });
  		}
	},
	// click Config Button
	clickConfig: function(configItem) {
		switch(configItem.name){
			case "networking":
				rpc.adminManager.generateAuthNonce(function (result, exception) {
					if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
					var alpacaUrl = "/alpaca/?" + result;
					var iframeWin=main.getIframeWin();
					iframeWin.show();
					iframeWin.setTitle("Networking");
					window.frames["iframeWin_iframe"].location.href=alpacaUrl;
				}.createDelegate(this));
				break;
			case "administration":
				main.loadResourceAndExecute("Ung.Administration","script/config/administration.js", function() {
					main.administrationWin=new Ung.Administration(configItem);
					main.administrationWin.show();
				});
				break;
            case "system":
                main.loadResourceAndExecute("Ung.System","script/config/system.js", function() {
                    main.systemWin=new Ung.System(configItem);
                    main.systemWin.show();
                });
                break;
            case "systemInfo":
                main.loadResourceAndExecute("Ung.SystemInfo","script/config/systemInfo.js", function() {
                    main.systemWin=new Ung.SystemInfo(configItem);
                    main.systemWin.show();
                });
                break;
			default:
				Ext.MessageBox.alert(i18n._("Failed"),"TODO: implement config "+configItem.name);
				break;
		}
	},
	
	todo: function() {
		Ext.MessageBox.alert(i18n._("Failed"),"TODO: implement this.");
	},
	
	destoyNodes: function () {
		if(this.nodes!==null) {
			for(var i=0;i<this.nodes.length;i++) {
				var node=this.nodes[i];
				var cmp=Ung.Node.getCmp(this.nodes[i].tid);
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
		var place=node.md.security?'security_nodes':'other_nodes';
		var position=this.getNodePosition(place,node.md.viewPosition);
		nodeWidget.render(place,position);
		Ung.AppItem.updateStateForNode(node.name, "installed");
	},
	// Show - hide Services header in the rack
	updateSeparator: function() {
		var hasUtilOrService=false;
		var hasCore=false;
		for(var i=0;i<this.nodes.length;i++) {
			if(this.nodes[i].md.util || this.nodes[i].md.service) {
				hasUtilOrService=true;
			} else if(this.nodes[i].md.core) {
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
	
	// build policies select box
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
		document.getElementById('rack_list').innerHTML=out.join('');
		this.loadNodes();
	},
	// change current policy
	changePolicy: function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
			this.loadNodes();
		}
	}
};	
