Ung.hasResource={};
Ung.RPC= function (){};
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
            "click",
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
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
       		}.createDelegate(this));
       	} else {
       		this.nodeContext.node.stop(function (result, exception) {
				this.runState="INITIALIZED";
				this.setState("Off");
				this.resetBlingers();
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
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
					Ext.MessageBox.alert("Failed", 'Failed loading I18N translations for this node' ); 
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
       		this.settings.loadData();
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
			if(exception) { Ext.MessageBox.alert("Failed",exception.message); 
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
	    //Ext.MessageBox.alert("Failed",settingsHTML);
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
		  html: Ung.Node.statusTip,
		  target: 'nodeStateIconImg_'+this.getId(),
		  autoWidth: true,
		  autoHeight: true,
		  showDelay: 0,
		  dismissDelay: 0,
		  hideDelay: 0
		});
		cmp=new Ext.ToolTip({
		  html: Ung.Node.powerTip,
		  target: 'nodePowerIconImg_'+this.getId(),
		  autoWidth: true,
		  autoHeight: true,
		  showDelay: 0,
		  dismissDelay: 0,
		  hideDelay: 0
		});
		cmp=new Ext.Button({
	        'iconCls': 'nodeSettingsIcon',
			'renderTo':'nodeSettingsButton_'+this.getId(),
	        'text': i18n._('Show Settings'),
	        'handler': function() {this.onSettingsClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        'iconCls': 'helpIcon',
			'renderTo':'nodeHelpButton_'+this.getId(),
	        'text': 'Help',
	        'handler': function() {this.onHelpClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
			'iconCls': 'nodeRemoveIcon',
			'renderTo':'nodeRemoveButton_'+this.getId(),
	        'text': 'Remove',
	        'handler': function() {this.onRemoveClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        'iconCls': 'cancelIcon',
			'renderTo':'nodeCancelButton_'+this.getId(),
	        'text': 'Cancel',
	        'handler': function() {this.onCancelClick();}.createDelegate(this)
        });
		cmp=new Ext.Button({
	        'iconCls': 'saveIcon',
			'renderTo':'nodeSaveButton_'+this.getId(),
	        'text': 'Save',
	        'handler': function() {this.onSaveClick();}.createDelegate(this)
        });
        this.updateRunState(this.runState);
       	this.initBlingers();
	}
});

Ung.Node.getCmp=function(nodeId) {
	return Ext.getCmp(nodeId);
};



Ung.Node.statusTip=['<div style="text-align: left;">',
'The <B>Status Indicator</B> shows the current operating condition of a particular software product.<BR>',
'<font color="#00FF00"><b>Green</b></font> indicates that the product is "on" and operating normally.<BR>',
'<font color="#FF0000"><b>Red</b></font> indicates that the product is "on", but that an abnormal condition has occurred.<BR>',
'<font color="#FFFF00"><b>Yellow</b></font> indicates that the product is saving or refreshing settings.<BR>',
'<b>Clear</b> indicates that the product is "off", and may be turned "on" by the user.</div>'].join('');
Ung.Node.powerTip='The <B>Power Button</B> allows you to turn a product "on" and "off".';
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


Ung.BlingerManager = {
	updateTime: 5000, //update interval in millisecond
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
					Ext.MessageBox.alert("Failed",exception.message, function() {
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
		var templateHTML=Ung.ActivityBlinger.template.applyTemplate({'id':this.getId()});
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
     			out.push('<div class="blingerText activityBlingerText" style="top:'+top+'px;">'+bar+'</div>');
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
'<div class="blingerName">activity</div>',
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
			var templateHTML=Ung.SystemBlinger.template.applyTemplate({'id':this.getId()});
			el.innerHTML=templateHTML;
			this.byteCountCurrent=0;
			this.byteCountLast=0;
			this.sessionCountCurrent=0;
			this.sessionCountTotal=0;
			this.sessionRequestLast=0;
			this.sessionRequestTotal=0;
			
			this.data=[];
			this.data.push({"name":"Current Session Count:","value":"&nbsp;"});
			this.data.push({"name":"ACC:","value":"&nbsp;"});
			this.data.push({"name":"REQ:","value":"&nbsp;"});
			this.data.push({"name":"Data rate:","value":"&nbsp;"});
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
'<div class="blingerName">system</div>',
'<div class="systemBlingerBox" id="blingerBox_{id}" style="width:100%">',
'</div>');
Ext.ComponentMgr.registerType('utgSystemBlinger', Ung.SystemBlinger);


//setting object
Ung.Settings = Ext.extend(Ext.Component, {
    i18n: null,
    node: null,
    tabs: null,
    rpc: null,
    autoEl: 'div',
	initComponent: function(container, position) {
		this.rpc={};
    	this.i18n=Ung.i18nNodeInstances[this.name];
    	Ung.Settings.superclass.initComponent.call(this);
	},
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
	getRpcNode: function() {
		return this.node.nodeContext.node;
	},
	getBaseSettings: function(forceReload) {
		if(forceReload || this.rpc.baseSettings===undefined) {
			this.rpc.baseSettings=this.getRpcNode().getBaseSettings();
		}
		return this.rpc.baseSettings;
	},
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
    //to override
    loadData: function() {
	},
    //to override
	save: function() {
	}
});
Ung.Settings._nodeScripts={};
Ung.Settings.loadNodeScript=function(nodeName,cmpId,callbackFn) {
	main.loadScript('script/'+nodeName+'/settings.js',function() {callbackFn(cmpId);});
};
Ung.Settings._classNames={};
Ung.Settings.getClassName=function(name) {
	var className=Ung.Settings._classNames[name];
	return className===undefined?null:className;
};
Ung.Settings.hasClassName=function(name) {
	return Ung.Settings._classNames[name]!==undefined;
};
Ung.Settings.registerClassName=function(name,className) {
	Ung.Settings._classNames[name]=className;
};

Ung.GridEventLog = Ext.extend(Ext.grid.GridPanel, {
	settingsCmp: null,
	hasRepositories: true,
	eventManagerFn: null,
	enableHdMenu: false,
	recordsPerPage: 20,
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
	initComponent: function(){
    	if(this.title==null) {
    		this.title=i18n._('Event Log');
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
		        fields: this.fields,
		        
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
						store: this.store//,
						//displayInfo: true,
						//displayMsg: 'Displaying topics {0} - {1} of {2}',
						//emptyMsg: "No topics to display"
					})];
        Ung.GridEventLog.superclass.initComponent.call(this);
	},
	onRender : function(container, position) {
		Ung.GridEventLog.superclass.onRender.call(this,container, position);
		this.eventManagerFn.getRepositoryDescs(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
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
    getSelectedRepository: function () {
    	var selObj=document.getElementById('selectRepository_'+this.getId()+'_'+this.settingsCmp.node.tid);
    	var result=null;
    	if(selObj!==null && selObj.selectedIndex>=0) {
    		result = selObj.options[selObj.selectedIndex].value;
    	}
		return result;
    },
	refreshList: function() {
		var selRepository=this.getSelectedRepository();
		if(selRepository!==null) {
			if(this.settingsCmp.rpc.repository[selRepository] === undefined) {
				this.settingsCmp.rpc.repository[selRepository]=this.eventManagerFn.getRepository(selRepository);
			}
			this.settingsCmp.rpc.repository[selRepository].getEvents(function (result, exception) {
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
				var events = result;
				if(this.settingsCmp!==null) {
					this.getStore().proxy.data=events;
					this.getStore().load({params:{start: 0, limit: this.recordsPerPage}});
				}
			}.createDelegate(this));
		}
	}
});


Ung.Window = Ext.extend(Ext.Window, {
	layout:'border',
	modal:true,
	renderTo:'container',
	title:null,
	contentHtml: null,
	buttonsHtml: null,
	closeAction:'hide',
	draggable:false,
	resizable:false,
	subCmps: null,
	sizeToRack: true,
	getContentEl: function() {
		return document.getElementById("window_content_"+this.getId());
	},
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
			'renderTo':'button_help_'+this.getId(),
			'iconCls': 'helpIcon',
			'text': i18n._('Help'),
			'handler': function() {this.helpAction();}.createDelegate(this)
		}));
		this.subCmps.push(new Ext.Button({
			'renderTo':'button_cancel_'+this.getId(),
			'iconCls': 'cancelIcon',
			'text': i18n._('Cancel'),
			'handler': function() {this.cancelAction();}.createDelegate(this)
		}));
		this.subCmps.push(new Ext.Button({
			'renderTo':'button_update_'+this.getId(),
			'iconCls': 'saveIcon',
			'text': i18n._('Update'),
			'handler': function() {this.updateAction();}.createDelegate(this)
		}));
	},
	//to override
	helpAction: function() {
		main.todo();
	},
	//to override
	cancelAction: function() {
		this.hide();
	},
	//to override
	updateAction: function() {
	}
});

