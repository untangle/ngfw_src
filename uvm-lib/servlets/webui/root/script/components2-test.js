
//resources map
Ung.hasResource={};

//Button component class
Ung.Button = Ext.extend(Ext.Component, {
    hidden: false,
    width:'100%',
    height:'100%',
    disabled: false,
    text: '',
    iconSrc: '',
    clickEvent : 'click',
	cls: '',
    initComponent: function(){
        Ung.Button.superclass.initComponent.call(this);
        this.addEvents(
            'click',
            'mouseover',
            'mouseout');
    },
    // private
    onRender : function(container, position) {
        var templateHTML=Ung.Button.template.applyTemplate({'width':this.width, 'height':this.height, 'imageSrc':this.imageSrc, 'text':this.text});
        var el= document.createElement("div");
        container.dom.insertBefore(el, position);
        el.className="utgButton";
        el.innerHTML=templateHTML;
        this.el = Ext.get(el);
        if(this.cls){
            this.el.addClass(this.cls);
        }
        this.el.on(this.clickEvent, this.onClick, this);
        this.el.on("mouseover", this.onMouseOver, this);
        this.el.on("mouseout", this.onMouseOut, this);
        //Ung.Button.superclass.onRender.call(this,container, position);
	},
	// private
	onMouseOver: function(e){
		if(!this.disabled){
			this.el.addClass("utgButtonHover");
			this.fireEvent('mouseover', this, e);
		}
	},
    // private
    onMouseOut: function(e){
        this.el.removeClass("utgButtonHover");
        this.fireEvent('mouseout', this, e);
    },
    onClick: function(e){
        if(e){
            e.preventDefault();
        }
        if(e.button !== 0){
            return;
        }
        if(!this.disabled){
            this.fireEvent("click", this, e);
            if(this.handler){
                this.handler.call(this, this, e);
            }
        }
    }
});
Ung.Button.template = new Ext.Template(
'<table border="0" width="{width}" height="{height}"><tr>',
'<td width="1%" style="text-align: left;vertical-align: middle;"><img src="{imageSrc}" style="vertical-align: middle;"/></td>',
'<td style="text-align: left;vertical-align: middle;padding-left:5px;font-size: 14px;">{text}</td>',
'</tr></table>');
Ext.ComponentMgr.registerType('utgButton', Ung.Button);

