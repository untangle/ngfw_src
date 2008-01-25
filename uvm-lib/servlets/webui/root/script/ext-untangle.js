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
    initComponent : function(){
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
    onMouseOver : function(e){
        if(!this.disabled){
                this.el.addClass("untangleButtonHover");
                this.fireEvent('mouseover', this, e);
        }
    },
    // private
    onMouseOut : function(e){
        this.el.removeClass("untangleButtonHover");
        this.fireEvent('mouseout', this, e);
    },
    onClick : function(e){
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
Ext.reg('untangleButton', Ext.untangle.Button);




Ext.untangle.Node = Ext.extend(Ext.Component, {
	    initComponent : function(){
	        Ext.untangle.Node.superclass.initComponent.call(this);
	    },
	    hidden : false,
	    disabled : false,

        name:"",
        displayName:"",
        image:"",
        state:"", // On, Off, Attention, Stopped
        powerOn:false,
        runState:'', // RUNNING, INITIALIZED
        helpLink:"",
        viewPosition:"",
        settings:null,
        settingsClassName:"",
        settingsInitialized:false,
        settingsScriptLoaded:false,
        setState:function(state) {
        	this.state=state;
        	var iconSrc="images/node/Icon"+this.state+"State28x28.png";
        	document.getElementById('nodeStateIconImg_'+this.id).src=iconSrc;
        },
        setPowerOn:function(powerOn) {
        	this.powerOn=powerOn;
        	var iconSrc="images/node/IconPower"+(powerOn?"On":"Off")+"State28x28.png";
        	document.getElementById('nodePowerIconImg_'+this.id).src=iconSrc;
        	document.getElementById('nodePowerOnHint_'+this.id).style.display=this.powerOn?"none":"";
        },
        updateBlingers: function () {
        	if(this.blingers!=null) {
        		for(var i=0;i<this.blingers.length;i++) {
        			Ext.getCmp(this.blingers[i].id).update();
        		}
        	}
        	setTimeout('Ext.untangle.Node.updateBlingers("'+this.id+'")',5000);
        },
        onPowerClick: function() {
        	this.setPowerOn(!this.powerOn);
        	this.setState("Attention");
			Ext.Ajax.request({
		        url:'rack.htm',
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
        loadNodeScript: function() {
        	if(!this.settingsScriptLoaded) {
        		idid=this.id;
	        	Ext.untangle.Settings.loadNodeScript(this.name, this, function() {Ext.getCmp(idid).settingsScriptLoaded=true;Ext.getCmp(idid).initSettings()});
	        } else {
	        	this.initSettings();
	        }
        },
        initSettings: function() {
        	if(!this.settingsInitialized) {
        		this.settingsClassName=Ext.untangle.Settings.getClassName(this.name);
        		if(this.settingsClassName!=null) {
	        		eval('this.settings=new '+this.settingsClassName+'({\'tid\':this.tid,\'name\':this.name});');
	        		this.settings.render('settings_'+this.id);
	        		this.settings.loadData();
	        		this.settingsInitialized=true;
	        	} else {
	        		alert("Error: There is no settings class for the node '"+this.name+"'");
	        	}
        	}
        },
        loadSettings: function(force) {
        	this.loadNodeScript();
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
				url:	"rack.htm",
				params :{'action':'removeFromRack','nodeId':this.tid,'installName':this.name},
				method: 'GET',
				'parentId':this.id,
				success: function ( result, request) { 
					var jsonResult=Ext.util.JSON.decode(result.responseText);
					if(jsonResult.success!=true) {
						Ext.MessageBox.alert('Failed', jsonResult.msg); 
					} else {
						//alert("Rack instaled: TODO: refresh rack node list, enable button in myApps");
						//Ext.getCmp(request.parentId).settingsWin.hide()
						Ext.getCmp(request.parentId).destroy();
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
	        if(this.setting) {
	        		this.settings.destroy();
        	}
        	this.settingsWin.hide();
        },
        _initBlingers: function () {
        	if(this.blingers!=null) {
        		var nodeBlingers=document.getElementById('nodeBlingers_'+this.id);
        		for(var i=0;i<this.blingers.length;i++) {
        			var blingerData=this.blingers[i];
        			//var blinger=new Ext.untangle.ActivityBlinger(blingerData);
       				eval('var blinger=new Ext.untangle.'+blingerData.type+'(blingerData);');
       				blinger.render('nodeBlingers_'+this.id);
        			this.blingers[i].id=blinger.id;
        			
        		}
        		this.updateBlingers();
        	}
        },
        onRender: function(container, position) {
        	//Ext.untangle.Node.superclass.onRender.call(this, ct, position);
        	this.on('beforedestroy', function() {
        		if(this.settingsWin) {
        			this.settingsWin.destroy();
        		}
        		if(this.settings) {
        			this.settings.destroy();
        		}
        	},this)
	        if(!this.template){
	            this.template = Ext.untangle.Node.template;
	        }
        	var templateHTML=this.template.applyTemplate({'id':this.id,'image':this.image,'displayName':this.displayName});
       
	        var el= document.createElement("div");
	        el.innerHTML=templateHTML;
	        el.setAttribute('viewPosition',this.viewPosition);
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
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
		        'iconCls': 'nodeCancelIcon',
				'renderTo':'nodeCancelButton_'+this.id,
		        'text': 'Cancel',
		        'handler': function() {Ext.getCmp(this.parentId).onCancelClick()}
	        });
			new Ext.Button({
				'parentId':this.id,
		        'iconCls': 'nodeSaveIcon',
				'renderTo':'nodeSaveButton_'+this.id,
		        'text': 'Save',
		        'handler': function() {Ext.getCmp(this.parentId).onSaveClick()}
	        });
	        this.setPowerOn(this.runState=="RUNNING")
	        this.setState((this.runState=="RUNNING")?"On":"Off")
        	this._initBlingers();
        }
});
Ext.untangle.Node.updateBlingers=function(cmpId) {
	var cmp=Ext.getCmp(id);
	if(cmp!=null) {
		cmp.updateBlingers();
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

Ext.reg('untangleNode', Ext.untangle.Node);


Ext.untangle.ActivityBlinger = Ext.extend(Ext.Component, {
        onRender: function (container, position) {
	        if(!this.template){
	            this.template = Ext.untangle.ActivityBlinger.template;
	        }
	        var el= document.createElement("div");
	        el.className="activityBlinger";
	        container.dom.insertBefore(el, position);
        	this.el = Ext.get(el);
        	this.id=Ext.id(this);
			var templateHTML=this.template.applyTemplate({'id':this.id});
			el.innerHTML=templateHTML;
        	if(this.bars!=null) {
        		var out=[];
        		for(var i=0;i<this.bars.length;i++) {
        			var bar=this.bars[i];
        			var top=3+i*15;
        			out.push('<div class="blingerText activityBlingerText" style="top:'+top+'px;">'+bar.name+'</div>');
        		}
        		document.getElementById("blingerBoxLabels_"+this.id).innerHTML=out.join("");
        	}
        },
        update: function() {
        	var out=[];
        	for(var i=0;i<this.bars.length;i++) {
        		var top=3+i*15;
        		var bar=this.bars[i];
        		bar.value=Math.floor(Math.random()*58);
        		out.push('<div class="activityBlingerBar" style="top:'+top+'px;width:'+bar.value+'px;"></div>');
        	}
        	document.getElementById("blingerBoxValues_"+this.id).innerHTML=out.join("");
        	out=null;
        }
        
});
Ext.untangle.ActivityBlinger.template = new Ext.Template(
'<div class="blingerName">activity</div>',
'<div class="blingerBox" style="width:60px;">',
'<div id="blingerBoxValues_{id}"></div>',
'<div id="blingerBoxLabels_{id}"></div>',
'</div>');
Ext.reg('untangleActivityBlinger', Ext.untangle.ActivityBlinger);



Ext.untangle.SessionsBlinger = Ext.extend(Ext.Component, {
        _stackLength1:60,
        _history1:null,
        _stackLength2:60,
        _history2:null,
        _color1:"#8080FF",
        _color2:"#804080",
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
        _lastValue1:0,
        _lastValue2:0,
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

Ext.reg('untangleSessionsBlinger', Ext.untangle.SessionsBlinger);







Ext.untangle.DataRateBlinger = Ext.extend(Ext.Component, {
        _stackLength:60,
        _history:null,
        color:"#8080FF",
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
        _lastValue:0,
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

Ext.reg('untangleDataRateBlinger', Ext.untangle.DataRateBlinger);

Ext.untangle.Settings = Ext.extend(Ext.Component, {
	'i18n':''	
});
Ext.untangle.Settings._nodeScripts={};
Ext.untangle.Settings.loadNodeScript=function(name,scope,callbackFn) {
	/*
	var script_container_id="script_"+name;
	if(document.getElementById(script_container_id)==null) {
		var node_url='nodes/'+name+'.js'
		Ext.Ajax.request({
			url:	node_url,
			method: 'GET',
			'parentId':scope,
			success: function ( result, request) { 
				var jsonResult=Ext.util.JSON.decode(result.responseText);
				if(jsonResult.success!=true) {
					Ext.MessageBox.alert('Failed', jsonResult.msg); 
				} else {
					//alert("Rack instaled: TODO: refresh rack node list, enable button in myApps");
					//Ext.getCmp(request.parentId).settingsWin.hide()
					Ext.getCmp(request.parentId).destroy();
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

		
		var el= document.createElement("div");
		el.id=script_container_id;
		el.style.display="none";
		document.body.appendChild(el);
		
	}
	*/
	MainPage.loadScript('nodes/'+name+'.js',scope,callbackFn);
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

Ext.reg('untangleSettings', Ext.untangle.Settings);