Ung.RowEditorWindow = Ext.extend(Ung.UpdateWindow, {
	grid: null,
	inputLines: null,
	contentTemplate: null,
	rowIndex: null,
	sizeToRack: false,
	sizeToGrid: false,
	initComponent: function() {
		if(!this.height && !this.width) {
			this.sizeToGrid=true;
		}
    	if(this.title==null) {
    		this.title=i18n._('Edit');
    	}
    	var contentTemplate=null;
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
Ung.RpcProxy = function(rpcFn){
    Ung.RpcProxy.superclass.constructor.call(this);
    this.rpcFn = rpcFn;
};

Ext.extend(Ung.RpcProxy, Ext.data.DataProxy, {
	setTotalRecords: function(totalRecords) {
		this.totalRecords=totalRecords;
	},
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
				Ext.MessageBox.alert("Failed",exception.message); 
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

Ung.MemoryProxy = function(config){
    Ext.apply(this, config);
    Ung.MemoryProxy.superclass.constructor.call(this);
};

Ext.extend(Ung.MemoryProxy, Ext.data.DataProxy, {
	root: null,
	data: null,
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
Ext.grid.CheckColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
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
        this.sortable = true;
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
Ext.grid.DeleteColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.id){
        this.id = Ext.id();
    }
    if(!this.header) {
        this.header = i18n._("Delete");
    }
    if(!this.width) {
        this.width = 35;
    }
    if(this.fixed==null) {
        this.fixed = true;
    }
    if(this.sortable==null) {
        this.sortable = true;
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

Ung.EditorGrid = Ext.extend(Ext.grid.EditorGridPanel, {
	recordsPerPage: 20,
	minPaginateCount: 60,
	totalRecords: null,
	settingsCmp: null,
	proxyRpcFn: null,
	fields: null,
	hasAdd: true,
	hasEdit: true,
	hasDelete: true,
	emptyRow: null,
	rowEditorInputLines: null,
	sortField: null,
	forcePaginate: false,
	recordJavaClass: null,
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
	},

	genAddedId: function () {
		this.addedId--;
		return this.addedId;
	},
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
	
	initialLoad: function() {
		this.setTotalRecords(this.totalRecords);
		if(!this.isPaginated()) {
			this.getStore().load();
		} else {
			this.getStore().load({params:{start: 0, limit: this.recordsPerPage}});
		}
		
	},
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
	hasChangedData: function () {
		var hasChangedData=false;
		for(id in this.changedData) {
			hasChangedData=true;
			break;
		}
		return hasChangedData;
	},
	updateChangedData: function( record, currentOp) {
		if(!this.hasChangedData()) {
			var cmConfig=this.getColumnModel().config;
			for(i in cmConfig) {
				cmConfig[i].sortable=false;
			}
			//this.getColumnModel().setConfig(cmConfig); //TODO: why this does not work
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
					cd.recData["id"]=""; //ok?
					added.push(cd.recData);
				} else {
					modified.push(cd.recData);
				}
			}
		}
		return [{list: added,"javaClass":"java.util.ArrayList"}, {list: deleted,"javaClass":"java.util.ArrayList"}, {list: modified,"javaClass":"java.util.ArrayList"}];
	}
});