//Node Class
Ung.Node = Ext.extend(Ext.Component, {
	initComponent : function(){
	    Ung.Node.superclass.initComponent.call(this);
	},
	hidden: false,
	disabled: false,
	
	name: "",
	displayName: "",
	image: "",
	state: "", // On, Off, Attention, Stopped
	powerOn: false,
	runState: "INITIALIZED", // RUNNING, INITIALIZED
	webContext: "",
	viewPosition: "",
	settings: null,
	settingsClassName: null,
	stats: null, //last blinger data received
	isRunning: function() {
		return (this.runState=="RUNNING")
	},
	setState: function(state) {
		this.state=state;
		var iconSrc="images/node/Icon"+this.state+"State28x28.png";
		document.getElementById('nodeStateIconImg_'+this.getId()).src=iconSrc;
	},
	setPowerOn: function(powerOn) {
		this.powerOn=powerOn;
		var iconSrc="images/node/IconPower"+(powerOn?"On":"Off")+"State28x28.png";
		document.getElementById('nodePowerIconImg_'+this.getId()).src=iconSrc;
		document.getElementById('nodePowerOnHint_'+this.getId()).style.display=this.powerOn?"none":"";
	},
	updateRunState: function(runState) {
		this.runState=runState;
		var isRunning=this.isRunning();
		this.setPowerOn(isRunning);
		this.setState(isRunning?"On":"Off");
	},
	updateBlingers: function () {
		if(this.blingers!==null) {
			if(this.powerOn && this.stats) {
				for(var i=0;i<this.blingers.length;i++) {
					Ext.getCmp(this.blingers[i].id).update(this.stats);
				}
			} else {
				this.resetBlingers();
			}
		}
	},
	resetBlingers: function () {
       	if(this.blingers!==null) {
       		for(var i=0;i<this.blingers.length;i++) {
       			Ext.getCmp(this.blingers[i].id).reset();
       		}
       	}
	},
	onPowerClick: function() {
    	this.loadNodeContext();
       	this.setPowerOn(!this.powerOn);
       	this.setState("Attention");
       	if(this.powerOn) {
       		this.nodeContext.node.start(function (result, exception) {
       			this.runState="RUNNING";
				this.setState("On");
				if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
       		}.createDelegate(this));
       	} else {
       		this.nodeContext.node.stop(function (result, exception) {
				this.runState="INITIALIZED";
				this.setState("Off");
				this.resetBlingers();
				if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
       		}.createDelegate(this));
       	}
	},

	onHelpClick: function () {
       	var helpLink=main.getHelpLink(this.displayName);
       	if(helpLink!==null && helpLink.length>0) {
       		window.open(helpLink);
       	}
	},
        
	onSettingsClick: function() {
       	this.settingsWin.show();
       	this.settingsWin.setPosition(222,0);
       	var objSize=main.viewport.getSize();
       	objSize.width=objSize.width-222;
       	this.settingsWin.setSize(objSize);
       	this.loadSettings();
	},
	loadNodeContext: function() {
		if(this.nodeContext===undefined) {
    		this.nodeContext=rpc.nodeManager.nodeContext(this.Tid);
			this.nodeContext.node=this.nodeContext.node();
			this.nodeContext.nodeDesc=this.nodeContext.getNodeDesc();
		}
	},
	initSettings: function(force) {
    	this.loadNodeContext();
		if(!Ung.i18nNodeInstances[this.name]) {
			Ext.Ajax.request({
		        url: "i18n",
		        params:{'nodeClassName':this.nodeContext.nodeDesc.className},
				method: 'GET',
				parentId: this.getId(),
				disableCaching: false,
				success: function ( result, request) {
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					var cmp=Ext.getCmp(request.parentId);
					Ung.i18nNodeInstances[cmp.name]=new Ung.NodeI18N({"map":i18n.map, "nodeMap":jsonResult});
					cmp.postInitSettings()
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert(i18n._("Failed"), i18n._("Failed loading I18N translations for this node") ); 
				}
			});
		} else {
			this.postInitSettings();
		}
	},
	postInitSettings: function() {
       	if(this.settings) {
       		this.settings.destroy();
       		this.settings=null;
       	}
      	if(this.settingsClassName!==null) {
       		eval('this.settings=new '+this.settingsClassName+'({\'node\':this,\'tid\':this.tid,\'name\':this.name});');
       		this.settings.render('settings_'+this.getId());
       	} else {
       		var settingsContent=document.getElementById('settings_'+this.getId());
       		settingsContent.innerHTML="Error: There is no settings class for the node '"+this.name+"'.";
       	}
	},
	
	loadSettings: function() {
        	this.settingsClassName=Ung.Settings.getClassName(this.name);
        	if(!this.settingsClassName) {
	        	Ung.Settings.loadNodeScript(this.name, this.getId(), function(cmpId) {
	        		var cmp=Ext.getCmp(cmpId);
	        		cmp.settingsClassName=Ung.Settings.getClassName(cmp.name);
	        		cmp.initSettings();
	        	});
	        } else {
	        	this.initSettings();
	        }
	},
	
	onRemoveClick: function() {
       	var message="Warning:\n"+this.displayName+"is about to be removed from the rack.\nIts settings will be lost and it will stop processing netwotk traffic.\n\nWould you like to continue removing?"; 
       	if(!confirm(message)) {
       		return;
       	}
       	if(this.settingsWin) {
       		this.settingsWin.cancelAction();
       	}
       	this.setState("Attention");
       	rpc.nodeManager.destroy(function (result, exception) {
			if(exception) { Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
				return;
			}
			if(this) {
				var nodeName=this.name;
				var cmp=this;
				Ext.destroy(cmp);
				cmp=null;
				var myAppButtonCmp=Ext.getCmp('myAppButton_'+nodeName);
				if(myAppButtonCmp!==null) {
					myAppButtonCmp.enable();
				}
				for(var i=0;i<main.nodes.length;i++) {
					if(nodeName==main.nodes[i].name) {
						main.nodes.splice(i,1);
						break;
					} 
				}
				main.updateSeparator();
			}
		}.createDelegate(this), this.Tid);
	},
	
	onSaveClick: function() {
       	if(this.settings) {
       		this.settings.save();
       	}
	},
	
	onCancelClick: function() {
       	this.settingsWin.cancelAction();
	},
	
	initBlingers: function () {
       	if(this.blingers!==null) {
       		var nodeBlingers=document.getElementById('nodeBlingers_'+this.getId());
       		for(var i=0;i<this.blingers.length;i++) {
       			var blingerData=this.blingers[i];
       			blingerData.parentId=this.getId();
       			blingerData.id="blinger_"+this.getId()+"_"+i;
      				eval('var blinger=new Ung.'+blingerData.type+'(blingerData);');
      				blinger.render('nodeBlingers_'+this.getId());
       			//this.blingers[i].id=blinger.id;
       			
       		}
       	}
	},
	beforeDestroy : function(){
		Ext.destroy(
			this.settingsWin,
			this.settings
		);
        Ung.Node.superclass.beforeDestroy.call(this);
    },
	onRender: function(container, position) {
       	//Ung.Node.superclass.onRender.call(this, ct, position);
        var el= document.createElement("div");
        el.setAttribute('viewPosition',this.viewPosition);
        container.dom.insertBefore(el, position);
       	this.el = Ext.get(el);
       	this.el.addClass("rackNode");
       	var templateHTML=Ung.Node.template.applyTemplate({'id':this.getId(),'image':this.image,'displayName':this.displayName});
        this.getEl().insertHtml("afterBegin",templateHTML);
      
	    var settingsHTML=Ung.Node.templateSettings.applyTemplate({'id':this.getId()});
	    var settingsButtonsHTML=Ung.Node.templateSettingsButtons.applyTemplate({'id':this.getId()});
	    //Ext.MessageBox.alert(i18n._("Failed"),settingsHTML);
	    this.settingsWin=new Ext.Window({
			id: 'settingsWin_'+this.getId(),
			layout:'border',
			modal:true,
			title:'Settings Window',
			closeAction:'cancelAction',
			autoCreate:true,
			width:740,
			height:690,
			draggable:false,
			resizable:false,
			items: [{
		        region:"center",
		        html: settingsHTML,
		        border: false,
		        autoScroll: true,
		        cls: 'windowBackground',
		        bodyStyle: 'background-color: transparent;'
		    	}, 
		    	{
		    	region: "south",
		    	html: settingsButtonsHTML,
		        border: false,
		        height:40,
		        cls: 'windowBackground',
		        bodyStyle: 'background-color: transparent;'
		    	}
			],
			cancelAction: function() {
				Ext.destroy(this.settings);
				this.settings=null;
				this.settingsWin.hide();
			}.createDelegate(this)
           });
		this.settingsWin.render('container');

		Ext.get('nodePowerIconImg_'+this.getId()).on('click', this.onPowerClick, this);
		var cmp=null;
		cmp=new Ext.ToolTip({
		  html: Ung.Node.getStatusTip(),
		  target: 'nodeStateIconImg_'+this.getId(),
		  autoWidth: true,
		  autoHeight: true,
		  showDelay: 0,
		  dismissDelay: 0,
		  hideDelay: 0
		});
		cmp=new Ext.ToolTip({
		  html: Ung.Node.getPowerTip(),
		  target: 'nodePowerIconImg_'+this.getId(),
		  autoWidth: true,
		  autoHeight: true,
		  showDelay: 0,
		  dismissDelay: 0,
		  hideDelay: 0
		});
		cmp=new Ext.Button({
	        iconCls: 'nodeSettingsIcon',
			renderTo: 'nodeSettingsButton_'+this.getId(),
	        text: i18n._('Show Settings'),
	        handler: function() {this.onSettingsClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        iconCls: 'helpIcon',
			renderTo: 'nodeHelpButton_'+this.getId(),
	        text: i18n._('Help'),
	        handler: function() {this.onHelpClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
			iconCls: 'nodeRemoveIcon',
			renderTo: 'nodeRemoveButton_'+this.getId(),
	        text: i18n._('Remove'),
	        handler: function() {this.onRemoveClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        iconCls: 'cancelIcon',
			renderTo: 'nodeCancelButton_'+this.getId(),
	        text: i18n._('Cancel'),
	        handler: function() {this.onCancelClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        iconCls: 'saveIcon',
			renderTo: 'nodeSaveButton_'+this.getId(),
	        text: i18n._('Save'),
	        handler: function() {this.onSaveClick();}.createDelegate(this)
        });
        this.updateRunState(this.runState);
       	this.initBlingers();
	}
});
// Get node component by nodeId
Ung.Node.getCmp=function(nodeId) {
	return Ext.getCmp(nodeId);
};

Ung.Node.getStatusTip=function() {
	return ['<div style="text-align: left;">',
	i18n._("The <B>Status Indicator</B> shows the current operating condition of a particular software product."),'<BR>',
	'<font color="#00FF00"><b>'+i18n._("Green")+'</b></font> '+i18n._('indicates that the product is "on" and operating normally.'),'<BR>',
	'<font color="#FF0000"><b>'+i18n._("Red")+'</b></font> '+i18n._('indicates that the product is "on", but that an abnormal condition has occurred.'),'<BR>',
	'<font color="#FFFF00"><b>'+i18n._("Yellow")+'</b></font> '+i18n._('indicates that the product is saving or refreshing settings.'),'<BR>',
	'<b>'+i18n._("Clear")+'</b> '+i18n._('indicates that the product is "off", and may be turned "on" by the user.'),'</div>'].join('');
}
Ung.Node.getPowerTip= function() {
	return i18n._('The <B>Power Button</B> allows you to turn a product "on" and "off".');
};
Ung.Node.template = new Ext.Template(
'<div class="nodeImage"><img src="{image}"/></div>',
'<div class="nodeLabel">{displayName}</div><div class="nodeBlingers" id="nodeBlingers_{id}"></div>',
'<div class="nodeStateIcon"><img id="nodeStateIconImg_{id}" src="images/node/IconOffState28x28.png"></div>',
'<div class="nodePowerIcon"><img id="nodePowerIconImg_{id}" src="images/node/IconPowerOffState28x28.png"></div>',
'<div id="nodePowerOnHint_{id}" class="nodePowerOnHint"><img src="images/node/IconPowerOnHint100.png"></div>',
'<div class="nodeSettingsButton" id="nodeSettingsButton_{id}"></div>',
'<div class="nodeHelpButton" id="nodeHelpButton_{id}"></div>');

Ung.Node.templateSettings=new Ext.Template(
'<div class="nodeSettingsContent" id="settings_{id}"></div>');
Ung.Node.templateSettingsButtons=new Ext.Template(
'<div class="nodeRemoveButton" id="nodeRemoveButton_{id}"></div>',
'<div class="nodeCancelButton" id="nodeCancelButton_{id}"></div>',
'<div class="nodeSaveButton" id="nodeSaveButton_{id}"></div>');

//Blinger Manager object
Ung.BlingerManager = {
	//update interval in millisecond
	updateTime: 5000,
	started: false,
	intervalId: null,
	cycleCompleted:true,
	
	start: function() {
		this.stop();
		this.intervalId=window.setInterval("Ung.BlingerManager.getNodesStats()",this.updateTime);
		this.started=true;
	},
	
	stop: function() {
		if(this.intervalId!==null) {
			window.clearInterval(this.intervalId);
		}
		this.cycleCompleted=true;
		this.started=false;
	},
	
	hasActiveNodes: function() {
		for(var i=0;i<main.nodes.length;i++) {
			var nodeCmp=Ung.Node.getCmp(main.nodes[i].tid);
			if(nodeCmp && nodeCmp.isRunning()) {
				return true;
			}
		}
		return false;
	},
	getNodesStats: function() {
		if(!this.cycleCompleted) {
			return;
		}
		if(this.hasActiveNodes()) {
			this.cycleCompleted=false;
			rpc.nodeManager.allNodeStats(function (result, exception) {
				if(exception) { 
					Ext.MessageBox.alert(i18n._("Failed"),exception.message, function() {
						Ung.BlingerManager.cycleCompleted=true;
					});
					return;
				}
				try {
					var allNodeStats=result;
					for(var i=0;i<main.nodes.length;i++) {
						var nodeCmp=Ung.Node.getCmp(main.nodes[i].tid);
						if(nodeCmp && nodeCmp.isRunning()) {
							nodeCmp.stats=allNodeStats.map[main.nodes[i].tid];
							nodeCmp.updateBlingers();
						}
					}
					Ung.BlingerManager.cycleCompleted=true;
				  } catch(err) {
					Ung.BlingerManager.cycleCompleted=true;
					throw err;
				  }
			});
		}	
	}
};

//Activity Blinger Class
Ung.ActivityBlinger = Ext.extend(Ext.Component, {
	parentId: null,
	bars: null,
	lastValues: null,
	decays:null,
	onRender: function (container, position) {
		var el= document.createElement("div");
		el.className="activityBlinger";
		container.dom.insertBefore(el, position);
		this.el = Ext.get(el);
		this.id=Ext.id(this);
		var templateHTML=Ung.ActivityBlinger.template.applyTemplate({'id':this.getId(),'blingerName':i18n._("activity")});
		el.innerHTML=templateHTML;
		this.lastValues=[];
		this.decays=[];
     	if(this.bars!==null) {
     		var out=[];
     		for(var i=0;i<this.bars.length;i++) {
     			var bar=this.bars[i];
     			var top=3+i*15;
     			this.lastValues.push(null);
     			this.decays.push(0);
     			out.push('<div class="blingerText activityBlingerText" style="top:'+top+'px;">'+i18n._(bar)+'</div>');
     			out.push('<div class="activityBlingerBar" style="top:'+top+'px;width:0px;display:none;" id="activityBar_'+this.getId()+'_'+i+'"></div>');
     		}
     		document.getElementById("blingerBox_"+this.getId()).innerHTML=out.join("");
     	}
	},
        
	update: function(stats) {
		for(var i=0;i<this.bars.length;i++) {
			var top=3+i*15;
			var bar=this.bars[i];
			var newValue=stats.counters[6+i];
			this.decays[i]=Ung.ActivityBlinger.decayValue(newValue, this.lastValues[i],this.decays[i]);
			this.lastValues[i]=newValue;
			var barPixelWidth=Math.floor(this.decays[i]*0.6);
			var barDiv=document.getElementById('activityBar_'+this.getId()+'_'+i);
			barDiv.style.width=barPixelWidth+"px";
			barDiv.style.display=(barPixelWidth===0)?"none":"";
		}
	},
	reset: function() {
		for(var i=0;i<this.bars.length;i++) {
			this.lastValues[i]=null;
			this.decays[i]=0;
			var barDiv=document.getElementById('activityBar_'+this.getId()+'_'+i);
			barDiv.style.width="0px";
			barDiv.style.display="none";
		}
	}
        
});
Ung.ActivityBlinger.template = new Ext.Template(
'<div class="blingerName">{blingerName}</div>',
'<div class="blingerBox" id="blingerBox_{id}" style="width:60px;">',
'</div>');
Ung.ActivityBlinger.decayFactor=Math.pow(0.94,Ung.BlingerManager.updateTime/1000);
Ung.ActivityBlinger.decayValue = function(newValue, lastValue, decay) {
	if(lastValue!==null && newValue!=lastValue) {
		decay=98;
	} else {
		decay=decay*Ung.ActivityBlinger.decayFactor;
	}
	return decay;
};
Ext.ComponentMgr.registerType('utgActivityBlinger', Ung.ActivityBlinger);

//System Blinger Class
Ung.SystemBlinger = Ext.extend(Ext.Component, {
	parentId: null,
	data: null,
	byteCountCurrent: null,
	byteCountLast: null,
	sessionCountCurrent: null,
	sessionCountTotal: null,
	sessionRequestLast: null,
	sessionRequestTotal: null,
	
	onRender: function (container, position) {
	        var el= document.createElement("div");
	        el.className="systemBlinger";
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
        	this.id=Ext.id(this);
			var templateHTML=Ung.SystemBlinger.template.applyTemplate({'id':this.getId(),'blingerName':i18n._("system")});
			el.innerHTML=templateHTML;
			this.byteCountCurrent=0;
			this.byteCountLast=0;
			this.sessionCountCurrent=0;
			this.sessionCountTotal=0;
			this.sessionRequestLast=0;
			this.sessionRequestTotal=0;
			
			this.data=[];
			this.data.push({"name":i18n._("Current Session Count:"),"value":"&nbsp;"});
			this.data.push({"name":i18n._("ACC:"),"value":"&nbsp;"});
			this.data.push({"name":i18n._("REQ:"),"value":"&nbsp;"});
			this.data.push({"name":i18n._("Data rate:"),"value":"&nbsp;"});
        	if(this.data!==null) {
        		var out=[];
        		for(var i=0;i<this.data.length;i++) {
        			var dat=this.data[i];
        			var top=3+i*15;
        			out.push('<div class="blingerText systemBlingerName" style="top:'+top+'px;" id="systemName_'+this.getId()+'_'+i+'">'+dat.name+'</div>');
        			out.push('<div class="blingerText systemBlingerValue" style="top:'+top+'px;" id="systemValue_'+this.getId()+'_'+i+'">'+dat.value+'</div>');
        		}
        		document.getElementById("blingerBox_"+this.getId()).innerHTML=out.join("");
        	}
	},
	
	update: function(stats) {
        	// UPDATE COUNTS
        	this.sessionCountCurrent=stats.tcpSessionCount+stats.udpSessionCount;
        	this.sessionCountTotal=stats.tcpSessionTotal+stats.udpSessionTotal;
        	this.sessionRequestTotal=stats.tcpSessionRequestTotal+stats.udpSessionRequestTotal;
            this.byteCountCurrent = stats.c2tBytes + stats.s2tBytes;
            // (RESET COUNTS IF NECESSARY)
            if( (this.byteCountLast === 0) || (this.byteCountLast > this.byteCountCurrent) ) {
                this.byteCountLast = this.byteCountCurrent;
            }
            if( (this.sessionRequestLast === 0) || (this.sessionRequestLast > this.sessionRequestTotal) ) {
                this.sessionRequestLast = this.sessionRequestTotal;
            }
        	var acc=this.sessionCountTotal;
        	var req=this.sessionRequestTotal;
        	var dataRate=(this.byteCountCurrent - this.byteCountLast)/Ung.BlingerManager.updateTime;
        	this.data[0].value=this.sessionCountCurrent;
        	this.data[1].value=acc;
        	this.data[2].value=req;
        	this.data[3].value = dataRate.toFixed(2)+"/KBPs";
        	if(this.data!==null) {
        		for(var i=0;i<this.data.length;i++) {
        			var valueDiv=document.getElementById('systemValue_'+this.getId()+'_'+i);
        			valueDiv.innerHTML=this.data[i].value;
        		}
        	}
	},
	reset: function() {
			this.byteCountCurrent=0;
			this.byteCountLast=0;
			this.sessionCountCurrent=0;
			this.sessionCountTotal=0;
			this.sessionRequestLast=0;
			this.sessionRequestTotal=0;
        	
        	if(this.data!==null) {
        		for(var i=0;i<this.data.length;i++) {
        			this.data[i].value="&nbsp;";
        			var valueDiv=document.getElementById('systemValue_'+this.getId()+'_'+i);
        			valueDiv.innerHTML=this.data[i].value;
        		}
        	}
	}
});
Ung.SystemBlinger.template = new Ext.Template(
'<div class="blingerName">{blingerName}</div>',
'<div class="systemBlingerBox" id="blingerBox_{id}" style="width:100%">',
'</div>');
Ext.ComponentMgr.registerType('utgSystemBlinger', Ung.SystemBlinger);



//Setting base class
Ung.Settings = Ext.extend(Ext.Component, {
    // node i18n
    i18n: null,
    // node object
    node: null,
    // settings tabs (if the settings has tabs layout)
    tabs: null,
    // holds the json rpc results for the settings class like baseSettings object, repository, repositoryDesc
    rpc: null, 
    autoEl: 'div',
	initComponent: function(container, position) {
		this.rpc={};
    	this.i18n=Ung.i18nNodeInstances[this.name];
    	Ung.Settings.superclass.initComponent.call(this);
	},
    // build Tab panel from an array of tab items
	buildTabPanel: function(itemsArray) {
		this.tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width: 690,
	        height: 400,
	        activeTab: 0,
	        frame: true,
	        parentId: this.getId(),
	        items: itemsArray,
		    listeners: {
		    	"render": {
		    		fn: function() {
		    			var settingsCmp=Ext.getCmp(this.parentId);
						var objSize=settingsCmp.node.settingsWin.items.get(0).getEl().getSize(true);
						objSize.width=objSize.width-24;
						objSize.height=objSize.height-17;
						this.setSize(objSize);
					}
				}
			}
	    });
	},
    // get nodeContext.node object
	getRpcNode: function() {
		return this.node.nodeContext.node;
	},
    // get base settings object
	getBaseSettings: function(forceReload) {
		if(forceReload || this.rpc.baseSettings===undefined) {
			this.rpc.baseSettings=this.getRpcNode().getBaseSettings();
		}
		return this.rpc.baseSettings;
	},
    // get Validator object
	getValidator: function() {
		if(this.node.nodeContext.node===undefined) {
			this.node.nodeContext.node.validator=this.getRpcNode().getValidator();
		}
		return this.node.nodeContext.node.validator;
	},
    // get eventManager object
	getEventManager: function () {
		if(this.node.nodeContext.node.eventManager===undefined) {
			this.node.nodeContext.node.eventManager=this.node.nodeContext.node.getEventManager();
		}
		return this.node.nodeContext.node.eventManager;
	},
	beforeDestroy : function(){
        Ext.destroy(this.tabs);
        Ung.Settings.superclass.beforeDestroy.call(this);
    },
    // All settings classes mut override the save method
	save: function() {
	}
});
Ung.Settings._nodeScripts={};

// Dynamically loads javascript file for a node
Ung.Settings.loadNodeScript=function(nodeName,cmpId,callbackFn) {
	main.loadScript('script/'+nodeName+'/settings.js',function() {callbackFn(cmpId);});
};
Ung.Settings._classNames={};

// Static function get the settings class name for a node
Ung.Settings.getClassName=function(name) {
	var className=Ung.Settings._classNames[name];
	return className===undefined?null:className;
};
// Static function to register a settings class name for a node
Ung.Settings.registerClassName=function(name,className) {
	Ung.Settings._classNames[name]=className;
};



//Event Log class
Ung.GridEventLog = Ext.extend(Ext.grid.GridPanel, {
	// the settings component
	settingsCmp: null,
	//if the event log has more than one repositories that can be selected
	hasRepositories: true,
	//Event manager rpc function to call
	// default is getEventManager() from settingsCmp
	eventManagerFn: null,
	//Records per page
	recordsPerPage: 20,
	// fields for the Store
	fields:null,
	// columns for the column model
	columns:null,
	enableHdMenu: false,
	//Predefined event log type. means the fileds and columns are predefined.
	//This was introduced for the speed of development. 
	// TYPE1: timeStamp, blocked, pipelineEndpoints, protocol, blocked, server
	predefinedType: null,
	//get the fields for a predefined type 
	getPredefinedFields: function(type) {
		var fields=null;
		switch(type) {
			case "TYPE1":
				fields=[
					{name: 'timeStamp'},
					{name: 'blocked'},
					{name: 'pipelineEndpoints'},
					{name: 'protocol'},
					{name: 'blocked'},
					{name: 'server'}
				];
				break;
		}
		return fields;
	},
	//get the columns for a predefined type
	getPredefinedColumns: function(type) {
		var columns=null;
		switch(type) {
			case "TYPE1":
				columns = [
					{header: i18n._("timestamp"), width: 120, sortable: true, dataIndex: 'timeStamp', renderer: function(value) {
				    	return i18n.timestampFormat(value);
				    }},
				    {header: i18n._("action"), width: 70, sortable: true, dataIndex: 'blocked', renderer: function(value) {
				    		return value?i18n._("blocked"):i18n._("passed");
				    	}.createDelegate(this)
				    },
				    {header: i18n._("client"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.CClientAddr.hostAddress+":"+value.CClientPort;}},
				    {header: i18n._("request"), width: 120, sortable: true, dataIndex: 'protocol'},
				    {header: i18n._("reason for action"), width: 120, sortable: true, dataIndex: 'blocked', renderer: function(value) {
				    		return value?i18n._("blocked in block list"):i18n._("not blocked in block list");
				    	}.createDelegate(this)
				    },
				    {header: i18n._("server"), width: 120, sortable: true, dataIndex: 'pipelineEndpoints', renderer: function(value) {return value===null?"" : value.SServerAddr.hostAddress+":"+value.SServerPort;}}
			    ];
				break;
		}
		return columns;
	},
	//initComponent
	initComponent: function(){
    	if(this.title==null) {
    		this.title=i18n._('Event Log');
    	}
    	if(this.autoExpandColumn==null) {
    		this.autoExpandColumn="timestamp";
    	}
    	if(this.predefinedType!=null) {
    		this.fields=this.getPredefinedFields(this.predefinedType);
    		this.columns=this.getPredefinedColumns(this.predefinedType);
    	}
    	if(this.eventManagerFn==null) {
    		this.eventManagerFn=this.settingsCmp.getEventManager();
    	}
    	this.settingsCmp.rpc.repository={};
	    this.store = new Ext.data.Store({
	        proxy: new Ung.MemoryProxy({root: 'list'}),
	        sortInfo: this.sortField?{field: this.sortField, direction: "ASC"}:null,
	        remoteSort: true,
	        reader: new Ext.data.JsonReader({
	        	totalProperty: "totalRecords",
	        	root: 'list',
		        fields: this.fields
			})
        });

        this.bbar=	[{xtype:'tbtext',text:'<span id="boxRepository_'+this.getId()+'_'+this.settingsCmp.node.tid+'"></span>'},
					 {	xtype: 'tbbutton',
			            text: i18n._('Refresh'),
			            tooltip: i18n._('Refresh'),
						iconCls: 'iconRefresh',
						handler: function() {this.refreshList();}.createDelegate(this)
					},
					new Ext.PagingToolbar({
						pageSize: this.recordsPerPage,
						store: this.store
					})];
        Ung.GridEventLog.superclass.initComponent.call(this);
	},
	onRender : function(container, position) {
		Ung.GridEventLog.superclass.onRender.call(this,container, position);
		this.eventManagerFn.getRepositoryDescs(function (result, exception) {
			if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
			if(this.settingsCmp) {
				this.settingsCmp.rpc.repositoryDescs=result;
				var out=[];
				out.push('<select id="selectRepository_'+this.getId()+'_'+this.settingsCmp.node.tid+'" class="height:11px; font-size:9px;">');
				var repList=this.settingsCmp.rpc.repositoryDescs.list;
				for(var i=0;i<repList.length;i++) {
					var repDesc=repList[i];
					var selOpt=(i===0)?"selected":"";
					out.push('<option value="'+repDesc.name+'" '+selOpt+'>'+this.settingsCmp.i18n._(repDesc.name)+'</option>');
				}
				out.push('</select>');
				var boxRepositoryDescEventLog=document.getElementById('boxRepository_'+this.getId()+'_'+this.settingsCmp.node.tid);
				boxRepositoryDescEventLog.innerHTML=out.join("");
			}
		}.createDelegate(this));
	},
	//get selected repository
    getSelectedRepository: function () {
    	var selObj=document.getElementById('selectRepository_'+this.getId()+'_'+this.settingsCmp.node.tid);
    	var result=null;
    	if(selObj!==null && selObj.selectedIndex>=0) {
    		result = selObj.options[selObj.selectedIndex].value;
    	}
		return result;
    },
    //Refresh the events list
	refreshList: function() {
		var selRepository=this.getSelectedRepository();
		if(selRepository!==null) {
			if(this.settingsCmp.rpc.repository[selRepository] === undefined) {
				this.settingsCmp.rpc.repository[selRepository]=this.eventManagerFn.getRepository(selRepository);
			}
			this.settingsCmp.rpc.repository[selRepository].getEvents(function (result, exception) {
				if(exception) {Ext.MessageBox.alert(i18n._("Failed"),exception.message); return;}
				var events = result;
				if(this.settingsCmp!==null) {
					this.getStore().proxy.data=events;
					this.getStore().load({params:{start: 0, limit: this.recordsPerPage}});
				}
			}.createDelegate(this));
		}
	}
});

//Standard Ung window
// has 2 sections: content and buttons
Ung.Window = Ext.extend(Ext.Window, {
	layout:'border',
	modal:true,
	renderTo:'container',
	//window title
	title:null,
	// content html
	contentHtml: null,
	// buttons html
	buttonsHtml: null,
	// function called by close action
	closeAction:'hide',
	draggable:false,
	resizable:false,
	//sub componetns - used by destroy function
	subCmps: null,
	// size to rack right side on show
	sizeToRack: true,
	// get the content element
	getContentEl: function() {
		return document.getElementById("window_content_"+this.getId());
	},
	// get the content element height
	getContentHeight: function() {
		return this.items.get(0).getEl().getHeight(true);
	},
	initComponent: function() {
		this.subCmps=[];
		if(!this.contentHtml) {
			this.contentHtml="";
		}
		this.items= [{
			region: "center",
			html: '<div id="window_content_'+this.getId()+'">'+this.contentHtml+'</div>',
			border: false,
			autoScroll: true,
			cls: 'windowBackground',
			bodyStyle: 'background-color: transparent;'
		}];
		if(this.buttonsHtml) { 
			this.items.push(
			{
				region: "south",
				html: this.buttonsHtml,
				border: false,
				height:40,
				cls: 'windowBackground',
				bodyStyle: 'background-color: transparent;'
			});
		}
        Ung.Window.superclass.initComponent.call(this);
	},
	beforeDestroy : function() {
		Ext.each(this.subCmps,Ext.destroy);
		Ung.Window.superclass.beforeDestroy.call(this);
	},
	//on show position and size
    show: function() {
    	Ung.Window.superclass.show.call(this);
		if(this.sizeToRack) {
			this.setPosition(222,0);
			var objSize=main.viewport.getSize();
			objSize.width=objSize.width-222;
			this.setSize(objSize);
		}
    }
});
Ung.Window.buttonsTemplate=new Ext.Template(
'<div class="buttonHelpPos" id="button_help_{id}"></div>',
'<div class="buttonCancelPos" id="button_cancel_{id}"></div>',
'<div class="buttonUpdatePos" id="button_update_{id}"></div>');


//update window
// has the content and 3 standard buttons: help, cancel, Update
Ung.UpdateWindow = Ext.extend(Ung.Window, {
	closeAction:'cancelAction',
	initComponent: function() {
		this.buttonsHtml=Ung.Window.buttonsTemplate.applyTemplate({id:this.getId()});
		Ung.UpdateWindow.superclass.initComponent.call(this);
	},
	afterRender: function() {
		Ung.UpdateWindow.superclass.afterRender.call(this);
		this.initButtons.defer(1, this);
	},
	initButtons: function() {
		this.subCmps.push(new Ext.Button({
			renderTo: 'button_help_'+this.getId(),
			iconCls: 'helpIcon',
			text: i18n._('Help'),
			handler: function() {this.helpAction();}.createDelegate(this)
		}));
		this.subCmps.push(new Ext.Button({
			renderTo: 'button_cancel_'+this.getId(),
			iconCls: 'cancelIcon',
			text: i18n._('Cancel'),
			handler: function() {this.cancelAction();}.createDelegate(this)
		}));
		this.subCmps.push(new Ext.Button({
			renderTo: 'button_update_'+this.getId(),
			iconCls: 'saveIcon',
			text: i18n._('Update'),
			handler: function() {this.updateAction();}.createDelegate(this)
		}));
	},
	// the help method
	//to override
	helpAction: function() {
		main.todo();
	},
	// the cancel action
	//to override
	cancelAction: function() {
		this.hide();
	},
	// the update actions
	//to override
	updateAction: function() {
	}
});
// Row editor window used by editor grid
Ung.RowEditorWindow = Ext.extend(Ung.UpdateWindow, {
	// the editor grid
	grid: null,
	// input lines for standard input lines (text, checkbox, textarea, ..)
	inputLines: null,
	// content template must be defied for row editors with non standard input lines
	contentTemplate: null,
	// the row Index of the curent edit
	rowIndex: null,
	sizeToRack: false,
	//size to grid on show
	sizeToGrid: false,
	initComponent: function() {
		if(!this.height && !this.width) {
			this.sizeToGrid=true;
		}
    	if(this.title==null) {
    		this.title=i18n._('Edit');
    	}
    	var contentTemplate=null;
    	//TODO: define comportament for select boxes
    	//TODO: can define custom input lines?
    	if(this.inputLines) {
    		var contentArr=[];
    		for(var i=0;i<this.inputLines.length;i++) {
    			var inputLine=this.inputLines[i];
    			switch(inputLine.type) {
    				case "text":
    					if(inputLine.style==null) {
    						inputLine.style="width:200px;"
    					}
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><input type="text" id="field_{id}_'+inputLine.name+'" style="'+inputLine.style+'"/></span></div>')
    					break;
    				case "checkbox":
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><input type="checkbox" id="field_{id}_'+inputLine.name+'"/></span></div>')
    					break;
    				case "textarea":
    					if(inputLine.style==null) {
    						inputLine.style="width:200px;height:60px;"
    					}
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><textarea type="text" id="field_{id}_'+inputLine.name+'" style="'+inputLine.style+'"></textarea></span></div>')
    					break;
    			}
    		}
    		contentTemplate=new Ext.Template(contentArr);
    	} else {
    		contentTemplate=this.contentTemplate;
    	}
    	this.contentHtml=contentTemplate.applyTemplate({id:this.getId()});
        Ung.RowEditorWindow.superclass.initComponent.call(this);
    },
    show: function() {
    	Ung.UpdateWindow.superclass.show.call(this);
		var objPosition=this.grid.getPosition();
		this.setPosition(objPosition);
		if(this.sizeToGrid) {
			var objSize=this.grid.getSize();
			this.setSize(objSize);
		}
    },
    // populate is called whent a record is edited, tot populate the edit window
	populate: function(record,rowIndex) {
		this.rowIndex=rowIndex;
		if(this.inputLines) {
    		for(var i=0;i<this.inputLines.length;i++) {
    			var inputLine=this.inputLines[i];
    			switch(inputLine.type) {
    				case "text":
    				case "textarea":
    					document.getElementById('field_'+this.getId()+'_'+inputLine.name).value=record.get(inputLine.name);
    					break;
    				case "checkbox":
    					document.getElementById('field_'+this.getId()+'_'+inputLine.name).checked=record.get(inputLine.name);
    					break;
    			}
			}
		}
	},
	// updateAction is called to update the record after the edit
	updateAction: function() {
		if(this.rowIndex!==null) {
			var rec=this.grid.getStore().getAt(this.rowIndex);
			if(this.inputLines) {
	    		for(var i=0;i<this.inputLines.length;i++) {
	    			var inputLine=this.inputLines[i];
	    			switch(inputLine.type) {
	    				case "text":
	    				case "textarea":
	    					rec.set(inputLine.name, document.getElementById('field_'+this.getId()+'_'+inputLine.name).value);
	    					break;
	    				case "checkbox":
	    					rec.set(inputLine.name, document.getElementById('field_'+this.getId()+'_'+inputLine.name).checked);
	    					break;
	    			}
				}			
			}
		}
		this.hide();
	}
});

//RpcProxy
// uses json rpc to get the information from the server
Ung.RpcProxy = function(rpcFn){
    Ung.RpcProxy.superclass.constructor.call(this);
    this.rpcFn = rpcFn;
};

Ext.extend(Ung.RpcProxy, Ext.data.DataProxy, {
	//sets the total number of records
	setTotalRecords: function(totalRecords) {
		this.totalRecords=totalRecords;
	},
	//load function for Proxy class
    load : function(params, reader, callback, scope, arg) {
    	var obj={};
    	obj.params=params;
    	obj.reader=reader;
    	obj.callback=callback;
    	obj.scope=scope;
    	obj.arg=arg;
    	obj.totalRecords=this.totalRecords;
    	var sortColumns=[];
    	if(params.sort) {
    		sortColumns.push((params.dir=="ASC"?"+":"-")+params.sort)
    	}
    	this.rpcFn(function (result, exception) {
			if(exception) {
				Ext.MessageBox.alert(i18n._("Failed"),exception.message); 
				this.callback.call(this.scope, null, this.arg, false);
				return;
			}
			var res=null;
			try {
	            res=this.reader.readRecords(result);
	            if(this.totalRecords) {
					res.totalRecords=this.totalRecords;
				}
				this.callback.call(this.scope, res, this.arg, true);
	        }catch(e){
	            this.callback.call(this.scope, null, this.arg, false);
	            return;
	        }
		}.createDelegate(obj), params.start?params.start:0, params.limit?params.limit:this.totalRecords!=null?this.totalRecords:2147483647, sortColumns);
	}
});

//Memory Proxy
// holds all the data and returns only the page data
// is used by Event Log store
Ung.MemoryProxy = function(config){
    Ext.apply(this, config);
    Ung.MemoryProxy.superclass.constructor.call(this);
};

Ext.extend(Ung.MemoryProxy, Ext.data.DataProxy, {
	//the root property
	root: null,
	// the data
	data: null,
	//load function for Proxy class
    load : function(params, reader, callback, scope, arg){
        params = params || {};
        var result;
        try {
        	var readerData={};
            if(this.data!=null) {
            	var list= (this.root!=null)?this.data[this.root]:this.data;
            	var totalRecords=list.length;
            	var pageList=null;
            	if(params.sort!=null) {
            		list.sort(function(obj1, obj2) {
            			var v1=obj1[params.sort];
            			var v2=obj2[params.sort];
            			var ret=params.dir=="ASC"?-1:1;
            			return v1==v2?0:(v1<v2)?ret:-ret;
            		});
            	}
            	if(params.start!=null && params.limit!=null && list!=null) {
            		pageList=list.slice(params.start,params.start+params.limit);
            	} else {
            		pageList=list;
            	}
            	if(this.root==null) {
            		readerData=pageList;
            	} else {
            		readerData[this.root]=pageList;
            		readerData.totalRecords=totalRecords;
            	}
            }
            result = reader.readRecords(readerData);
        }catch(e){
            this.fireEvent("loadexception", this, arg, null, e);
            callback.call(scope, null, arg, false);
            return;
        }
        callback.call(scope, result, arg, true);
    }
});

// Grid check column
Ext.grid.CheckColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.width) {
        this.width = 40;
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            record.set(this.dataIndex, !record.data[this.dataIndex]);
        }
    },

    renderer: function(value, metadata, record){
        metadata.css += ' x-grid3-check-col-td'; 
        return '<div class="x-grid3-check-col'+(value?'-on':'')+' x-grid3-cc-'+this.id+'">&#160;</div>';
    }
};
// Grid edit column
Ext.grid.EditColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.header) {
        this.header = i18n._("Edit");
    }
    if(!this.width) {
        this.width = 35;
    }
    if(this.fixed==null) {
        this.fixed = true;
    }
    if(this.sortable==null) {
        this.sortable = false;
    }
    if(!this.dataIndex) {
        this.dataIndex = null;
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.EditColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('editRow') != -1){
            e.stopEvent();
           var index = this.grid.getView().findRowIndex(t);
           var record = this.grid.store.getAt(index);
            //populate row editor
           this.grid.rowEditor.populate(record,index);
           this.grid.rowEditor.show();
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="editRow">&nbsp;</div>';
    }
};
// Grid delete column
Ext.grid.DeleteColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.header) {
        this.header = i18n._("Delete");
    }
    if(!this.width) {
        this.width = 39;
    }
    if(this.fixed==null) {
        this.fixed = true;
    }
    if(this.sortable==null) {
        this.sortable = false;
    }
    if(!this.dataIndex) {
        this.dataIndex = null;
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.DeleteColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('deleteRow') != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.grid.updateChangedData(record,"deleted");
            //this.grid.getView().addRowClass(index, "grid-row-deleted");
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="deleteRow">&nbsp;</div>';
    }
};


