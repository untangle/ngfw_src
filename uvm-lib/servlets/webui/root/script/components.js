Ext.namespace('Ung');
// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=new Ung.I18N({"map":null});;
// the main json rpc object
var rpc=null;

Ext.override(Ext.Button, {
    listeners : {
        "render" : {
            fn : function() {
                if (this.name && this.getEl()) {
                    this.getEl().set({
                        'name' : this.name
                    });
                }
            }
        }
    }
});
Ext.override(Ext.Panel, {
    listeners : {
        "render" : {
            fn : function() {
                if (this.name && this.getEl()) {
                    this.getEl().set({
                        'name' : this.name + " Content"
                    });
                }
            }
        }
    }
});
Ext.override(Ext.PagingToolbar, {
    listeners : {
        "render" : {
            fn : function() {
                if (this.getEl()) {
                    this.getEl().set({
                        'name' : "Paging Toolbar"
                    });
                }
            }
        }
    }
});
Ext.override(Ext.TabPanel, {
    addNamesToPanels: function () {
        if (this.items) {
            var items=this.items.getRange();
            for(var i=0;i<items.length;i++) {
                var tabEl=Ext.get(this.getTabEl(items[i]));
                if(items[i].name && tabEl) {
                    tabEl.set({
                        'name' : items[i].name
                    });
                }
            }
        }
    }
});

Ung.Util= {
    goToStartPage: function () {
        Ext.MessageBox.wait(i18n._("Redirecting to the start page..."), i18n._("Please wait"));
        window.location.href="/webui";
    },
    rpcExHandler: function(exception) {
        if(exception instanceof JSONRpcClient.Exception)
        {
            if(exception.code == 550)
            {
                Ext.MessageBox.alert(i18n._("Failed"),i18n._("The Session has expired. You will be redirected to the start page."), Ung.Util.goToStartPage);
            }
        }
        if(exception) {
            throw exception;
        }
        else {
            throw i18n._("Error making rpc request to server");
        }
    },
    handleException: function(exception, handler, type, continueExecution) { //type: alertCallback, alert, noAlert
        if(exception) {
            if(exception.code==550) {
                Ext.MessageBox.alert(i18n._("Failed"),i18n._("The Session has expired. You will be redirected to the start page."), Ung.Util.goToStartPage);
                return true;
			} else {
                if(handler==null) {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                } else if(type==null || type== "alertCallback"){
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message, handler);
                } else if (type== "alert") {
                    Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                    handler();
                } else if (type== "noAlert") {
                    handler();
                }
                return !continueExecution;
			}
		}
		return false;
    },
	
    encode : function (obj) {
        if(obj == null || typeof(obj) != 'object') {
            return obj;
        }
        var msg="";
        var val=null;
        for(var key in obj) {
            val=obj[key]
            if(val!=null) {
              msg+=" | "+key+" - "+val;
            }
        }
        return msg;
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
    loadScript: function(sScriptSrc, handler) {
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
        if(handler) {
            handler.call(this);
        }
        return error;
    },
    // Load a resource if not loaded and execute a callback function
    loadResourceAndExecute: function(resource,sScriptSrc, handler) {
        if(Ung.hasResource[resource]) {
            handler.call(this);
        } else {
            Ung.Util.loadScript(sScriptSrc, handler);
        }
    },
    loadModuleTranslations : function(moduleName, dependencyMap, handler) {
    	if(!Ung.i18nModuleInstances[moduleName]) {
            rpc.languageManager.getTranslations(function(result, exception, opt, moduleName, dependencyMap, handler) {
            	if(Ung.Util.handleException(exception)) return;
                var moduleMap=result.map;
                if(dependencyMap!=null) {
                	Ext.applyIf(moduleMap, dependencyMap)
                }
                Ung.i18nModuleInstances[moduleName] = new Ung.ModuleI18N({
                        "map" : i18n.map,
                        "moduleMap" : moduleMap
                });
                handler.call(this);
            }.createDelegate(this,[moduleName, dependencyMap, handler],true), moduleName);
    	} else {
    		handler.call(this);
    	}
    },
    todo: function() {
        Ext.MessageBox.alert(i18n._("TODO"),"TODO: implement this.");
    },
    interfaceStore:null,
    getInterfaceStore : function() {
        if(this.interfaceStore==null) {
            this.interfaceStore=new Ext.data.SimpleStore({
                fields : ['key', 'name'],
                data :[
                    ["any", i18n._("any")],
                    ["0", i18n._("External")],
                    ["7", i18n._("VPN")],
                    ["1", i18n._("Internal")],
                    ["more_trusted", i18n._("More Trusted")],
                    ["less_trusted", i18n._("Less Trusted")]
                ]
            });
        }
        return this.interfaceStore;
    },
    protocolStore:null,
    getProtocolStore : function() {
        if(this.protocolStore==null) {
            this.protocolStore=new Ext.data.SimpleStore({
                fields : ['key', 'name'],
                data :[
                    ["TCP & UDP", i18n._("TCP & UDP")],
                    ["UDP", i18n._("UDP")],
                    ["TCP", i18n._("TCP")],
                    ["ANY", i18n._("ANY")]
                ]
            });
        }
        return this.protocolStore;
    },
    formatTime : function(value, rec) {
        if(value==null) {
            return null;
        } else {
            var d=new Date();
            d.setTime(value.time);
            return d.format("H:i");
        }
    },

    // Test if there is data in the specified object
    hasData : function(obj) {
        var hasData = false;
        for (id in obj) {
            hasData = true;
            break;
        }
        return hasData;
    },
    
    // Check if two object are equal
    equals : function(obj1, obj2) {
    	// the two objects have different types
    	if (typeof obj1 !== typeof obj2) {
    		return false;
    	}
        // check null objects
    	if (obj1 === null || obj2 === null) {
    		return obj1 === null && obj2 === null;
    	}
    	var count = 0;
        for (prop in obj1) {
            // the two properties have different types
        	if (typeof obj1[prop] !== typeof obj1[prop]) {
        		return false;
        	}
        	
            switch (typeof obj1[prop]) {
              case 'number':
              case 'string':
              case 'boolean':
                if (obj1[prop] !== obj2[prop]) {
                	return false;
                }
                break;
              case 'undefined':
                break;
              case 'object':
                if (!Ung.Util.equals(obj1[prop], obj2[prop])){
                	return false;
                }
                break;
            }
        	count++;
        }
        
        // check that the two objects have the same number of properties
        for (prop in obj2) {
        	count--;
        }
        if (count != 0) {
            return false
        }
        return true;
    },
    
    // Clone object
    clone : function (obj){
        if(obj == null || typeof(obj) != 'object')
            return obj;
    
        var temp = new obj.constructor();
        for(var key in obj)
            temp[key] = Ung.Util.clone(obj[key]);
    
        return temp;
    },    

    // Get the save list from the changed data
    getSaveList : function(changedData, recordJavaClass) {
        var added = [];
        var deleted = [];
        var modified = [];
        for (id in changedData) {
            var cd = changedData[id]
            if ("deleted" == cd.op) {
                if (id > 0) {
                    deleted.push(parseInt(id));
                }
            } else {
                if (recordJavaClass != null){
                    cd.recData["javaClass"] = recordJavaClass;
                }
                if (id < 0) {
                    added.push(cd.recData);
                } else {
                    modified.push(cd.recData);
                }
            }
        }
        return [{
            list : added,
            "javaClass" : "java.util.ArrayList"
        }, {
            list : deleted,
            "javaClass" : "java.util.ArrayList"
        }, {
            list : modified,
            "javaClass" : "java.util.ArrayList"
        }];
    },
    // If the grid is not rendered it sets the new store data; otherwise it
    // loads the now store data
    loadGridStoreData : function(grid, storeData) {
        if (grid.rendered) {
            grid.store.loadData(storeData);
        } else {
            grid.store.proxy.data = storeData;
        }
    },
    bytesToMBs : function(value) {
        return Math.round(value/10000)/100;
    },
    resizeWindows : function() {
    	Ext.WindowMgr.each(Ung.Util.setSizeToRack);
    },
    setSizeToRack:function (win) {
    	if(win!=null && win.sizeToRack==true) {
            win.setSizeToRack();
    	}
    },
    defaultRenderer :function (value) {
    	return (typeof value == 'string') ? 
    	   value.length<1? "&#160;": Ext.util.Format.htmlEncode(value) :
    	   value;
    }
};


Ung.Util.InterfaceCombo=Ext.extend(Ext.form.ComboBox, {
    initComponent : function() {
        this.store = Ung.Util.getInterfaceStore();
        Ung.Util.InterfaceCombo.superclass.initComponent.call(this);
    },
    displayField : 'name',
    valueField : 'key',
    editable: false,
    mode : 'local',
    triggerAction : 'all',
    listClass : 'x-combo-list-small'
});

Ung.Util.ProtocolCombo=Ext.extend(Ext.form.ComboBox, {
    initComponent : function() {
        this.store = Ung.Util.getProtocolStore();
        Ung.Util.ProtocolCombo.superclass.initComponent.call(this);
    },
    displayField : 'name',
    valueField : 'key',
    editable: false,
    mode : 'local',
    triggerAction : 'all',
    listClass : 'x-combo-list-small'
});

/**
 * @class Ung.SortTypes
 * @singleton
 * Defines custom sorting (casting?) comparison functions used when sorting data.
 */
Ung.SortTypes = {
    /**
     * Timestamp sorting
     * @param {Mixed} value The value being converted
     * @return {Number} The comparison value
     */
    asTimestamp : function(value){
        return value.time;
    },
    
    /**
     * @param {Mixed} value The PipelineEndpoints value being converted
     * @return {String} The comparison value
     */
    asClient : function(value) {
        return value === null ? "" : value.CClientAddr + ":" + value.CClientPort;
    },
    
    /**
     * @param {Mixed} value The PipelineEndpoints value being converted
     * @return {String} The comparison value
     */
    asServer : function(value) {
        return value === null ? "" : value.SServerAddr + ":" + value.SServerPort;
    }};


// resources map
Ung.hasResource = {};

Ung.ConfigItem = Ext.extend(Ext.Component, {
    item: null,
    renderTo : 'configItems',
    autoEl : 'div',
    onRender : function(container, position) {
        Ung.ConfigItem.superclass.onRender.call(this, container, position);
        var html = Ung.ConfigItem.template.applyTemplate({
            'iconCls' : this.item.iconClass,
            'text' : this.item.displayName
        });
        this.getEl().insertHtml("afterBegin", html);
        this.getEl().addClass("appItem");
        this.getEl().on("click", this.onClick, this);
    },
    onClick: function(e) {
        if (e!=null) {
            e.stopEvent();
        }
        main.clickConfig(this.item);
    }
});
Ung.ConfigItem.template = new Ext.Template(
    '<div class="icon"><div class="{iconCls}"></div></div>', '<div class="text textCenter">{text}</div>');

Ung.AppItem = Ext.extend(Ext.Component, {
    libItem: null,
    node : null,
    trialLibItem : null,

    iconSrc : null,
    iconCls : null,
    renderTo : 'appsItems',
    operationSemaphore : false,
    autoEl : 'div',
    state : null,
    // buy button
    buttonBuy : null,
    // progress bar component
    progressBar : null,
    subCmps:null,
    constructor : function(config) {
        var name="";
        this.subCmps=[];
        this.isValid=true;
        if(config.libItem!=null) {
            name=config.libItem.name;
            this.item=config.libItem;
        } else if(config.node!=null) {
            name=config.node.name;
            this.item=config.node;
        } else {
           Ext.MessageBox.alert(i18n._("Apps Error"), i18n._("Error in Rack View applications list."));
           this.isValid=false;
           // error
        }
        this.id = "appItem_" + name;
        Ung.AppItem.superclass.constructor.apply(this, arguments);
    },
    onRender : function(container, position) {
        if(!this.isValid) {
            return;
        }
        Ung.AppItem.superclass.onRender.call(this, container, position);

        if (this.name && this.getEl()) {
            this.getEl().set({
                'name' : this.name
            });
        }
        var imageHtml = null;
        if (this.iconCls == null) {
            if (this.iconSrc == null) {
                this.iconSrc = 'image?name=' + this.item.name;
            }
            imageHtml = '<img src="' + this.iconSrc + '" style="vertical-align: middle;"/>';
        } else {
            imageHtml = '<div class="' + this.iconCls + '"></div>';
        }
        var html = Ung.AppItem.template.applyTemplate({
            id : this.getId(),
            'imageHtml' : imageHtml,
            'text' : this.item.displayName
        });
        this.getEl().insertHtml("afterBegin", html);
        this.getEl().addClass("appItem");

        this.progressBar = new Ext.ProgressBar({
            text : '',
            id : 'progressBar_' + this.getId(),
            renderTo : "state_" + this.getId(),
            height : 17,
            width : 120,
            waitDefault : function(updateText) {
            	this.reset();
                this.wait({
                    text:  updateText,
                    interval : 100,
                    increment : 15
                });
            }
        });

        this.buttonBuy = Ext.get("buttonBuy_" + this.getId());
        this.buttonBuy.setVisible(false);
        this.actionEl = Ext.get("action_" + this.getId());
        this.progressBar.hide();
        if(this.libItem!=null && this.node==null) { // libitem
            this.getEl().on("click", this.linkToStoreFn, this);
            // this.actionEl.on("click", this.linkToStoreFn, this);
            this.actionEl.insertHtml("afterBegin", i18n._("More Info"));
            this.actionEl.addClass("iconInfo");
        } else if(this.node!=null) { // node
            this.getEl().on("click", this.installNodeFn, this);
            // this.actionEl.on("click", this.installNodeFn, this);
            this.actionEl.insertHtml("afterBegin", this.libItem==null?i18n._("Install"):i18n._("Trial Install"));
            this.actionEl.addClass("iconArrowRight");
            if(this.libItem!=null) { // libitem and trial node
                this.buttonBuy.setVisible(true);
                this.buttonBuy.insertHtml("afterBegin", i18n._("Buy"));
                this.buttonBuy.on("click", this.linkToStoreFn, this);
                this.buttonBuy.addClass("buttonBuy");
                this.buttonBuy.addClass("iconArrowDown");
            }
        } else {
            return;
            // error
        }
    },
    // get the node name associated with the App
    getNodeName : function() {
        var nodeName = null; // for libitems with no trial node return null
        if (this.node) { // nodes
            nodeName = this.node.name;
        }
        return nodeName;
    },
    setState : function(newState, options) {
        switch (newState) {
            case "unactivated" :
            case null:
                this.displayButtonsOrProgress(true);
                break;
            case "unactivating" :
                this.displayButtonsOrProgress(false);
                this.progressBar.reset();
                this.progressBar.waitDefault(i18n._("Unactivating..."));
                break;
            case "activated" :
                this.displayButtonsOrProgress(true);
                break;
            case "installed" :
                this.displayButtonsOrProgress(true);
                break;
            case "installing" :
                this.displayButtonsOrProgress(false);
                this.progressBar.waitDefault(i18n._("Installing..."));
                break;
            case "uninstalling" :
                this.show();
                this.displayButtonsOrProgress(false);
                this.progressBar.waitDefault(i18n._("Uninstalling..."));
                break;
            case "download_progress" :
                this.displayButtonsOrProgress(false);
                if (options == null) {
                    this.progressBar.reset();
                    this.progressBar.updateText(i18n._("Downloading..."));
                } else {
                    var currentPercentComplete = parseFloat(options.bytesDownloaded) / parseFloat(options.size != 0 ? options.size : 1);
                    var progressIndex = parseFloat(0.9 * currentPercentComplete);
                    var progressString = String.format(i18n._("Get@{0}"), options.speed);
                    this.progressBar.updateProgress(progressIndex, progressString);
                }
                break;
            case "download" :
                this.displayButtonsOrProgress(false);
                this.progressBar.waitDefault(i18n._("Downloading..."));
                break;
            case "activate_timeout" :
                this.displayButtonsOrProgress(false);
                this.progressBar.waitDefault(i18n._("Activate timeout."));
                break;
            case "waiting" :
                this.displayButtonsOrProgress(false);
                this.progressBar.waitDefault(i18n._("..."));
                break;
        }
        this.state = newState;

    },
    // before Destroy
    beforeDestroy : function() {
        this.actionEl.removeAllListeners();
        this.buttonBuy.removeAllListeners();
        this.progressBar.reset(true);
        this.progressBar.destroy();
        Ext.each(this.subCmps, Ext.destroy);
        Ung.AppItem.superclass.beforeDestroy.call(this);
    },

    // display Buttons xor Progress barr
    displayButtonsOrProgress : function(displayButtons) {
        this.actionEl.setVisible(displayButtons);
        this.buttonBuy.setVisible(displayButtons);
        if (displayButtons) {
            this.getEl().unmask();
            this.buttonBuy.unmask();
            this.progressBar.reset(true);
        } else {
            this.getEl().mask();
            this.buttonBuy.mask();
            this.progressBar.show();
        }
    },
    // open store page in a new frame
    linkToStoreFn : function(e) {
        if (e!=null) {
            e.stopEvent();
        }
        if(!this.progressBar.hidden) {
            return;
        }
        main.warnOnUpgrades(function() {
        	 this.openStore();  
        }.createDelegate(this));
    },
    openStore : function () {
        var currentLocation = window.location;
        var query = "&host=" + currentLocation.hostname;
        query += "&port=" + currentLocation.port;
        query += "&protocol=" + currentLocation.protocol.replace(/:$/, "");
        query += "&action=browse";
        query += "&libitem=" + this.libItem.name;

        var url = "../library/launcher?" + query;
        var iframeWin = main.getIframeWin();
        iframeWin.show();
        iframeWin.setTitle(String.format(i18n._("More Info - {0}"),this.item.displayName));
        window.frames["iframeWin_iframe"].location.href = url;
    },
    // install node / uninstall App
    installNodeFn : function(e) {
        e.preventDefault();
        if(!this.progressBar.hidden) {
            return;
        }
        if (e.shiftKey) { // uninstall App
            main.unactivateNode(this.node);
        } else { // install node
            main.installNode(this.node, this);
        }
    }

});
Ung.AppItem.template = new Ext.Template('<div class="icon">{imageHtml}</div>', '<div class="text">{text}</div>',
        '<div id="buttonBuy_{id}"></div>', '<div id="action_{id}" class="action"></div>', '<div class="statePos" id="state_{id}"></div>');
