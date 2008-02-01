Ext.namespace('Ext.untangle');

Ext.untangle.Button = Ext.extend(Ext.Component, {
    'hidden' : false,
    'width':'100%',
    'height':'100%',
    'disabled' : false,
    'text':'',
    'iconSrc':'',
    'clickEvent' : 'click',
	'cls':'',
    initComponent: function(){
        Ext.untangle.Button.superclass.initComponent.call(this);
        this.addEvents(
            "click",
            'mouseover',
            'mouseout'
        );
    },
    // private
    onRender : function(container, position) {
        if(!this.template){
            this.template = Ext.untangle.Button.template;
        }
       
        var templateHTML=this.template.applyTemplate({'width':this.width, 'height':this.height, 'imageSrc':this.imageSrc, 'text':this.text});
        var el= document.createElement("div");
        container.dom.insertBefore(el, position);
        el.className="untangleButton";
        el.innerHTML=templateHTML;
        this.el = Ext.get(el);
        if(this.cls){
            this.el.addClass(this.cls);
        }
        this.el.on(this.clickEvent, this.onClick, this)
        this.el.on("mouseover", this.onMouseOver, this);
        this.el.on("mouseout", this.onMouseOut, this);
        //Ext.untangle.Button.superclass.onRender.call(this,container, position);
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
        if(e.button != 0){
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
Ext.untangle.Button.template = new Ext.Template(
'<table border="0" width="{width}" height="{height}"><tr>',
'<td width="1%" style="text-align: left;vertical-align: middle;"><img src="{imageSrc}" style="vertical-align: middle;"/></td>',
'<td style="text-align: left;vertical-align: middle;padding-left:5px;font-size: 14px;">{text}</td>',
'</tr></table>'
);
Ext.ComponentMgr.registerType('untangleButton', Ext.untangle.Button);


Ext.untangle.Node = Ext.extend(Ext.Component, {
	    initComponent : function(){
	        Ext.untangle.Node.superclass.initComponent.call(this);
	    },
	    hidden: false,
	    disabled: false,

        name: "",
        displayName: "",
        image: "",
        state: "", // On, Off, Attention, Stopped
        powerOn: false,
        runState: '', // RUNNING, INITIALIZED
        helpLink: "",
        webContext: "",
        viewPosition: "",
        settings: null,
        settingsClassName: null,
        stats: null, //last blinger data received
        
        setState: function(state) {
        	this.state=state;
        	var iconSrc="images/node/Icon"+this.state+"State28x28.png";
        	document.getElementById('nodeStateIconImg_'+this.id).src=iconSrc;
        },
        
        setPowerOn: function(powerOn) {
        	this.powerOn=powerOn;
        	var iconSrc="images/node/IconPower"+(powerOn?"On":"Off")+"State28x28.png";
        	document.getElementById('nodePowerIconImg_'+this.id).src=iconSrc;
        	document.getElementById('nodePowerOnHint_'+this.id).style.display=this.powerOn?"none":"";
        },
        
        updateBlingers: function () {
        	if(this.blingers!=null) {
        		if(this.powerOn) {
	        		for(var i=0;i<this.blingers.length;i++) {
	        			Ext.getCmp(this.blingers[i].id).update(this.stats);
	        		}
	        	} else {
	        		this.resetBlingers();
	        	}
        	}
		},
		resetBlingers: function () {
        	if(this.blingers!=null) {
        		for(var i=0;i<this.blingers.length;i++) {
        			Ext.getCmp(this.blingers[i].id).reset();
        		}
        	}
        },
        onPowerClick: function() {
        	this.setPowerOn(!this.powerOn);
        	this.setState("Attention");
			Ext.Ajax.request({
		        url: MainPage.rackUrl,
		        params:{'action':this.powerOn?"startNode":"stopNode",'nodeName':this.name,'nodeId':this.tid},
				method: 'POST',
				'parentId':this.id,
				success: function ( result, request) {
					var cmp=Ext.getCmp(request.parentId);
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						cmp.setState("Attention");
						Ext.MessageBox.alert('Failed', jsonResult.msg);
					} else {
						cmp.setState(cmp.powerOn?"On":"Off");
					}
					if(!cmp.powerOn) {
						cmp.resetBlingers();
					}
				},
				failure: function ( result, request) { 
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
				} 
			});	
        },

        onHelpClick: function () {
        	if(this.helpLink!=null && this.helpLink.length>0) {
        		window.open(this.helpLink);
        	}
        },
        
        onSettingsClick: function() {
        	this.settingsWin.show();
        	this.settingsWin.setPosition(document.getElementById('racks').offsetLeft,1);
        	this.loadSettings();
        },
        
        initSettings: function(force) {
        	if(this.settings) {
        		if(!force) {
        			return;
        		}
        		this.settings.destroy();
        		this.settings=null;
        	}
       		if(this.settingsClassName!=null) {
        		eval('this.settings=new '+this.settingsClassName+'({\'tid\':this.tid,\'name\':this.name});');
        		this.settings.render('settings_'+this.id);
        		this.settings.loadData();
        	} else {
        		alert("Error: There is no settings class for the node '"+this.name+"'");
        	}
        	
        },
        
        loadSettings: function(force) {
        	this.settingsClassName=Ext.untangle.Settings.getClassName(this.name);
        	if(!this.settingsClassName) {
	        	Ext.untangle.Settings.loadNodeScript(this.webContext, this.id, function(cmpId) {
	        		var cmp=Ext.getCmp(cmpId);
	        		cmp.settingsClassName=Ext.untangle.Settings.getClassName(cmp.name);
	        		cmp.initSettings(force);
	        	});
	        } else {
	        	this.initSettings(force);
	        }
        },
        
        onRemoveClick: function() {
        	var message="Warning:\n"+this.displayName+"is about to be removed from the rack.\nIts settings will be lost and it will stop processing netwotk traffic.\n\nWould you like to continue removing?" 
        	if(!confirm(message)) {
        		return;
        	}
        	if(this.settingsWin) {
        		this.settingsWin.hide();
        	}
        	this.setState("Attention");
			Ext.Ajax.request({
				url: MainPage.rackUrl,
				params: {'action':'removeFromRack','nodeId':this.tid,'installName':this.name},
				method: 'GET',
				parentId: this.id,
				success: function ( result, request) { 
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.MessageBox.alert('Failed', jsonResult.msg); 
					} else {
						//alert("Rack instaled: TODO: refresh rack node list, enable button in myApps");
						//Ext.getCmp(request.parentId).settingsWin.hide()
						var cmp=Ext.getCmp(request.parentId);
						cmp.destroy();
						cmp=null;
						Ext.getCmp('myAppButton_'+request.params.installName).enable();
						for(var i=0;i<MainPage.nodes.length;i++) {
							if(request.params.installName==MainPage.nodes[i].name) {
								MainPage.nodes.splice(i,1);
								break;
							} 
						}
						MainPage.updateSeparator();
					}
				},
				failure: function ( result, request) { 
					
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date); 
				} 
			});	
        },
        
        onSaveClick: function() {
        	//alert("Save Settings");
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
        	if(this.blingers!=null) {
        		var nodeBlingers=document.getElementById('nodeBlingers_'+this.id);
        		for(var i=0;i<this.blingers.length;i++) {
        			var blingerData=this.blingers[i];
        			blingerData.parentId=this.id;
        			blingerData.id="blinger_"+this.id+"_"+i;
       				eval('var blinger=new Ext.untangle.'+blingerData.type+'(blingerData);');
       				blinger.render('nodeBlingers_'+this.id);
        			//this.blingers[i].id=blinger.id;
        			
        		}
        	}
        },
        
        onRender: function(container, position) {
        	//Ext.untangle.Node.superclass.onRender.call(this, ct, position);
	        var el= document.createElement("div");
	        el.setAttribute('viewPosition',this.viewPosition);
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);

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
        	var templateHTML=Ext.untangle.Node.template.applyTemplate({'id':this.id,'image':this.image,'displayName':this.displayName});
	        this.getEl().insertHtml("afterBegin",templateHTML);
       
		    var settingsHTML=Ext.untangle.Node.templateSettings.applyTemplate({'id':this.id});
		    //
		    //alert(settingsHTML);
		    this.settingsWin=new Ext.Window({
                id: 'settingsWin_'+this.id,
                layout:'fit',
                modal:true,
                title:'Settings Window',
                closeAction:'hide',
                autoCreate:true,                
                width:740,
                height:690,
                draggable:false,
                resizable:false,
	            items: {
			        html: settingsHTML,
			        border: false
			    }
            });
			this.settingsWin.render('container');

			Ext.get('nodePowerIconImg_'+this.id).on('click', this.onPowerClick, this);
			var tip = new Ext.ToolTip({
			  html:Ext.untangle.Node.statusTip,
			  target: 'nodeStateIconImg_'+this.id,
			  autoWidth: true,
			  autoHeight: true,
			  showDelay: 0,
			  dismissDelay: 0,
			  hideDelay: 0
			});
			var tip = new Ext.ToolTip({
			  html:Ext.untangle.Node.powerTip,
			  target: 'nodePowerIconImg_'+this.id,
			  autoWidth: true,
			  autoHeight: true,
			  showDelay: 0,
			  dismissDelay: 0,
			  hideDelay: 0
			});
			new Ext.Button({
				'parentId':this.id,
		        'iconCls': 'nodeSettingsIcon',
				'renderTo':'nodeSettingsButton_'+this.id,
		        'text': 'Show Settings',
		        'handler': function() {Ext.getCmp(this.parentId).onSettingsClick()}
	        });
			new Ext.Button({
				'parentId':this.id,
		        'iconCls': 'nodeHelpIcon',
				'renderTo':'nodeHelpButton_'+this.id,
		        'text': 'Help',
		        'handler': function() {Ext.getCmp(this.parentId).onHelpClick()}
	        });
			new Ext.Button({
				'parentId':this.id,
				'iconCls': 'nodeRemoveIcon',
				'renderTo':'nodeRemoveButton_'+this.id,
		        'text': 'Remove',
		        'handler': function() {Ext.getCmp(this.parentId).onRemoveClick()}
	        });
	        /*
			new Ext.Button({
				'parentId':this.id,
				'iconCls': 'nodeSettingsHideIcon',
				'renderTo':'nodeSettingsHideButton_'+this.id,
		        'text': 'Hide Settings',
		        'handler': function() {Ext.getCmp(this.parentId).settingsWin.hide();}
	        });
	        */
			new Ext.Button({
				'parentId':this.id,
		        'iconCls': 'cancelIcon',
				'renderTo':'nodeCancelButton_'+this.id,
		        'text': 'Cancel',
		        'handler': function() {Ext.getCmp(this.parentId).onCancelClick()}
	        });
			new Ext.Button({
				'parentId':this.id,
		        'iconCls': 'saveIcon',
				'renderTo':'nodeSaveButton_'+this.id,
		        'text': 'Save',
		        'handler': function() {Ext.getCmp(this.parentId).onSaveClick()}
	        });
	        this.setPowerOn(this.runState=="RUNNING")
	        this.setState((this.runState=="RUNNING")?"On":"Off")
        	this.initBlingers();
        }
});