//Editor Grid class
Ung.EditorGrid = Ext.extend(Ext.grid.EditorGridPanel, {
	// record per page
	recordsPerPage: 20,
	// the minimum number of records for pagination
	minPaginateCount: 60,
	// the total number of records
	totalRecords: null,
	// settings component
	settingsCmp: null,
	//proxy Json Rpc function to populate the Store
	proxyRpcFn: null,
	// the list of fields used to by the Store
	fields: null,
	// has Add button
	hasAdd: true,
	// has Edit buton on each record
	hasEdit: true,
	// has Delete buton on each record
	hasDelete: true,
	// the default Empty record for a new row
	emptyRow: null,
	// input lines used by the row editor
	rowEditorInputLines: null,
	// the default sort field
	sortField: null,
	// the columns are sortable by default, if sortable is not specified
	columnsDefaultSortable: null,
	// force paginate, even if the totalRecords is smaller than minPaginateCount
	forcePaginate: false,
	// javaClass of the record, used in save function to create correct json-rpc object
	recordJavaClass: null,
	// the map of changed data in the grid
	// used by rendering functions and by save
	changedData: null,
	stripeRows: true,
	clicksToEdit: 1,
	enableHdMenu: false,
	addedId:0,
	
	initComponent: function() {
	    this.changedData={};
	    if(this.hasEdit) {
	    	var editColumn=new Ext.grid.EditColumn();
	    	if(!this.plugins) {
	    		this.plugins=[];
	    	}
	    	this.plugins.push(editColumn);
	    	this.columns.push(editColumn);
	    }
	    if(this.hasDelete) {
	    	var deleteColumn=new Ext.grid.DeleteColumn();
	    	if(!this.plugins) {
	    		this.plugins=[];
	    	}
	    	this.plugins.push(deleteColumn);
	    	this.columns.push(deleteColumn);
	    }
	    this.store = new Ext.data.Store({
	        proxy: new Ung.RpcProxy(this.proxyRpcFn),
	        sortInfo: this.sortField?{field: this.sortField, direction: "ASC"}:null,
	        reader: new Ext.data.JsonReader({
	        	totalProperty: "totalRecords",
	        	root: 'list',
		        fields: this.fields
			}),
			
			remoteSort: true,
			getPageStart: function () {
				if(this.lastOptions && this.lastOptions.params) {
					return this.lastOptions.params.start
				} else {
					return 0;
				}
			},
			listeners: {
				"update": {
					fn: function(store, record, operation ) {
						this.updateChangedData(record,"modified");
					}.createDelegate(this)
				},
				"load": {
					fn: function(store, records, options ) {
						this.updateFromChangedData(records, options);
					}.createDelegate(this)
				}
			}
        });
		this.bbar= new Ext.PagingToolbar({
			pageSize: this.recordsPerPage,
			store: this.store,
			displayInfo: true,
			displayMsg: 'Displaying topics {0} - {1} of {2}',
			emptyMsg: "No topics to display"
		});
		if(this.rowEditorInputLines!=null) {
			this.rowEditor=new Ung.RowEditorWindow({
				grid: this,
				inputLines: this.rowEditorInputLines
			});
			this.rowEditor.render('container');
		}
		if(this.hasAdd) {
			this.tbar=[{
				text: i18n._('Add'),
				tooltip: i18n._('Add New Row'),
				iconCls: 'add',
				parentId:this.getId(),
				handler: function() {
					var record=new Ext.data.Record(Ext.decode(Ext.encode(this.emptyRow)));
					record.set("id",this.genAddedId());
					this.stopEditing();
					this.getStore().insert(0, [record]);
					this.updateChangedData(record,"added");
					var row = this.getView().findRowIndex(this.getView().getRow(0));
					if(this.rowEditor) {
						this.rowEditor.populate(record,0);
						this.rowEditor.show();
					} else {
						this.startEditing(0, 0);
					}
		        }.createDelegate(this)
			}];
		}
		Ung.EditorGrid.superclass.initComponent.call(this);
		if(this.columnsDefaultSortable!==null) {
			this.getColumnModel().defaultSortable=this.columnsDefaultSortable;
		}
	},

	genAddedId: function () {
		this.addedId--;
		return this.addedId;
	},
	// is grid paginated
	isPaginated: function() {
		return this.forcePaginate || (this.totalRecords!=null && this.totalRecords>=this.minPaginateCount)
	},
	
	afterRender: function() {
		Ung.EditorGrid.superclass.afterRender.call(this);
		this.getView().getRowClass=function(record,index,rowParams,store) {
			var id=record.get("id");
			if(id==null || id<0) {
				return "grid-row-added";
			} else {
				var d=this.grid.changedData[id];
				if(d) {
					if(d.op=="deleted") {
						return "grid-row-deleted";
					} else {
						return "grid-row-modified";
					}
				}
			}
			return "";
		}
		this.initialLoad.defer(1, this);
	},
	//load first page initialy
	initialLoad: function() {
		this.setTotalRecords(this.totalRecords);
		this.loadPage(0);
	},
	// load a page
	loadPage: function(pageStart,callback, scope, arg) {
		if(!this.isPaginated()) {
			this.getStore().load({callback:callback, scope:scope, arg: arg});
		} else {
			this.getStore().load({params:{start: pageStart, limit: this.recordsPerPage}, callback:callback, scope:scope, arg: arg});
		}
	},
	//when a page is rendered load the changedData for it
	updateFromChangedData: function(store, records, options) {
		var pageStart=this.store.getPageStart();
		for(id in this.changedData) {
			var cd=this.changedData[id];
			if(pageStart==cd.pageStart) {
				if("added"==cd.op) {
					var record=new Ext.data.Record(cd.recData);
					this.store.insert(0,[record]);
				} else if("modified"==cd.op) {
					var recIndex=this.store.find("id",id);
					if(recIndex) {
						var rec=this.store.getAt(recIndex);
						rec.data=cd.recData;
						rec.commit();
					}
				}
			}
		}
	},
	//Test if there are changed data
	hasChangedData: function () {
		var hasChangedData=false;
		for(id in this.changedData) {
			hasChangedData=true;
			break;
		}
		return hasChangedData;
	},
	//Update Changed data after an operation (modifyed, deleted, added)
	updateChangedData: function( record, currentOp) {
		if(!this.hasChangedData()) {
			var cmConfig=this.getColumnModel().config;
			for(i in cmConfig) {
				cmConfig[i].sortable=false;
			}
		}
		var id=record.get("id");
		var cd=this.changedData[id];
		if(cd==null) {
			this.changedData[id]={op: currentOp, recData: record.data, pageStart: this.store.getPageStart()};
			if("deleted"==currentOp) {
				var index=this.store.indexOf(record);
				this.getView().refreshRow(record);
			}
		} else {
			if("deleted"==currentOp) {
				if("added"==cd.op) {
					this.store.remove(record);
					this.changedData[id]=null;
					delete this.changedData[id];
				} else {
					this.changedData[id]={op: currentOp, recData: record.data, pageStart: this.store.getPageStart()};
					this.getView().refreshRow(record);
				}
			} else {
				if("added"==cd.op) {
					this.changedData[id].recData=record.data;
				} else {
					this.changedData[id]={op: currentOp, recData: record.data, pageStart: this.store.getPageStart()};
				}
			}
		}
		
	},
	//Set the total number of records
	setTotalRecords: function(totalRecords) {
		this.totalRecords=totalRecords;
		if(this.totalRecords!=null) {
			this.getStore().proxy.setTotalRecords(this.totalRecords);
		}
		if(this.isPaginated()) {
			this.getBottomToolbar().show();
			this.getBottomToolbar().enable();
		} else {
			this.getBottomToolbar().hide();
			this.getBottomToolbar().disable();
		}
		this.getBottomToolbar().syncSize();
	},
	findFirstChangedDataByFieldValue: function(field,value) {
		for(id in this.changedData) {
			var cd=this.changedData[id];
			if(cd.op!="deleted" && cd.recData[field]==value) {
				return cd;
				break;
			}
		}
		return null;
	},
	
	focusChangedDataField: function(cd, field) {
		var recIndex=this.store.find("id",cd.recData["id"]);
		if(recIndex>=0) {
			this.getView().focusRow(recIndex);
		}
	},
	//focus the first changed row matching a field value
	//used by validation functions
	focusFirstChangedDataByFieldValue: function(field,value) {
		var cd=this.findFirstChangedDataByFieldValue("ipMaddr",value)
		if(cd!=null) {
			this.loadPage(cd.pageStart, function(r, options,success) {
				if(success) {
					this.focusChangedDataField(options.arg,"ipMaddr");
				}
			}.createDelegate(this), this, cd);
		} 
	},
	//Get the save list from the changed data
	getSaveList: function() {
		var added=[];
		var deleted=[];
		var modified=[];
		for(id in this.changedData) {
			var cd=this.changedData[id]
			if("deleted"==cd.op) {
				if(id>0) {
					deleted.push(parseInt(id));
				}
			} else {
				cd.recData["javaClass"]=this.recordJavaClass;
				if(id<0) {
					added.push(cd.recData);
				} else {
					modified.push(cd.recData);
				}
			}
		}
		return [{list: added,"javaClass":"java.util.ArrayList"}, {list: deleted,"javaClass":"java.util.ArrayList"}, {list: modified,"javaClass":"java.util.ArrayList"}];
	},
	//Get the entire list
	// for the unpaginated grids, that send all the records on save
	getFullSaveList: function() {
		//TODO: implement this
	}

});
###############################################################################
#
#  MIME-TYPES and the extensions that represent them
#
#  This file is part of the "mime-support" package.  Please send email (not a
#  bug report) to mime-support@packages.debian.org if you would like new types
#  and/or extensions to be added.
#
#  Users can add their own types if they wish by creating a ".mime.types"
#  file in their home directory.  Definitions included there will take
#  precedence over those listed here.
#
#  Note: Compression schemes like "gzip", "bzip", and "compress" are not
#  actually "mime-types".  They are "encodings" and hence must _not_ have
#  entries in this file to map their extensions.  The "mime-type" of an
#  encoded file refers to the type of data that has been encoded, not the
#  type of the encoding.
#
###############################################################################


