<%@ include file="/WEB-INF/jsp/include.jsp" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" >
<head>
    <title>Untangle rack - ext model</title>
    
    <style type="text/css">
        @import "ext-2.0/resources/css/ext-all.css";
        @import "untangle.css";
    </style>
<!--
	<script type="text/javascript" src="ext-2.0/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext-2.0/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0/ext-all-debug.js"></script>
-->
	<script type="text/javascript" src="ext-2.0/adapter/ext/ext-base.js"></script>
	<script type="text/javascript" src="ext-2.0/ext-all.js"></script>
    <script type="text/javascript" src="script/graphics.js"></script>
    <script type="text/javascript" src="script/ext-untangle.js"></script>
<script type="text/javascript">
	MainPage = function() {}
	
	MainPage.tabs=null;
	MainPage.library=null;
	MainPage.myApps=null;
	MainPage.config=null;
	MainPage.currentRack=null;
	MainPage.virtuaRacks=null;
	MainPage.nodes=null;
/*	
	MainPage.loadScript=function(url)
	{
	   var e = document.createElement("script");
	   e.src = url;
	   e.type="text/javascript";
	   document.getElementsByTagName("head")[0].appendChild(e);
	}
*/	
	MainPage.loadScript=function(url,scope,callbackFn)
	{
		Ext.get('scripts_container').load({
			'url':url,
			'text':"loading...",
			'discardUrl':true,
			'callback': callbackFn,
			'scripts':true,
			'nocache':false
		});
		/*
		Ext.Ajax.request({
			url:	url,
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
		*/
	}
	
	MainPage.loadTools = function() {
		MainPage.loadLibarary();
		MainPage.loadMyApps();
		MainPage.loadConfig();
	}
	MainPage.loadLibarary = function() {
		Ext.Ajax.request({
			url:	"rack.htm",
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
	}
	MainPage.loadMyApps = function() {
		Ext.Ajax.request({
			url:	"rack.htm",
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
	}
	MainPage.loadConfig = function() {
		Ext.Ajax.request({
			url:	"rack.htm",
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
	}
	MainPage.loadVirtualRacks = function() {
		Ext.Ajax.request({
			url:	"rack.htm",
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
	}

	MainPage.loadNodes = function() {
		Ext.Ajax.request({
			url:	"rack.htm",
			params :{'action':'getNodes', 'rackName':MainPage.currentRack.name},
			method: 'GET',
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else {
					MainPage.destoyNodes();
					MainPage.nodes=jsonResult.data;
					MainPage.buildNodes();
				}
			},
			failure: function ( result, request) { 
				Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
			} 
		});				
	}
	MainPage.buildTabs = function () {
		MainPage.tabs = new Ext.TabPanel({
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
	}
	
	MainPage.clickMyApps = function(item) {
		if(item!=null) {
			Ext.getCmp('myAppButton_'+item.name).disable();
			Ext.Ajax.request({
				url:	"rack.htm",
				params :{'action':'addToRack','rackName':MainPage.currentRack.name,'installName':item.name},
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
	}

	MainPage.clickLibrary = function(item) {
		if(item!=null) {
			Ext.Ajax.request({
				url:	"rack.htm",
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
	}
	MainPage.clickConfig = function(item) {
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
	}
	
	MainPage.todo=function() {
		alert("TODO: implement this.")
	}
	
	MainPage.buildLibrary = function() {
  		var out=[];
  		for(var i=0;i<MainPage.library.length;i++) {
  			var item=MainPage.library[i];
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
	}

	MainPage.buildMyApps = function() {
  		var out=[];
  		for(var i=0;i<MainPage.myApps.length;i++) {
  			var item=MainPage.myApps[i];
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
	}
	
	MainPage.buildConfig = function() {
  		var out=[];
  		for(var i=0;i<MainPage.config.length;i++) {
  			var item=MainPage.config[i];
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
	}
	MainPage.destoyNodes= function () {
		if(MainPage.nodes!=null) {
			for(var i=0;i<MainPage.nodes.length;i++) {
				var node=MainPage.nodes[i];
				var cmp=Ext.getCmp(MainPage.nodes[i].id);
				if(cmp) {
					cmp.destroy();
				}
			}
		}
	}
	MainPage.buildNodes = function () {
		var hasServices=false;
		for(var i=0;i<MainPage.nodes.length;i++) {
			var node=MainPage.nodes[i];
			MainPage.addNode(node);
		}
		MainPage.updateSeparator();
		MainPage.updateMyAppsButtons();
	}
	
	MainPage.getNodePosition=function(place, viewPosition) {
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
	}
	
	MainPage.addNode = function (node) {
		var nodeWidget=new Ext.untangle.Node(node);
		var place=node.isSecurity?'security_nodes':'other_nodes';
		var position=MainPage.getNodePosition(place,node.viewPosition);
		nodeWidget.render(place,position);
		Ext.getCmp('myAppButton_'+node.name).disable();
	}
	MainPage.updateSeparator = function() {
		var hasUtilOrService=false;
		var hasCore=false;
		for(var i=0;i<MainPage.nodes.length;i++) {
			if(MainPage.nodes[i].isUtil || MainPage.nodes[i].isService) {
				hasUtilOrService=true;
			} else if(MainPage.nodes[i].isCore) {
				hasCore=true;
			}
		}
		document.getElementById("nodes_separator_text").innerHTML=hasUtilOrService?"Services & Utilities":hasCore?"Services":"";
		document.getElementById("nodes_separator").style.display=hasUtilOrService || hasCore?"":"none";
	}
	
	MainPage.updateMyAppsButtons = function() {
		for(var i=0;i<MainPage.myApps.length;i++) {
			Ext.getCmp('myAppButton_'+MainPage.myApps[i].name).enable();
		}
		for(var i=0;i<MainPage.nodes.length;i++) {
			Ext.getCmp('myAppButton_'+MainPage.nodes[i].name).disable();
		}
	}
	
	MainPage.buildVirtualRacks = function () {
		var out=[];
		out.push('<select id="rack_select" onchange="MainPage.changeVirtualRack()">');
		for(var i=0;i<MainPage.virtualRacks.length;i++) {
			var selVirtualRack=MainPage.virtualRacks[i].isDefault==true?"selected":"";
			if(MainPage.virtualRacks[i].isDefault==true) {
				MainPage.currentRack=MainPage.virtualRacks[i];
			}
			out.push('<option value="'+MainPage.virtualRacks[i].id+'" '+selVirtualRack+'>'+MainPage.virtualRacks[i].name+'</option>');
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
		MainPage.loadNodes();
	}
	
	MainPage.changeVirtualRack= function () {
		var rack_select=document.getElementById('rack_select');
		if(rack_select.selectedIndex>=0) {
			MainPage.currentRack=MainPage.virtualRacks[rack_select.selectedIndex];
			alert("TODO: Change Virtual Rack");
			MainPage.loadNodes();
		}
	}
	
	function init() {
		new Ext.untangle.Button({
			'height':'46px',
			'width':'86px',
			'renderTo':'help',
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
	}
	Ext.onReady(init);
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