Ext.untangle.Node.getCmp=function(nodeId) {
	return Ext.getCmp(nodeId);
}

Ext.untangle.BlingerManager = {
	updateTime: 3000, //update interval in millisecond
	started: false,
	intervalId: null,
	cycleCompleted:true,
	
	start: function() {
		this.stop();
		this.intervalId=window.setInterval("Ext.untangle.BlingerManager.getNodesStats()",this.updateTime);
	},
	
	stop: function() {
		if(this.intervalId!=null) {
			window.clearInterval(this.intervalId);
		}
		this.cycleCompleted=true;
	},
	
	getActiveNodes: function() {
		var activeNodes=[];
		for(var i=0;i<MainPage.nodes.length;i++) {
			if(true) {
				activeNodes.push({"nodeId":MainPage.nodes[i].tid,"nodeName":MainPage.nodes[i].name});
			}
		}
		return activeNodes;
	},
	
	getNodesStats: function() {
		if(!this.cycleCompleted) {
			return;
		}
		var activeNodes=this.getActiveNodes();
		if(activeNodes.length>0) {
			this.cycleCompleted=false;
			Ext.Ajax.request({
		        url: MainPage.rackUrl,
		        params:{"action":"nodesStats", "nodes": Ext.encode(activeNodes)},
				method: 'POST',
				success: function ( result, request) {
				  try {
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.MessageBox.alert('Failed', jsonResult.msg);
					} else {
						for(var i=0;i<jsonResult.data.length;i++) {
							var nodeStats=jsonResult.data[i];
							var nodeCmp=Ext.untangle.Node.getCmp(nodeStats.nodeId);
							if(nodeCmp) {
								nodeCmp.stats=nodeStats.stats;
								nodeCmp.updateBlingers()
							}
						} 
					}
					Ext.untangle.BlingerManager.cycleCompleted=true;
				  } catch(err) {
					Ext.untangle.BlingerManager.cycleCompleted=true;
					throw err;
				  }				
				},
				failure: function ( result, request) {
					Ext.MessageBox.alert('Failed', 'Successfully posted form: '+result.date);
					Ext.untangle.BlingerManager.cycleCompleted=true; 
				} 
			});
		}	
	}
}

