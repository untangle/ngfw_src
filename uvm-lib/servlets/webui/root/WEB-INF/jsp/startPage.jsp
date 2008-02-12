<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Untangle rack - Prototype 4</title>
    
    <style type="text/css">
        @import "ext-2.0.1/resources/css/ext-all.css";
        @import "untangle.css";
    </style>
	<script type="text/javascript" src="ext-2.0.1/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext-2.0.1/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all-debug.js"></script>
<!--
	<script type="text/javascript" src="ext-2.0.1/adapter/ext/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all.js"></script>
-->
	<script type="text/javascript" src="jsonrpc/jsonrpc.js"></script>

    <script type="text/javascript" src="script/ext-untangle.js"></script>
    <!-- script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script -->
    <script language="javascript" type="text/javascript"  src="firebug/firebug.js"></script>
    
<script type="text/javascript">
rpc = {}
//TODO: do all rpc requests asyncronous
rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
rpc.nodeManager = rpc.jsonrpc.RemoteUvmContext.nodeManager();
rpc.policyManager=rpc.jsonrpc.RemoteUvmContext.policyManager();
rpc.toolboxManager=rpc.jsonrpc.RemoteUvmContext.toolboxManager();

MainPage = {
	tabs: null,
	library: null,
	myApps: null,
	config: null,
	nodes: null,
	rackUrl: "rack.do",
	viewport: null,
	init: function() {
		MainPage.buildTabs();
		MainPage.viewport = new Ext.Viewport({
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
       		MainPage.tabs.setHeight(newSize);
        });
        Ext.getCmp("west").fireEvent("resize");
        /*  
        MainPage.viewport.on('resize', , MainPage.viewport);
        */
		new Ext.untangle.Button({
			'height': '46px',
			'width': '86px',
			'renderTo': 'help',
	        'text': 'Help',
	        'handler': function() {	  
	        		var rackBaseHelpLink = '<uvm:help source="rack"/>'
	        		window.open(rackBaseHelpLink);
				},
	        'imageSrc': 'images/IconHelp36x36.png'
		});
		MainPage.loadTools();
		MainPage.loadVirtualRacks();
	},
	
	loadScript: function(url,callbackFn) {
		Ext.get('scripts_container').load({
			'url':url,
			'text':"loading...",
			'discardUrl':true,
			'callback': callbackFn,
			'scripts':true,
			'nocache':false
		});
	},
	
	loadTools: function() {
		this.loadLibarary();
		this.loadMyApps();
		this.loadConfig();
	},
	
	loadLibarary: function() {
		rpc.toolboxManager.uninstalled(function (result, exception) {
			if(exception) { alert(exception.message); return;}
			var uninstalledMD=result;
			if(uninstalledMD==null) {
				MainPage.library=null;
			} else {
				MainPage.library=[];
				for(var i=0;i<uninstalledMD.length;i++) {
					var md=uninstalledMD[i];
					if(md.type=="LIB_ITEM" && md.viewPositio>=0) {
						MainPage.library.push(md);
					}
				}
				MainPage.buildLibrary();
			}
		});
	},
	
	loadMyApps: function() {
		if(MainPage.myApps!=null) {
			for(var i=0;i<MainPage.myApps.length;i++) {
				var cmp=Ext.getCmp('myAppButton_'+this.myApps[i].name);
				if(cmp!=null) {
					cmp.destroy()
					cmp=null;
				}
			}
			MainPage.myApps=null;
		}
		rpc.toolboxManager.installedVisible(function (result, exception) {
			if(exception) { alert(exception.message); return;}
			var installedVisibleMD=result;
			MainPage.myApps=[];
			for(var i=0;i<installedVisibleMD.length;i++) {
				var md=installedVisibleMD[i];
				if(md.type=="NODE" && (md.core || md.security)) {
					MainPage.myApps.push(md);
				}
			}
			MainPage.buildMyApps();
			MainPage.updateMyAppsButtons();
		});
	},
	
	loadConfig: function() {
		rpc.toolboxManager.getConfigItems(function (result, exception) {
			if(exception) { alert(exception.message); return;}
			MainPage.config = result;
			MainPage.buildConfig();
		});
	},
	
	loadVirtualRacks: function() {
		rpc.policyManager.getPolicies( function (result, exception) {
			if(exception) { alert(exception.message); return; }
			rpc.policies=result;
			MainPage.buildPolicies();
		});
	},
	createNode: function (Tid) {
		var node={};
		node.nodeContext=rpc.nodeManager.nodeContext(Tid);
		node.nodeContext.tid=Tid;
		node.nodeContext.node=node.nodeContext.node();
		node.nodeContext.node.runState=node.nodeContext.node.getRunState();
		node.nodeContext.nodeDesc=node.nodeContext.getNodeDesc();
		node.nodeContext.mackageDesc=node.nodeContext.getMackageDesc();
		
		//shortcut properties
		node.id=node.tid=node.nodeContext.tid.id;
		node.name=node.nodeContext.nodeDesc.name;
		node.displayName=node.nodeContext.nodeDesc.displayName;
		node.viewPosition=node.nodeContext.mackageDesc.viewPosition;
		node.rackType=node.nodeContext.mackageDesc.rackType;
		node.isService=node.nodeContext.mackageDesc.service;
		node.isUtil=node.nodeContext.mackageDesc.util;
		node.isSecurity=node.nodeContext.mackageDesc.security;
		node.isCore=node.nodeContext.mackageDesc.core;
		node.runState=node.nodeContext.node.runState;

		node.image='image?name='+node.name;
		node.helpLink='';
		node.blingers=eval([{'type':'ActivityBlinger','bars':['ACT 1','ACT 2','ACT 3','ACT 4']},{'type':'SystemBlinger'}]);
		return node;
	},
	loadNodes: function() {
		Ext.untangle.BlingerManager.stop();
		MainPage.destoyNodes();
		rpc.policyTids=rpc.nodeManager.nodeInstancesVisible(rpc.currentPolicy).list;
		rpc.commonTids=rpc.nodeManager.nodeInstancesVisible(null).list;
		rpc.tids=[];
		for(var i=0;i<rpc.policyTids.length;i++) {
			rpc.tids.push(rpc.policyTids[i]);
		}
		for(var i=0;i<rpc.commonTids.length;i++) {
			rpc.tids.push(rpc.commonTids[i]);
		}
		MainPage.nodes=[];
		for(var i=0;i<rpc.tids.length;i++) {
			var node=this.createNode(rpc.tids[i])
			MainPage.nodes.push(node);
		}
		MainPage.buildNodes();
		Ext.untangle.BlingerManager.start();
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
		if(item!=null) {
			Ext.getCmp('myAppButton_'+item.name).disable();
			var policy=null;
			if (!item.service && !item.util && !item.core) {
        		policy = rpc.currentPolicy;
        	}
			rpc.nodeManager.instantiate(function (result, exception) {
				if(exception) { alert(exception.message); return;}
				var tid = result;
				rpc.tids.push(tid);
				var node=MainPage.createNode(tid)
				MainPage.nodes.push(node);
				MainPage.addNode(node);
				MainPage.updateSeparator();
			}, item.name, policy);
		}
	},

	clickLibrary: function(item) {
		if(item!=null) {
			Ext.getCmp('libraryButton_'+item.name).disable();
			rpc.nodeManager.install(function (result, exception) {
				if(exception) { alert(exception.message); return;}
				MainPage.loadMyApps();
				alert("Purchase: TODO: add to myApps buttons, remove from library");

			}, item.name);
		}
	},
	
	clickConfig: function(item) {
		if(item!=null && item.action!=null) {
			alert("TODO: implement config "+item.name);
			/*
			var action=item.action;
			if(item.action.url!=null) {
				window.open(item.action.url);
			} else if(item.action.method!=null) {
				eval(item.action.method);
			}
			*/
		}
	},
	
	todo: function() {
		alert("TODO: implement this.")
	},
	
	buildLibrary: function() {
  		var out=[];
  		if(this.library!=null) {
	  		for(var i=0;i<this.library.length;i++) {
	  			var item=this.library[i];
	  			new Ext.untangle.Button({
	  				'id':'libraryButton_'+item.name,
					'libraryIndex':i,
					'height':'50px',
					'renderTo':'toolsLibrary',
					'cls':'toolboxButton',
			        'text': item.displayName,
			        'handler': function() {MainPage.clickLibrary(MainPage.library[this.libraryIndex])},
			        'imageSrc': item.image
		        });
	  		}
	  	}
	},

	buildMyApps: function() {
  		var out=[];
  		for(var i=0;i<this.myApps.length;i++) {
  			var item=this.myApps[i];
  			new Ext.untangle.Button({
  				'id':'myAppButton_'+item.name,
				'myAppIndex':i,
				'height':'50px',
				'renderTo':'toolsMyApps',
				'cls':'toolboxButton',
		        'text': item.displayName,
		        'handler': function() {MainPage.clickMyApps(MainPage.myApps[this.myAppIndex])},
		        'imageSrc': 'image?name='+ item.name,
		        'disabled':true
	        });
  		}
	},
	
	buildConfig: function() {
  		var out=[];
  		for(var i=0;i<this.config.length;i++) {
  			var item=this.config[i];
  			new Ext.untangle.Button({
				'configIndex':i,
				'height':'42px',
				'renderTo':'toolsConfig',
				'cls':'toolboxButton',
		        'text': item.displayName,
		        'handler': function() {MainPage.clickConfig(MainPage.config[this.configIndex])},
		        'imageSrc': item.image
	        });
  		}
	},
	
	destoyNodes: function () {
		if(this.nodes!=null) {
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
	
	buildNodes: function () {
		var hasServices=false;
		for(var i=0;i<this.nodes.length;i++) {
			var node=this.nodes[i];
			this.addNode(node);
		}
		this.updateSeparator();
		this.updateMyAppsButtons();
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
		var nodeWidget=new Ext.untangle.Node(node);
		var place=node.isSecurity?'security_nodes':'other_nodes';
		var position=this.getNodePosition(place,node.viewPosition);
		nodeWidget.render(place,position);
		var cmp=Ext.getCmp('myAppButton_'+node.name);
		if(cmp!=null) {
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
		if(this.myApps!=null && this.nodes!=null) {
			for(var i=0;i<this.myApps.length;i++) {
				Ext.getCmp('myAppButton_'+this.myApps[i].name).enable();
			}
			
			for(var i=0;i<this.nodes.length;i++) {
				Ext.getCmp('myAppButton_'+this.nodes[i].name).disable();
			}
		}
	},
	
	buildPolicies: function () {
		var out=[];
		out.push('<select id="rack_select" onchange="MainPage.changePolicy()">');
		for(var i=0;i<rpc.policies.length;i++) {
			//rpc.policies[i].isDefault=rpc.policies[i]["default"];
			//delete rpc.policies[i]["default"];
			var selVirtualRack=true;rpc.policies[i]["default"]==true?"selected":"";
			
			if(rpc.policies[i]["default"]==true) {
				rpc.currentPolicy=rpc.policies[i];
			}
			out.push('<option value="'+rpc.policies[i].id+'" '+selVirtualRack+'>'+rpc.policies[i].name+'</option>');
		}
		//out.push('<option value="">Show Policy Manager</option>');
		out.push('</select>');
		out.push('<div id="rack_policy_button" style="position:absolute;top:15px;left:500px;"></div>');
		document.getElementById('rack_list').innerHTML=out.join('');
		new Ext.Button({
			'renderTo':'rack_policy_button',
	        'text': 'Show Policy Manager',
	        'handler': function() {alert("TODO:Show Policy Manager")}
        });
		this.loadNodes();
	},
	
	changePolicy: function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			rpc.currentPolicy=rpc.policies[rack_select.selectedIndex];
			alert("TODO: Change Virtual Rack");
			this.loadNodes();
		}
	}
}	

Ext.onReady(MainPage.init);
</script>
</head>
<body>
<div id="scripts_container" style="display: none;"></div>
<div id="container">
	<div id="contentleft">
		<div id="logo"><img src="images/Logo150x96.gif"/></div>
		<div id="tabs">
		</div>
			<div id="tabLibrary" class="x-hide-display">
			    <div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Learn More</div>
			    <div id="toolsLibrary"></div>
			</div>
			<div id="tabMyApps" class="x-hide-display">
			    <div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Install into Rack</div>
			    <div id="toolsMyApps"></div>
			</div>
			<div id="tabConfig" class="x-hide-display">
               	<div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Configure</div>
               	<div id="toolsConfig"></div>
			</div>
 		<div id="help"></div>
	</div>
	<div id="contentright">
		<div id="racks">
			<div id="leftBorder"></div>
			<div id="rightBorder"></div>
			<div id="rack_list"></div>
			<div id="rack_nodes">
			<div id="security_nodes"></div>
			<div id="nodes_separator" style="display:none;"><div id="nodes_separator_text"></div></div>
			<div id="other_nodes"></div>
			</div>
		</div>
	</div>
	<div id="test" style="display: none;"></div>
</div>
</body>
</html>