application/activemessage
application/andrew-inset			ez
application/applefile
application/atomicmail
application/batch-SMTP
application/beep+xml
application/cals-1840
application/commonground
application/cu-seeme				csm cu
application/cybercash
application/dca-rft
application/dec-dx
application/docbook+xml
application/dsptype				tsp
application/dvcs
application/edi-consent
application/edifact
application/edi-x12
application/eshop
application/font-tdpfr
application/futuresplash			spl
application/ghostview
application/hta					hta
application/http
application/hyperstudio
application/iges
application/index
application/index.cmd
application/index.obj
application/index.response
application/index.vnd
application/iotp
application/ipp
application/isup
application/mac-compactpro			cpt
application/marc
application/mac-binhex40			hqx
application/macwriteii
application/mathematica				nb
application/mathematica-old
application/msaccess				mdb
application/msword				doc dot
application/news-message-id
application/news-transmission
application/octet-stream			bin
application/ocsp-request
application/ocsp-response
application/oda					oda
application/ogg					ogg
application/parityfec
application/pics-rules				prf
application/pgp-encrypted
application/pgp-keys				key
application/pdf					pdf
application/pgp-signature			pgp
application/pkcs10
application/pkcs7-mime
application/pkcs7-signature
application/pkix-cert
application/pkixcmp
application/pkix-crl
application/postscript				ps ai eps
application/prs.alvestrand.titrax-sheet
application/prs.cww
application/prs.nprend
application/qsig
application/rar					rar
application/rdf+xml				rdf
application/remote-printing
application/riscos
application/rss+xml				rss
application/rtf					rtf
application/sdp
application/set-payment
application/set-payment-initiation
application/set-registration
application/set-registration-initiation
application/sgml
application/sgml-open-catalog
application/sieve
application/slate
application/smil				smi smil
application/timestamp-query
application/timestamp-reply
application/vemmi
application/whoispp-query
application/whoispp-response
application/wita
application/wordperfect5.1			wp5
application/x400-bp
application/xhtml+xml				xht xhtml
application/xml					xml xsl
application/xml-dtd
application/xml-external-parsed-entity
application/zip					zip
application/vnd.3M.Post-it-Notes
application/vnd.accpac.simply.aso
application/vnd.accpac.simply.imp
application/vnd.acucobol
application/vnd.aether.imp
application/vnd.anser-web-certificate-issue-initiation
application/vnd.anser-web-funds-transfer-initiation
application/vnd.audiograph
application/vnd.bmi
application/vnd.businessobjects
application/vnd.canon-cpdl
application/vnd.canon-lips
application/vnd.cinderella			cdy
application/vnd.claymore
application/vnd.commerce-battelle
application/vnd.commonspace
application/vnd.comsocaller
application/vnd.contact.cmsg
application/vnd.cosmocaller
application/vnd.ctc-posml
application/vnd.cups-postscript
application/vnd.cups-raster
application/vnd.cups-raw
application/vnd.cybank
application/vnd.dna
application/vnd.dpgraph
application/vnd.dxr
application/vnd.ecdis-update
application/vnd.ecowin.chart
application/vnd.ecowin.filerequest
application/vnd.ecowin.fileupdate
application/vnd.ecowin.series
application/vnd.ecowin.seriesrequest
application/vnd.ecowin.seriesupdate
application/vnd.enliven
application/vnd.epson.esf
application/vnd.epson.msf
application/vnd.epson.quickanime
application/vnd.epson.salt
application/vnd.epson.ssf
application/vnd.ericsson.quickcall
application/vnd.eudora.data
application/vnd.fdf
application/vnd.ffsns
application/vnd.flographit
application/vnd.framemaker
application/vnd.fsc.weblaunch
application/vnd.fujitsu.oasys
application/vnd.fujitsu.oasys2
application/vnd.fujitsu.oasys3
application/vnd.fujitsu.oasysgp
application/vnd.fujitsu.oasysprs
application/vnd.fujixerox.ddd
application/vnd.fujixerox.docuworks
application/vnd.fujixerox.docuworks.binder
application/vnd.fut-misnet
application/vnd.grafeq
application/vnd.groove-account
application/vnd.groove-identity-message
application/vnd.groove-injector
application/vnd.groove-tool-message
application/vnd.groove-tool-template
application/vnd.groove-vcard
application/vnd.hhe.lesson-player
application/vnd.hp-HPGL
application/vnd.hp-PCL
application/vnd.hp-PCLXL
application/vnd.hp-hpid
application/vnd.hp-hps
application/vnd.httphone
application/vnd.hzn-3d-crossword
application/vnd.ibm.MiniPay
application/vnd.ibm.afplinedata
application/vnd.ibm.modcap
application/vnd.informix-visionary
application/vnd.intercon.formnet
application/vnd.intertrust.digibox
application/vnd.intertrust.nncp
application/vnd.intu.qbo
application/vnd.intu.qfx
application/vnd.irepository.package+xml
application/vnd.is-xpr
application/vnd.japannet-directory-service
application/vnd.japannet-jpnstore-wakeup
application/vnd.japannet-payment-wakeup
application/vnd.japannet-registration
application/vnd.japannet-registration-wakeup
application/vnd.japannet-setstore-wakeup
application/vnd.japannet-verification
application/vnd.japannet-verification-wakeup
application/vnd.koan
application/vnd.lotus-1-2-3
application/vnd.lotus-approach
application/vnd.lotus-freelance
application/vnd.lotus-notes
application/vnd.lotus-organizer
application/vnd.lotus-screencam
application/vnd.lotus-wordpro
application/vnd.mcd
application/vnd.mediastation.cdkey
application/vnd.meridian-slingshot
application/vnd.mif	      mif
application/vnd.minisoft-hp3000-save
application/vnd.mitsubishi.misty-guard.trustweb
application/vnd.mobius.daf
application/vnd.mobius.dis
application/vnd.mobius.msl
application/vnd.mobius.plc
application/vnd.mobius.txf
application/vnd.motorola.flexsuite
application/vnd.motorola.flexsuite.adsi
application/vnd.motorola.flexsuite.fis
application/vnd.motorola.flexsuite.gotap
application/vnd.motorola.flexsuite.kmr
application/vnd.motorola.flexsuite.ttc
application/vnd.motorola.flexsuite.wem
application/vnd.mozilla.xul+xml			xul
application/vnd.ms-artgalry
application/vnd.ms-asf
application/vnd.ms-excel			xls xlb xlt
application/vnd.ms-lrm
application/vnd.ms-pki.seccat			cat
application/vnd.ms-pki.stl			stl
application/vnd.ms-powerpoint			ppt pps
application/vnd.ms-project
application/vnd.ms-tnef
application/vnd.ms-works
application/vnd.mseq
application/vnd.msign
application/vnd.music-niff
application/vnd.musician
application/vnd.netfpx
application/vnd.noblenet-directory
application/vnd.noblenet-sealer
application/vnd.noblenet-web
application/vnd.novadigm.EDM
application/vnd.novadigm.EDX
application/vnd.novadigm.EXT
application/vnd.osa.netdeploy
application/vnd.palm
application/vnd.pg.format
application/vnd.pg.osasli
application/vnd.powerbuilder6
application/vnd.powerbuilder6-s
application/vnd.powerbuilder7
application/vnd.powerbuilder7-s
application/vnd.powerbuilder75
application/vnd.powerbuilder75-s
application/vnd.previewsystems.box
application/vnd.publishare-delta-tree
application/vnd.pvi.ptid1
application/vnd.pwg-xhtml-print+xml
application/vnd.rapid
application/vnd.s3sms
application/vnd.seemail
application/vnd.shana.informed.formdata
application/vnd.shana.informed.formtemplate
application/vnd.shana.informed.interchange
application/vnd.shana.informed.package
application/vnd.smaf				mmf
application/vnd.sss-cod
application/vnd.sss-dtf
application/vnd.sss-ntf
application/vnd.stardivision.calc		sdc
application/vnd.stardivision.draw		sda
application/vnd.stardivision.impress		sdd sdp
application/vnd.stardivision.math		smf
application/vnd.stardivision.writer		sdw vor
application/vnd.stardivision.writer-global	sgl
application/vnd.street-stream
application/vnd.sun.xml.calc			sxc
application/vnd.sun.xml.calc.template		stc
application/vnd.sun.xml.draw			sxd
application/vnd.sun.xml.draw.template		std
application/vnd.sun.xml.impress			sxi
application/vnd.sun.xml.impress.template	sti
application/vnd.sun.xml.math			sxm
application/vnd.sun.xml.writer			sxw
application/vnd.sun.xml.writer.global		sxg
application/vnd.sun.xml.writer.template		stw
application/vnd.svd
application/vnd.swiftview-ics
application/vnd.symbian.install			sis
application/vnd.triscape.mxs
application/vnd.trueapp
application/vnd.truedoc
application/vnd.tve-trigger
application/vnd.ufdl
application/vnd.uplanet.alert
application/vnd.uplanet.alert-wbxml
application/vnd.uplanet.bearer-choice
application/vnd.uplanet.bearer-choice-wbxml
application/vnd.uplanet.cacheop
application/vnd.uplanet.cacheop-wbxml
application/vnd.uplanet.channel
application/vnd.uplanet.channel-wbxml
application/vnd.uplanet.list
application/vnd.uplanet.list-wbxml
application/vnd.uplanet.listcmd
application/vnd.uplanet.listcmd-wbxml
application/vnd.uplanet.signal
application/vnd.vcx
application/vnd.vectorworks
application/vnd.vidsoft.vidconference
application/vnd.visio				vsd
application/vnd.vividence.scriptfile
application/vnd.wap.sic
application/vnd.wap.slc
application/vnd.wap.wbxml			wbxml
application/vnd.wap.wmlc			wmlc
application/vnd.wap.wmlscriptc			wmlsc
application/vnd.webturbo
application/vnd.wrq-hp3000-labelled
application/vnd.wt.stf
application/vnd.xara
application/vnd.xfdl
application/vnd.yellowriver-custom-menu
application/x-123				wk
application/x-apple-diskimage			dmg
application/x-bcpio				bcpio
application/x-bittorrent			torrent
application/x-cdf				cdf
application/x-cdlink				vcd
application/x-chess-pgn				pgn
application/x-chm				chm
application/x-core
application/x-cpio				cpio
application/x-csh				csh
application/x-debian-package			deb
application/x-director				dcr dir dxr
application/x-doom				wad
application/x-dms				dms
application/x-dvi				dvi
application/x-executable
application/x-flac				flac
application/x-font				pfa pfb gsf pcf pcf.Z
application/x-futuresplash			spl
application/x-gnumeric				gnumeric
application/x-go-sgf				sgf
application/x-graphing-calculator		gcf
application/x-gtar				gtar tgz taz
application/x-hdf				hdf
application/x-httpd-php				phtml pht php
application/x-httpd-php-source			phps
application/x-httpd-php3			php3
application/x-httpd-php3-preprocessed		php3p
application/x-httpd-php4			php4
application/x-ica				ica
application/x-internet-signup			ins isp
application/x-iphone				iii
application/x-java-applet
application/x-java-archive			jar
application/x-java-bean
application/x-java-jnlp-file			jnlp
application/x-java-serialized-object		ser
application/x-java-vm				class
application/x-javascript			js
application/x-kdelnk
application/x-kchart				chrt
application/x-killustrator			kil
application/x-kpresenter			kpr kpt
application/x-koan				skp skd skt skm
application/x-kspread				ksp
application/x-kword				kwd kwt
application/x-latex				latex
application/x-lha				lha
application/x-lzh				lzh
application/x-lzx				lzx
application/x-maker				frm maker frame fm fb book fbdoc
application/x-mif				mif
application/x-ms-wmz				wmz
application/x-ms-wmd				wmd
application/x-msdos-program			com exe bat dll
application/x-msi				msi
application/x-netcdf				nc
application/x-ns-proxy-autoconfig		pac
application/x-nwc				nwc
application/x-object				o
application/x-oz-application			oza
application/x-pkcs7-certreqresp			p7r
application/x-pkcs7-crl				crl
application/x-python-code			pyc pyo
application/x-quicktimeplayer			qtl
application/x-redhat-package-manager		rpm
application/x-rx
application/x-sh
application/x-shar				shar
application/x-shellscript
application/x-shockwave-flash			swf swfl
application/x-sh				sh
application/x-stuffit				sit
application/x-sv4cpio				sv4cpio
application/x-sv4crc				sv4crc
application/x-tar				tar
application/x-tcl				tcl
application/x-tex-gf				gf
application/x-tex-pk				pk
application/x-texinfo				texinfo texi
application/x-trash				~ % bak old sik
application/x-troff				t tr roff
application/x-troff-man				man
application/x-troff-me				me
application/x-troff-ms				ms
application/x-ustar				ustar
application/x-videolan
application/x-wais-source			src
application/x-wingz				wz
application/x-x509-ca-cert			crt
application/x-xcf				xcf
application/x-xfig				fig