Ext.untangle.Node.statusTip=['<div style="text-align: left;">',
'The <B>Status Indicator</B> shows the current operating condition of a particular software product.<BR>',
'<font color="#00FF00"><b>Green</b></font> indicates that the product is "on" and operating normally.<BR>',
'<font color="#FF0000"><b>Red</b></font> indicates that the product is "on", but that an abnormal condition has occurred.<BR>',
'<font color="#FFFF00"><b>Yellow</b></font> indicates that the product is saving or refreshing settings.<BR>',
'<b>Clear</b> indicates that the product is "off", and may be turned "on" by the user.</div>'].join('');
Ext.untangle.Node.powerTip='The <B>Power Button</B> allows you to turn a product "on" and "off".';
Ext.untangle.Node.template = new Ext.Template(
'<div class="rackNode"><div class="nodeImage"><img src="{image}"/></div>',
'<div class="nodeLabel">{displayName}</div><div class="nodeBlingers" id="nodeBlingers_{id}"></div>',
'<div class="nodeStateIcon"><img id="nodeStateIconImg_{id}" src=""></div>',
'<div class="nodePowerIcon"><img id="nodePowerIconImg_{id}" src=""></div>',
'<div id="nodePowerOnHint_{id}" class="nodePowerOnHint"><img src="images/node/IconPowerOnHint100.png"></div>',
'<div class="nodeSettingsButton" id="nodeSettingsButton_{id}"></div>',
'<div class="nodeHelpButton" id="nodeHelpButton_{id}"></div>',
'</div>'
);

