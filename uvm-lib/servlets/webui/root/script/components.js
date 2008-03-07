Untangle.RPC=function(){}
Untangle.RPC.prototype = {
	callBackWithObject: function() {
		var functionToCall = arguments[0];
		var callBackObj = arguments[1];
		var callBackFunction = arguments[2];
		var args=[];
		var callBackFunctionPlusArg = function(result, exception) {
			callBackFunction(result, exception, callBackObj);
		};
		args.push(callBackFunctionPlusArg);
		for(var i=3;i<arguments.length;i++) {
			args.push(arguments[i]);
		}
		functionToCall.apply(null,args);
	}
}

Untangle.Button = Ext.extend(Ext.Component, {
    'hidden' : false,
    'width':'100%',
    'height':'100%',
    'disabled' : false,
    'text':'',
    'iconSrc':'',
    'clickEvent' : 'click',
	'cls':'',
    initComponent: function(){
        Untangle.Button.superclass.initComponent.call(this);
        this.addEvents(
            "click",
            'mouseover',
            'mouseout');
    },
    // private
    onRender : function(container, position) {
        var templateHTML=Untangle.Button.template.applyTemplate({'width':this.width, 'height':this.height, 'imageSrc':this.imageSrc, 'text':this.text});
        var el= document.createElement("div");
        container.dom.insertBefore(el, position);
        el.className="untangleButton";
        el.innerHTML=templateHTML;
        this.el = Ext.get(el);
        if(this.cls){
            this.el.addClass(this.cls);
        }
        this.el.on(this.clickEvent, this.onClick, this);
        this.el.on("mouseover", this.onMouseOver, this);
        this.el.on("mouseout", this.onMouseOut, this);
        //Untangle.Button.superclass.onRender.call(this,container, position);
    },
    // private
    onMouseOver: function(e){
        if(!this.disabled){
                this.el.addClass("untangleButtonHover");
                this.fireEvent('mouseover', this, e);
        }
    },
    // private
    onMouseOut: function(e){
        this.el.removeClass("untangleButtonHover");
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
Untangle.Button.template = new Ext.Template(
'<table border="0" width="{width}" height="{height}"><tr>',
'<td width="1%" style="text-align: left;vertical-align: middle;"><img src="{imageSrc}" style="vertical-align: middle;"/></td>',
'<td style="text-align: left;vertical-align: middle;padding-left:5px;font-size: 14px;">{text}</td>',
'</tr></table>');
Ext.ComponentMgr.registerType('untangleButton', Untangle.Button);


Untangle.Node = Ext.extend(Ext.Component, {
	    initComponent : function(){
	        Untangle.Node.superclass.initComponent.call(this);
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
        	var iconSrc="images/node/Icon"+this.state+"State28x28.png?"+main.version;
        	document.getElementById('nodeStateIconImg_'+this.getId()).src=iconSrc;
        },
        
        setPowerOn: function(powerOn) {
        	this.powerOn=powerOn;
        	var iconSrc="images/node/IconPower"+(powerOn?"On":"Off")+"State28x28.png?"+main.version;
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
	    	if(this.nodeContext===undefined) {
				this.nodeContext=rpc.nodeManager.nodeContext(this.Tid);
				this.nodeContext.node=this.nodeContext.node();
			}
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
        initSettings: function(force) {
	    	if(this.nodeContext===undefined) {
	    		this.nodeContext=rpc.nodeManager.nodeContext(this.Tid);
				this.nodeContext.node=this.nodeContext.node();
				this.nodeContext.nodeDesc=this.nodeContext.getNodeDesc();
			}
			Ext.Ajax.request({
		        url: "i18n",
		        params:{'nodeClassName':this.nodeContext.nodeDesc.className, 'version':main.version},
				method: 'GET',
				parentId: this.getId(),
				disableCaching: false,
				success: function ( result, request) {
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					var cmp=Ext.getCmp(request.parentId);
					Untangle.i18nNodeInstances[cmp.name]=new Untangle.NodeI18N({"map":i18n.map, "nodeMap":jsonResult});
					cmp.postInitSettings()
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert("Failed", 'Failed loading I18N translations for this node' ); 
				}
			});
			
        },
        postInitSettings: function(force) {
        	if(this.settings) {
        		if(!force) {
        			return;
        		}
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
        
        loadSettings: function(force) {
        	this.settingsClassName=Untangle.Settings.getClassName(this.name);
        	if(!this.settingsClassName) {
	        	Untangle.Settings.loadNodeScript(this.name, this.getId(), function(cmpId) {
	        		var cmp=Ext.getCmp(cmpId);
	        		cmp.settingsClassName=Untangle.Settings.getClassName(cmp.name);
	        		cmp.initSettings(force);
	        	});
	        } else {
	        	this.initSettings(force);
	        }
        },
        
        onRemoveClick: function() {
        	var message="Warning:\n"+this.displayName+"is about to be removed from the rack.\nIts settings will be lost and it will stop processing netwotk traffic.\n\nWould you like to continue removing?"; 
        	if(!confirm(message)) {
        		return;
        	}
        	if(this.settingsWin) {
        		this.settingsWin.hide();
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
	        if(this.settings) {
	        	this.settings.destroy();
	        	this.settings=null;
        	}
        	this.settingsWin.hide();
        },
        
        initBlingers: function () {
        	if(this.blingers!==null) {
        		var nodeBlingers=document.getElementById('nodeBlingers_'+this.getId());
        		for(var i=0;i<this.blingers.length;i++) {
        			var blingerData=this.blingers[i];
        			blingerData.parentId=this.getId();
        			blingerData.id="blinger_"+this.getId()+"_"+i;
       				eval('var blinger=new Untangle.'+blingerData.type+'(blingerData);');
       				blinger.render('nodeBlingers_'+this.getId());
        			//this.blingers[i].id=blinger.id;
        			
        		}
        	}
        },
        
        onRender: function(container, position) {
        	//Untangle.Node.superclass.onRender.call(this, ct, position);
	        var el= document.createElement("div");
	        el.setAttribute('viewPosition',this.viewPosition);
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
        	this.el.addClass("rackNode");
        	this.on('beforedestroy', function() {
        		if(this.settingsWin) {
        			this.settingsWin.destroy();
        			this.settingsWin=null;
        		}
        		if(this.settings) {
        			this.settings.destroy();
        			this.settings=null;
        		}
        	},this);
        	var templateHTML=Untangle.Node.template.applyTemplate({'id':this.getId(),'image':this.image,'displayName':this.displayName,'version':main.version});
	        this.getEl().insertHtml("afterBegin",templateHTML);
       
		    var settingsHTML=Untangle.Node.templateSettings.applyTemplate({'id':this.getId()});
		    var settingsButtonsHTML=Untangle.Node.templateSettingsButtons.applyTemplate({'id':this.getId()});
		    //Ext.MessageBox.alert("Failed",settingsHTML);
		    this.settingsWin=new Ext.Window({
                id: 'settingsWin_'+this.getId(),
                layout:'border',
                modal:true,
                title:'Settings Window',
                closeAction:'hide',
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
			    ]
            });
			this.settingsWin.render('container');
			//this.settingsWin.on("resize", function() {Ext.MessageBox.alert("Resize",123);},this.settingsWin);

			Ext.get('nodePowerIconImg_'+this.getId()).on('click', this.onPowerClick, this);
			var cmp=null;
			cmp=new Ext.ToolTip({
			  html: Untangle.Node.statusTip,
			  target: 'nodeStateIconImg_'+this.getId(),
			  autoWidth: true,
			  autoHeight: true,
			  showDelay: 0,
			  dismissDelay: 0,
			  hideDelay: 0
			});
			cmp=new Ext.ToolTip({
			  html: Untangle.Node.powerTip,
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

Untangle.Node.getCmp=function(nodeId) {
	return Ext.getCmp(nodeId);
};



Untangle.Node.statusTip=['<div style="text-align: left;">',
'The <B>Status Indicator</B> shows the current operating condition of a particular software product.<BR>',
'<font color="#00FF00"><b>Green</b></font> indicates that the product is "on" and operating normally.<BR>',
'<font color="#FF0000"><b>Red</b></font> indicates that the product is "on", but that an abnormal condition has occurred.<BR>',
'<font color="#FFFF00"><b>Yellow</b></font> indicates that the product is saving or refreshing settings.<BR>',
'<b>Clear</b> indicates that the product is "off", and may be turned "on" by the user.</div>'].join('');
Untangle.Node.powerTip='The <B>Power Button</B> allows you to turn a product "on" and "off".';
Untangle.Node.template = new Ext.Template(
'<div class="nodeImage"><img src="{image}"/></div>',
'<div class="nodeLabel">{displayName}</div><div class="nodeBlingers" id="nodeBlingers_{id}"></div>',
'<div class="nodeStateIcon"><img id="nodeStateIconImg_{id}" src="images/node/IconOffState28x28.png?{version}"></div>',
'<div class="nodePowerIcon"><img id="nodePowerIconImg_{id}" src="images/node/IconPowerOffState28x28.png?{version}"></div>',
'<div id="nodePowerOnHint_{id}" class="nodePowerOnHint"><img src="images/node/IconPowerOnHint100.png?{version}"></div>',
'<div class="nodeSettingsButton" id="nodeSettingsButton_{id}"></div>',
'<div class="nodeHelpButton" id="nodeHelpButton_{id}"></div>');

Untangle.Node.templateSettings=new Ext.Template(
'<div class="nodeSettingsContent" id="settings_{id}"></div>');
Untangle.Node.templateSettingsButtons=new Ext.Template(
'<div class="nodeRemoveButton" id="nodeRemoveButton_{id}"></div>',
'<div class="nodeCancelButton" id="nodeCancelButton_{id}"></div>',
'<div class="nodeSaveButton" id="nodeSaveButton_{id}"></div>');

Ext.ComponentMgr.registerType('untangleNode', Untangle.Node);


Untangle.BlingerManager = {
	updateTime: 5000, //update interval in millisecond
	started: false,
	intervalId: null,
	cycleCompleted:true,
	
	start: function() {
		this.stop();
		this.intervalId=window.setInterval("Untangle.BlingerManager.getNodesStats()",this.updateTime);
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
			var nodeCmp=Untangle.Node.getCmp(main.nodes[i].tid);
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
						Untangle.BlingerManager.cycleCompleted=true;
					});
					return;
				}
				try {
					var allNodeStats=result;
					for(var i=0;i<main.nodes.length;i++) {
						var nodeCmp=Untangle.Node.getCmp(main.nodes[i].tid);
						if(nodeCmp && nodeCmp.isRunning()) {
							nodeCmp.stats=allNodeStats.map[main.nodes[i].tid];
							nodeCmp.updateBlingers();
						}
					}
					Untangle.BlingerManager.cycleCompleted=true;
				  } catch(err) {
					Untangle.BlingerManager.cycleCompleted=true;
					throw err;
				  }
			});
		}	
	}
};

Untangle.ActivityBlinger = Ext.extend(Ext.Component, {
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
			var templateHTML=Untangle.ActivityBlinger.template.applyTemplate({'id':this.getId()});
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
        		this.decays[i]=Untangle.ActivityBlinger.decayValue(newValue, this.lastValues[i],this.decays[i]);
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
Untangle.ActivityBlinger.template = new Ext.Template(
'<div class="blingerName">activity</div>',
'<div class="blingerBox" id="blingerBox_{id}" style="width:60px;">',
'</div>');
Untangle.ActivityBlinger.decayFactor=Math.pow(0.94,Untangle.BlingerManager.updateTime/1000);
Untangle.ActivityBlinger.decayValue = function(newValue, lastValue, decay) {
	if(lastValue!==null && newValue!=lastValue) {
		decay=98;
	} else {
		decay=decay*Untangle.ActivityBlinger.decayFactor;
	}
	return decay;
};
Ext.ComponentMgr.registerType('untangleActivityBlinger', Untangle.ActivityBlinger);

Untangle.SystemBlinger = Ext.extend(Ext.Component, {
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
			var templateHTML=Untangle.SystemBlinger.template.applyTemplate({'id':this.getId()});
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
        	var dataRate=(this.byteCountCurrent - this.byteCountLast)/Untangle.BlingerManager.updateTime;
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
Untangle.SystemBlinger.template = new Ext.Template(
'<div class="blingerName">system</div>',
'<div class="systemBlingerBox" id="blingerBox_{id}" style="width:100%">',
'</div>');
Ext.ComponentMgr.registerType('untangleSystemBlinger', Untangle.SystemBlinger);


//setting object
Untangle.Settings = Ext.extend(Ext.Component, {
    i18n: null,
    node:null,
    tabs: null,
    rpc: null,
	onRender: function(container, position) {
	    var el= document.createElement("div");
	    container.dom.insertBefore(el, position);
        this.el = Ext.get(el);
    	this.i18n=Untangle.i18nNodeInstances[this.name];
    	this.rpc={};
    	this.rpc.repository={};
    	if(this.node.nodeContext.node.eventManager===undefined) {
			this.node.nodeContext.node.eventManager=this.node.nodeContext.node.getEventManager();
		}
		this.rpc.node = this.node.nodeContext.node;
		this.rpc.eventManager=this.node.nodeContext.node.eventManager;
	},
	buldTabPanel: function(itemsArray) {
		this.tabs = new Ext.TabPanel({
	        renderTo: this.getEl().id,
	        width: 690,
	        height: 400,
	        activeTab: 0,
	        frame: true,
	        //deferredRender: false,
	        items: itemsArray
	    });
	},
	getRpcNode: function() {
		return this.node.nodeContext.node;
	},
	getEventManager: function () {
		if(this.node.nodeContext.node.eventManager===undefined) {
			this.node.nodeContext.node.eventManager=this.node.nodeContext.node.getEventManager();
		}
		return this.node.nodeContext.node.eventManager;
	}
	
});
Untangle.Settings._nodeScripts={};
Untangle._hasResource={};
Untangle.Settings.loadNodeScript=function(nodeName,cmpId,callbackFn) {
	main.loadScript('script/'+nodeName+'/settings.js?'+main.version,function() {callbackFn(cmpId);});
};
Untangle.Settings._classNames={};
Untangle.Settings.getClassName=function(name) {
	var className=Untangle.Settings._classNames[name];
	return className===undefined?null:className;
};
Untangle.Settings.hasClassName=function(name) {
	return Untangle.Settings._classNames[name]!==undefined;
};
Untangle.Settings.registerClassName=function(name,className) {
	Untangle.Settings._classNames[name]=className;
};

Ext.ComponentMgr.registerType('untangleSettings', Untangle.Settings);


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
Ext.grid.RemoveColumn = function(config){
    Ext.apply(this, config);
    if(!this.id){
        this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.RemoveColumn.prototype ={
    init: function(grid){
        this.grid = grid;
        this.grid.on('render', function(){
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown: function(e, t){
        if(t.className && t.className.indexOf('removeRow') != -1){
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.grid.store.remove(record);
        }
    },

    renderer: function(value, metadata, record){
        return '<div class="removeRow">&nbsp;</div>';
    }
};
Untangle.GridEventLog = Ext.extend(Ext.grid.GridPanel, {
	settingsCmp: null,
	hasRepositories: true,
	repositoryDescsFn: null,
	enableHdMenu: false,
	initComponent: function(){
    	if(this.title==null) {
    		this.title=this.settingsCmp.i18n._('Event Log');
    	}
    	if(this.repositoryDescsFn==null) {
    		this.repositoryDescsFn=this.settingsCmp.rpc.eventManager.getRepositoryDescs;
    	}
    	
        this.bbar=	[{
        				xtype:'tbtext',
						text:'<span id="boxRepository_'+this.getId()+'_'+this.settingsCmp.node.tid+'"></span>'},
					{
						xtype: 'tbbutton',
			            text: this.settingsCmp.i18n._('Refresh'),
			            tooltip: this.settingsCmp.i18n._('Refresh'),
						iconCls: 'iconRefresh',
						handler: function() {this.refreshList();}.createDelegate(this)
					}];
        Untangle.GridEventLog.superclass.initComponent.call(this);
	},
	onRender : function(container, position) {
		Untangle.GridEventLog.superclass.onRender.call(this,container, position);
		this.repositoryDescsFn(function (result, exception) {
			if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
			if(this.settingsCmp) {
				this.settingsCmp.rpc.repositoryDescs=result;
				var out=[];
				out.push('<select id="selectRepository_'+this.getId()+'_'+this.settingsCmp.node.tid+'">');
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
				this.settingsCmp.rpc.repository[selRepository]=this.settingsCmp.rpc.eventManager.getRepository(selRepository);
			}
			this.settingsCmp.rpc.repository[selRepository].getEvents(function (result, exception) {
				if(exception) {Ext.MessageBox.alert("Failed",exception.message); return;}
				var events = result;
				if(this.settingsCmp!==null) {
					this.getStore().loadData(events.list);
				}
			}.createDelegate(this));
		}
	}
});

Untangle.RowEditorWindow = Ext.extend(Ext.Window, {
	key: null,
	settingsCmp: null,
	grid: null,
	inputLines: null,
	contentTemplate: null,
	title: null,
	
	rowIndex: null,
	closeAction: 'hide',
	autoCreate: true,                
	layout:'border',
	modal: true,
	width: 400,
	height: 300,
	draggable: false,
	resizable: false,
	initContent: function() {
		var cmp=null;
		cmp=new Ext.Button({
			'renderTo':'rowEditor_help_'+this.settingsCmp.node.tid+'_'+this.key,
			'iconCls': 'helpIcon',
			'text': this.settingsCmp.i18n._('Help'),
			'handler': function() {Ext.MessageBox.alert("TODO","Implement Help Page");}.createDelegate(this)
		});
		cmp=new Ext.Button({
			'renderTo':'rowEditor_cancel_'+this.settingsCmp.node.tid+'_'+this.key,
			'iconCls': 'cancelIcon',
			'text': this.settingsCmp.i18n._('Cancel'),
			'handler': function() {this.hide();}.createDelegate(this)
		});
		cmp=new Ext.Button({
			'renderTo':'rowEditor_update_'+this.settingsCmp.node.tid+'_'+this.key,
			'iconCls': 'saveIcon',
			'text': this.settingsCmp.i18n._('Update'),
			'handler': function() {this.updateData();}.createDelegate(this)
		});
	},
	listeners: {
		'show': {
			fn: function(cmp) {
				var objPosition=cmp.grid.getPosition();
				cmp.setPosition(objPosition);
				//var objSize=grid.getSize();
				//this.gridProtocolList.rowEditor.setSize(objSize);
			}
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
    					document.getElementById('field_'+this.settingsCmp.node.tid+'_'+this.key+'_'+inputLine.name).value=record.data[inputLine.name];
    					break;
    				case "checkbox":
    					document.getElementById('field_'+this.settingsCmp.node.tid+'_'+this.key+'_'+inputLine.name).checked=record.data[inputLine.name];
    					break;
    			}
			}
		}
	},
	updateData: function() {
		if(this.rowIndex!==null) {
			var rec=this.grid.getStore().getAt(this.rowIndex);
			if(this.inputLines) {
	    		for(var i=0;i<this.inputLines.length;i++) {
	    			var inputLine=this.inputLines[i];
	    			switch(inputLine.type) {
	    				case "text":
	    				case "textarea":
	    					rec.set(inputLine.name, document.getElementById('field_'+this.settingsCmp.node.tid+'_'+this.key+'_'+inputLine.name).value);
	    					break;
	    				case "checkbox":
	    					rec.set(inputLine.name, document.getElementById('field_'+this.settingsCmp.node.tid+'_'+this.key+'_'+inputLine.name).checked);
	    					break;
	    			}
				}			
			}
		}
		this.hide();
	},

	initComponent: function() {
    	if(this.title==null) {
    		this.title=this.settingsCmp.i18n._('Edit');
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
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><input type="text" id="field_{tid}_{key}_'+inputLine.name+'" style="'+inputLine.style+'"/></span></div>')
    					break;
    				case "checkbox":
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><input type="checkbox" id="field_{tid}_{key}_'+inputLine.name+'"/></span></div>')
    					break;
    				case "textarea":
    					if(inputLine.style==null) {
    						inputLine.style="width:200px;height:60px;"
    					}
    					contentArr.push('<div class="inputLine"><span class="label">'+inputLine.label+':</span><span class="formw"><textarea type="text" id="field_{tid}_{key}_'+inputLine.name+'" style="'+inputLine.style+'"></textarea></span></div>')
    					break;
    			}
    		}
    		contentTemplate=new Ext.Template(contentArr);
    	} else {
    		contentTemplate=this.contentTemplate;
    	}
    	
    	
    	this.items= [{
				region:"center",
				html: contentTemplate.applyTemplate({'tid':this.settingsCmp.node.tid, 'key':this.key}),
				border: false,
				autoScroll: true,
				cls: 'windowBackground',
				bodyStyle: 'background-color: transparent;'
			}, 
	    	{
		    	region: "south",
		    	html: Untangle.RowEditorWindow.template.applyTemplate({'tid':this.settingsCmp.node.tid, 'key':this.key}),
		        border: false,
		        height:40,
		        cls: 'windowBackground',
		        bodyStyle: 'background-color: transparent;'
	    	}];
        Untangle.RowEditorWindow.superclass.initComponent.call(this);

    },
    onRender: function(container, position) {
    	Untangle.RowEditorWindow.superclass.onRender.call(this,container, position);
    },
    afterRender: function() {
    	Untangle.RowEditorWindow.superclass.afterRender.call(this);
    	//this.initContent();
    }
    
	
});
Untangle.RowEditorWindow.template=new Ext.Template(
'<div class="rowEditorHelp" id="rowEditor_help_{tid}_{key}"></div>',
'<div class="rowEditorCancel" id="rowEditor_cancel_{tid}_{key}"></div>',
'<div class="rowEditorUpdate" id="rowEditor_update_{tid}_{key}"></div>');
/*
Untangle.RightWindow = Ext.extend(Ext.Window, {
	layout:'border',
	modal:true,
	title:'Window',
	closeAction:'hide',
	autoCreate:true,
	width:740,
	height:690,
	draggable:false,
	resizable:false,
	initComponent: function() {
    	if(this.title==null) {
    		this.title='';
    	}
		items: [{
			region:"center",
			html: Untangle.RightWindow.template.applyTemplate({'id':this.getId()}),,
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
		}]
        Untangle.RightWindow.superclass.initComponent.call(this);

    },

});
Untangle.RightWindow.template = new Ext.Template(
'<div class="nodeSettingsContent" id="windowContent_{id}"></div>');
*/

Untangle.RpcProxy = function(rpcFn){
    Untangle.RpcProxy.superclass.constructor.call(this);
    this.rpcFn = rpcFn;
};

Ext.extend(Untangle.RpcProxy, Ext.data.DataProxy, {
	setTotalRecords: function(totalRecords) {
		this.totalRecords=totalRecords;
	},
    load : function(params, reader, callback, scope, arg) {
    	var obj={}
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
		}.createDelegate(obj), params.start, params.limit,sortColumns);
	}

});