<%@ include file="/WEB-INF/jsp/include.jsp" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" >
<head>
    <title>Untangle rack - ext model</title>
    
    <style type="text/css">
        @import "ext-2.0.1/resources/css/ext-all.css";
        @import "untangle.css";
    </style>
<!--
	<script type="text/javascript" src="ext-2.0.1/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext-2.0.1/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all-debug.js"></script>
-->
	<!-- script type="text/javascript" src="ext-2.0.1/adapter/jquery/jquery.js"></script-->
	<script type="text/javascript" src="ext-2.0.1/adapter/ext/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all.js"></script>
    <script type="text/javascript" src="script/graphics.js"></script>
    <script type="text/javascript" src="script/ext-untangle.js"></script>
<script type="text/javascript">
MainPage = {
	tabs: null,
	library: null,
	myApps: null,
	config: null,
	currentRack: null,
	virtuaRacks: null,
	nodes: null,
	rackUrl: "rack.do",
	init: function() {
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
		MainPage.buildTabs();
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
		Ext.Ajax.request({
			url: this.rackUrl,
			params :{'action':'getStoreItems'},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else { 
					MainPage.library=jsonResult.data;
					MainPage.buildLibrary();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});	
	},
	
	loadMyApps: function() {
		Ext.Ajax.request({
			url: this.rackUrl,
			params :{'action':'getToolboxItems'},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else { 
					MainPage.myApps=jsonResult.data;
					MainPage.buildMyApps();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});	
	},
	
	loadConfig: function() {
		Ext.Ajax.request({
			url: this.rackUrl,
			params :{'action':'getConfigItems'},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else { 
					MainPage.config=jsonResult.data;
					MainPage.buildConfig();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});	
	},
	
	loadVirtualRacks: function() {
		Ext.Ajax.request({
			url: this.rackUrl,
			params :{'action':'getVirtualRacks'},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else { 
					MainPage.virtualRacks=jsonResult.data;
					MainPage.buildVirtualRacks();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});				
	},

	loadNodes: function() {
		Ext.untangle.BlingerManager.stop();
		Ext.Ajax.request({
			url: this.rackUrl,
			params :{'action':'getNodes', 'rackName':this.currentRack.name},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else {
					MainPage.destoyNodes();
					MainPage.nodes=jsonResult.data;
					MainPage.buildNodes();
					Ext.untangle.BlingerManager.start();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
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
		if(item!=null) {
			Ext.getCmp('myAppButton_'+item.name).disable();
			Ext.Ajax.request({
				url: this.rackUrl,
				params :{'action':'addToRack','rackName':this.currentRack.name,'installName':item.name},
				method: 'GET',
				success: function ( result, request) { 
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.getCmp('myAppButton_'+item.name).enable();						
						Ext.MessageBox.alert('Failed', jsonResult.msg); 
					} else {
						var node=jsonResult.data;
						MainPage.nodes.push(node);
						MainPage.addNode(node);
						MainPage.updateSeparator();
					}
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
				} 
			});	
		}
	},

	clickLibrary: function(item) {
		if(item!=null) {
			Ext.Ajax.request({
				url: this.rackUrl,
				params :{'action':'purchase','installName':item.name},
				method: 'GET',
				success: function ( result, request) { 
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.MessageBox.alert('Failed', jsonResult.msg); 
					} else { 
						alert("Purchase: TODO: add to myApps buttons, remove from library");
					}
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
				} 
			});	
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
  		for(var i=0;i<this.library.length;i++) {
  			var item=this.library[i];
  			new Ext.untangle.Button({
				'libraryIndex':i,
				'height':'50px',
				'renderTo':'toolsLibrary',
				'cls':'toolboxButton',
		        'text': item.displayName,
		        'handler': function() {MainPage.clickLibrary(MainPage.library[this.libraryIndex])},
		        'imageSrc': item.image
	        });
  			
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
		        'imageSrc': item.image,
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
		Ext.getCmp('myAppButton_'+node.name).disable();
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
		for(var i=0;i<this.myApps.length;i++) {
			Ext.getCmp('myAppButton_'+this.myApps[i].name).enable();
		}
		for(var i=0;i<this.nodes.length;i++) {
			Ext.getCmp('myAppButton_'+this.nodes[i].name).disable();
		}
	},
	
	buildVirtualRacks: function () {
		var out=[];
		out.push('<select id="rack_select" onchange="MainPage.changeVirtualRack()">');
		for(var i=0;i<this.virtualRacks.length;i++) {
			var selVirtualRack=this.virtualRacks[i].isDefault==true?"selected":"";
			if(this.virtualRacks[i].isDefault==true) {
				this.currentRack=this.virtualRacks[i];
			}
			out.push('<option value="'+this.virtualRacks[i].id+'" '+selVirtualRack+'>'+this.virtualRacks[i].name+'</option>');
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
	
	changeVirtualRack: function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			this.currentRack=this.virtualRacks[rack_select.selectedIndex];
			alert("TODO: Change Virtual Rack");
			this.loadNodes();
		}
	}
}	

Ext.onReady(MainPage.init);
</script>
<!-- Just for test -->
<!--  %@ include file="/nodes/untangle-node-protofilter.js" %-->
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
	<div id="test" style="display: none;"></div>
</div>
</body>
</html>