audio/32kadpcm
audio/basic					au snd
audio/g.722.1
audio/l16
audio/midi					mid midi kar
audio/mp4a-latm
audio/mpa-robust
audio/mpeg					mpga mpega mp2 mp3 m4a
audio/mpegurl					m3u
audio/parityfec
audio/prs.sid					sid
audio/telephone-event
audio/tone
audio/vnd.cisco.nse
audio/vnd.cns.anp1
audio/vnd.cns.inf1
audio/vnd.digital-winds
audio/vnd.everad.plj
audio/vnd.lucent.voice
audio/vnd.nortel.vbk
audio/vnd.nuera.ecelp4800
audio/vnd.nuera.ecelp7470
audio/vnd.nuera.ecelp9600
audio/vnd.octel.sbc
audio/vnd.qcelp
audio/vnd.rhetorex.32kadpcm
audio/vnd.vmx.cvsd
audio/x-aiff					aif aiff aifc
audio/x-gsm					gsm
audio/x-mpegurl					m3u
audio/x-ms-wma					wma
audio/x-ms-wax					wax
audio/x-pn-realaudio-plugin
audio/x-pn-realaudio				ra rm ram
audio/x-realaudio				ra
audio/x-scpls					pls
audio/x-sd2					sd2
audio/x-wav					wav