// update node state for the app with a node name
Ung.AppItem.updateStateForNode = function(nodeName, state, options) {
    var app = Ung.AppItem.getAppForNode(nodeName);
    if (app != null) {
        app.setState(state, options);
    }
}
// update state for the app with a node name
Ung.AppItem.updateState = function(itemName, state, options) {
    var app = Ung.AppItem.getApp(itemName);
    if (app != null) {
        app.setState(state, options);
    }
}
// get the app item having a node name
Ung.AppItem.getAppForNode = function(nodeName) {
    if (main.apps !== null) {
        for (var i = 0; i < main.apps.length; i++) {
            var app = main.apps[i];
            if (nodeName == app.getNodeName()) {
                return app;
            }
        }
    }
    return null;
};
// get the app item having a item name
Ung.AppItem.getApp = function(itemName) {
    if (main.apps !== null) {
        return Ext.getCmp("appItem_" + itemName);
    }
    return null;
};

// Button component class
Ung.Button = Ext.extend(Ext.Component, {
    autoEl : 'div',
    width : null,
    height : null,
    disabled : false,
    text : '',
    iconSrc : null,
    iconCls : null,
    clickEvent : 'click',
    initComponent : function() {
        if (!this.width) {
            this.width = "100%";
        }
        if (!this.height) {
            this.height = "100%";
        }
        Ung.Button.superclass.initComponent.call(this);
        this.addEvents('click', 'mouseover', 'mouseout');
    },
    // private
    onRender : function(container, position) {
        Ung.Button.superclass.onRender.call(this, container, position);
        this.getEl().addClass("ungButton");
        var imageHtml = null;
        if (this.iconSrc != null) {
            imageHtml = '<img src="' + this.iconSrc + '" style="vertical-align: middle;"/>';
        } else {
            imageHtml = '<div class="' + this.iconCls + '"></div>';
        }
        var templateHTML = Ung.Button.template.applyTemplate({
            'width' : this.width,
            'height' : this.height,
            'imageHtml' : imageHtml,
            'text' : this.text
        });
        this.getEl().insertHtml("afterBegin", templateHTML);
        this.getEl().on(this.clickEvent, this.onClick, this);
        this.getEl().on("mouseover", this.onMouseOver, this);
        this.getEl().on("mouseout", this.onMouseOut, this);
        if (this.name && this.getEl()) {
            this.getEl().set({
                'name' : this.name
            });
        }
    },
    // private
    onMouseOver : function(e) {
        if (!this.disabled) {
            this.getEl().addClass("ungButtonHover");
            this.fireEvent('mouseover', this, e);
        }
    },
    // private
    onMouseOut : function(e) {
        this.getEl().removeClass("ungButtonHover");
        this.fireEvent('mouseout', this, e);
    },
    onClick : function(e) {
        if (e) {
            e.preventDefault();
        }
        if (e.button !== 0) {
            return;
        }
        if (!this.disabled) {
            this.fireEvent("click", this, e);
            if (this.handler) {
                this.handler.call(this, this, e);
            }
        }
    }
});
Ung.Button.template = new Ext.Template('<table border="0" width="{width}" height="{height}"><tr>',
        '<td width="1%" style="text-align: left;vertical-align: middle;">{imageHtml}</td>',
        '<td style="text-align: left;vertical-align: middle;padding-left:5px;font-size: 14px;">{text}</td>', '</tr></table>');
Ext.ComponentMgr.registerType('ungButton', Ung.Button);

// Node Class
Ung.Node = Ext.extend(Ext.Component, {
    autoEl : "div",
    // ---Node specific attributes------
    // node name
    name : null,
    // node image
    image : null,
    // mackage description
    md : null,
    // --------------------------------
    // node state
    state : null, // On, Off, Attention, Stopped
    // is powered on,
    powerOn : null,
    // running state
    runState : null, // RUNNING, INITIALIZED

    // settings Component
    settings : null,
    // settings Window
    settingsWin : null,
    // settings Class name
    settingsClassName : null,
    // last blinger data received
    stats : null,
    activityBlinger: null,
    systemBlinger: null,
    subCmps : null,
    fnCallback: null,
    constructor : function(config) {
        this.id = "node_" + config.tid;
        config.helpSource=config.md.displayName.toLowerCase().replace(/ /g,"_");
        if(config.runState==null) {
        	config.runState="INITIALIZED";
        }
        this.subCmps = [];
        Ung.Window.superclass.constructor.apply(this, arguments);
    },
    // before Destroy
    beforeDestroy : function() {
    	if(this.settingsWin && this.settingsWin.isVisible()) {
    		this.settingsWin.cancelAction();
    	}
        Ext.each(this.subCmps, Ext.destroy);
        Ext.get('nodePower_' + this.getId()).removeAllListeners();
        Ung.Node.superclass.beforeDestroy.call(this);
    },
    onRender : function(container, position) {
        Ung.Node.superclass.onRender.call(this, container, position);
        this.getEl().addClass("node");
        this.getEl().set({
            'viewPosition' : this.md.viewPosition
        });
        this.getEl().set({
            'name' : this.md.displayName
        });

        var trialFlag = "";
        var trialDays = "";
        if(this.licenseStatus.trial) {
        	trialFlag = i18n._("Trial");
        	if(this.licenseStatus.expired) {
        		trialDays = i18n._("Trial expired");
        	} else {
        		var daysRemain = parseInt(this.licenseStatus.timeRemaining.replace(" days remain", ""))
                if (!isNaN(daysRemain)) {
                    trialDays = String.format(i18n._("{0} days remain"), daysRemain);
                }
        	}
        }
        var templateHTML = Ung.Node.template.applyTemplate({
            'id' : this.getId(),
            'image' : this.image,
            'displayName' : this.md.displayName,
            'trialDays' : trialDays,
            'trialFlag' : trialFlag
        });
        this.getEl().insertHtml("afterBegin", templateHTML);

        Ext.get('nodePower_' + this.getId()).on('click', this.onPowerClick, this);
        this.subCmps.push(new Ext.ToolTip({
            html : Ung.Node.getStatusTip(),
            target : 'nodeState_' + this.getId(),
            autoWidth : true,
            autoHeight : true,
            showDelay : 20,
            dismissDelay : 0,
            hideDelay : 0
        }));
        this.subCmps.push(new Ext.ToolTip({
            html : Ung.Node.getPowerTip(),
            target : 'nodePower_' + this.getId(),
            autoWidth : true,
            autoHeight : true,
            showDelay : 20,
            dismissDelay : 0,
            hideDelay : 0
        }));
        this.subCmps.push(new Ext.Button({
            name : "Show Settings",
            iconCls : 'nodeSettingsIcon',
            renderTo : 'nodeSettingsButton_' + this.getId(),
            text : i18n._('Show Settings'),
            handler : function() {
                this.onSettingsAction();
            }.createDelegate(this)
        }));
        this.subCmps.push(new Ext.Button({
            name : "Help",
            iconCls : 'iconHelp',
            renderTo : 'nodeHelpButton_' + this.getId(),
            text : i18n._('Help'),
            handler : function() {
                this.onHelpAction();
            }.createDelegate(this)
        }));
        this.updateRunState(this.runState);
        this.initBlingers();
    },
    // is runState "RUNNING"
    isRunning : function() {
        return (this.runState == "RUNNING")
    },
    setState : function(state) {
        this.state = state;
        document.getElementById('nodeState_' + this.getId()).className = "nodeState iconState" + this.state;
    },
    setPowerOn : function(powerOn) {
        this.powerOn = powerOn;
    },
    updateRunState : function(runState) {
        this.runState = runState;
        var isRunning = this.isRunning();
        this.setPowerOn(isRunning);
        this.setState(isRunning ? "On" : "Off");
    },
    updateBlingers : function() {
        if (this.powerOn && this.stats) {
            if(this.activityBlinger!=null) {
                this.activityBlinger.update(this.stats);
            }
            if(this.systemBlinger!=null) {
                this.systemBlinger.update(this.stats);
            }
        } else {
            this.resetBlingers();
        }
    },
    resetBlingers : function() {
        if(this.activityBlinger!=null) {
            this.activityBlinger.reset();
        }
        if(this.systemBlinger!=null) {
            this.systemBlinger.reset();
        }
    },
    onPowerClick : function() {
        if (!this.powerOn) {
        	this.start();
        } else {
            this.stop();
        }
    },
    start : function () {
        if(this.state=="Attention") {
            return
        }
    	this.loadNodeContext(function() {
            this.setPowerOn(true);
            this.setState("Attention");
            this.nodeContext.rpcNode.start(function(result, exception) {
            	if(Ung.Util.handleException(exception, function() {
            	   this.updateRunState("INITIALIZED");
            	}.createDelegate(this),"alert")) return;
                this.updateRunState("RUNNING");
            }.createDelegate(this));
    	}.createDelegate(this));
    },
    stop : function () {
        if(this.state=="Attention") {
            return
        }
        this.loadNodeContext(function() {
            this.setPowerOn(false);
            this.setState("Attention");
            this.nodeContext.rpcNode.stop(function(result, exception) {
                this.updateRunState("INITIALIZED");
                this.resetBlingers();
                if(Ung.Util.handleException(exception)) return;
            }.createDelegate(this));
        }.createDelegate(this));
    },
    // on click help
    onHelpAction : function() {
        main.openHelp(this.helpSource);
    },
    // on click settings
    onSettingsAction : function(fnCallback) {
    	this.fnCallback=fnCallback;
        this.loadSettings();
    },
    getNodeContext: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if (this.nodeContext === undefined) {
            rpc.nodeManager.nodeContext(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeContext = result;
            }.createSequence(handler).createDelegate(this), this.Tid);
            
        } else {
        	handler.call(this);
        }
    },
    getRpcNode: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if(this.nodeContext==null) {
        	return;
        }
        if (this.nodeContext.rpcNode === undefined) {
            this.nodeContext.node(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeContext.rpcNode = result;
            }.createSequence(handler).createDelegate(this));
        } else {
        	handler.call(this);
        }
    },
    getNodeDesc: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if(this.nodeContext==null) {
            return;
        }
        if (this.nodeContext.nodeDesc === undefined) {
            this.nodeContext.getNodeDesc(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeContext.nodeDesc = result;
            }.createSequence(handler).createDelegate(this));
        } else {
        	handler.call(this);
        }
    },
    // load Node Context
    loadNodeContext : function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        this.getNodeContext.createDelegate(this,[this.getRpcNode.createDelegate(this,[this.getNodeDesc.createDelegate(this,[handler])])]).call(this);
    },
    loadSettings : function() {
    	Ext.MessageBox.wait(i18n._("Loading Settings..."), i18n._("Please wait"));
        this.settingsClassName = Ung.Settings.getClassName(this.name);
        if (!this.settingsClassName) {
            // Dynamically load node javaScript
            Ung.Settings.loadNodeScript(this, function() {
                this.settingsClassName = Ung.Settings.getClassName(this.name);
                this.initSettings();
            }.createDelegate(this));
        } else {
            this.initSettings();
        }
    },
    // init settings
    initSettings : function() {
    	this.loadNodeContext.createDelegate(this,[this.initSettingsTranslations.createDelegate(this,[this.openSettings.createDelegate(this)])]).call(this);
    },
    initSettingsTranslations : function(handler) {
        if(Ung.Settings.dependency[this.name]) {
            var dependencyName=Ung.Settings.dependency[this.name].name;
            Ung.Util.loadModuleTranslations.call(this, dependencyName, null, function(nodeName,dependencyName) {
                Ung.Util.loadModuleTranslations.call(this,nodeName, Ung.i18nModuleInstances[dependencyName].moduleMap,handler);
            }.createDelegate(this,[this.name, dependencyName]));
        } else {
        	Ung.Util.loadModuleTranslations.call(this, this.name, null, handler);
        }
    },
    // open settings window
    openSettings : function() {
    	var items=null;
        if (this.settingsClassName !== null) {
            eval('this.settings=new ' + this.settingsClassName + '({\'node\':this,\'tid\':this.tid,\'name\':this.name});');
            items=this.settings;
        } else {
            items =[{
            	anchor: '100% 100%',
            	bodyStyle : "padding: 15px 5px 5px 15px;",
                html: String.format(i18n._("Error: There is no settings class for the node '{0}'."),this.name)
            }];
        }
        this.settingsWin = new Ung.NodeSettingsWin({
            nodeCmp : this,
            items: items
        });
        this.settingsWin.show();
        Ext.MessageBox.hide();
    },

    // remove node
    removeAction : function()
    {
        /* A hook for doing something in a node before attempting to remove it */
        if ( this.settings.preRemoveAction ) {
            this.settings.preRemoveAction( this, this.completeRemoveAction.createDelegate( this ));
            return;
        }
        
        this.completeRemoveAction();
    },

    completeRemoveAction : function()
    {
        var message = this.md.displayName
                + " is about to be removed from the rack.\nIts settings will be lost and it will stop processing network traffic.\n\nWould you like to continue removing?";
        Ext.Msg.confirm(i18n._("Warning:"), message, function(btn, text) {
            if (btn == 'yes') {
                if (this.settingsWin) {
                    this.settingsWin.cancelAction();
                }
                this.setState("Attention");
                this.getEl().mask();
                Ung.AppItem.updateStateForNode(this.name, "uninstalling");
                rpc.nodeManager.destroy(function(result, exception) {
                    if(Ung.Util.handleException(exception, function() {
                        this.getEl().unmask();
                    }.createDelegate(this),"alert")) return;
                    if (this) {
                        var nodeName = this.name;
                        var cmp = this;
                        Ext.destroy(cmp);
                        cmp = null;
                        for (var i = 0; i < main.nodes.length; i++) {
                            if (nodeName == main.nodes[i].name) {
                                main.nodes.splice(i, 1);
                                break;
                            }
                        }
                        main.updateSeparator();
                        // update AppItem button
                        main.loadApps();
                    }
                }.createDelegate(this), this.Tid);
            }
        }.createDelegate(this));
    },
    // initialize blingers
    initBlingers : function() {
        if (this.blingers !== null) {
            if(this.blingers.activityDescs!=null && this.blingers.activityDescs.list.length>0) {
                this.activityBlinger=new Ung.ActivityBlinger({
                   parentId : this.getId(),
                   bars: this.blingers.activityDescs.list
                });
                this.activityBlinger.render('nodeBlingers_' + this.getId());
                this.subCmps.push(this.activityBlinger);
            }
            var dispMetricDescs=[];
            if(this.blingers.metricDescs!=null) {
                for(var i=0;i<this.blingers.metricDescs.list.length;i++) {
                    if(this.blingers.metricDescs.list[i].displayable) {
                        dispMetricDescs.push(this.blingers.metricDescs.list[i])
                    }
                }
            }
            this.blingers.dispMetricDescs=dispMetricDescs;
            if(this.blingers.dispMetricDescs.length>0) {
                this.systemBlinger=new Ung.SystemBlinger({
                   parentId : this.getId(),
                   metric: this.blingers.dispMetricDescs
                });
                this.systemBlinger.render('nodeBlingers_' + this.getId());
                this.subCmps.push(this.systemBlinger);
            }
        }
    }
});
// Get node component by tid
Ung.Node.getCmp = function(tid) {
    return Ext.getCmp("node_" + tid);
};