Ext.untangle.Node.templateSettings=new Ext.Template(
'<div id="nodeSettings_{id}" class="nodeSettings">',
'<div class="nodeSettingsContent" id="settings_{id}"></div>',
'<div class="nodeRemoveButton" id="nodeRemoveButton_{id}"></div>',
'<div class="nodeSettingsHideButton" id="nodeSettingsHideButton_{id}"></div>',
'<div class="nodeCancelButton" id="nodeCancelButton_{id}"></div>',
'<div class="nodeSaveButton" id="nodeSaveButton_{id}"></div>',
'</div>'
);

Ext.ComponentMgr.registerType('untangleNode', Ext.untangle.Node);


Ext.untangle.ActivityBlinger = Ext.extend(Ext.Component, {
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
			var templateHTML=Ext.untangle.ActivityBlinger.template.applyTemplate({'id':this.id});
			el.innerHTML=templateHTML;
			this.lastValues=[];
			this.decays=[];
        	if(this.bars!=null) {
        		var out=[];
        		for(var i=0;i<this.bars.length;i++) {
        			var bar=this.bars[i];
        			var top=3+i*15;
        			this.lastValues.push(null);
        			this.decays.push(0);
        			out.push('<div class="blingerText activityBlingerText" style="top:'+top+'px;">'+bar+'</div>');
        			out.push('<div class="activityBlingerBar" style="top:'+top+'px;width:0px;display:none;" id="activityBar_'+this.id+'_'+i+'"></div>');
        		}
        		document.getElementById("blingerBox_"+this.id).innerHTML=out.join("");
        	}
        },
        
        update: function(stats) {
        	for(var i=0;i<this.bars.length;i++) {
        		var top=3+i*15;
        		var bar=this.bars[i];
        		var newValue=stats.counters[6+i];
        		this.decays[i]=Ext.untangle.ActivityBlinger.decayValue(newValue, this.lastValues[i],this.decays[i]);
        		this.lastValues[i]=newValue;
        		var barPixelWidth=Math.floor(this.decays[i]*0.6);
        		var barDiv=document.getElementById('activityBar_'+this.id+'_'+i);
        		barDiv.style.width=barPixelWidth+"px";
        		barDiv.style.display=(barPixelWidth==0)?"none":"";
        	}
        },
        reset: function() {
       		for(var i=0;i<this.bars.length;i++) {
       			this.lastValues[i]=null;
       			this.decays[i]=0;
       			var barDiv=document.getElementById('activityBar_'+this.id+'_'+i);
       			barDiv.style.width="0px";
       			barDiv.style.display="none";
       		}
        }
        
});
Ext.untangle.ActivityBlinger.template = new Ext.Template(
'<div class="blingerName">activity</div>',
'<div class="blingerBox" id="blingerBox_{id}" style="width:60px;">',
'</div>');
Ext.untangle.ActivityBlinger.decayFactor=Math.pow(0.94,Ext.untangle.BlingerManager.updateTime/1000);
Ext.untangle.ActivityBlinger.decayValue = function(newValue, lastValue, decay) {
	if(lastValue!=null && newValue!=lastValue) {
		decay=98;
	} else {
		decay=decay*Ext.untangle.ActivityBlinger.decayFactor;
	}
	return decay;
}
Ext.ComponentMgr.registerType('untangleActivityBlinger', Ext.untangle.ActivityBlinger);