chemical/x-pdb					pdb
chemical/x-xyz					xyz

image/cgm
image/g3fax
image/gif					gif
image/ief					ief
image/jpeg					jpeg jpg jpe
image/naplps
image/pcx					pcx
image/png					png
image/prs.btif
image/prs.pti
image/svg+xml					svg svgz
image/tiff					tiff tif
image/vnd.cns.inf2
image/vnd.djvu					djvu djv
image/vnd.dwg
image/vnd.dxf
image/vnd.fastbidsheet
image/vnd.fpx
image/vnd.fst
image/vnd.fujixerox.edmics-mmr
image/vnd.fujixerox.edmics-rlc
image/vnd.mix
image/vnd.net-fpx
image/vnd.svf
image/vnd.wap.wbmp				wbmp
image/vnd.xiff
image/x-cmu-raster				ras
image/x-coreldraw				cdr
image/x-coreldrawpattern			pat
image/x-coreldrawtemplate			cdt
image/x-corelphotopaint				cpt
image/x-icon					ico
image/x-jg					art
image/x-jng					jng
image/x-ms-bmp					bmp
image/x-photoshop				psd
image/x-portable-anymap				pnm
image/x-portable-bitmap				pbm
image/x-portable-graymap			pgm
image/x-portable-pixmap				ppm
image/x-rgb					rgb
image/x-xbitmap					xbm
image/x-xpixmap					xpm
image/x-xwindowdump				xwd

