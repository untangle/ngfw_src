// all packages need to dojo.provide() _something_, and only one thing
dojo.provide("untangle.Node");
// AccordionContainer is the module with dijit.layout.AccordionPane
// our declared class
dojo.require("dojo.parser");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit.Tooltip"); 
dojo.require("dojo.io.iframe")
dojo.require("dojo.io.script")

dojo.declare("untangle.Node", [dijit._Widget, dijit._Templated], 
        {
        widgetsInTemplate: true,
        templatePath: dojo.moduleUrl("untangle","templates/Node.html"),
        templateString: "",
        code:"",
        label:"",
        image:"",
        state:"On",
        powerOn:true,
        helpLink:"",
        settingsExpanded:false,
        _getNodeStateIcon: function() {
        	return "images/node/Icon"+this.state+"State28x28.png";
        },
        _getNodePowerIcon: function() {
        	return "images/node/IconPower"+(this.powerOn?"On":"Off")+"State28x28.png";
        },
        _updateNodeStateIcon: function () {
        	this.nodeStateIcon.src=this._getNodeStateIcon();
        },
        _updateNodePower: function () {
        	this.nodePowerIcon.src=this._getNodePowerIcon();
        	this.nodePowerOnHint.style.display=this.powerOn?"none":"";
        },
        updateBlingers: function () {
        	if(this.blingers!=null) {
        		for(var i=0;i<this.blingers.length;i++) {
        			dijit.byId(this.blingers[i].id).update();
        		}
        	}
        	setTimeout('dijit.byId("'+this.id+'").updateBlingers()',2000);
        },
        onPowerClick: function() {
        	this.powerOn=!this.powerOn;
        	this.state=this.powerOn?"On":"Off"
        	this._updateNodeStateIcon();
        	this._updateNodePower();
        },
        onHelpClick: function () {
        	if(this.helpLink!=null && this.helpLink.length>0) {
        		window.open(this.helpLink);
        	}
        },
        onSettingsClick: function() {
        	this.settingsExpanded=!this.settingsExpanded;
        	this.nodeSettings.style.display=this.settingsExpanded?"":"none";
        	dijit.byId("settings_on_"+this.code).domNode.style.display=this.settingsExpanded?"":"none"
        	dijit.byId("settings_off_"+this.code).domNode.style.display=!this.settingsExpanded?"":"none"
        },
        onRemoveClick: function() {
        	alert("Remove Node");
        },
        onSaveClick: function() {
        	alert("Save Settings");
        },
        onCancelClick: function() {
        	alert("Cancel Settings");
        },
        _initBlingers: function () {
        	if(this.blingers!=null) {
        		for(var i=0;i<this.blingers.length;i++) {
        			var blingerData=this.blingers[i];
        			
        			var blinger=null;
        			
        			dojo.require("untangle.blingers."+blingerData.type);
        			eval('var blinger=new untangle.blingers.'+blingerData.type+'(blingerData,dojo.doc.createElement("div"));');
        			this.nodeBlingers.appendChild(blinger.domNode);
        			blinger.startup();
        			this.blingers[i].id=blinger.id;
        		}
        		this.updateBlingers();
        	}
        },
        startup: function () {
        	this.inherited("startup",arguments);
        	this.state=this.powerOn?"On":"Off";
        	this._updateNodeStateIcon();
        	this._updateNodePower();
        	this._initBlingers();
        	//dijit.byId("settings_"+this.code).setHref(this.settings.url);
        	//dijit.byId("settings_"+this.code).
        	//dijit.byId("settings_"+this.code).refresh();
			
			var el = Ext.get("settings_"+this.code);
			el.load({
		        url: this.settings.url, 
		        scripts: true,
		        text: "Loading ..."
		   });
        }
});