Ext.untangle.SystemBlinger = Ext.extend(Ext.Component, {
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
			var templateHTML=Ext.untangle.SystemBlinger.template.applyTemplate({'id':this.id});
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
        	if(this.data!=null) {
        		var out=[];
        		for(var i=0;i<this.data.length;i++) {
        			var dat=this.data[i];
        			var top=3+i*15;
        			out.push('<div class="blingerText systemBlingerName" style="top:'+top+'px;" id="systemName_'+this.id+'_'+i+'">'+dat.name+'</div>');
        			out.push('<div class="blingerText systemBlingerValue" style="top:'+top+'px;" id="systemValue_'+this.id+'_'+i+'">'+dat.value+'</div>');
        		}
        		document.getElementById("blingerBox_"+this.id).innerHTML=out.join("");
        	}
        },
        
        update: function(stats) {
        	// UPDATE COUNTS
        	this.sessionCountCurrent=stats.tcpSessionCount+stats.udpSessionCount;
        	this.sessionCountTotal=stats.tcpSessionTotal+stats.udpSessionTotal;
        	this.sessionRequestTotal=stats.tcpSessionRequestTotal+stats.udpSessionRequestTotal;
            this.byteCountCurrent = stats.c2tBytes + stats.s2tBytes;
            // (RESET COUNTS IF NECESSARY)
            if( (this.byteCountLast == 0) || (this.byteCountLast > this.byteCountCurrent) )
                this.byteCountLast = this.byteCountCurrent;
            if( (this.sessionRequestLast == 0) || (this.sessionRequestLast > this.sessionRequestTotal) )
                this.sessionRequestLast = this.sessionRequestTotal;
        	var acc=this.sessionCountTotal;
        	var req=this.sessionRequestTotal;
        	var dataRate=(this.byteCountCurrent - this.byteCountLast)/Ext.untangle.BlingerManager.updateTime;
        	this.data[0].value=this.sessionCountCurrent;
        	this.data[1].value=acc;
        	this.data[2].value=req;
        	this.data[3].value = dataRate.toFixed(2)+"/KBPs";
        	if(this.data!=null) {
        		for(var i=0;i<this.data.length;i++) {
        			var valueDiv=document.getElementById('systemValue_'+this.id+'_'+i);
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
        	
        	if(this.data!=null) {
        		for(var i=0;i<this.data.length;i++) {
        			this.data[i].value="&nbsp;";
        			var valueDiv=document.getElementById('systemValue_'+this.id+'_'+i);
        			valueDiv.innerHTML=this.data[i].value;
        		}
        	}
        }
});
Ext.untangle.SystemBlinger.template = new Ext.Template(
'<div class="blingerName">system</div>',
'<div class="systemBlingerBox" id="blingerBox_{id}" style="width:100%">',
'</div>');
Ext.ComponentMgr.registerType('untangleSystemBlinger', Ext.untangle.SystemBlinger);


/*
Ext.untangle.SessionsBlinger = Ext.extend(Ext.Component, {
        _stackLength1: 60,
        _history1: null,
        _stackLength2: 60,
        _history2: null,
        _color1: "#8080FF",
        _color2: "#804080",
        onRender: function (container, position) {
	        if(!this.template){
	            this.template = Ext.untangle.SessionsBlinger.template;
	        }
	        var el= document.createElement("div");
	        el.className="activityBlinger";
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
        	this.id=Ext.id(this);
			var templateHTML=this.template.applyTemplate({'id':this.id});
			el.innerHTML=templateHTML;

        	this._history1=[];
        	for(var i=0;i<this._stackLength1;i++) {
        		this._history1.push(0);
        	}
        	this._history2=[];
        	for(var i=0;i<this._stackLength2;i++) {
        		this._history2.push(0);
        	}
        },
        _lastValue1: 0,
        _lastValue2: 0,
        _randomValue1: function () {
        	var prob=Math.floor(Math.random()*100);
        	var genVal=0;
        	if(this._lastValue1!=0) {
        		if(prob<15) {
        			genVal=0;
        		} else if(prob<75) {
        			genVal=this._lastValue1;
        		} else {
        			genVal=Math.floor(Math.random()*1000)*3/1000;
        		}
        	} else {
	        	if(prob<83) {
	        		genVal=0;
	        	} else {
	        		genVal=Math.floor(Math.random()*1000)*3/1000;
	        	}
        	} 
        	this._lastValue1=genVal;
        	return genVal;
        },
        _randomValue2: function () {
        	var prob=Math.floor(Math.random()*100);
        	var genVal=0;
        	if(this._lastValue2!=0) {
        		if(prob<15) {
        			genVal=0;
        		} else if(prob<75) {
        			genVal=this._lastValue1;
        		} else {
        			genVal=Math.floor(Math.random()*1000)*3/1000;
        		}
        	} else {
	        	if(prob<83) {
	        		genVal=0;
	        	} else {
	        		genVal=Math.floor(Math.random()*1000)*3/1000;
	        	}
        	
        	} 
        	this._lastValue2=genVal;
        	return genVal;
        },
        update: function() {

        	var val1=this._randomValue1();
        	var val2=this._randomValue2();
        	document.getElementById("topText_"+this.id).innerHTML=val1+" K ACC";
        	document.getElementById("bottomText_"+this.id).innerHTML=val2+" K ACC";
        	document.getElementById("verticalAxis_"+this.id).innerHTML='<div class="blingerText" style="position:absolute;top:-4px;right:-1px;height:11px;color:#808080;">1 -</div>'+'<div class="blingerText" style="position:absolute;bottom:-1px;right:-1px;height:11px;color:#808080;">0 -</div>';
        	//return;

        	//var t0 = (new Date()).getTime();
        	this._history1.splice(0,1);
        	this._history1.push(val1);
        	document.getElementById("blingerBoxValues1_"+this.id).innerHTML="";
        	var out=[];
        	
        	for(var i=0;i<this._stackLength1-1;i++) {
        		var y1=Math.floor(this._history1[i]*11)+6;
        		var x1=i+15;
        		var y2=Math.floor(this._history1[i+1]*11)+6;
        		var x2=i+16;
        		if(Math.abs(y1-y2)>=2){
        			out.push(Graphics.drawLine(x1, y1, x2, y2,this._color1));
        		} else {
        			out.push(Graphics.setPixel(x1,y1,1,1,this._color1));
        		}
        		
        	}
        	document.getElementById("blingerBoxValues1_"+this.id).innerHTML=out.join('');
			out=null;

        	this._history2.splice(0,1);
        	this._history2.push(val2);
        	document.getElementById("blingerBoxValues2_"+this.id).innerHTML="";
        	out=[];
        	
        	for(var i=0;i<this._stackLength2-1;i++) {
        		var y1=50-Math.floor((this._history2[i])*11)+6;
        		var x1=i+15;
        		var y2=50-Math.floor(this._history2[i+1]*11)+6;
        		var x2=i+16;
        		if(Math.abs(y1-y2)>=2){
        			out.push(Graphics.drawLine(x1, y1, x2, y2,this._color2));
        		} else {
        			out.push(Graphics.setPixel(x1,y1,1,1,this._color2));
        		}
        	}
        	document.getElementById("blingerBoxValues2_"+this.id).innerHTML=out.join('');
        	out=null;
        	//var t1 = (new Date()).getTime();
        }
});


Ext.untangle.SessionsBlinger.template = new Ext.Template(
'<div class="sessionBlinger"><div class="blingerName">sessions</div>',
'<div class="blingerBox" style="width:80px;">',
'<div class="blingerVerticalAxis" id="verticalAxis_{id}"></div>',
'<div class="blingerText blingerGraphTopText" id="topText_{id}"></div>',
'<div class="blingerText blingerGraphBottomText" id="bottomText_{id}"></div>',
'<div id="blingerBoxValues1_{id}"></div>',
'<div id="blingerBoxValues2_{id}"></div>',
'</div></div>');

Ext.ComponentMgr.registerType('untangleSessionsBlinger', Ext.untangle.SessionsBlinger);



Ext.untangle.DataRateBlinger = Ext.extend(Ext.Component, {
        _stackLength: 60,
        _history: null,
        color: "#8080FF",
        onRender: function (container, position) {
	        if(!this.template){
	            this.template = Ext.untangle.DataRateBlinger.template;
	        }
	        var el= document.createElement("div");
	        el.className="activityBlinger";
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
        	this.id=Ext.id(this);
			var templateHTML=this.template.applyTemplate({'id':this.id});
			el.innerHTML=templateHTML;

        	
        	this._history=[];
        	for(var i=0;i<this._stackLength;i++) {
        		this._history.push(0);
        	}
        },
        _lastValue: 0,
        _randomValue: function () {
        	var prob=Math.floor(Math.random()*100);
        	var genVal=0;
        	if(this._lastValue!=0) {
        		if(prob<15) {
        			genVal=0;
        		} else if(prob<75) {
        			genVal=this._lastValue;
        		} else {
        			genVal=Math.floor(Math.random()*1000)*5/1000;
        		}
        	} else {
	        	if(prob<83) {
	        		genVal=0;
	        	} else {
	        		genVal=Math.floor(Math.random()*1000)*5/1000;
	        	}
        	
        	} 
        	this._lastValue=genVal;
        	return genVal;
        },
        update: function() {
        	var val1=this._randomValue();
        	//var t0 = (new Date()).getTime();
        	document.getElementById("verticalAxis_"+this.id).innerHTML='<div class="blingerText" style="position:absolute;bottom:0px;right:-1px;height:11px;color:#808080;">0 -</div>'
        	this._history.splice(0,1);
        	this._history.push(val1);
        	document.getElementById("topText_"+this.id).innerHTML=val1+" GB";
        	
        	document.getElementById("blingerBoxValues_"+this.id).innerHTML="";
        	var out=[];
        	
        	for(var i=0;i<this._stackLength-1;i++) {
        		var y1=50-Math.floor(this._history[i]*11)+5;
        		var x1=i+15;
        		var y2=50-Math.floor(this._history[i+1]*11)+5;
        		var x2=i+16;
        		if(Math.abs(y1-y2)>=2){
        			out.push(Graphics.drawLine(x1, y1, x2, y2,this.color));
        		} else {
        			out.push(Graphics.setPixel(x1,y1,1,1,this.color));
        		}
        	}
        	document.getElementById("blingerBoxValues_"+this.id).innerHTML=out.join('');
        	out=null;
        	//var t1 = (new Date()).getTime();
        }
});
Ext.untangle.DataRateBlinger.template = new Ext.Template(
'<div class="dataRateBlinger"><div class="blingerName">data rate (KBps)</div>',
'<div class="blingerBox" style="width:80px;">',
'<div class="blingerVerticalAxis" id="verticalAxis_{id}"></div>',
'<div class="blingerText blingerGraphTopText" id="topText_{id}"></div>',
'<div id="blingerBoxValues_{id}"></div>',
'</div></div>');

Ext.ComponentMgr.registerType('untangleDataRateBlinger', Ext.untangle.DataRateBlinger);
*/

//setting object
Ext.untangle.Settings = Ext.extend(Ext.Component, {
	'i18n':{}	
});
Ext.untangle.Settings._nodeScripts={};
Ext.untangle._hasResource={}
Ext.untangle.Settings.loadNodeScript=function(webContext,cmpId,callbackFn) {
	//MainPage.loadScript('nodes/'+name+'.js',scope,callbackFn);
	MainPage.loadScript('/'+webContext+'/script/settings.js',function() {callbackFn(cmpId)})
	//jQuery.getScript('nodes/'+name+'.js?_dc='+(new Date()).getTime(),function() {callbackFn(cmpId)});
}
Ext.untangle.Settings._classNames={};
Ext.untangle.Settings.getClassName=function(name) {
	return Ext.untangle.Settings._classNames[name];
}
Ext.untangle.Settings.hasClassName=function(name) {
	return Ext.untangle.Settings._classNames[name]!=null;
}
Ext.untangle.Settings.registerClassName=function(name,className) {
	Ext.untangle.Settings._classNames[name]=className;
}

Ext.ComponentMgr.registerType('untangleSettings', Ext.untangle.Settings);