Ung.Node.getStatusTip = function() {
    return [
            '<div style="text-align: left;">',
            i18n._("The <B>Status Indicator</B> shows the current operating condition of a particular software product."),
            '<BR>',
            '<font color="#00FF00"><b>' + i18n._("Green") + '</b></font> '
                    + i18n._('indicates that the product is "on" and operating normally.'),
            '<BR>',
            '<font color="#FF0000"><b>' + i18n._("Red") + '</b></font> '
                    + i18n._('indicates that the product is "on", but that an abnormal condition has occurred.'),
            '<BR>',
            '<font color="#FFFF00"><b>' + i18n._("Yellow") + '</b></font> '
                    + i18n._('indicates that the product is saving or refreshing settings.'), '<BR>',
            '<b>' + i18n._("Clear") + '</b> ' + i18n._('indicates that the product is "off", and may be turned "on" by the user.'),
            '</div>'].join('');
}
Ung.Node.getPowerTip = function() {
    return i18n._('The <B>Power Button</B> allows you to turn a product "on" and "off".');
};
Ung.Node.template = new Ext.Template('<div class="nodeImage"><img src="{image}"/></div>', '<div class="nodeLabel">{displayName}</div>',
        '<div class="nodeTrialDays">{trialDays}</div>', '<div class="nodeTrial">{trialFlag}</div>',
        '<div class="nodeBlingers" id="nodeBlingers_{id}"></div>', '<div class="nodeState" id="nodeState_{id}" name="State"></div>',
        '<div class="nodePower" id="nodePower_{id}" name="Power"></div>',
        '<div class="nodeSettingsButton" id="nodeSettingsButton_{id}"></div>',
        '<div class="nodeHelpButton" id="nodeHelpButton_{id}"></div>');