inode/chardevice
inode/blockdevice
inode/directory-locked
inode/directory
inode/fifo
inode/socket

message/delivery-status
message/disposition-notification
message/external-body
message/http
message/s-http
message/news
message/partial
message/rfc822

model/iges					igs iges
model/mesh					msh mesh silo
model/vnd.dwf
model/vnd.flatland.3dml
model/vnd.gdl
model/vnd.gs-gdl
model/vnd.gtw
model/vnd.mts
model/vnd.vtu
model/vrml					wrl vrml

multipart/alternative
multipart/appledouble
multipart/byteranges
multipart/digest
multipart/encrypted
multipart/form-data
multipart/header-set
multipart/mixed
multipart/parallel
multipart/related
multipart/report
multipart/signed
multipart/voice-message

text/calendar					ics icz
text/comma-separated-values			csv
text/css					css
text/directory
text/english
text/enriched
text/h323					323
text/html					htm html shtml
text/iuls					uls
text/mathml					mml
text/parityfec
text/plain					asc txt text diff pot
text/prs.lines.tag
text/rfc822-headers
text/richtext					rtx
text/rtf					rtf
text/scriptlet					sct wsc
text/t140
text/texmacs					tm ts
text/tab-separated-values			tsv
text/uri-list
text/vnd.abc
text/vnd.curl
text/vnd.DMClientScript
text/vnd.flatland.3dml
text/vnd.fly
text/vnd.fmi.flexstor
text/vnd.in3d.3dml
text/vnd.in3d.spot
text/vnd.IPTC.NewsML
text/vnd.IPTC.NITF
text/vnd.latex-z
text/vnd.motorola.reflex
text/vnd.ms-mediapackage
text/vnd.sun.j2me.app-descriptor		jad
text/vnd.wap.si
text/vnd.wap.sl
text/vnd.wap.wml				wml
text/vnd.wap.wmlscript				wmls
text/x-c++hdr					h++ hpp hxx hh
text/x-c++src					c++ cpp cxx cc
text/x-chdr					h
text/x-crontab
text/x-csh					csh
text/x-csrc					c
text/x-java					java
text/x-makefile
text/x-moc					moc
text/x-pascal					p pas
text/x-pcs-gcd					gcd
text/x-perl					pl pm
text/x-python					py
text/x-server-parsed-html
text/x-setext					etx
text/x-sh					sh
text/x-tcl					tcl tk
text/x-tex					tex ltx sty cls
text/x-vcalendar				vcs
text/x-vcard					vcf

video/dl					dl
video/fli					fli
video/gl					gl
video/mpeg					mpeg mpg mpe
video/mp4					mp4
video/quicktime					qt mov
video/mp4v-es
video/parityfec
video/pointer
video/vnd.fvt
video/vnd.motorola.video
video/vnd.motorola.videop
video/vnd.mpegurl				mxu
video/vnd.mts
video/vnd.nokia.interleaved-multimedia
video/vnd.vivo
video/x-dv					dif dv
video/x-la-asf					lsf lsx
video/x-mng					mng
video/x-ms-asf					asf asx
video/x-ms-wm					wm
video/x-ms-wmv					wmv
video/x-ms-wmx					wmx
video/x-ms-wvx					wvx
video/x-msvideo					avi
video/x-sgi-movie				movie

x-conference/x-cooltalk				ice

x-world/x-vrml					vrm vrml wrl