// Message Manager object
Ung.MessageManager = {
    // update interval in millisecond
    updateTime : 5000,
    started : false,
    intervalId : null,
    cycleCompleted : true,
    upgradeMode: false,
    upgradeUpdateTime:1000,
    upgradeSummary: null, 
    upgradesComplete: 0,
    messageHistory:[], // for debug info
    errorToleranceTime: null,
    
    start : function(now) {
        this.stop();
        if(now) {
        	Ung.MessageManager.run();
        }
        this.intervalId = window.setInterval("Ung.MessageManager.run()", this.updateTime);
        this.started = true;
    },
    resetErrorTolerance: function () {
    	this.errorToleranceTime=(new Date()).getTime();
    },
    startUpgradeMode: function() {
        this.stop();
        this.upgradeMode=true;
        this.intervalId = window.setInterval("Ung.MessageManager.run()", this.upgradeUpdateTime);
        this.started = true;
    	
    },
    stop : function() {
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        }
        this.cycleCompleted = true;
        this.started = false;
    },
    run : function () {
        if (!this.cycleCompleted) {
            return;
        }
        this.cycleCompleted = false;
        rpc.messageManager.getMessageQueue(function(result, exception) {
            if(Ung.Util.handleException(exception, function() {
                var tolearteError=exception.code==500 && this.errorToleranceTime!=null && (new Date()).getTime()-this.errorToleranceTime<60000;
                if(tolearteError) {
                    //Tolerate Error 500: Internal Server Error after an install 
                    //Keep silent because apache may reload
                    this.cycleCompleted = true;
                    return;
                }
                Ext.MessageBox.alert(i18n._("Failed"), exception.message, function() {
                    this.cycleCompleted = true;
                }.createDelegate(this));
        	}.createDelegate(this),"noAlert")) return;
           this.cycleCompleted = true;
           try {
               var messageQueue=result;
               if(messageQueue.messages.list!=null && messageQueue.messages.list.length>0) {
                   var refreshApps=false;
                   Ung.MessageManager.messageHistory.push(messageQueue.messages.list); // for
                                                                                        // debug
                                                                                        // info
                   for(var i=0;i<messageQueue.messages.list.length;i++) {
                       var msg=messageQueue.messages.list[i];
                        if (msg.javaClass.indexOf("MackageInstallRequest") >= 0) {
                        	if(!msg.installed) {
                                var policy=null;
                                policy = rpc.currentPolicy;
                                var appItemName=msg.mackageDesc.type=="TRIAL"?msg.mackageDesc.fullVersion:msg.mackageDesc.name
                                Ung.AppItem.updateState(appItemName, "download");
                                this.resetErrorTolerance();
                                rpc.toolboxManager.installAndInstantiate(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                }.createDelegate(this),msg.mackageDesc.name, policy);
                        	}
                        } else if(msg.javaClass.indexOf("NodeInstantiated") != -1) {
                        	refreshApps=true;
                            var node=main.getNode(msg.nodeDesc.mackageDesc.name)
                            if(!node) {
                                var node=main.createNode(msg.nodeDesc.tid, msg.nodeDesc.mackageDesc, msg.statDescs, msg.licenseStatus,"INITIALIZED");
                                main.nodes.push(node);
                                main.addNode(node, true);
                            }
                            main.startNode(Ung.Node.getCmp(node.tid));
                        } else if(msg.javaClass.indexOf("InstallAndInstantiateComplete") != -1) {
                        	refreshApps=true;
                        } else {
                        	if(msg.upgrade==false) {
                        		var appItemName=msg.requestingMackage.type=="TRIAL"?msg.requestingMackage.fullVersion:msg.requestingMackage.name; 
                                if(msg.javaClass.indexOf("DownloadSummary") != -1) {
                                	this.resetErrorTolerance();
                                	Ung.AppItem.updateState(appItemName, "download");
                                } else if(msg.javaClass.indexOf("DownloadProgress") != -1) {
                                	this.resetErrorTolerance();
                                	Ung.AppItem.updateState(appItemName, "download_progress", msg);
                                } else if(msg.javaClass.indexOf("DownloadComplete") != -1) {
                                	this.resetErrorTolerance();
                                	Ung.AppItem.updateState(appItemName, "download");
                                } else if(msg.javaClass.indexOf("InstallComplete") != -1) {
                                	this.resetErrorTolerance();
                                	Ung.AppItem.updateState(appItemName, "installing");
                                } else if(msg.javaClass.indexOf("InstallTimeout") != -1) {
                                    Ung.AppItem.updateState(appItemName, "activate_timeout");
                                }
                        	} else if(msg.upgrade==true) {
                                if(msg.javaClass.indexOf("DownloadSummary") != -1) {
                                	this.resetErrorTolerance();
                                	Ext.MessageBox.wait(i18n._("Downloading updates..."), i18n._("Please wait"));
                                	this.upgradeSummary=msg;
                                } else if(msg.javaClass.indexOf("DownloadProgress") != -1) {
                                	this.resetErrorTolerance();
                                	var text=String.format(i18n._("Downloading {0}. <br/>Status: {1} KB/{2} KB downloaded. <br/>Speed: {3}."),msg.name, Math.round(msg.bytesDownloaded/1024), Math.round(msg.size/1024), msg.speed);
                                	if(this.upgradeSummary) {
                                		text+=String.format(i18n._("<br/>Package {0}/{1}."),this.upgradesComplete+1, this.upgradeSummary.count);
                                	}
                                	
                                    Ext.MessageBox.show({
                                               title : i18n._("Updating... Please wait"),
                                               msg : text,
                                               closable: true, 
                                               modal : true,
                                               progress: true,
                                               wait: msg.size==0,
                                               progressText : ""
                                            });
                                    if(msg.size!=0) {
                                        var currentPercentComplete = msg.bytesDownloaded/ msg.size;
                                        var progressIndex = parseFloat(currentPercentComplete);
                                        Ext.MessageBox.updateProgress(progressIndex, "");
                                    }
                                } else if(msg.javaClass.indexOf("DownloadComplete") != -1) {
                                	this.resetErrorTolerance();
                                	this.upgradesComplete++;
                                } else if(msg.javaClass.indexOf("InstallComplete") != -1) {
                                	this.resetErrorTolerance();
                                	this.stop();
                                	Ext.MessageBox.alert(
                                	   i18n._("Upgrade Successful"),
                                	   i18n._("The Upgrade succeeded. You will be redirected to the start page now. After an upgrade the UVM may restart making the console temporary unavailable. So you might have to wait a few minutes before you can log in again."),
                                	   Ung.Util.goToStartPage);
                                } else if(msg.javaClass.indexOf("InstallTimeout") != -1) {
                                	this.stop();
                                    Ext.MessageBox.alert(
                                       i18n._("Upgrade Timeout"),
                                       i18n._("The Upgrade failed. You will be redirected to the start page now. After an upgrade the UVM may restart making the console temporary unavailable. So you might have to wait a few minutes before you can log in again."),
                                       Ung.Util.goToStartPage);
                                }
                                if(!this.upgradeMode) {
                                    this.startUpgradeMode();
                                }
                                
                        	}
                        }
                    }
                    if(refreshApps && !Ung.MessageManager.upgradeMode) {
                        main.updateSeparator();
                        main.loadApps();
                    }
                }
                if(!Ung.MessageManager.upgradeMode) {
                    // update system stats
                    main.systemStats.update(messageQueue.systemStats)
                    // upgrade nodes blingers
                    for (var i = 0; i < main.nodes.length; i++) {
                        var nodeCmp = Ung.Node.getCmp(main.nodes[i].tid);
                        if (nodeCmp && nodeCmp.isRunning()) {
                            nodeCmp.stats = messageQueue.stats.map[main.nodes[i].tid];
                            nodeCmp.updateBlingers();
                        }
                    }
                }
            } catch (err) {
                Ext.MessageBox.alert("Exception in MessageManager", err.message);
            }
        }.createDelegate(this), rpc.messageKey, rpc.currentPolicy);
    }
};
Ung.SystemStats = Ext.extend(Ext.Component, {
    autoEl : 'div',
    renderTo: "rack_list",
    constructor : function(config) {
        this.id = "system_stats";
        Ung.SystemStats.superclass.constructor.apply(this, arguments);
    },
    onRender : function(container, position) {
        Ung.SystemStats.superclass.onRender.call(this, container, position);
        this.getEl().addClass("systemStats");
        var contentSystemStatsArr=[
            '<div class="label" style="width:100px;left:0px;">'+i18n._("Network")+'</div>',
            '<div class="label" style="width:70px;left:103px;">'+i18n._("CPU Load")+'</div>',
            '<div class="label" style="width:75px;left:180px;">'+i18n._("Memory")+'</div>',
            '<div class="label" style="width:40px;right:-5px;">'+i18n._("Disk")+'</div>',
            '<div class="network"><div class="tx">'+i18n._("Tx:")+'<div class="tx_value"></div></div><div class="rx">'+i18n._("Rx:")+'<div class="rx_value"></div></div></div>',
            '<div class="cpu"></div>',
            '<div class="memory"><div class="free">'+i18n._("F:")+'<div class="free_value"></div></div><div class="used">'+i18n._("U:")+'<div class="used_value"></div></div></div>',
            '<div class="disk"><div name="disk_value"></div></div>'
        ];
        this.getEl().insertHtml("afterBegin", contentSystemStatsArr.join(''));

        // network tooltip
        var networkArr=[
            '<div class="title">'+i18n._("RX Speed:")+'</div>',
            '<div class="values"><span name="rx_speed"></span> KB/sec</div>',
            '<div class="title">'+i18n._("TX Speed:")+'</div>',
            '<div class="values"><span name="tx_speed"></span> KB/sec</div>'
        ];
        this.networkToolTip= new Ext.ToolTip({
            target: this.getEl().child("div[class=network]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extendedStats',
            html: networkArr.join(''),
            show : function(){
                this.showBy("system_stats","tl-bl?");
            }
        });
        this.networkToolTip.render(Ext.getBody());

        // cpu tooltip
        var cpuArr=[
            '<div class="title">'+i18n._("Number of Processors / Type / Speed:")+'</div>',
            '<div class="values"><span name="num_cpus"></span>, <span name="cpu_model"></span>, <span name="cpu_speed"></span></div>',
            '<div class="title">'+i18n._("Uptime:")+'</div>',
            '<div class="values"><span name="uptime"></span> sec</div>',
            '<div class="title">'+i18n._("Tasks (Processes)")+
            // "/"+i18n._("Threads")+
            '</div>',
            '<div class="values"><span name="tasks"></span>'+
            // ', <span name="threads"></span>'+
            '</div>',
            '<div class="title">'+i18n._("CPU Utilization by User:")+'</div>',
            '<div class="values"><span name="cpu_utilization_user"></span>  %</div>',
            '<div class="title">'+i18n._("CPU Utilization by System:")+'</div>',
            '<div class="values"><span name="cpu_utilization_system"></span> %</div>',
            '<div class="title">'+i18n._("Load average (1 min / 5 min / 15 min):")+'</div>',
            '<div class="values"><span name="load_average_1_min"></span>, <span name="load_average_5_min"></span>, <span name="load_average_15_min"></span></div>'
        ];
        this.cpuToolTip= new Ext.ToolTip({
            target: this.getEl().child("div[class=cpu]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extendedStats',
            html: cpuArr.join(''),
            show : function(){
                this.showBy("system_stats","tl-bl?");
            }
        });
        this.cpuToolTip.render(Ext.getBody());

        // memory tooltip
        var memoryArr=[

            '<div class="title">'+i18n._("Total Memory:")+'</div>',
            '<div class="values"><span name="memory_total"></span> MBs</div>',
            '<div class="title">'+i18n._("Memory Used:")+'</div>',
            '<div class="values"><span name="memory_used"></span> MBs, <span name="memory_used_percent"></span> %</div>',
            '<div class="title">'+i18n._("Memory Free:")+'</div>',
            '<div class="values"><span name="memory_free"></span> MBs, <span name="memory_free_percent"></span> %</div>',
            '<div class="title">'+i18n._("Memory Pages:")+'</div>',
            '<div class="values"><span name="memory_pages_active"></span> MBs '+i18n._("active")+'</div>',
            '<div class="values"><span name="memory_pages_inactive"></span> MBs '+i18n._("inactive")+'</div>',
            '<div class="values"><span name="memory_pages_cached"></span> MBs '+i18n._("cached")+', <span name="memory_pages_buffers"></span> MBs '+i18n._("buffers")+'</div>',
            '<div class="title">'+i18n._("VM Statistics:")+'</div>',
            '<div class="values"><span name="vm_pageins"></span> '+i18n._("pageins")+', <span name="vm_pageouts"></span> '+i18n._("pageouts")+'</div>',
            '<div class="values"><span name="vm_page_faults"></span> '+i18n._("page faults")+'</div>',
            '<div class="title">'+i18n._("Swap Files:")+'</div>',
            '<div class="values"><span name="swap_total"></span> MBs '+i18n._("total swap space")+' (<span name="swap_used"></span> MBs '+i18n._("used")+')</div>'
        ];
        this.memoryToolTip= new Ext.ToolTip({
            target: this.getEl().child("div[class=memory]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extendedStats',
            html: memoryArr.join(''),
            show : function(){
                this.showBy("system_stats","tl-bl?");
            }
        });
        this.memoryToolTip.render(Ext.getBody());

        // disk tooltip
        var diskArr=[
            '<div class="title">'+i18n._("Total Disk Space:")+'</div>',
            '<div class="values"><span name="total_disk_space"></span> GBs</div>',
            '<div class="title">'+i18n._("Free Disk Space:")+'</div>',
            '<div class="values"><span name="free_disk_space"></span> GBs</div>',
            '<div class="title">'+i18n._("Data read:")+'</div>',
            '<div class="values"><span name="disk_reads"></span> MB, <span name="disk_reads_per_second"></span> b/sec</div>',
            '<div class="title">'+i18n._("Data write:")+'</div>',
            '<div class="values"><span name="disk_writes"></span> MB, <span name="disk_writes_per_second"></span> b/sec</div>'
        ];
        this.diskToolTip= new Ext.ToolTip({
            target: this.getEl().child("div[class=disk]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extendedStats',
            html: diskArr.join(''),
            show : function(){
                this.showBy("system_stats","tl-bl?");
            }
        });
        this.diskToolTip.render(Ext.getBody());

    },
    update : function(stats) {
        this.getEl().child("div[class=cpu]").dom.innerHTML=Math.round((stats.map.userCpuUtilization+stats.map.systemCpuUtilization)*100.0/stats.map.numCpus)+"%";
        var txSpeed=Math.round(stats.map.txBps/10)/100;
        var rxSpeed=Math.round(stats.map.rxBps/10)/100;
        this.getEl().child("div[class=tx_value]").dom.innerHTML=txSpeed+"KB/sec";
        this.getEl().child("div[class=rx_value]").dom.innerHTML=rxSpeed+"KB/sec";
        var memoryFree=Ung.Util.bytesToMBs(stats.map.MemFree)
        var memoryUsed=Ung.Util.bytesToMBs(stats.map.MemTotal-stats.map.MemFree);
        this.getEl().child("div[class=free_value]").dom.innerHTML=memoryFree+" MBs";
        this.getEl().child("div[class=used_value]").dom.innerHTML=memoryUsed+" MBs";
        var diskPercent=Math.round((1-stats.map.freeDiskSpace/stats.map.totalDiskSpace)*20 )*5;
        this.getEl().child("div[name=disk_value]").dom.className="disk"+diskPercent;
        if(this.networkToolTip.rendered) {
            var toolTipEl=this.networkToolTip.getEl();
            toolTipEl.child("span[name=tx_speed]").dom.innerHTML=txSpeed;
            toolTipEl.child("span[name=rx_speed]").dom.innerHTML=rxSpeed;
            /*
             * toolTipEl.child("span[name=data_received]").dom.innerHTML="TODO";
             * toolTipEl.child("span[name=data_sent]").dom.innerHTML="TODO";
             * toolTipEl.child("span[name=total_throughput]").dom.innerHTML="TODO";
             */
        }
        if(this.cpuToolTip.rendered) {
            var toolTipEl=this.cpuToolTip.getEl();
            toolTipEl.child("span[name=num_cpus]").dom.innerHTML=stats.map.numCpus;
            toolTipEl.child("span[name=cpu_model]").dom.innerHTML=stats.map.cpuModel;
            toolTipEl.child("span[name=cpu_speed]").dom.innerHTML=stats.map.cpuSpeed;
            toolTipEl.child("span[name=uptime]").dom.innerHTML=Math.round(stats.map.uptime);
            toolTipEl.child("span[name=tasks]").dom.innerHTML=stats.map.numProcs;
            // toolTipEl.child("span[name=threads]").dom.innerHTML="TODO";
            toolTipEl.child("span[name=cpu_utilization_user]").dom.innerHTML=Math.round(stats.map.userCpuUtilization*100.0/stats.map.numCpus);
            toolTipEl.child("span[name=cpu_utilization_system]").dom.innerHTML=Math.round(stats.map.systemCpuUtilization*100.0/stats.map.numCpus);
            toolTipEl.child("span[name=load_average_1_min]").dom.innerHTML=stats.map.oneMinuteLoadAvg;
            toolTipEl.child("span[name=load_average_5_min]").dom.innerHTML=stats.map.fiveMinuteLoadAvg;
            toolTipEl.child("span[name=load_average_15_min]").dom.innerHTML=stats.map.fifteenMinuteLoadAvg;
        }
        if(this.memoryToolTip.rendered) {
            var toolTipEl=this.memoryToolTip.getEl();
            toolTipEl.child("span[name=memory_used]").dom.innerHTML=memoryUsed;
            toolTipEl.child("span[name=memory_free]").dom.innerHTML=memoryFree;
            toolTipEl.child("span[name=memory_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.MemTotal);
            toolTipEl.child("span[name=memory_used_percent]").dom.innerHTML=Math.round((stats.map.MemTotal-stats.map.MemFree)/stats.map.MemTotal*100);
            toolTipEl.child("span[name=memory_free_percent]").dom.innerHTML=Math.round(stats.map.MemFree/stats.map.MemTotal*100);
            toolTipEl.child("span[name=memory_pages_active]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.Active);
            toolTipEl.child("span[name=memory_pages_inactive]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.Inactive);
            toolTipEl.child("span[name=memory_pages_cached]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.Cached);
            toolTipEl.child("span[name=memory_pages_buffers]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.Buffers);

            toolTipEl.child("span[name=vm_pageins]").dom.innerHTML=stats.map.pgpgin;
            toolTipEl.child("span[name=vm_pageouts]").dom.innerHTML=stats.map.pgpgout;
            toolTipEl.child("span[name=vm_page_faults]").dom.innerHTML=stats.map.pgfault;
            toolTipEl.child("span[name=swap_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.SwapTotal);
            toolTipEl.child("span[name=swap_used]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.SwapTotal-stats.map.SwapFree);
        }
        if(this.diskToolTip.rendered) {
            var toolTipEl=this.diskToolTip.getEl();
            toolTipEl.child("span[name=total_disk_space]").dom.innerHTML=Math.round(stats.map.totalDiskSpace/10000000)/100;
            toolTipEl.child("span[name=free_disk_space]").dom.innerHTML=Math.round(stats.map.freeDiskSpace/10000000)/100;
            toolTipEl.child("span[name=disk_reads]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.diskReads);
            toolTipEl.child("span[name=disk_reads_per_second]").dom.innerHTML=Math.round(stats.map.diskReadsPerSecond*100)/100;
            toolTipEl.child("span[name=disk_writes]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.diskWrites);
            toolTipEl.child("span[name=disk_writes_per_second]").dom.innerHTML=Math.round(stats.map.diskWritesPerSecond*100)/100;
        }
    },
    reset : function() {
    }
});

// Activity Blinger Class
Ung.ActivityBlinger = Ext.extend(Ext.Component, {
    autoEl: "div",
    parentId : null,
    bars : null,
    lastValues : null,
    decays : null,
    constructor : function(config) {
        this.id = "blinger_activity_" + config.parentId;
        Ung.ActivityBlinger.superclass.constructor.apply(this, arguments);
    },
    onRender : function(container, position) {
        Ung.ActivityBlinger.superclass.onRender.call(this, container, position);
        this.getEl().addClass("activityBlinger")
        var templateHTML = Ung.ActivityBlinger.template.applyTemplate({
            'id' : this.getId(),
            'blingerName' : i18n._("activity")
        });
        this.getEl().insertHtml("afterBegin", templateHTML);
        this.lastValues = [];
        this.decays = [];
        if (this.bars !== null) {
            var out = [];
            for (var i = 0; i < this.bars.length; i++) {
                var bar = this.bars[i].action;
                var top = 3 + i * 15;
                this.lastValues.push(null);
                this.decays.push(0);
                out.push('<div class="blingerText activityBlingerText" style="top:' + top + 'px;">' + i18n._(bar) + '</div>');
                out.push('<div class="activityBlingerBar" style="top:' + top + 'px;width:0px;display:none;" id="activityBar_'
                        + this.getId() + '_' + i + '"></div>');
            }
            Ext.get("blingerBox_" + this.getId()).insertHtml("afterBegin", out.join(""));
        }
    },

    update : function(stats) {
        for (var i = 0; i < this.bars.length; i++) {
            var top = 3 + i * 15;
            var bar = this.bars[i];
            var barPixelWidth=0;
            if(stats.activities.map[bar.name]!=null) {
                var newValue = stats.activities.map[bar.name].count;
                this.decays[i] = Ung.ActivityBlinger.decayValue(newValue, this.lastValues[i], this.decays[i]);
                this.lastValues[i] = newValue;
                barPixelWidth = Math.floor(this.decays[i]);
            }
            var barDiv = document.getElementById('activityBar_' + this.getId() + '_' + i);
            barDiv.style.width = barPixelWidth + "px";
            barDiv.style.display = (barPixelWidth === 0) ? "none" : "";
        }
    },
    reset : function() {
        for (var i = 0; i < this.bars.length; i++) {
            this.lastValues[i] = null;
            this.decays[i] = 0;
            var barDiv = document.getElementById('activityBar_' + this.getId() + '_' + i);
            barDiv.style.width = "0px";
            barDiv.style.display = "none";
        }
    }

});
Ung.ActivityBlinger.template = new Ext.Template('<div class="blingerName">{blingerName}</div>',
        '<div class="activityBlingerBox" id="blingerBox_{id}"></div>');
Ung.ActivityBlinger.decayFactor = Math.pow(0.94, Ung.MessageManager.updateTime / 1000);
Ung.ActivityBlinger.decayValue = function(newValue, lastValue, decay) {
    if (lastValue !== null && newValue != lastValue) {
        decay = 98;
    } else {
        decay = decay * Ung.ActivityBlinger.decayFactor;
    }
    return decay;
};
Ext.ComponentMgr.registerType('ungActivityBlinger', Ung.ActivityBlinger);

// System Blinger Class
Ung.SystemBlinger = Ext.extend(Ext.Component, {
    autoEl:"div",
    parentId : null,
    data : null,
    byteCountCurrent : null,
    byteCountLast : null,
    sessionCountCurrent : null,
    sessionCountTotal : null,
    sessionRequestLast : null,
    sessionRequestTotal : null,
    constructor : function(config) {
        this.id = "blinger_system_" + config.parentId;
        Ung.SystemBlinger.superclass.constructor.apply(this, arguments);
    },

    onRender : function(container, position) {
        Ung.SystemBlinger.superclass.onRender.call(this, container, position);
        this.getEl().addClass("systemBlinger")
        var templateHTML = Ung.SystemBlinger.template.applyTemplate({
            'id' : this.getId(),
            'blingerName' : i18n._("system")
        });
        this.getEl().insertHtml("afterBegin", templateHTML);

        var out = [];
        for (var i = 0; i < 4; i++) {
            var top = 1 + i * 15;
            out.push('<div class="blingerText systemBlingerLabel" style="top:' + top + 'px;" id="systemName_' + this.getId() + '_' + i + '"></div>');
            out.push('<div class="blingerText systemBlingerValue" style="top:' + top + 'px;" id="systemValue_' + this.getId() + '_' + i + '"></div>');
        }
        var blingerBoxEl=Ext.get("blingerBox_" + this.getId());
        blingerBoxEl.insertHtml("afterBegin", out.join(""));
        this.buildActiveMetrics();

        blingerBoxEl.on("click", this.showBlingerSettings , this);

    },
    buildActiveMetrics : function () {
        var nodeCmp = Ext.getCmp(this.parentId);
        var activeMetrics=nodeCmp.blingers.activeMetrics.list;
        if(activeMetrics.length>4) {
            Ext.MessageBox.alert(i18n._("Failed"), String.format(i18n._("The node {0} has {1} metrics. The maximum number of metrics is {2}."),nodeCmp.displayName ,activeMetrics.length,4));
        }
        var metricsLen=Math.min(activeMetrics.length,4);
        for(var i=0; i<metricsLen;i++) {
            var activeMetric=activeMetrics[i];
            for(var j=0;j<nodeCmp.blingers.dispMetricDescs.length;j++) {
                var metric=nodeCmp.blingers.dispMetricDescs[j];
                if(activeMetric.name==metric.name) {
                    activeMetric.metricDesc=metric;
                    activeMetric.index=i;
                    var nameDiv=document.getElementById('systemName_' + this.getId() + '_' + i);
                    var valueDiv=document.getElementById('systemValue_' + this.getId() + '_' + i);
                    nameDiv.innerHTML = i18n._(metric.displayName);
                    nameDiv.style.display="";
                    valueDiv.innerHTML = "&nbsp;";
                    valueDiv.style.display="";
                }
            }
        }
        for(var i=activeMetrics.length; i<4;i++) {
            var nameDiv=document.getElementById('systemName_' + this.getId() + '_' + i);
            var valueDiv=document.getElementById('systemValue_' + this.getId() + '_' + i);
            nameDiv.innerHTML = "&nbsp;";
            nameDiv.style.display="none";
            valueDiv.innerHTML = "&nbsp;";
            valueDiv.style.display="none";
        }

    },
    showBlingerSettings : function() {
        var nodeCmp = Ext.getCmp(this.parentId);
        this.tempMetrics=[];
        if(this.configWin==null) {
            var configItems=[];
            for(var i=0;i<nodeCmp.blingers.dispMetricDescs.length;i++) {
                var metric=nodeCmp.blingers.dispMetricDescs[i];
                configItems.push({
                    xtype : 'checkbox',
                    boxLabel : i18n._(metric.displayName),
                    hideLabel : true,
                    name : metric.displayName,
                    dataIndex: metric.name,
                    checked : false,
                    listeners : {
                        "check" : {
                            fn : function(elem, checked) {
                                if(checked && this.tempMetrics.length>=4) {
                                    Ext.MessageBox.alert(i18n._("Failed"),i18n._("Please set up to four items."));
                                    elem.setValue(false);
                                    return;
                                }
                                var itemIndex=-1;
                                for(var i=0;i<this.tempMetrics.length;i++) {
                                    if(this.tempMetrics[i]==elem.dataIndex) {
                                        itemIndex=i;
                                        break;
                                    }
                                }
                                if(checked) {
                                    if(itemIndex==-1) {
                                        // add element
                                        this.tempMetrics.push(elem.dataIndex);
                                    }
                                } else {
                                    if(itemIndex!=-1) {
                                        // remove element
                                        this.tempMetrics.splice(itemIndex,1);
                                    }
                                }

                            }.createDelegate(this)
                        }
                    }
                });
            }
            this.configWin=new Ung.Window({
                blingerCmp: this,
                modal : true,
                title:"Set up to four",
                bodyStyle : "padding: 5px 5px 5px 15px;",
                items: configItems,
                autoScroll : true,
                draggable : true,
                resizable : true,
                renderTo : 'container',
                buttons: [{
                    name : 'Ok',
                    text : i18n._("Ok"),
                    handler : function() {
                        this.updateActiveMetrics();
                        this.configWin.hide();
                    }.createDelegate(this)
                },{
                    name : 'Cancel',
                    text : i18n._("Cancel"),
                    handler : function() {
                        this.configWin.hide();
                    }.createDelegate(this)
                }],
                show : function() {
                    Ung.Window.superclass.show.call(this);
                    this.setSize({width:260,height:280});
                    this.alignTo(this.blingerCmp.getEl(),"tr-br");
                    var pos=this.getPosition();
                    var sub=pos[1]+280-main.viewport.getSize().height;
                    if(sub>0) {
                        this.setPosition( pos[0],pos[1]-sub);
                    }
                }
            });
        }

        var activeMetrics=nodeCmp.blingers.activeMetrics.list;
        for(var i=0;i<this.configWin.items.length;i++) {
            this.configWin.items.get(i).setValue(false);
        }
        for(var j=0;j<activeMetrics.length;j++) {
            for(var i=0;i<this.configWin.items.length;i++) {
                var metricItem=this.configWin.items.get(i)
                if(activeMetrics[j].name==metricItem.dataIndex) {
                    metricItem.setValue(true);
                    break;
                }
            }
        }
        this.configWin.show();
    },
    updateActiveMetrics : function() {
        var activeMetrics=[];
        var nodeCmp = Ext.getCmp(this.parentId);
        for(var i=0; i<this.tempMetrics.length;i++) {
            for(var j=0;j<nodeCmp.blingers.dispMetricDescs.length;j++) {
                var metric=nodeCmp.blingers.dispMetricDescs[j];
                if(this.tempMetrics[i]==metric.name) {
                    activeMetrics.push({
                       javaClass : "com.untangle.uvm.message.ActiveStat",
                       name : metric.name,
                       interval: "SINCE_MIDNIGHT"
                    });
                }
            }
        }
        rpc.messageManager.setActiveMetrics(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var nodeCmp = Ext.getCmp(this.parentId);
            nodeCmp.blingers.activeMetrics.list=activeMetrics;
            this.buildActiveMetrics();
        }.createDelegate(this),nodeCmp.Tid,{javaClass:"java.util.List", list:activeMetrics});

    },
    update : function(stats) {
        // UPDATE COUNTS
        var nodeCmp = Ext.getCmp(this.parentId);
        var activeMetrics=nodeCmp.blingers.activeMetrics.list;
        for (var i = 0; i < activeMetrics.length; i++) {
            var activeMetric=activeMetrics[i];
            var newValue="&nbsp;"
            if(stats.metrics.map[activeMetric.name]!=null) {
                newValue=stats.metrics.map[activeMetric.name].count;
                if(activeMetric.metricDesc!=null && activeMetric.metricDesc.unit!=null) {
                    newValue +=activeMetric.metricDesc.unit;
                }
            }
            var valueDiv = document.getElementById('systemValue_' + this.getId() + '_' + activeMetric.index);
            if(valueDiv!=null) {
                valueDiv.innerHTML = newValue;
            }
        }
    },
    reset : function() {
        for (var i = 0; i < 4; i++) {
            var valueDiv = document.getElementById('systemValue_' + this.getId() + '_' + i);
            valueDiv.innerHTML = "&nbsp;";
        }
}
});
Ung.SystemBlinger.template = new Ext.Template('<div class="blingerName">{blingerName}</div>',
        '<div class="systemBlingerBox" id="blingerBox_{id}"></div>',
        '<div class="systemStatSettings" id="systemStatSettings_{id}"></div>');
Ext.ComponentMgr.registerType('ungSystemBlinger', Ung.SystemBlinger);

// Setting base class
Ung.Settings = Ext.extend(Ext.Panel, {
	layout : 'anchor',
	anchor: '100% 100%',
    // node i18n
    i18n : null,
    // node object
    node : null,
    // settings tabs (if the settings has tabs layout)
    tabs : null,
    // holds the json rpc results for the settings class like baseSettings
    // object, repository, repositoryDesc
    rpc : null,
    constructor : function(config) {
        var nodeName=config.node.name;
        this.id = "settings_" + nodeName + "_" + rpc.currentPolicy.id;
        config.rpc = {};
        // initializes the node i18n instance
        config.i18n = Ung.i18nModuleInstances[nodeName];
        Ung.Settings.superclass.constructor.apply(this, arguments);
    },
    // build Tab panel from an array of tab items
    buildTabPanel : function(itemsArray) {
        this.tabs = new Ext.TabPanel({
        	anchor: '100% 100%',
            autoWidth : true,
            defaults: {
            	anchor: '100% 100%',
            	autoWidth : true,
                autoScroll: true
            },

            height : 400,
            activeTab : 0,
            frame : true,
            parentId : this.getId(),
            items : itemsArray,
            layoutOnTabChange : true
        });
        this.items=this.tabs;
    },
    // get nodeContext.rpcNode object
    getRpcNode : function() {
        return this.node.nodeContext.rpcNode;
    },
    // get base settings object
    getBaseSettings : function(forceReload) {
        if (forceReload || this.rpc.baseSettings === undefined) {
        	try {
                this.rpc.baseSettings = this.getRpcNode().getBaseSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.baseSettings;
    },
    // get Validator object
    getValidator : function() {
        if (this.node.nodeContext.rpcNode.validator === undefined) {
        	try {
                this.node.nodeContext.rpcNode.validator = this.getRpcNode().getValidator();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.node.nodeContext.rpcNode.validator;
    },
    // get eventManager object
    getEventManager : function() {
        if (this.node.nodeContext.rpcNode.eventManager === undefined) {
        	try {
                this.node.nodeContext.rpcNode.eventManager = this.node.nodeContext.rpcNode.getEventManager();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.node.nodeContext.rpcNode.eventManager;
    },
    cancelAction : function() {
        this.node.settingsWin.cancelAction()
    },
    // All settings classes must override the save method
    saveAction : function() {
    },
    // validation functions
    validateClient : function() {
        return true;
    },
    validateServer : function() {
        return true;
    },
    validate : function() {
        return this.validateClient() && this.validateServer();
    }
});
Ung.Settings._nodeScripts = {};

// Dynamically loads javascript file for a node
Ung.Settings.loadNodeScript = function(settingsCmp, handler) {
    Ung.Util.loadScript('script/' + settingsCmp.name + '/settings.js', function() {
        this.settingsClassName = Ung.Settings.getClassName(this.name);
        if(!Ung.Settings.dependency[this.name]) {
            handler.call(this);
        } else {
            var dependencyClassName=Ung.Settings.getClassName(Ung.Settings.dependency[this.name].name);
            if(!dependencyClassName) {
                Ung.Util.loadScript('script/' + Ung.Settings.dependency[this.name].name + '/settings.js', function() {
                    Ung.Settings.dependency[this.name].fn.call(this);
                    handler.call(this);
                }.createDelegate(this));
            } else {
                Ung.Settings.dependency[this.name].fn.call(this);
                handler.call(this);
            }
        }
    }.createDelegate(settingsCmp));
};

Ung.Settings.classNames = {};
Ung.Settings.dependency = {};
// Static function get the settings class name for a node
Ung.Settings.getClassName = function(name) {
    var className = Ung.Settings.classNames[name];
    return className === undefined ? null : className;
};
// Static function to register a settings class name for a node
Ung.Settings.registerClassName = function(name, className) {
    Ung.Settings.classNames[name] = className;
};

// Event Log class
Ung.GridEventLog = Ext.extend(Ext.grid.GridPanel, {
    // the settings component
    settingsCmp : null,
    // if the event log has more than one repositories that can be selected
    hasRepositories : true,
    // Event manager rpc function to call
    // default is getEventManager() from settingsCmp
    eventManagerFn : null,
    // Records per page
    recordsPerPage : 20,
    // fields for the Store
    fields : null,
    // columns for the column model
    columns : null,
    enableHdMenu : false,
    enableColumnMove: false,
    // for internal use
    rpc : null,
    helpSource : 'event_log',
    // called when the component is initialized
    initComponent : function() {
        this.rpc = {};

        if (this.title == null) {
            this.title = i18n._('Event Log');
        }
        if (this.autoExpandColumn == null) {
            this.autoExpandColumn = "timestamp";
        }
        if (this.name == null) {
            this.name = "Event Log";
        }
        if (this.eventManagerFn == null && this.hasRepositories == true) {
            this.eventManagerFn = this.settingsCmp.getEventManager();
        }
        this.rpc.repository = {};
        this.store = new Ext.data.Store({
            proxy : new Ung.MemoryProxy({
                root : 'list'
            }),
            sortInfo : this.sortField ? {
                field : this.sortField,
                direction : "ASC"
            } : null,
            remoteSort : true,
            reader : new Ext.data.JsonReader({
                totalProperty : "totalRecords",
                root : 'list',
                fields : this.fields
            })
        });

        this.pagingToolbar = new Ext.PagingToolbar({
            pageSize : this.recordsPerPage,
            store : this.store
        });
        
        this.bbar = [{
            xtype : 'tbtext',
            text : '<span id="boxRepository_' + this.getId() + '_' + this.settingsCmp.node.tid + '"></span>'
        }, {
            xtype : 'tbbutton',
            text : i18n._('Refresh'),
            name : "Refresh",
            tooltip : i18n._('Refresh'),
            iconCls : 'iconRefresh',
            handler : function() {
            	Ext.MessageBox.wait(i18n._("Refreshing..."), i18n._("Please wait"));
                this.refreshList();
            }.createDelegate(this)
        }, {
            xtype : 'tbtext',
            text : '<div style="width:30px;"></div>'
        }, this.pagingToolbar];
        Ung.GridEventLog.superclass.initComponent.call(this);
        var columnModel=this.getColumnModel();
        columnModel.getRenderer = function(col){
            if(!this.config[col].renderer){
                return Ung.Util.defaultRenderer;
            }
            return this.config[col].renderer;
        };
    },
    // called when the component is rendered
    onRender : function(container, position) {
        Ung.GridEventLog.superclass.onRender.call(this, container, position);
        this.getGridEl().child("div[class*=x-grid3-viewport]").set({'name' : "Table"});
        this.pagingToolbar.loading.hide();
        if (this.hasRepositories) {
            this.eventManagerFn.getRepositoryDescs(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                if (this.settingsCmp) {
                    this.rpc.repositoryDescs = result;
                    var repList = this.rpc.repositoryDescs.list;
                    var displayStyle="";
                    if(repList.length==1) {
                        displayStyle="display:none;";
                    }
                    var out = [];
                    out.push('<select name="Event Type" id="selectRepository_' + this.getId() + '_' + this.settingsCmp.node.tid
                            + '" style="'+displayStyle+'">');

                    for (var i = 0; i < repList.length; i++) {
                        var repDesc = repList[i];
                        var selOpt = (i === 0) ? "selected" : "";
                        out.push('<option value="' + repDesc.name + '" ' + selOpt + '>' + this.settingsCmp.i18n._(repDesc.name)
                                + '</option>');
                    }
                    out.push('</select>');
                    var boxRepositoryDescEventLog = document.getElementById('boxRepository_' + this.getId() + '_'
                            + this.settingsCmp.node.tid);
                    boxRepositoryDescEventLog.innerHTML = out.join("");
                }
            }.createDelegate(this));
        }
    },
    // get selected repository
    getSelectedRepository : function() {
        var selObj = document.getElementById('selectRepository_' + this.getId() + '_' + this.settingsCmp.node.tid);
        var result = null;
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    // Refresh the events list
    refreshCallback : function(result, exception) {
        if(Ung.Util.handleException(exception)) return;
        var events = result;
        if (this.settingsCmp !== null) {
            this.getStore().proxy.data = events;
            this.getStore().load({
                params : {
                    start : 0,
                    limit : this.recordsPerPage
                }
            });
        }
        Ext.MessageBox.hide();
    },
    refreshList : function() {
        if (this.hasRepositories) {
            var selRepository = this.getSelectedRepository();
            if (selRepository !== null) {
                if (this.rpc.repository[selRepository] === undefined) {
                    this.eventManagerFn.getRepository(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        this.rpc.repository[selRepository] = result;
                        this.rpc.repository[selRepository].getEvents(this.refreshCallback.createDelegate(this));
                    }.createDelegate(this),selRepository);
                } else {
                    this.rpc.repository[selRepository].getEvents(this.refreshCallback.createDelegate(this));
                }
            }
        }
    }
});

// Standard Ung window
Ung.Window = Ext.extend(Ext.Window, {
    modal : true,
    renderTo : 'container',
    // window title
    title : null,
    // breadcrumbs
    breadcrumbs : null,
    // function called by close action
    closeAction : 'hide',
    draggable : false,
    resizable : false,
    // sub componetns - used by destroy function
    subCmps : null,
    // size to rack right side on show
    sizeToRack : true,
    constructor : function(config) {
        this.subCmps = [];
        Ung.Window.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() {
        if (!this.title) {
            this.title = '<span id="title_' + this.getId() + '"></span>';
        }
        Ung.Window.superclass.initComponent.call(this);
    },
    afterRender : function() {
        Ung.Window.superclass.afterRender.call(this);
        if (this.name && this.getEl()) {
            this.getEl().set({
                'name' : this.name
            });
        }
        if (this.breadcrumbs) {
            this.subCmps.push(new Ung.Breadcrumbs({
                renderTo : 'title_' + this.getId(),
                elements : this.breadcrumbs
            }));
        }
    },

    beforeDestroy : function() {
        Ext.each(this.subCmps, Ext.destroy);
        Ung.Window.superclass.beforeDestroy.call(this);
    },
    // on show position and size
    show : function() {
        if (this.sizeToRack) {
            this.setSizeToRack();
        }
        Ung.Window.superclass.show.call(this);
    },
    setSizeToRack: function () {
        var objSize = main.viewport.getSize();
        var viewportWidth=objSize.width;
        objSize.width = Math.min(viewportWidth,Math.max(1024,viewportWidth - main.contentLeftWidth));
        this.setPosition(viewportWidth-objSize.width, 0);
        objSize.width = Math.min(viewportWidth,Math.max(1024,viewportWidth - main.contentLeftWidth));
        this.setSize(objSize);
    }
    
});
// Buttons Window
// has the content and 3 standard buttons one in the left and 2 in the right
Ung.ButtonsWindow = Ext.extend(Ung.Window, {
    layout : 'anchor',
    defaults: {
    	anchor: '100% 100%',
    	autoScroll: true,
    	autoWidth : true
    },
    closeAction : 'cancelAction',
    // the cancel action
    // to override
    cancelAction : function() {
        this.hide();
    }
});

// Node Settings Window
Ung.NodeSettingsWin = Ext.extend(Ung.ButtonsWindow, {
    nodeCmp : null,
    initComponent : function() {
        this.breadcrumbs = [{
            title : i18n._(rpc.currentPolicy.name),
            action : function() {
                this.cancelAction();
            }.createDelegate(this)
        }, {
            title : this.nodeCmp.md.displayName
        }];
        if(this.bbar==null) {
            this.bbar=[{
                name : "Remove",
                id : this.getId() + "_removeBtn",
                iconCls : 'nodeRemoveIcon',
                text : i18n._('Remove'),
                handler : function() {
                    this.removeAction();
                }.createDelegate(this)
            },{
                name : 'Help',
                id : this.getId() + "_helpBtn",
                iconCls : 'iconHelp',
                text : i18n._('Help'),
                handler : function() {
                    this.helpAction();
                }.createDelegate(this)
            },'->',{
                name : "Cancel",
                id : this.getId() + "_cancelBtn",
                iconCls : 'cancelIcon',
                text : i18n._('Cancel'),
                handler : function() {
                    this.cancelAction();
                }.createDelegate(this)
            },{
                name : "Save",
                id : this.getId() + "_saveBtn",
                iconCls : 'saveIcon',
                text : i18n._('Save'),
                handler : function() {
                    this.saveAction.defer(1, this);
                }.createDelegate(this)
            }];
        }
        Ung.NodeSettingsWin.superclass.initComponent.call(this);
    },
    helpAction : function() {
        var helpSource=this.nodeCmp.helpSource;
        if(this.nodeCmp.settings!=null && this.nodeCmp.settings.tabs!=null && this.nodeCmp.settings.tabs.getActiveTab()!=null) {
            var tabHelpSource=this.nodeCmp.settings.tabs.getActiveTab().helpSource;
            if(tabHelpSource!=null) {
                helpSource+="_"+tabHelpSource;
            }
        }
        main.openHelp(helpSource);
    },
    
    removeAction : function() {
        this.nodeCmp.removeAction();
    },
    cancelAction : function() {
        this.hide();
        if(this.nodeCmp.fnCallback) {
            this.nodeCmp.fnCallback.call();
        }
        Ext.destroy(this);
    },
    saveAction : function() {
        if (this.nodeCmp.settings) {
            this.nodeCmp.settings.saveAction();
        }
    }
});
// Config Window
Ung.ConfigWin = Ext.extend(Ung.ButtonsWindow, {
    // config i18n
    i18n : null,
    // holds the json rpc results for the settings classes
    rpc : null,
    // tabs (if the window has tabs layout)
    tabs : null,
    // class constructor
    constructor : function(config) {
        // for config elements we have the untangle-libuvm translation map
        this.i18n = i18n;
        this.rpc = {};
        Ung.ConfigWin.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() {
        if (!this.name) {
            this.name = "configWin_" + this.name;
        }
        if(this.bbar==null) {
            this.bbar=[{
                name : 'Help',
                id : this.getId() + "_helpBtn",
                iconCls : 'iconHelp',
                text : i18n._('Help'),
                handler : function() {
                    this.helpAction();
                }.createDelegate(this)
            },'->',{
                name : 'Cancel',
                id : this.getId() + "_cancelBtn",
                iconCls : 'cancelIcon',
                text : i18n._('Cancel'),
                handler : function() {
                    this.cancelAction();
                }.createDelegate(this)
            },{
                name : 'Save',
                id : this.getId() + "_saveBtn",
                iconCls : 'saveIcon',
                text : i18n._('Save'),
                handler : function() {
                    this.saveAction.defer(1, this);;
                }.createDelegate(this)
            }];
        }
        Ung.ConfigWin.superclass.initComponent.call(this);
    },
    // build Tab panel from an array of tab items
    buildTabPanel : function(itemsArray) {
        this.tabs = new Ext.TabPanel({
            autoWidth : true,
            height : 400,
            activeTab : 0,
            frame : true,
            parentId : this.getId(),
            items : itemsArray,
            layoutOnTabChange : true
        });
        this.items=this.tabs;
    },
    helpAction : function() {
    	var helpSource=this.helpSource;
    	if(this.tabs && this.tabs.getActiveTab()!=null) {
            var tabHelpSource=this.tabs.getActiveTab().helpSource;
            if(tabHelpSource!=null) {
                helpSource+="_"+tabHelpSource;
            }
    	}
        main.openHelp(helpSource);
    },
    cancelAction : function() {
        this.hide();
        Ext.destroy(this);
    },
    // to override
    saveAction : function() {
        Ung.Util.todo();
    },
    // validation functions
    validateClient : function() {
        return true;
    },
    validateServer : function() {
        return true;
    },
    validate : function() {
        return this.validateClient() && this.validateServer();
    }
});
// update window
// has the content and 3 standard buttons: help, cancel, Update
Ung.UpdateWindow = Ext.extend(Ung.ButtonsWindow, {
    initComponent : function() {
        if(this.bbar==null) {
            this.bbar=['->',{
                name : 'Cancel',
                iconCls : 'cancelIcon',
                text : i18n._('Cancel'),
                handler : function() {
                    this.cancelAction();
                }.createDelegate(this)
            },{
                name : 'Update',
                iconCls : 'saveIcon',
                text : i18n._('Update'),
                handler : function() {
                    this.updateAction();
                }.createDelegate(this)
            }];
        }
        Ung.UpdateWindow.superclass.initComponent.call(this);
    },
    // the update actions
    // to override
    updateAction : function() {
        Ung.Util.todo();
    }
});

// Manage list popup window
Ung.ManageListWindow = Ext.extend(Ung.UpdateWindow, {
    // the editor grid
    grid : null,
    initComponent : function() {
    	this.items=this.grid;
        Ung.ManageListWindow.superclass.initComponent.call(this);
    },
    cancelAction : function() {
        this.grid.changedData = Ext.decode(this.initialChangedData);
        this.grid.initialLoad();
        this.hide();
    },
    updateAction : function() {
        this.hide();
    },
    listeners : {
        'show' : {
            fn : function() {
                this.initialChangedData = Ext.encode(this.grid.changedData);
            }
        }
    }
});

// Row editor window used by editor grid
Ung.RowEditorWindow = Ext.extend(Ung.UpdateWindow, {
    // the editor grid
    grid : null,
    // input lines for standard input lines (text, checkbox, textarea, ..)
    inputLines : null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,    
    // the record currently edit
    record : null,
    // initial record data
    initialRecordData : null,
    sizeToRack : false,
    // size to grid on show
    sizeToGrid : false,
    addMode: null,
    initComponent : function() {
        if (!this.height && !this.width) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Edit');
        }
        if (this.rowEditorLabelWidth == null) {
            this.rowEditorLabelWidth = 100;
        }
        this.items = new Ext.FormPanel({
            anchor: "100% 100%",
            labelWidth : this.rowEditorLabelWidth,
            buttonAlign : 'right',
            border : false,
            bodyStyle : 'padding:10px 10px 0px 10px;',
            autoScroll: true,
            defaults : {
                selectOnFocus : true,
                msgTarget : 'side'
            },
            items : this.inputLines
        });
        this.inputLines=this.items.items.getRange();
        Ung.RowEditorWindow.superclass.initComponent.call(this);
    },
    show : function() {
        Ung.UpdateWindow.superclass.show.call(this);
        var objPosition = this.grid.getPosition();
        this.setPosition(objPosition);
        if (this.sizeToGrid) {
            var objSize = this.grid.getSize();
            this.setSize(objSize);
        }
    },
    // populate is called whent a record is edited, tot populate the edit window
    populate : function(record, addMode) {
        this.addMode=addMode;
        this.record = record;
        this.initialRecordData = Ext.encode(record.data);
        for (var i = 0; i < this.inputLines.length; i++) {
            var inputLine = this.inputLines[i];
            if(inputLine.dataIndex!=null) {
                inputLine.suspendEvents();
                inputLine.setValue(record.get(inputLine.dataIndex));
                inputLine.resumeEvents();
            }
        }
    },
    // check if the form is valid;
    // this is the default functionality which can be overwritten
    isFormValid : function() {
        for (var i = 0; i < this.inputLines.length; i++) {
            var inputLine = this.inputLines[i];
            if (!inputLine.isValid()) {
                return false;
            }
        }
        return true;
    },
    // updateAction is called to update the record after the edit
    updateAction : function() {
        if (this.isFormValid()) {
            if (this.record !== null) {
                if (this.inputLines) {
                    for (var i = 0; i < this.inputLines.length; i++) {
                        var inputLine = this.inputLines[i];
                        if(inputLine.dataIndex!=null) {
                            this.record.set(inputLine.dataIndex, inputLine.getValue());
                        }
                    }
                }
                if(this.addMode) {
                        this.grid.getStore().insert(0, [this.record]);
                        this.grid.updateChangedData(this.record, "added");
                }
            }
            this.hide();
        } else {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("The form is not valid!"));
        }
    },
    cancelAction : function() {
        this.record.data = Ext.decode(this.initialRecordData);
        this.hide();
    }
});

// RpcProxy
// uses json rpc to get the information from the server
Ung.RpcProxy = function(rpcFn, rpcFnArgs, paginated ) {
    Ung.RpcProxy.superclass.constructor.call(this);
    this.rpcFn = rpcFn;
    // specified if we fetch data paginated or all at once
    // default to true
    if (paginated === undefined) {
        this.paginated = true;
    } else {
        this.paginated = paginated;
    }
    // specified if we have aditional args for rpcFnArgs
    this.rpcFnArgs = rpcFnArgs;
};

Ext.extend(Ung.RpcProxy, Ext.data.DataProxy, {
    // sets the total number of records
    setTotalRecords : function(totalRecords) {
        this.totalRecords = totalRecords;
    },
    // load function for Proxy class
    load : function(params, reader, callback, scope, arg) {
        var obj = {};
        obj.params = params;
        obj.reader = reader;
        obj.callback = callback;
        obj.scope = scope;
        obj.arg = arg;
        obj.totalRecords = this.totalRecords;
        var sortColumns = [];
        if (params.sort) {
            var type = scope.fields.get(params.sort).type;
            var sortField = params.sort;
            if (type == 'string') {
            	sortField = "UPPER("+params.sort+")";
            }
            sortColumns.push((params.dir == "ASC" ? "+" : "-") + sortField)
        }
        if (this.paginated) {
            if (this.rpcFnArgs == null) {
                this.rpcFn(this.errorHandler.createDelegate(obj), params.start ? params.start : 0, params.limit
                        ? params.limit
                        : this.totalRecords != null ? this.totalRecords : 2147483647, sortColumns);
            } else {
                var args = [this.errorHandler.createDelegate(obj)].
                            concat(this.rpcFnArgs).
                                concat([params.start ? params.start : 0,
                                    params.limit ? params.limit : this.totalRecords != null ? this.totalRecords : 2147483647,
                                    sortColumns]);
                this.rpcFn.apply(this, args);
            }
        } else {
            if (this.rpcFnArgs == null) {
                this.rpcFn(this.errorHandler.createDelegate(obj));
            } else {
                var args = [this.errorHandler.createDelegate(obj)].concat(this.rpcFnArgs);
                this.rpcFn.apply(this, args);
            }
        }
    },
    errorHandler : function(result, exception) {
        if(Ung.Util.handleException(exception, function() {
            this.callback.call(this.scope, null, this.arg, false);
        }.createDelegate(this),"alert")) return;

        var res = null;
        try {
            res = this.reader.readRecords(result);
            if (this.totalRecords) {
                res.totalRecords = this.totalRecords;
            }
            this.callback.call(this.scope, res, this.arg, true);
        } catch (e) {
            this.callback.call(this.scope, null, this.arg, false);
            return;
        }
    }
});

// Memory Proxy
// holds all the data and returns only the page data
// is used by Event Log store
Ung.MemoryProxy = function(config) {
    Ext.apply(this, config);
    Ung.MemoryProxy.superclass.constructor.call(this);
};

Ext.extend(Ung.MemoryProxy, Ext.data.DataProxy, {
    // sets the total number of records
    setTotalRecords : function(totalRecords) {
        this.totalRecords = totalRecords;
    },
    // the root property
    root : null,
    // the data 
    data : null,
    // load function for Proxy class
    load : function(params, reader, callback, scope, arg) {
        params = params || {};
        var result;
        try {
            var readerData = {};
            var list = null;
            if (this.data != null) {
                list = (this.root != null) ? this.data[this.root] : this.data;
            }
            if(list==null) {
                list = [];
            }
            var totalRecords = list.length;
            if (this.root == null) {
                readerData = list;
            } else {
                readerData[this.root] = list;
                readerData.totalRecords = totalRecords;
            }
            result = reader.readRecords(readerData);

            if (params.sort != null) {
                var st = scope.fields.get(params.sort).sortType;
                var fn = function(r1, r2) {
                    var v1 = st(r1.data[params.sort]), v2 = st(r2.data[params.sort]);
                    var ret = params.dir == "ASC" ? -1 : 1;
                    return v1 == v2 ? 0 : (v1 < v2) ? ret : -ret;
                };
                result.records.sort(fn);
            }
            if (params.start != null && params.limit != null && list != null) {
                result.records = result.records.slice(params.start, params.start + params.limit);
            } 
        } catch (e) {
            this.fireEvent("loadexception", this, arg, null, e);
            callback.call(scope, null, arg, false);
            return;
        }
        callback.call(scope, result, arg, true);
    }
});

// Grid check column
Ext.grid.CheckColumn = function(config) {
    Ext.apply(this, config);
    if (!this.id) {
        this.id = Ext.id();
    }
    if (!this.width) {
        this.width = 40;
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype = {
    init : function(grid) {
        this.grid = grid;
        this.grid.on('render', function() {
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },
    changeRecord : function(record) {
        record.set(this.dataIndex, !record.data[this.dataIndex]);
    },
    onMouseDown : function(e, t) {
        if (t.className && t.className.indexOf('x-grid3-cc-' + this.id) != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.changeRecord(record);
        }
    },

    renderer : function(value, metadata, record) {
        metadata.css += ' x-grid3-check-col-td';
        return '<div class="x-grid3-check-col' + (value ? '-on' : '') + ' x-grid3-cc-' + this.id + '">&#160;</div>';
    }
};
// Grid edit column
Ext.grid.IconColumn = Ext.extend(Object, {
    constructor : function(config) {
        Ext.apply(this, config);
        if (!this.id) {
            this.id = Ext.id();
        }
        if (!this.width) {
            this.width = 35;
        }
        if (this.fixed == null) {
            this.fixed = true;
        }
        if (this.sortable == null) {
            this.sortable = false;
        }
        if (!this.dataIndex) {
            this.dataIndex = null;
        }
        this.renderer = this.renderer.createDelegate(this);
    },
    init : function(grid) {
        this.grid = grid;
        this.grid.on('render', function() {
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown : function(e, t) {
        if (t.className && t.className.indexOf(this.iconClass) != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.handle(record, index)
        }
    },

    renderer : function(value, metadata, record) {
        return '<div class="'+this.iconClass+'">&nbsp;</div>';
    }
});
// Grid edit column
Ext.grid.EditColumn=Ext.extend(Ext.grid.IconColumn, {
    constructor : function(config) {
        if (!this.header) {
            this.header = i18n._("Edit");
        }
        if (!this.width) {
            this.width = 35;
        }
        Ext.grid.EditColumn.superclass.constructor.call(this);
    },
    iconClass: 'iconEditRow',
    handle : function(record) {
        this.grid.editHandler(record);
    }
});
// Grid edit column
Ext.grid.DeleteColumn=Ext.extend(Ext.grid.IconColumn, {
    constructor : function(config) {
        if (!this.header) {
            this.header = i18n._("Delete");
        }
        if (!this.width) {
            this.width = 39;
        }
        Ext.grid.DeleteColumn.superclass.constructor.call(this);
    },
    iconClass: 'iconDeleteRow',
    handle : function(record) {
        this.grid.deleteHandler(record);
    }
});
// Grid delete column
Ext.grid.ReorderColumn = function(config) {
    Ext.apply(this, config);
    if (!this.id) {
        this.id = Ext.id();
    }
    if (!this.header) {
        this.header = i18n._("Reorder");
    }
    if (!this.width) {
        this.width = 50;
    }
    if (this.fixed == null) {
        this.fixed = true;
    }
    if (this.sortable == null) {
        this.sortable = false;
    }
    if (!this.dataIndex) {
        this.dataIndex = null;
    }
    this.renderer = this.renderer.createDelegate(this);
};
Ext.grid.ReorderColumn.prototype = {
    init : function(grid) {
        this.grid = grid;
    },

    renderer : function(value, metadata, record) {
        return '<div class="iconDrag">&nbsp;</div>';
    }
};

// Editor Grid class
Ung.EditorGrid = Ext.extend(Ext.grid.EditorGridPanel, {
    // record per page
    recordsPerPage : 20,
    // the minimum number of records for pagination
    minPaginateCount : 60,
    // the total number of records
    totalRecords : null,
    // settings component
    settingsCmp : null,
    // proxy Json Rpc function to populate the Store
    proxyRpcFn : null,
    // specified if we have aditional args for proxyRpcFn
    proxyRpcFnArgs : null,
    // the list of fields used to by the Store
    fields : null,
    // has Add button
    hasAdd : true,
    // has Edit buton on each record
    hasEdit : true,
    // has Delete buton on each record
    hasDelete : true,
    // the default Empty record for a new row
    hasReorder : false,
    // the default Empty record for a new row
    emptyRow : null,
    // input lines used by the row editor
    rowEditorInputLines : null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,    
    // the default sort field
    sortField : null,
    // the columns are sortable by default, if sortable is not specified
    columnsDefaultSortable : null,
    // force paginate, even if the totalRecords is smaller than minPaginateCount
    forcePaginate : false,
    // paginate the grid by default
    paginated: true,
    // javaClass of the record, used in save function to create correct json-rpc
    // object
    recordJavaClass : null,
    // the map of changed data in the grid
    // used by rendering functions and by save
    dataRoot: null,
    changedData : null,
    stripeRows : true,
    clicksToEdit : 1,
    enableHdMenu : false,
    enableColumnMove: false,
    autoGenerateId: false,
    addedId : 0,
    generatedId:1,
    loadMask: null,
    subCmps:null,
    constructor : function(config) {
        this.subCmps=[];
        this.changedData = {};
        Ung.EditorGrid.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() {
        if(this.loadMask===null) {
           this.loadMask={msg: i18n._("Loading ...")} ;
        }
        if (this.hasReorder) {
            this.enableDragDrop = true;
            this.selModel= new Ext.grid.RowSelectionModel({singleSelect:true});
            this.dropConfig= {
                appendOnly:true
            };

            var reorderColumn = new Ext.grid.ReorderColumn();
            if (!this.plugins) {
                this.plugins = [];
            }
            this.plugins.push(reorderColumn);
            this.columns.push(reorderColumn);
        }
        if (this.hasEdit) {
            var editColumn = new Ext.grid.EditColumn();
            if (!this.plugins) {
                this.plugins = [];
            }
            this.plugins.push(editColumn);
            this.columns.push(editColumn);
        }
        if (this.hasDelete) {
            var deleteColumn = new Ext.grid.DeleteColumn();
            if (!this.plugins) {
                this.plugins = [];
            }
            this.plugins.push(deleteColumn);
            this.columns.push(deleteColumn);
        }
        if(this.autoGenerateId && this.fields!=null) {
            this.fields.push({
                name: 'id',
                mapping: null,
                convert : function(val, rec) {
                    return this.generatedId++;
                }.createDelegate(this)
            })
        }
        if (this.proxyRpcFn) {
            this.store = new Ext.data.Store({
                proxy : new Ung.RpcProxy(this.proxyRpcFn, this.proxyRpcFnArgs, this.paginated),
                sortInfo : this.sortField ? {
                    field : this.sortField,
                    direction : "ASC"
                } : null,
                reader : new Ext.data.JsonReader({
                    totalProperty : "totalRecords",
                    root : 'list',
                    fields : this.fields
                }),

                remoteSort : this.paginated,
                listeners : {
                    "update" : {
                        fn : function(store, record, operation) {
                            this.updateChangedData(record, "modified");
                        }.createDelegate(this)
                    },
                    "load" : {
                        fn : function(store, records, options) {
                            this.updateFromChangedData(records, options);
                        }.createDelegate(this)
                    }
                }
            });
        } else if(this.data) {
            this.store = new Ext.data.Store({
                proxy : new Ung.MemoryProxy({
                    root : this.dataRoot,
                    data: this.data
                }),
                sortInfo : this.sortField ? {
                    field : this.sortField,
                    direction : "ASC"
                } : null,
                remoteSort : false,
                reader : new Ext.data.JsonReader({
                    totalProperty : "totalRecords",
                    root : this.dataRoot,
                    fields : this.fields
                }),
                listeners : {
                    "update" : {
                        fn : function(store, record, operation) {
                            this.updateChangedData(record, "modified");
                        }.createDelegate(this)
                    },
                    "load" : {
                        fn : function(store, records, options) {
                            this.updateFromChangedData(records, options);
                        }.createDelegate(this)
                    }
                }
            });
            this.getStore().loadData(this.data);
            this.totalRecords=this.data.length
        }
        if(this.paginated) {
            this.bbar = new Ext.PagingToolbar({
                pageSize : this.recordsPerPage,
                store : this.store,
                displayInfo : true,
                displayMsg : i18n._('Displaying topics {0} - {1} of {2}'),
                emptyMsg : i18n._("No topics to display")
            });
        }
        if (this.rowEditor==null && this.rowEditorInputLines != null) {
        	/*
             * this fixes the cursor but introduces a bigger bug with
             * positioning if(Ext.isGecko && !Ext. isGecko3) { for(var i=0;i<this.rowEditorInputLines.length;i++) {
             * var xtype=this.rowEditorInputLines[i].xtype if(xtype=="textfield" ||
             * xtype=="textarea" || xtype=="numberfield" || xtype=="timefield") {
             * this.rowEditorInputLines[i].ctCls="fixedPos"; } } }
             */
            this.rowEditor = new Ung.RowEditorWindow({
                grid : this,
                inputLines : this.rowEditorInputLines,
                rowEditorLabelWidth : this.rowEditorLabelWidth
            });
        }
        if(this.rowEditor!=null) {
            this.rowEditor.render('container');
            this.subCmps.push(this.rowEditor);
        }
        if (this.hasAdd) {
            this.tbar = [{
                text : i18n._('Add'),
                tooltip : i18n._('Add New Row'),
                iconCls : 'iconAddRow',
                name : 'Add',
                parentId : this.getId(),
                handler : this.addHandler.createDelegate(this)
            }];
        }
        Ung.EditorGrid.superclass.initComponent.call(this);
        var columnModel=this.getColumnModel();
        columnModel.getRenderer = function(col){
            if(!this.config[col].renderer){
                return Ung.Util.defaultRenderer;
            }
            return this.config[col].renderer;
        };
        if (this.columnsDefaultSortable !== null) {
            columnModel.defaultSortable = this.columnsDefaultSortable;
        }
    },
    addHandler : function() {
        var record = new Ext.data.Record(Ext.decode(Ext.encode(this.emptyRow)));
        record.set("id", this.genAddedId());
        this.stopEditing();
        if (this.rowEditor) {
            this.rowEditor.populate(record, true);
            this.rowEditor.show();
        } else {
            this.getStore().insert(0, [record]);
            this.updateChangedData(record, "added");
            this.startEditing(0, 0);
        }
    },
    editHandler : function(record) {
    	this.stopEditing();
        // populate row editor
        this.rowEditor.populate(record);
        this.rowEditor.show();
    },
    deleteHandler : function(record) {
    	this.stopEditing();
        this.updateChangedData(record, "deleted");
    },
    getPageStart : function() {
        if (this.store.lastOptions && this.store.lastOptions.params) {
            return this.store.lastOptions.params.start
        } else {
            return 0;
        }
    },
    genAddedId : function() {
        this.addedId--;
        return this.addedId;
    },
    // is grid paginated
    isPaginated : function() {
        return this.forcePaginate || (this.totalRecords != null && this.totalRecords >= this.minPaginateCount)
    },
    clearChangedData : function () {
        this.changedData = {};
    },
    
    beforeDestroy : function() {
        Ext.each(this.subCmps, Ext.destroy);
        Ung.EditorGrid.superclass.beforeDestroy.call(this);
    },
    afterRender : function() {
        Ung.EditorGrid.superclass.afterRender.call(this);
        if(this.hasReorder) {
            var ddrowTarget = new Ext.dd.DropTarget(this.container, {
                ddGroup: "GridDD",
                // copy:false,
                notifyDrop : function(dd, e, data){
                    var sm = this.getSelectionModel();
                    var rows = sm.getSelections();
                    var cindex = dd.getDragData(e).rowIndex;    // Here is need

                    var dsGrid = this.getStore();

                    for(i = 0; i < rows.length; i++) {
                        rowData = dsGrid.getById(rows[i].id);
                        dsGrid.remove(dsGrid.getById(rows[i].id));
                        dsGrid.insert(cindex, rowData);
                    };

                    this.getView().refresh();

                    // put the cursor focus on the row of the gridRules which we
                    // just draged
                    this.getSelectionModel().selectRow(cindex);
                }.createDelegate(this)
            });
        }

        this.getGridEl().child("div[class*=x-grid3-viewport]").set({'name' : "Table"});

        this.getView().getRowClass = function(record, index, rowParams, store) {
            var id = record.get("id");
            if (id == null || id < 0) {
                return "grid-row-added";
            } else {
                var d = this.grid.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
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
    // load first page initialy
    initialLoad : function() {
        this.setTotalRecords(this.totalRecords);
        this.loadPage(0);
    },
    // load a page
    loadPage : function(pageStart, callback, scope, arg) {
        if (!this.isPaginated()) {
            this.getStore().load({
                callback : callback,
                scope : scope,
                arg : arg
            });
        } else {
            this.getStore().load({
                params : {
                    start : pageStart,
                    limit : this.recordsPerPage
                },
                callback : callback,
                scope : scope,
                arg : arg
            });
        }
    },
    // when a page is rendered load the changedData for it
    updateFromChangedData : function(store, records, options) {
        var pageStart = this.getPageStart();
        for (id in this.changedData) {
            var cd = this.changedData[id];
            if (pageStart == cd.pageStart) {
                if ("added" == cd.op) {
                    var record = new Ext.data.Record(cd.recData);
                    this.store.insert(0, [record]);
                } else if ("modified" == cd.op) {
                    var recIndex = this.store.find("id", id);
                    if (recIndex) {
                        var rec = this.store.getAt(recIndex);
                        rec.data = cd.recData;
                        rec.commit();
                    }
                }
            }
        }
    },
    // Test if there are changed data
    hasChangedData : function() {
        return Ung.Util.hasData(this.changedData);
    },
    // Update Changed data after an operation (modifyed, deleted, added)
    updateChangedData : function(record, currentOp) {
        if (!this.hasChangedData()) {
            var cmConfig = this.getColumnModel().config;
            for (i in cmConfig) {
                cmConfig[i].sortable = false;
            }
        }
        var id = record.get("id");
        var cd = this.changedData[id];
        if (cd == null) {
            this.changedData[id] = {
                op : currentOp,
                recData : record.data,
                pageStart : this.getPageStart()
            };
            if ("deleted" == currentOp) {
                var index = this.store.indexOf(record);
                this.getView().refreshRow(record);
            }
        } else {
            if ("deleted" == currentOp) {
                if ("added" == cd.op) {
                    this.store.remove(record);
                    this.changedData[id] = null;
                    delete this.changedData[id];
                } else {
                    this.changedData[id] = {
                        op : currentOp,
                        recData : record.data,
                        pageStart : this.getPageStart()
                    };
                    this.getView().refreshRow(record);
                }
            } else {
                if ("added" == cd.op) {
                    this.changedData[id].recData = record.data;
                } else {
                    this.changedData[id] = {
                        op : currentOp,
                        recData : record.data,
                        pageStart : this.getPageStart()
                    };
                }
            }
        }

    },
    // Set the total number of records
    setTotalRecords : function(totalRecords) {
        this.totalRecords = totalRecords;
        if (this.totalRecords != null) {
            this.getStore().proxy.setTotalRecords(this.totalRecords);
        }
        if(this.paginated) {
            if (this.isPaginated()) {
                this.getBottomToolbar().show();
                this.getBottomToolbar().enable();
            } else {
                this.getBottomToolbar().hide();
                this.getBottomToolbar().disable();
            }
            this.getBottomToolbar().syncSize();
        }
    },
    findFirstChangedDataByFieldValue : function(field, value) {
        for (id in this.changedData) {
            var cd = this.changedData[id];
            if (cd.op != "deleted" && cd.recData[field] == value) {
                return cd;
                break;
            }
        }
        return null;
    },

    focusChangedDataField : function(cd, field) {
        var recIndex = this.store.find("id", cd.recData["id"]);
        if (recIndex >= 0) {
            this.getView().focusRow(recIndex);
        }
    },
    // focus the first changed row matching a field value
    // used by validation functions
    focusFirstChangedDataByFieldValue : function(field, value) {
        var cd = this.findFirstChangedDataByFieldValue(field, value)
        if (cd != null) {
            this.loadPage(cd.pageStart, function(r, options, success) {
                if (success) {
                    this.focusChangedDataField(options.arg, field);
                }
            }.createDelegate(this), this, cd);
        }
    },
    editRowChangedDataByFieldValue : function(field, value) {
        var cd = this.findFirstChangedDataByFieldValue(field, value)
        if (cd != null) {
            this.loadPage(cd.pageStart, function(r, options, success) {
                if (success) {
                     alert("todo");
                }
            }.createDelegate(this), this, cd);
        }
    },
    // Get the save list from the changed data
    getSaveList : function() {
        return Ung.Util.getSaveList(this.changedData, this.recordJavaClass);
    },
    // Get the entire list
    // for the unpaginated grids, that send all the records on save
    getFullSaveList : function() {
        var list=[];
        var records=this.store.getRange();
        for(var i=0; i<records.length;i++) {
            var id = records[i].get("id");
            if (id != null && id >= 0) {
                var d = this.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        continue
                    }
                }
            }
            if (this.recordJavaClass != null){
                records[i].data["javaClass"] = this.recordJavaClass;
            }
            var recData=Ext.decode(Ext.encode(records[i].data));
            if(recData.id<0) {
                delete recData.id
            }
            list.push(recData)
        }

        return list;
    },
    getDeletedList : function() {
        var list=[];
        var records=this.store.getRange();
        for(var i=0; i<records.length;i++) {
            var id = records[i].get("id");
            if (id != null && id >= 0) {
                var d = this.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        if (this.recordJavaClass != null){
                            records[i].data["javaClass"] = this.recordJavaClass;
                        }
                        list.push(records[i].data);
                    }
                }
            }
        }
        return list;
    }
});
// Reads a list of strings form a json object
// and creates a list of records
Ung.JsonListReader = Ext.extend(Ext.data.JsonReader, {
    autoGenerateId:true,
    generatedId:1,
    readRecords : function(o) {
        var sid = this.meta ? this.meta.id : null;
        var recordType = this.recordType, fields = recordType.prototype.fields;
        var records = [];
        this.getRoot = this.meta.root ? this.getJsonAccessor(this.meta.root) : function(p) {
            return p;
        };
        var root = this.getRoot(o);
        for (var i = 0; i < root.length; i++) {
            var n = root[i];
            var values = {};
            var id = ((sid || sid === 0) && n[sid] !== undefined && n[sid] !== "" ? n[sid] : null);
            var fName = (fields && fields.length > 0) ? fields.items[0].name : "name"
            values[fName] = n
            if(this.autoGenerateId) {
                values['id'] = this.generatedId++;
            }
            var record = new recordType(values, id);
            record.json = n;

            records[records.length] = record;
        }
        return {
            records : records,
            totalRecords : records.length
        };
    }
});
Ung.Breadcrumbs = Ext.extend(Ext.Component, {
    autoEl : "div",
    // ---Node specific attributes------
    elements : null,
    onRender : function(container, position) {
        Ung.Breadcrumbs.superclass.onRender.call(this, container, position);
        if (this.elements != null) {
            for (var i = 0; i < this.elements.length; i++) {
                if (i > 0) {
                    this.getEl().insertHtml('beforeEnd', '<span class="iconBreadcrumbsSeparator">&nbsp;&nbsp;&nbsp;&nbsp;</span>');
                }
                var crumb = this.elements[i];
                if (crumb.action) {
                    var crumbEl = document.createElement("span");;
                    crumbEl.className = 'breadcrumbLink';
                    crumbEl.innerHTML = crumb.title;
                    crumbEl = Ext.get(crumbEl);
                    crumbEl.on("click", crumb.action, this);
                    this.getEl().appendChild(crumbEl);

                } else {
                    this.getEl().insertHtml('beforeEnd', '<span class="breadcrumbText" >' + crumb.title + '</span>')
                }

            }
        }
    }
});


Ext.grid.ButtonColumn = function(config) {
    Ext.apply(this, config);
    if (!this.id) {
        this.id = Ext.id();
    }
    if (!this.width) {
        this.width = 80;
    }
    if (this.fixed == null) {
        this.fixed = true;
    }
    if (this.sortable == null) {
        this.sortable = false;
    }
    if (!this.dataIndex) {
        this.dataIndex = null;
    }
    this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.ButtonColumn.prototype = {
    init : function(grid) {
        this.grid = grid;
        this.grid.on('render', function() {
            var view = this.grid.getView();
            view.mainBody.on('mousedown', this.onMouseDown, this);
            view.mainBody.on('mouseover', this.onMouseOver, this);
            view.mainBody.on('mouseout', this.onMouseOut, this);
        }, this);
    },

    onMouseDown : function(e, t) {
        if (t.className && t.className.indexOf('ungButton') != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.handle(record)
        }
    },
    // to override
    handle : function(record) {
    },
    // private
    onMouseOver : function(e,t) {
        if (t.className && t.className.indexOf('ungButton') != -1) {
            t.className="ungButton buttonColumn ungButtonHover";
        }
    },
    // private
    onMouseOut : function(e,t) {
        if (t.className && t.className.indexOf('ungButton') != -1) {
            t.className="ungButton buttonColumn";
        }
    },
    renderer : function(value, metadata, record) {
        return '<div class="ungButton buttonColumn">'+value+'</div>';
    }
};

// Row editor window used by editor grid
Ung.UsersWindow = Ext.extend(Ung.UpdateWindow, {
    // the record currently edit
    record : null,
    sizeToRack : true,
    // size to grid on show
    sizeToGrid : false,
    singleSelectUser : false,
    loadActiveDirectoryUsers : true,
    loadLocalDirectoryUsers : true,
    userDataIndex : null,
    usersGrid:null,
    populateSemaphore: null,
    userEntries: null,
    fnCallback: null,
    initComponent : function() {
        if (!this.height && !this.width) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Portal Question');
        }
        var selModel = new Ext.grid.CheckboxSelectionModel({singleSelect : this.singleSelectUser});
        this.usersGrid=new Ext.grid.GridPanel({
           // title: i18n._('Users'),
           height: 210,
           width: 290,
           enableHdMenu : false,
           enableColumnMove: false,
           store: new Ext.data.Store({
                proxy : new Ung.MemoryProxy({
                    root : 'list'
                }),
                sortInfo : this.sortField ? {
                    field : this.sortField,
                    direction : "ASC"
                } : null,
                remoteSort : false,
                reader : new Ext.data.JsonReader({
                    totalProperty : "totalRecords",
                    root : 'list',
                    fields : [{
                        name: "UID"
                    }, {
                        name: "name",
                        mapping: "UID",
                        convert : function(val, rec) {
                            var name=val;
                            var repository=null;
                            if(rec.storedIn) {
                                if(rec.storedIn=="MS_ACTIVE_DIRECTORY") {
                                    repository=i18n._('Active Directory');
                                } else if(rec.storedIn=="LOCAL_DIRECTORY") {
                                    repository=i18n._('Local');
                                } else {
                                    repository=i18n._('UNKNOWN');
                                }
                            }
                            if(repository) {
                                name+=" ("+repository+")";
                            }
                            return name;
                        }
                    }]
                })
            }),
            columns: [selModel, {
                header : i18n._("user"),
                width : 250,
                fixed :true,
                sortable : false,
                dataIndex : 'name'
            }],
            selModel : selModel
        });
        this.items = new Ext.FormPanel({
            labelWidth : 75,
            buttonAlign : 'right',
            border : false,
            bodyStyle : 'padding:10px 10px 0px 10px;',
            autoScroll: true,
            defaults : {
                selectOnFocus : true,
                msgTarget : 'side'
            },
            items : [{
                xtype : 'fieldset',
                title : this.singleSelectUser ? i18n._('Select User') : i18n._('Select Users'),
                autoHeight : true,
                items: [{
                    bodyStyle : 'padding:0px 0px 5px 5px;',
                    border : false,
                    html: this.singleSelectUser ? i18n._("You may choose user ID/Login that exists in the User Directory (either local or remote Active Directory), or you can add a new user to the User Directory, and then choose that user.")
                                : i18n._("You may choose user IDs/Logins that exist in the User Directory (either local or remote Active Directory), or you can add a new user to the User Directory, and then choose that user.")
                }, {
                    xtype : 'fieldset',
                    title : this.singleSelectUser ? i18n._('Select an existing user') : i18n._('Select an existing user or users'),
                    autoHeight : true,
                    items: [this.usersGrid]
                }, {
                    xtype : 'fieldset',
                    title : i18n._('Add a new user'),
                    autoHeight : true,
                    buttonAlign : 'left',
                    buttons:[{
                        xtype: "button",
                        name : 'Open Local Directory',
                        text : i18n._("Open Local Directory"),
                        disabled : !this.loadLocalDirectoryUsers,
                        handler : function() {
                            Ung.Util.loadResourceAndExecute("Ung.LocalDirectory","script/config/localDirectory.js", function() {
                                main.localDirectoryWin=new Ung.LocalDirectory({"name":"localDirectory",fnCallback: function() {
                                    this.populate(this.record,this.fnCallback)
                                }.createDelegate(this)});
                                main.localDirectoryWin.show();
                            }.createDelegate(this));
                        }.createDelegate(this)
                    }, {
                        xtype: "button",
                        name : 'Open Active Directory',
                        text : i18n._("Open Active Directory"),
                        disabled : !this.loadActiveDirectoryUsers || !main.isNodeRunning('untangle-node-adconnector'),
                        handler : function() {
                            var node = main.getNode('untangle-node-adconnector');
                            if (node != null) {
                                var nodeCmp = Ung.Node.getCmp(node.tid);
                                if (nodeCmp != null) {
                                    nodeCmp.onSettingsAction(function() {
                                        this.populate(this.record,this.fnCallback)
                                    }.createDelegate(this));
                                }
                            }
                        }.createDelegate(this)
                    }]
                }]
            }]
        });
        Ung.UsersWindow.superclass.initComponent.call(this);
    },
    initSubComponents : function(container, position) {
    },
    // populate is called whent a record is edited, tot populate the edit window
    populate : function(record,fnCallback) {
        this.fnCallback=fnCallback;
        this.record = record;
        Ext.MessageBox.wait(i18n._("Loading..."), i18n._("Please wait"));
        this.usersGrid.getSelectionModel().clearSelections();
        var store=this.usersGrid.getStore();
        store.proxy.data = {list:[]};
        store.load({
            params : {
                start : 0
            }
        });
        this.populateSemaphore=2;
        this.userEntries=this.singleSelectUser ? [] : [{UID: "[any]"}];
        if (this.loadActiveDirectoryUsers && main.isNodeRunning('untangle-node-adconnector')){
            main.getAppAddressBook().getUserEntries(function(result, exception) {
                if(Ung.Util.handleException(exception, function() {
                    Ext.MessageBox.alert(i18n._("Failed"), i18n._("There was a problem refreshing Active Directory users.  Please check your Active Directory settings and then try again."), function(){
                        this.populateCallback();
                    }.createDelegate(this));
                }.createDelegate(this),"noAlert")) return;
                this.userEntries=this.userEntries.concat(result.list);
                this.populateCallback();
            }.createDelegate(this),'MS_ACTIVE_DIRECTORY')
        } else {
            this.populateSemaphore--;
        }
        if (this.loadLocalDirectoryUsers) {
            main.getAppAddressBook().getUserEntries(function(result, exception) {
                if(Ung.Util.handleException(exception, function() {
                    Ext.MessageBox.alert(i18n._("Failed"), i18n._("There was a problem refreshing Local Directory users.  Please check your Local Directory settings and try again."), function(){
                        this.populateCallback();
                    }.createDelegate(this));
                }.createDelegate(this),"noAlert")) return;
                this.userEntries=this.userEntries.concat(result.list);
                this.populateCallback();
            }.createDelegate(this),'LOCAL_DIRECTORY')
        } else {
            this.populateCallback();
        }
    },
    populateCallback : function () {
        this.populateSemaphore--;
        if (this.populateSemaphore == 0) {
            if (this.settingsCmp !== null) {
                var sm=this.usersGrid.getSelectionModel();
                sm.clearSelections()
                var store=this.usersGrid.getStore();
                store.proxy.data = {list:this.userEntries};
                store.load({
                    params : {
                        start : 0
                    }
                });
                var users=this.record.get(this.userDataIndex);
                if(users!=null) {
                    users=users.split(",");
                }
                for(var i=0;i<users.length;i++) {
                    var index=store.find("UID",users[i]);
                    if(index>=0) {
                       sm.selectRow(index,true);
                    }
                }
            }
            Ext.MessageBox.hide();
        }
    },
    // check if the form is valid;
    // this is the default functionality which can be overwritten
    isFormValid : function() {
        if (this.singleSelectUser) {
            // one user must be selected
            return (this.usersGrid.getSelectionModel().getSelections().length == 1);
        }
        return true;
    },
    // updateAction is called to update the record after the edit
    updateAction : function() {
        if (this.isFormValid()) {
            if (this.record !== null) {
                var sm=this.usersGrid.getSelectionModel();
                var users=[];
                var selRecs=sm.getSelections();
                for(var i=0;i<selRecs.length;i++) {
                    var uid=selRecs[i].get("UID");
                    if(uid=="[any]") {
                        users=[uid];
                        break;
                    } else {
                      users.push(selRecs[i].get("UID"));
                    }
                }
                this.record.set(this.userDataIndex,users.join(","));
                if(this.fnCallback) {
                    this.fnCallback.call()
                }
            }
            this.hide();
        } else {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("Please choose a user id/login or press Cancel!"));
        }
    },
    cancelAction : function() {
        this.hide();
    }
});