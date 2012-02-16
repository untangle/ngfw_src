Ext.namespace('Ung');
Ext.namespace('Ung.form');
Ext.namespace('Ung.grid');
Ext.BLANK_IMAGE_URL = '/ext4/resources/themes/images/default/tree/s.gif'; // The location of the blank pixel image

var i18n=Ext.create('Ung.I18N',{"map":null}); // the main internationalization object
var rpc=null; // the main json rpc object

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

Ext.override(Ext.form.Field, { 
	clearDirty: function() {
        if(this.xtype=='radiogroup') {
            this.items.each(function(item) {
                item.clearDirty();
    		}); 
        } else {
        	this.originalValue=this.getValue();
        }
	},
    afterRender : Ext.Function.createSequence(Ext.form.Field.prototype.afterRender,function(){
        Ext.QuickTips.init();    
        var qt = this.tooltip,
            target = null;
        try{
            if(this.xtype=='checkbox'){
                target = this.labelEl;
            }else{        
                target = this.container.dom.parentNode.childNodes[0];        
            }
        }catch(exn){
            //don't bother if there's nothing to target
        }

        if (qt && target){ 
            Ext.QuickTips.register({
                target: target,
                title: '',
                text: qt,
                enabled: true,
                showDelay: 20
            });
        }
    })
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

Ext.override(Ext.Toolbar,{
    nextBlock : function(){
        var td = document.createElement("td");
        if (this.columns && (this.tr.cells.length == this.columns)) {
            this.tr = document.createElement("tr");
            var tbody = this.el.down("tbody", true);
            tbody.appendChild(this.tr);
        }
        this.tr.appendChild(td);
        return td;
    },
    insertButton: Ext.Function.createSequence(function(){
        if (this.columns) {
            throw "This method won't work with multiple rows";
        }
    }, Ext.Toolbar.prototype.insertButton)
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
    	return; //TODO:  fix this for extjs4
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

Ext.override( Ext.form.Field, {
    showContainer : function()
    {
        this.show();
        this.enable();
        /* show entire container and children (including label if applicable) */
        this.getEl().up('.x-form-item').setDisplayed( true );
    },
    hideContainer : function()
    {
        this.disable();
        this.hide();
        this.getEl().up('.x-form-item').setDisplayed( false );
    },
    setContainerVisible: function(visible) {
        if (visible) {
            this.showContainer();
        } else {
            this.hideContainer();
        }
        return this;
    },
    isContainerVisible: function() {
        return this.getEl().up('.x-form-item').isDisplayed();
    }
});

Ext.override( Ext.form.TextField, {
    afterRender : Ext.Function.createSequence(Ext.form.TextField.prototype.afterRender, function(){
        var parent = this.el.parent();
        if( this.boxLabel ) {
            this.labelEl = parent.createChild({
                tag: 'label',
                htmlFor: this.el.id,
                cls: 'x-form-textfield-detail',
                html: this.boxLabel
            });
        }
    }),
    updateBoxLabel : function(html){
        if(this.labelEl) {
            this.labelEl.dom.innerHTML = html;
        }
    }
});

Ext.define("Ung.form.TimeField", {
	extend: "Ext.form.TimeField",
    alias:"widget.utimefield",
	
    /* Default the format to 24 hour */
    format : "H:i",

    initComponent : function()
    {
        /* Save the store before init to determin if one was passed in */
        var store = this.store;

        Ung.form.TimeField.superclass.initComponent.call(this);
        
        /* If necesary, add the last minute of the day. */
        if ( this.endTime && store != null && this.maxValue == null && this.minValue == null && this.format == "H:i" ) {
            this.store.add([new Ext.data.Record({text : "23:59"})]);
        }
    }
});


Ung.Util= {
    goToStartPage: function () {
        Ext.MessageBox.wait(i18n._("Redirecting to the start page..."), i18n._("Please wait"));
        window.location.href="/webui";
    },
    rpcExHandler: function(exception) {
        console.log("In rpcExHandler:" + exception);
        if(exception instanceof JSONRpcClient.Exception)
        {
            if(exception.code == 550 || exception.code == 12029 || exception.code == 12019 )
            {
                Ext.MessageBox.alert(i18n._("Warning"),i18n._("The connection to the server has been lost. Press OK to return to the login page."), Ung.Util.goToStartPage);
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
            var message=null;
            var gotoStartPage=false;
            /* special text for apt error */
            if (exception.name == "com.untangle.uvm.toolbox.PackageInstallException" && (exception.message.indexOf("exited with") >= 0)) {
                message  = i18n._("The server is unable to properly communicate with the app store.") + "<br/>";
                message += i18n._("Check internet connectivity and network settings.") + "<br/>";
                message += i18n._("Check that the server is fully up to date.") + "<br/>";
                message += i18n._("<br/>");
                message =  i18n._("Unable to contact app store") + ":<br/>";
                message += i18n._("An error has occured: ") + exception.message + "<br/>";
            }
            /* special text for apt error */
            if (exception.name == "com.untangle.uvm.toolbox.PackageException" && (exception.message.indexOf("timed out") >= 0)) {
                message  = i18n._("Unable to contact the app store") + ":<br/>";
                message += i18n._("Connection timed out") + "<br/>";
                message += i18n._("<br/>");
                message += i18n._("Check internet connectivity and network settings.") + "<br/>";
                message += i18n._("An error has occured: ") + exception.message + "<br/>";
            }
            /* special text for rack error */
            if (exception.name == "com.untangle.uvm.node.DeployException" && (exception.message.indexOf("already exists in Policy") >= 0)) {
                message  = i18n._("This application already exists in this policy/rack.") + ":<br/>";
                message += i18n._("Each application can only be installed once in each policy/rack.");
            }
            /* handle connection lost */
            if(exception.code==550 || exception.code == 12029 || exception.code == 12019) {
                message  = i18n._("The connection to the server has been lost.") + "<br/>";
                message += i18n._("Press OK to return to the login page.") + "<br/>";
                message += i18n._("<br/>");
                message += i18n._("An error has occured") + ": " + exception.name + ": " + exception.message + "<br/>";
                if (type !== "noAlert")
                    handler = Ung.Util.goToStartPage; //override handler
            }
            /* handle connection lost (this happens on windows only for some reason) */
            if(exception.name == "JSONRpcClientException" && exception.fileName.indexOf("jsonrpc") >= 0) {
                message  = i18n._("The connection to the server has been lost.") + "<br/>";
                message += i18n._("Press OK to return to the login page.") + "<br/>";
                message += i18n._("<br/>");
                message += i18n._("An error has occured") + ": " + exception.name + ": " + exception.message + "<br/>";
                if (type !== "noAlert")
                    handler = Ung.Util.goToStartPage; //override handler
            }
            /* special text for "method not found" and "Service Temporarily Unavailable" */
            if (exception.message.indexOf("ethod not found") >= 0 || exception.message.indexOf("ervice Temporarily Unavailable") >= 0) {
                message  = i18n._("The connection to the server has been lost.") + "<br/>";
                message += i18n._("Press OK to return to the login page.") + "<br/>";
                message += i18n._("<br/>");
                message += i18n._("An error has occured") + ": " + exception.name + ": " + exception.message + "<br/>";
                if (type !== "noAlert")
                    handler = Ung.Util.goToStartPage; //override handler
            }

            /* otherwise just describe the exception */
            if (message == null && exception != null) {
                message  = i18n._("An exception has occurred") + ":" + "<br/>";
                message += i18n._("<br/>");
                if (exception.name != null)
                    message += exception.name + "<br/>";
                if (exception.message != null)
                    message += exception.message + "<br/>";
                if (exception.stack != null) {
                    message += "<br/>";
                    message += exception.stack + "<br/>";
                }
            }
            /* worst case - just say something */
            if (message == null) {
                message = i18n._("An unknown error has occurred.") + "<br/>";
                message += i18n._("Please Try Again");
            }
            
            if (handler==null) {
                Ext.MessageBox.alert(i18n._("Warning"), message);
            } else if(type==null || type== "alertCallback"){
                Ext.MessageBox.alert(i18n._("Warning"), message, handler);
            } else if (type== "alert") {
                Ext.MessageBox.alert(i18n._("Warning"), message);
                handler();
            } else if (type== "noAlert") {
                handler();
            }
            return !continueExecution;
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
            val=obj[key];
            if(val!=null) {
              msg+=" | "+key+" - "+val;
            }
        }
        return msg;
    },
    addBuildStampToUrl: function(url){
        if (url.indexOf("?") >= 0) {
            return url + "&" + main.buildStamp;
        } else {
            return url + "?" + main.buildStamp;
        }
    },
    getScriptSrc: function(sScriptSrc){
        //return main.debugMode ? sScriptSrc : sScriptSrc.replace(/\.js$/, "-min.js");
        return sScriptSrc ;
    },
    // Load css file Dynamically
    loadCss: function(filename) {
        var fileref=document.createElement("link");
        fileref.setAttribute("rel", "stylesheet");
        fileref.setAttribute("type", "text/css");
        fileref.setAttribute("href", Ung.Util.addBuildStampToUrl(filename));
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
            req.open("GET",Ung.Util.addBuildStampToUrl(sScriptSrc),false);
            req.send(null);
            if( window.execScript)
                window.execScript(req.responseText);
            else
                window.eval(req.responseText);
        } catch (e) {
            error=e;
            alert(error);
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
    loadModuleTranslations : function(moduleName, handler) {
        if(!Ung.i18nModuleInstances[moduleName]) {
            rpc.languageManager.getTranslations(Ext.bind(function(result, exception, opt, moduleName, handler) {
                if(Ung.Util.handleException(exception)) return;
                var moduleMap=result.map;
                Ung.i18nModuleInstances[moduleName] = Ext.create('Ung.ModuleI18N',{
                        "map" : i18n.map,
                        "moduleMap" : moduleMap
                });
                handler.call(this);
            },this,[moduleName, handler],true), moduleName);
        } else {
            handler.call(this);
        }
    },
    todo: function() {
        Ext.MessageBox.alert(i18n._("TODO"),"TODO: implement this.");
    },
    possibleInterfaces : null,
    getInterfaceList : function(wanMatchers, anyMatcher) {
        var data = [];
        var datacount = 0;

        if(this.possibleInterfaces==null) {
            var netManager = main.getNetworkManager();
            this.possibleInterfaces = netManager.getPossibleInterfaces();
        }

        for ( var c = 0 ; c < this.possibleInterfaces.length ; c++ ) {
            var key =this.possibleInterfaces[c];
            var name = key;
            switch ( key ) {
            case "any":
                if ( anyMatcher === false ) {
                    key = null;
                    break;
                }
                name = i18n._("Any") ;
                break;
            case "1": name = i18n._("External") ; break;
            case "2": name = i18n._("Internal") ; break;
            case "3": name = i18n._("DMZ") ; break;
            case "250": name = i18n._("OpenVPN") ; break;
            case "wan": 
                if ( wanMatchers === false ) {
                    key = null;
                    break;
                }
                name = i18n._("Any WAN") ; 
                break;

            case "non_wan": 
                if ( wanMatchers === false ) {
                    key = null;
                    break;
                }
                name = i18n._("Any non-WAN") ;
                break;

            case "4": 
            case "5": 
                /* ... */
            case "254":
            default :
                name = Ext.String.format( i18n._("Interface {0}"), key );
                break;

            }
            
            if ( key != null ) {
                data[datacount] = [ key,name ];
                datacount++;
            }
        }

        return data;
    },
    getWanList : function() {
        var data = [];
        var datacount = 0;

        if( this.wanInterfaces==null ) {
            var netManager = main.getNetworkManager();
            this.wanInterfaces = netManager.getWanInterfaces();
        }

        for ( var c = 0 ; c < this.wanInterfaces.length ; c++ ) {
            var key =this.wanInterfaces[c];
            var name = key;
            switch ( key ) {
            case "1": name = i18n._("External") ; break;
            case "2": name = i18n._("Internal") ; break;
            case "3": name = i18n._("DMZ") ; break;
            case "250": name = i18n._("OpenVPN") ; break;
            case "4": 
            case "5": 
                /* ... */
            case "254":
            default :
                name = String.format( i18n._("Interface {0}"), key );
                break;

            }
            
            if ( key != null ) {
                data[datacount] = [ key,name ];
                datacount++;
            }
        }

        return data;
    },
    getInterfaceStore : function(simpleMatchers) {

        var data = [];
        
        /* simple Matchers excludes WAN matchers */
        if (simpleMatchers)
            data = this.getInterfaceList(false, true);
        else
            data = this.getInterfaceList(true, true);
            
        var interfaceStore=Ext.create('Ext.data.ArrayStore',{
            idIndex:0,
            fields : ['key', 'name'],
            data : data
        });

        
        return interfaceStore;
    },
    clearInterfaceStore : function()
    {
        /* It will automatically reload the next time the interface store is fetched. */
        this.interfaceStore = null;
    },
    protocolStore:null,
    getProtocolStore : function() {
        if(this.protocolStore==null) {
        this.protocolStore=Ext.create('Ext.data.ArrayStore',{
                idIndex:0,
                fields : ['key', 'name'],
                data :[
                    ["tcp&udp", i18n._("TCP & UDP")],
                    ["udp", i18n._("UDP")],
                    ["tcp", i18n._("TCP")],
                    ["any", i18n._("ANY")]
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
            return false;
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
          var cd = changedData[id];
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
    defaultRenderer : function (value) {
        return (typeof value == 'string') ?
           value.length<1? "&#160;": Ext.util.Format.htmlEncode(value) :
           value;
    },
    getQueryStringParam : function(name){
        name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
        var regexS = "[\\?&]"+name+"=([^&#]*)";
        var regex = new RegExp( regexS );
        var results = regex.exec( window.location.href );
        if( results == null )
            return null;
        else
            return results[1];
    },
    maximize: function() {
        top.window.moveTo(1,1);
        if(Ext.isIE) {
            top.window.resizeTo(screen.availWidth,screen.availHeight);
        } else {
            top.window.outerHeight = top.screen.availHeight-30;
            top.window.outerWidth = top.screen.availWidth-30;

        }
    },
    generateListIds: function(list) {
        if(list == null) return;
        for(var i=0; i<list.length; i++) {
            //if(list[i]["id"] === undefined || list[i]["id"] == null)
                list[i]["id"]=i+1;
        }
    },
    getGenericRuleFields: function(settingsCmp) {
        return [{
                name : 'id'
            }, {
                name : 'name',
                type : 'string',
                convert : function(v) {
                    return settingsCmp.i18n._(v);
                }
            }, {
                name : 'string',
                type : 'string'
            }, {
                name : 'description',
                type : 'string',
                convert : function(v) {
                    return settingsCmp.i18n._(v);
                }
            }, {
                name : 'category',
                type : 'string'
            }, {
                name : 'enabled'
            }, {
                name : 'blocked'
            }, {
                name : 'flagged'
            }];
    },
    maxRowCount : 2147483647,
    timestampFieldWidth : 130,
    ipFieldWidth : 100,
    portFieldWidth : 70,
    hostnameFieldWidth : 120,
    uriFieldWidth : 200,
    usernameFieldWidth : 120,
    booleanFieldWidth : 60,
    emailFieldWidth : 150
};

Ung.Util.RetryHandler = {
    /**
     * retryFunction
     * @param fn Remote call to execute.
     * @param fnScope Scope to use when calling fn.
     * @param params array of parameters to pass to fn.
     * @param callback Callback to execute on success.
     * @param timeout Delay to wait until retrying the call.
     * @param count Number of retries remaining.
     */
    retry : function( fn, fnScope, params, callback, timeout, count )
    {
        var input = {
            "fn" : fn,
            "fnScope" : fnScope,
            "params" : params,
            "callback" : callback,
            "timeout" : timeout,
            "count" : count
        };

        this.callFunction( input );
    },

    completeRetry : function( result, exception, input )
    {
        var handler = Ext.bind(this.tryAgain, this, [ exception, input ] );
        var type = "noAlert";

        var count = input["count"];

        /* Do not retry any more */
        if (( count == null ) || ( count < 1 )) {
            handler = null;
            type = null;
        } else {
            input["count"]--;
        }

        if ( Ung.Util.handleException( exception, handler, type )) {
            return;
        }

        input["callback"]( result, exception );
    },

    tryAgain : function( exception, input )
    {
        if( exception.code == 500 ) {
            /* If necessary try calling the function again. */
            window.setTimeout( Ext.bind(this.callFunction, this, [ input ] ), input["timeout"] );
            // Console is not globally supported.
            // console.debug( "Retrying the call in " + input["timeout"] + " ms." );
            return;
        }

        var message = exception.message;
        if (message == null || message == "Unknown") {
            message = i18n._("Please Try Again");
            if (exception.javaStack != null)
                message += "<br/><br/>" + exception.javaStack;
        }
        Ext.MessageBox.alert(i18n._("Warning"), message);
    },

    callFunction : function( input )
    {
        var d = Ext.bind(this.completeRetry, this, [ input ], 2 );
        var fn = input["fn"];
        var fnScope = input["fnScope"];
        var params = [ d ];

        if ( input["params"] != null ) {
            params = params.concat( input["params"] );
        }

        fn.apply( fnScope, params );
    }
};

Ext.define("Ung.Util.InterfaceCombo", {
	extend:"Ext.form.ComboBox",
    initComponent : function() {
        if (( this.width == null ) && ( this.listWidth == null )) {
          this.listWidth = 200;
        }

        if ( this.simpleMatchers === null ) {
            this.simpleMatchers = false;
        }
        this.store = Ung.Util.getInterfaceStore( this.simpleMatchers );

        Ung.Util.InterfaceCombo.superclass.initComponent.call(this);
    },
    displayField : 'name',
    valueField : 'key',
    editable: false,
    mode : 'local',
    triggerAction : 'all',
    listClass : 'x-combo-list-small'
});

Ext.define("Ung.Util.ProtocolCombo", {
	extend: "Ext.form.ComboBox",
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
    },
    /**
     * @param value of the UID for users / groups
     * @reutrn the comparison value
     */
    asUID : function (value){
        if ( value == "[any]" || value == "[authenticated]" || value == "[unauthenticated]" ) {
            return "";
        }
        return value;
    },
    /**
     * @param value of the last name field - if no value is given it is pushed to the last.
     */
    asLastName : function (value){
        if(value == null || value == ""){
            return null;
        }
        return value;
    }
};

// resources map
Ung.hasResource = {};

Ext.define("Ung.ConfigItem", {
	extend: "Ext.Component",
    item: null,
    renderTo : 'configItems',
    autoEl : 'div',
    constructor : function(config) {
        this.id = "configItem_" + config.item.name;
        Ung.ConfigItem.superclass.constructor.apply(this, arguments);
    },
    onRender : function(container, position) {
        Ung.ConfigItem.superclass.onRender.call(this, container, position);
        var html = Ung.ConfigItem.template.applyTemplate({
            'iconCls' : this.item.iconClass,
            'text' : this.item.displayName
        });
        this.getEl().insertHtml("afterBegin", Ung.AppItem.buttonTemplate.applyTemplate({content:html}));
        this.getEl().addCls("app-item");
        this.getEl().on("click", this.onClick, this);
    },
    onClick: function(e) {
        if (e!=null) {
            e.stopEvent();
        }
        if(this.item.handler!=null) {
            this.item.handler.call(this,this.item);
        } else {
            Ext.MessageBox.alert(i18n._("Warning"),"TODO: implement config "+this.item.name);
        }
    },
    setIconCls: function(iconCls) {
        this.getEl().down("div[name=iconCls]").dom.className=iconCls;
    }
});
Ung.ConfigItem.template = new Ext.Template(
    '<div class="icon"><div name="iconCls" class="{iconCls}"></div></div>', '<div class="text text-center">{text}</div>');

Ext.define("Ung.AppItem", {
	extend: "Ext.Component",
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
    download: null,
    constructor : function(config) {
        var name="";
        this.subCmps=[];
        this.isValid=true;
        if(config.libItem!=null) {
            name=config.libItem.displayName;
            this.item=config.libItem;
        } else if(config.node!=null) {
            name=config.node.displayName;
            this.item=config.node;
        } else {
           this.isValid=false;
            // ignore this error from the esoft web filter rename
            if (config.trialLibitem != null && config.trialItem.displayName != "eSoft Web Filter")
                Ext.MessageBox.alert(i18n._("Apps Error"), i18n._("Error in Rack View applications list."));
           // error
        }
        this.id = "app-item_" + name;
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
        this.getEl().insertHtml("afterBegin", Ung.AppItem.buttonTemplate.applyTemplate({content:html}));
        this.getEl().addCls("app-item");

        this.progressBar = new Ext.ProgressBar({
            text : '',
            ctCls : 'progress-bar-text',
            id : 'progressBar_' + this.getId(),
            renderTo : "state_" + this.getId(),
            height : 17,
            width : 140,
            waitDefault : function(updateText) {
                this.reset();
                this.wait({
                    text:  updateText,
                    interval : 100,
                    increment : 15
                });
            }
        });

        this.buttonBuy = Ext.get("button-buy_" + this.getId());
        this.buttonBuy.setVisible(false);
        this.actionEl = Ext.get("action_" + this.getId());
        this.progressBar.hide();
        if(this.libItem!=null && this.node==null) { // libitem
            this.getEl().on("click", this.linkToStoreFn, this);
            // this.actionEl.on("click", this.linkToStoreFn, this);
            this.actionEl.insertHtml("afterBegin", i18n._("More Info"));
            this.actionEl.addCls("icon-info");
        } else if(this.node!=null) { // node
            this.getEl().on("click", this.installNodeFn, this);
            // this.actionEl.on("click", this.installNodeFn, this);
            this.actionEl.insertHtml("afterBegin", this.libItem==null?i18n._("Install"):i18n._("Trial Install"));
            this.actionEl.addCls("icon-arrow-install");
            if(this.libItem!=null) { // libitem and trial node
                this.buttonBuy.setVisible(true);
                this.buttonBuy.insertHtml("afterBegin", i18n._("Buy"));
                this.buttonBuy.on("click", Ext.bind(this.linkToStoreBuyFn,this), "buy");
                this.buttonBuy.addCls("button-buy");
                this.buttonBuy.addCls("icon-arrow-buy");
            }
        } else {
            return;
            // error
        }
        var appsLastState=main.appsLastState[this.item.displayName];
        if(appsLastState!=null) {
            this.download=appsLastState.download;
            this.setState(appsLastState.state,appsLastState.options);
        };
    },
    // get the node name associated with the App
    getNodeName : function() {
        var nodeName = null; // for libitems with no trial node return null
        if (this.node) { // nodes
            nodeName = this.node.name;
        }
        return nodeName;
    },
    // hack because I cant figure out how to tell extjs to apply style to progress text
    stylizeProgressText : function (str) {
        return "<p style=\"font-size:xx-small;text-align:left;align:left\">&nbsp;&nbsp;" + str + "</p>";
    },
    // set the state of the progress bar
    setState : function(newState, options) {
        var progressString = "";
        switch (newState) {
          case null:
          case "installed" :
            this.displayButtonsOrProgress(true);
            this.download=null;
            break;
          case "unactivating" :
            this.displayButtonsOrProgress(false);
            this.progressBar.reset();
            progressString = this.stylizeProgressText(i18n._("Unactivating..."));
            this.progressBar.waitDefault(progressString);
            break;
          case "download" :
            this.displayButtonsOrProgress(false);
            this.progressBar.reset();
            progressString = this.stylizeProgressText(i18n._("Downloading..."));
            Ung.MessageManager.setFrequency(Ung.MessageManager.highFrequency);
            this.progressBar.updateProgress(0, progressString);
            this.progressBar.waitDefault(progressString);
            break;
          case "download_summary" :
            this.displayButtonsOrProgress(false);
            this.download={
                summary:options,
                completeSize:0,
                completePackages:0
            };
            progressString = this.stylizeProgressText(Ext.String.format(i18n._("{0} Packages"), this.download.summary.count));
            this.progressBar.reset();
            this.progressBar.updateProgress(0, progressString);
            break;
          case "download_complete" :
            if(this.download!=null && this.download.summary!=null) {
                this.download.completePackages++;
                var currentPercentComplete = parseFloat(this.download.completePackages) / parseFloat(this.download.summary.size > 0 ? this.download.summary.size : 1);
                var progressIndex = parseFloat(0.99 * currentPercentComplete);
                progressString = this.stylizeProgressText(i18n._("DL") + Ext.String.format(" {0}/{1} ",this.download.completePackages, this.download.summary.count) + i18n._("done"));
                //the progress bar works better without these updates
                //this.progressBar.reset();
                //this.progressBar.updateProgress(progressIndex, progressString);
            }
            break;
          case "download_progress" :
            this.displayButtonsOrProgress(false);
            if(this.download!=null && this.download.summary!=null) {
                this.download.completeSize=options.bytesDownloaded;
                var currentPercentComplete = parseFloat(options.bytesDownloaded) / parseFloat(options.size != 0 ? options.size : 1);
                var progressIndex = parseFloat(0.99 * currentPercentComplete);
                progressString = this.stylizeProgressText(i18n._("DL") + Ext.String.format(" {0}/{1} @ {2} KB/s", this.download.completePackages, this.download.summary.count, options.speed));
                this.progressBar.reset();
                this.progressBar.updateProgress(progressIndex, progressString);
            }
            break;
          case "apt_progress" :
            this.displayButtonsOrProgress(false);
            if(this.download!=null && this.download.summary!=null) {
                var action = "";
                if (options.action.indexOf("unpack") != -1) {
                    action = i18n._("Unpacking");
                } 
                var currentPercentComplete = parseFloat(options.count) / parseFloat(options.totalCount != 0 ? options.totalCount : 1);
                var progressIndex = parseFloat(0.99 * currentPercentComplete);
                //progressString = this.stylizeProgressText(action + " " + Ext.String.format("{0}/{1}", options.count, options.totalCount));
                progressString = this.stylizeProgressText(action + " " + Ext.String.format("{0}%", Math.round(progressIndex*100)) + "&nbsp;");
                this.progressBar.reset();
                this.progressBar.updateProgress(progressIndex, progressString);
            }
            break;
          case "loadapps" :
            this.displayButtonsOrProgress(false);
            progressString = this.stylizeProgressText(i18n._("Loading Apps..."));
            this.progressBar.waitDefault(progressString);
            break;
          case "loadapp" :
            this.displayButtonsOrProgress(false);
            progressString = this.stylizeProgressText(i18n._("Loading App..."));
            this.progressBar.waitDefault(progressString);
            break;
          case "activate_timeout" :
            this.displayButtonsOrProgress(false);
            progressString = this.stylizeProgressText(i18n._("Activate timeout."));
            this.progressBar.reset();
            this.progressBar.updateProgress(1, progressString);
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
            if(!this.progressBar.isVisible()) {
                this.progressBar.show();
            }
        }
    },
    // open store buy page in a new frame
    linkToStoreBuyFn : function(e) {
        if (e!=null) {
            e.stopEvent();
        }
        if(!this.progressBar.hidden) {
            return;
        }
        main.warnOnUpgrades(Ext.bind(function() {
            main.openStoreToLibItem(this.libItem.name,Ext.String.format(i18n._("More Info - {0}"),this.item.displayName),"buy");
        },this));
    },
    // open store page in a new frame
    linkToStoreFn : function(e,action) {
        if (e!=null) {
            e.stopEvent();
        }
        if(!this.progressBar.hidden) {
            return;
        }
            main.warnOnUpgrades(Ext.bind(function() {
                main.openStoreToLibItem(this.libItem.name,Ext.String.format(i18n._("More Info - {0}"),this.item.displayName),action);
            },this));
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
        '<div id="button-buy_{id}"></div>', '<div id="action_{id}" class="action"></div>', '<div class="state-pos" id="state_{id}"></div>');
Ung.AppItem.buttonTemplate = new Ext.Template('<table cellspacing="0" cellpadding="0" border="0" style="width: 100%; height:100%"><tbody><tr><td class="app-item-left"></td><td class="app-item-center">{content}</td><td class="app-item-right"></td></tr></tbody></table>');
// update state for the app with a displayName
Ung.AppItem.updateState = function(displayName, state, options) {
    var app = Ung.AppItem.getApp(displayName);
    main.setAppLastState(displayName, state, options, app!=null?app.download:null);
    if (app != null) {
        app.setState(state, options);
    }
};
// get the app item having a item name
Ung.AppItem.getApp = function(displayName) {
    if (main.apps !== null) {
        return Ext.getCmp("app-item_" + displayName);
    }
    return null;
};
// get the app item having a libitem name
Ung.AppItem.getAppByLibItem = function(libItemName) {
    if(main.apps!=null) {
        for(var i=0; i<main.apps.length; i++) {
            if(main.apps[i].libItem!=null && main.apps[i].libItem.name==libItemName) {
                return main.apps[i];
            }
        }
    }
    return null;
};

// Node Class
Ext.define("Ung.Node", {
	extend: "Ext.Component",
    autoEl : "div",
    // ---Node specific attributes------
    // node name
    name : null,
    // node image
    image : null,
    // mackage description
    md : null,
    // --------------------------------
    hasPowerButton: null,
    // node state
    state : null, // on, off, attention, stopped
    // is powered on,
    powerOn : null,
    // running state
    runState : null, // RUNNING, INITIALIZED, DESTROYED

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
    buttonsPanel: null,
    subCmps : null,
    fnCallback: null,
    //can the node be edited on the gui
    isNodeEditable : true,
    constructor : function(config) {
        this.id = "node_" + config.nodeId;
        config.helpSource=config.displayName.toLowerCase().replace(/ /g,"_");
        if(config.runState==null) {
            config.runState="INITIALIZED";
        }
        this.subCmps = [];
        if(config.Tid.policy!=null){
            this.isNodeEditable = config.Tid.policy.id == rpc.currentPolicy.id ? true : false;
        }
        Ung.Node.superclass.constructor.apply(this, arguments);
    },
    // before Destroy
    beforeDestroy : function() {
        if(this.settingsWin && this.settingsWin.isVisible()) {
            this.settingsWin.closeWindow();
        }
        Ext.each(this.subCmps, Ext.destroy);
        if(this.hasPowerButton) {
            Ext.get('node-power_' + this.getId()).removeAllListeners();
        }
        Ung.Node.superclass.beforeDestroy.call(this);
    },
    onRender : function(container, position) {
        Ung.Node.superclass.onRender.call(this, container, position);
        main.removeNodePreview(this.name);

        this.getEl().addCls("node");
        this.getEl().set({
            'viewPosition' : this.viewPosition
        });
        this.getEl().set({
            'name' : this.displayName
        });
        if(this.fadeIn) {
            this.getEl().scrollIntoView(Ext.getCmp("center").body);
            this.getEl().syncFx();
            this.getEl().fadeIn({duration: 6});
            this.getEl().frame(null, 1, { duration: 1 });
        }
        var nodeButtons=[{
            xtype: "button",
            name : "Show Settings",
            iconCls : 'node-settings-icon',
            text : i18n._('Settings'),
            handler : Ext.bind(function() {
                this.onSettingsAction();
            },this)
        }, {
            xtype: "button",
            name : "Help",
            iconCls : 'icon-help',
            text : i18n._('Help'),
            handler : Ext.bind(function() {
                this.onHelpAction();
            },this)
        },{
            xtype: "button",
            name : "Buy",
            id: 'node-buy-button_'+this.getId(),
            iconCls : 'icon-buy',
            hidden : !(this.license && this.license.trial),
            ctCls:'buy-button-text',
            text : i18n._('Buy Now'),
            handler : Ext.bind(this.onBuyNowAction,this)
        }];
        var templateHTML = Ung.Node.template.applyTemplate({
            'id' : this.getId(),
            'image' : this.image,
            'isNodeEditable' : this.isNodeEditable === true ? "none" : "",
            'displayName' : this.displayName,
            'nodePowerCls': this.hasPowerButton?((this.license && !this.license.valid)?"node-power-expired":"node-power"):"",
            'licenseMessage' : this.getLicenseMessage()
        });
        this.getEl().insertHtml("afterBegin", templateHTML);

        this.buttonsPanel=Ext.create('Ext.panel.Panel',{
            renderTo : 'node-buttons_' + this.getId(),
            border: false,
            bodyStyle : 'background-color: transparent;',
            width: 290,
            buttonAlign: "left",
            layout:'table',
            layoutConfig: {
                columns: 3
            },
            buttons : nodeButtons
        });
        this.subCmps.push(this.buttonsPanel);
        if(this.hasPowerButton) {
            Ext.get('node-power_' + this.getId()).on('click', this.onPowerClick, this);
            this.subCmps.push(new Ext.ToolTip({
                html : Ung.Node.getStatusTip(),
                target : 'node-state_' + this.getId(),
                autoWidth : true,
                autoHeight : true,
                showDelay : 20,
                dismissDelay : 0,
                hideDelay : 0
            }));
            this.subCmps.push(new Ext.ToolTip({
                html : Ung.Node.getPowerTip(),
                target : 'node-power_' + this.getId(),
                autoWidth : true,
                autoHeight : true,
                showDelay : 20,
                dismissDelay : 0,
                hideDelay : 0
            }));
            if(this.isNodeEditable==false){
                this.subCmps.push(new Ext.ToolTip({
                    html : Ung.Node.getNonEditableNodeTip(),
                    target : 'node_' + this.nodeId,
                    autoWidth : true,
                    autoHeight : true,
                    showDelay : 20,
                    dismissDelay : 0,
                    hideDelay : 0
                }));                
            }
        }
        this.updateRunState(this.runState, true);
        this.initBlingers();
    },
    // is runState "RUNNING"
    isRunning : function() {
      return (this.runState == "RUNNING");
    },
    setState : function(state) {
        this.state = state;
        if(this.hasPowerButton) {
            document.getElementById('node-state_' + this.getId()).className = "node-state icon-state-" + this.state;
        }
    },
    setPowerOn : function(powerOn) {
        this.powerOn = powerOn;
    },
    updateRunState : function(runState, force) {
        if(runState!=this.runState || force) {
            this.runState = runState;
            switch ( runState ) {
              case "RUNNING":
                this.setPowerOn(true);
                this.setState("on");
                break;
              case "INITIALIZED":
              case "LOADED":
                this.setPowerOn(false);
                this.setState("off");
                break;
              case "DESTROYED":
                //update app display
                main.updateSeparator();
                main.loadApps();
                main.loadRackView();
                break;
            default:
                alert("Unknown runState: " + runState);
            }
        }
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
        if(this.state=="attention") {
          return;
        }
        this.loadNodeContext(Ext.bind(function() {
            this.setPowerOn(true);
            this.setState("attention");
            this.nodeContext.rpcNode.start(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception, Ext.bind(function() {
                    var title = Ext.String.format( i18n._( "Unable to start {0}" ), this.displayName );
                    Ext.MessageBox.alert(title, exception.message);
                   //this.updateRunState("INITIALIZED");
                },this),"noAlert")) return;
            },this));
        },this));
    },
    stop : function () {
        if(this.state=="attention") {
            return;
        }
        this.loadNodeContext(Ext.bind(function() {
            this.setPowerOn(false);
            this.setState("attention");
            this.nodeContext.rpcNode.stop(Ext.bind(function(result, exception) {
                //this.updateRunState("INITIALIZED");
                this.resetBlingers();
                if(Ung.Util.handleException(exception)) return;
            },this));
        },this));
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
    //on Buy Now Action
    onBuyNowAction :function(){
        var appItem=Ung.AppItem.getApp(this.displayName);
        if(appItem!=null) {
            appItem.linkToStoreFn(null,"buy");
        }
    },
    getNodeContext: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if (this.nodeContext === undefined) {
            // This asynchronous call was removed because it causes firebug to freak out
            // XXX
            //            rpc.nodeManager.nodeContext(Ext.bind(function(result, exception) {
            //                if(Ung.Util.handleException(exception)) return;
            //                this.nodeContext = result;
            //            }.createSequence(handler),this), this.Tid);
            this.nodeContext = rpc.nodeManager.nodeContext(this.Tid);
            handler.call(this);
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
            // This asynchronous call was removed because it causes firebug to freak out
            // XXX
            //            this.nodeContext.node(Ext.bind(function(result, exception) {
            //                if(Ung.Util.handleException(exception)) return;
            //                this.nodeContext.rpcNode = result;
            //            }.createSequence(handler),this));
            //        } else {
            //            handler.call(this);
            //        }
            this.nodeContext.rpcNode = this.nodeContext.node();
        } 
        handler.call(this);
    },
    getNodeDesc: function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        if(this.nodeContext==null) {
            return;
        }
        if (this.nodeContext.nodeDesc === undefined) {
            this.nodeContext.getNodeDesc(Ext.Function.createSequence(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeContext.nodeDesc = result;
            }, this), handler));
        } else {
            handler.call(this);
        }
    },
    // load Node Context
    loadNodeContext : function(handler) {
        if(handler==null) {handler=Ext.emptyFn;}
        Ext.bind(this.getNodeContext,this,[Ext.bind(this.getRpcNode,this,[Ext.bind(this.getNodeDesc,this,[handler])])]).call(this);
    },
    loadSettings : function() {
        Ext.MessageBox.wait(i18n._("Loading Settings..."), i18n._("Please wait"));
        this.settingsClassName = Ung.NodeWin.getClassName(this.name);
        if (!this.settingsClassName) {
            // Dynamically load node javaScript
            Ung.NodeWin.loadNodeScript(this, Ext.bind(function() {
                this.settingsClassName = Ung.NodeWin.getClassName(this.name);
                this.initSettings();
            },this));
        } else {
            this.initSettings();
        }
    },
    // init settings
    initSettings : function() {
    	Ext.bind(this.loadNodeContext,this,[Ext.bind(this.initSettingsTranslations,this,[Ext.bind(this.openSettings,this)])]).call(this);
    },
    initSettingsTranslations : function(handler) {
        Ung.Util.loadModuleTranslations.call(this, this.name, handler);
    },
    // open settings window
    openSettings : function() {
        var items=null;
        if (this.settingsClassName !== null) {
        	this.settingsWin=Ext.create(this.settingsClassName, {'node':this,'tid':this.nodeId,'name':this.name});
        } else {
            this.settingsWin = Ext.create('Ung.NodeWin',{
                node : this,
                items: [{
                    anchor: '100% 100%',
                    cls: 'description',
                    bodyStyle : "padding: 15px 5px 5px 15px;",
                    html: Ext.String.format(i18n._("Error: There is no settings class for the node '{0}'."),this.name)
                }]
            });
        }
        this.settingsWin.show();
        Ext.MessageBox.hide();
    },

    // remove node
    removeAction : function()
    {
        /* A hook for doing something in a node before attempting to remove it */
        if ( this.preRemoveAction ) {
            this.preRemoveAction( this, Ext.bind(this.completeRemoveAction, this ));
            return;
        }

        this.completeRemoveAction();
    },

    completeRemoveAction : function()
    {
        var message = Ext.String.format(
                i18n._("{0} is about to be removed from the rack.\nIts settings will be lost and it will stop processing network traffic.\n\nWould you like to continue removing?"),this.displayName);
        Ext.Msg.confirm(i18n._("Warning:"), message, Ext.bind(function(btn, text) {
            if (btn == 'yes') {
                if (this.settingsWin) {
                    this.settingsWin.closeWindow();
                }
                this.setState("attention");
                this.getEl().mask();
                this.getEl().fadeOut({ endOpacity: 0.1, duration: 2, remove: false, useDisplay :false});
                rpc.nodeManager.destroy(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception, Ext.bind(function() {
                        this.getEl().unmask();
                        this.getEl().stopFx();
                    },this),"alert")) return;
                    if (this) {
                        this.getEl().stopFx();
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
                        main.loadRackView();
                    }
                },this), this.Tid);
            }
        },this));
    },
    // initialize blingers
    initBlingers : function() {
        if (this.blingers !== null) {
            if(this.blingers.activityDescs!=null && this.blingers.activityDescs.list.length>0) {
                this.activityBlinger=new Ung.ActivityBlinger({
                   parentId : this.getId(),
                   bars: this.blingers.activityDescs.list
                });
                this.activityBlinger.render('node-blingers_' + this.getId());
                this.subCmps.push(this.activityBlinger);
            }
            var dispMetricDescs=[];
            if(this.blingers.metricDescs!=null) {
                for(var i=0;i<this.blingers.metricDescs.list.length;i++) {
                    if(this.blingers.metricDescs.list[i].displayable) {
                      dispMetricDescs.push(this.blingers.metricDescs.list[i]);
                    }
                }
            }
            this.blingers.dispMetricDescs=dispMetricDescs;
            if(this.blingers.dispMetricDescs.length>0) {
                this.systemBlinger=new Ung.SystemBlinger({
                   parentId : this.getId(),
                   metric: this.blingers.dispMetricDescs
                });
                this.systemBlinger.render('node-blingers_' + this.getId());
                this.subCmps.push(this.systemBlinger);
            }
        }
    },
    getLicenseMessage : function() {
        var licenseMessage = "";
        if (!this.license) {
            return licenseMessage;
        }
        if(this.license.trial) {
            if(this.license.expired) {
                licenseMessage = i18n._("Free Trial Ended");
            } else if (this.license.daysRemaining < 2) {
                licenseMessage = i18n._("Free Trial.") + " " + i18n._("Expires today");
            } else if (this.license.daysRemaining < 32) {
                licenseMessage = i18n._("Free Trial.") + " " + Ext.String.format("{0} ",this.license.daysRemaining) + i18n._("days remain");
            } else {
                licenseMessage = i18n._("Free Trial.");
            }
        }
        else { /* not a trial */
            if (this.license.valid) { 
                /* if its valid - say if its close to expiring otherwise say nothing */
                if (this.license.daysRemaining < 5) {
                    licenseMessage = i18n._("Expires in") + Ext.String.format(" {0} ",this.license.daysRemaining) + i18n._("days");
                } 
            } else {
                /* if its invalid say the reason */
                licenseMessage = this.license.status;
            }
        }
        return licenseMessage;
    },
    updateLicense : function (license) {
        this.license=license;
        this.getEl().down("div[class=node-faceplate-info]").dom.innerHTML=this.getLicenseMessage();
        document.getElementById("node-power_"+this.getId()).className=this.hasPowerButton?(this.license && !this.license.valid)?"node-power-expired":"node-power":"";
        var nodeBuyButton=Ext.getCmp("node-buy-button_"+this.getId());
        if(nodeBuyButton) {
            if(this.license && this.license.trial) {
                nodeBuyButton.show();
            } else {
              nodeBuyButton.hide();
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
        i18n._("The <B>Status Indicator</B> shows the current operating condition of a particular application."),
        '<BR>',
        '<font color="#00FF00"><b>' + i18n._("Green") + '</b></font> '
                + i18n._('indicates that the application is "on" and operating normally.'),
        '<BR>',
        '<font color="#FF0000"><b>' + i18n._("Red") + '</b></font> '
                + i18n._('indicates that the application is "on", but that an abnormal condition has occurred.'),
        '<BR>',
        '<font color="#FFFF00"><b>' + i18n._("Yellow") + '</b></font> '
                + i18n._('indicates that the application is saving or refreshing settings.'), '<BR>',
        '<b>' + i18n._("Clear") + '</b> ' + i18n._('indicates that the application is "off", and may be turned "on" by the user.'),
        '</div>'].join('');
};
Ung.Node.getPowerTip = function() {
    return i18n._('The <B>Power Button</B> allows you to turn a application "on" and "off".');
};
Ung.Node.getNonEditableNodeTip = function (){
    return i18n._('This node belongs to the parent rack shown above.<br/> To access the settings for this node, select the parent rack.'); 
};
Ung.Node.template = new Ext.Template('<div class="node-cap" style="display:{isNodeEditable}"></div><div class="node-image"><img src="{image}"/></div>', '<div class="node-label">{displayName}</div>',
    '<div class="node-faceplate-info">{licenseMessage}</div>',
    '<div class="node-blingers" id="node-blingers_{id}"></div>',
    '<div class="node-state" id="node-state_{id}" name="State"></div>',
    '<div class="{nodePowerCls}" id="node-power_{id}" name="Power"></div>',
    '<div class="node-buttons" id="node-buttons_{id}"></div>');
Ext.define("Ung.NodePreview", {
	extend: "Ext.Component",
    autoEl : "div",
    constructor : function(config) {
        this.id = "node_preview_" + config.name;
        Ung.NodePreview.superclass.constructor.apply(this, arguments);
    },

    onRender : function(container, position) {
        Ung.NodePreview.superclass.onRender.call(this, container, position);
        this.getEl().addCls("node");
        this.getEl().set({
            'viewPosition' : this.viewPosition
        });
        var templateHTML = Ung.NodePreview.template.applyTemplate({
            'id' : this.getId(),
            'image' : 'image?name='+this.name,
            'displayName' : this.displayName
        });
        this.getEl().insertHtml("afterBegin", templateHTML);
        this.getEl().scrollIntoView(Ext.getCmp("center").body);
        this.getEl().fadeIn({ endOpacity: 0.58, duration: 20});
    }
});
Ung.NodePreview.template = new Ext.Template('<div class="node-image"><img src="{image}"/></div>', '<div class="node-label">{displayName}</div>');

// Message Manager object
Ung.MessageManager = {
    // update interval in millisecond
    normalFrequency : 5000,
    highFrequency : 1000,
    started : false,
    intervalId : null,
    cycleCompleted : true,
    upgradeMode: false,
    installInProgress:0,
    upgradeSummary: null,
    upgradesComplete: 0,
    historyMaxSize:100,
    messageHistory:[], // for debug info
    firstToleratedError: null,
    errorToleranceInterval: 600000, //10 minutes

    start : function(now) {
        this.stop();
        if(now) {
            Ung.MessageManager.run();
        }
        this.setFrequency(this.normalFrequency);
        this.started = true;
    },
    startUpgradeMode: function() {
        this.stop();
        this.upgradeMode=true;
        this.setFrequency(this.highFrequency);
        this.started = true;
    },
    setFrequency: function(timeMs) {
        this.currentFrequency = timeMs;
        if (this.intervalId !== null) {
            window.clearInterval(this.intervalId);
        }
        this.intervalId = window.setInterval("Ung.MessageManager.run()", timeMs);
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
        rpc.messageManager.getMessageQueue(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception, Ext.bind(function() {
                //Tolerate Error 500: Internal Server Error after an install
                //Keep silent for maximum 10 minutes of sequential error messages
                //because apache may reload
                if(exception.code==500 || exception.code ==12031) {
                    if(this.firstToleratedError==null) {
                        this.firstToleratedError=(new Date()).getTime();
                        this.cycleCompleted = true;
                        return;
                    } else if(((new Date()).getTime()-this.firstToleratedError)<this.errorToleranceInterval) {
                        this.cycleCompleted = true;
                        return;
                    }
                }
                /* After a hostname change and the certificate is regenerated. */
                else if ( exception.code == 12019 ) {
                    Ext.MessageBox.alert(i18n._("System Busy"), "Please refresh the page", Ext.bind(function() {
                        this.cycleCompleted = true;
                    },this));
                    return;
                }

                // otherwise call handleException but without "noAlert"
                Ung.Util.handleException(exception, Ext.bind(function() {
                    this.cycleCompleted = true;
                },this));
            },this),"noAlert")) return;
            this.firstToleratedError=null; //reset error tolerance on a good response
            this.cycleCompleted = true;
            try {
                var messageQueue=result;
                if(messageQueue.messages.list!=null && messageQueue.messages.list.length>0) {
                    Ung.MessageManager.messageHistory.push(messageQueue.messages.list);
                    if(Ung.MessageManager.messageHistory.length>Ung.MessageManager.historyMaxSize) {
                        Ung.MessageManager.messageHistory.shift();
                    }

                    var refreshApps=false;
                    var startUpgradeMode=false;
                    var lastUpgradeDownloadProgressMsg=null;
                    for(var i=0;i<messageQueue.messages.list.length;i++) {
                        var msg=messageQueue.messages.list[i];
                        if(msg.javaClass.indexOf("NodeStateChange") >= 0) {
                            var node=Ung.Node.getCmp(msg.nodeDesc.nodeId.id);
                            if(node!=null) {
                                node.updateRunState(msg.nodeState);
                            }
                        } else if (msg.javaClass.indexOf("PackageInstallRequest") >= 0) {
                            if(!msg.installed) {
                                var policy=null;
                                policy = rpc.currentPolicy;
                                var appItemDisplayName=msg.packageDesc.type=="TRIAL"?main.findLibItemDisplayName(msg.packageDesc.fullVersion):msg.packageDesc.displayName;
                                Ung.AppItem.updateState(appItemDisplayName, "download");
                                if ( main.isIframeWinVisible()) {
                                    main.getIframeWin().closeActionFn();
                                }
                                if(main.IEWin != null){
                                    main.IEWin.close();
                                    main.IEWin=null;
                                }

                                //already checked for upgrades
                                //main.warnOnUpgrades(Ext.bind(function() {

                                rpc.toolboxManager.installAndInstantiate(Ext.bind(function(result, exception) {
                                    if (exception)
                                        Ung.AppItem.updateState(appItemDisplayName, null);
                                    if(Ung.Util.handleException(exception)) return;
                                },this),msg.packageDesc.name, policy);

                                //},this));
                            }
                        } else if (msg.javaClass.indexOf("PackageUninstallRequest") >= 0) {
                            if(!msg.installed) {
                                var appItemDisplayName=msg.packageDesc.type=="TRIAL"?main.findLibItemDisplayName(msg.packageDesc.fullVersion):msg.packageDesc.displayName;
                                Ung.AppItem.updateState(appItemDisplayName, "uninstall");
                                rpc.toolboxManager.unregister(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                },this),msg.packageDesc.name);
                                rpc.toolboxManager.uninstall(Ext.bind(function(result, exception) {
                                    if(Ung.Util.handleException(exception)) return;
                                    if ( main.isIframeWinVisible()) {
                                        main.getIframeWin().closeActionFn();
                                    }
                                },this),msg.packageDesc.name);
                            }
                        } else if(msg.javaClass.indexOf("NodeInstantiated") != -1) {
                            if(msg.policy==null || msg.policy.id == rpc.currentPolicy.id) {
                                refreshApps=true;
                                var node=main.getNode(msg.nodeDesc.name,msg.nodeDesc.nodeId.policy);
                                if(!node) {
                                    node=main.createNode(msg.nodeDesc, msg.statDescs, msg.license,"INITIALIZED");
                                    main.nodes.push(node);
                                    main.addNode(node,true);
                                    main.removeParentNode(node,msg.nodeDesc.nodeId.policy);
                                } else {
                                    main.loadLicenses();
                                }
                            } else {
                                Ung.AppItem.updateState(msg.nodeDesc.displayName, null);
                            }
                        } else if(msg.javaClass.indexOf("InstallAndInstantiateComplete") != -1) {
                            refreshApps=true;
                            this.installInProgress--;
                            var appItemDisplayName=msg.requestingPackage.type=="TRIAL"?main.findLibItemDisplayName(msg.requestingPackage.fullVersion):msg.requestingPackage.displayName;
                            Ung.MessageManager.setFrequency(Ung.MessageManager.normalFrequency);
                            Ung.AppItem.updateState(appItemDisplayName, null);
                        } else if(msg.javaClass.indexOf("LicenseUpdateMessage") != -1) {
                            main.loadLicenses();
                        } else {
                            if( msg.upgrade==false || msg.upgrade === undefined ) {
                                var appItemDisplayName=msg.requestingPackage.type=="TRIAL"?main.findLibItemDisplayName(msg.requestingPackage.fullVersion):msg.requestingPackage.displayName;
                                if(msg.javaClass.indexOf("DownloadSummary") != -1) {
                                    Ung.AppItem.updateState(appItemDisplayName, "download_summary", msg);
                                } else if(msg.javaClass.indexOf("DownloadProgress") != -1) {
                                    Ung.AppItem.updateState(appItemDisplayName, "download_progress", msg);
                                } else if(msg.javaClass.indexOf("DownloadComplete") != -1) {
                                    if(msg.success) {
                                       Ung.AppItem.updateState(appItemDisplayName, "download_complete");
                                    } else {
                                        Ext.MessageBox.alert(i18n._("Warning"), Sting.format(i18n._("Error downloading package {0}: {1}"),appItemDisplayName,msg.errorMessage));
                                        Ung.AppItem.updateState(appItemDisplayName);
                                    }
                                } else if(msg.javaClass.indexOf("DownloadAllComplete") != -1) {
                                    if(msg.success) {
                                       Ung.AppItem.updateState(appItemDisplayName, "loadapps");
                                    } else {
                                        Ext.MessageBox.alert(i18n._("Warning"), Sting.format(i18n._("Error installing package {0}: Aborted."),appItemDisplayName));
                                        Ung.AppItem.updateState(appItemDisplayName);
                                    }
                                } else if(msg.javaClass.indexOf("AptMessage") != -1) {
                                    if(msg.action.indexOf("alldone") != -1) {
                                        this.installInProgress++;
                                        Ung.AppItem.updateState(appItemDisplayName, "loadapps");
                                    } else {
                                        Ung.AppItem.updateState(appItemDisplayName, "apt_progress", msg);
                                    }
                                } 
                            } else if(msg.upgrade==true) {
                                if(startUpgradeMode!="stop" && !this.upgradeMode) {
                                    startUpgradeMode=true;
                                }
                                if(msg.javaClass.indexOf("DownloadSummary") != -1) {
                                    if(Ext.MessageBox.isVisible() && Ext.MessageBox.title==i18n._("Downloading upgrades...")) {
                                        Ext.MessageBox.wait(i18n._("Downloading upgrades..."), i18n._("Please wait"));
                                    }
                                    this.upgradeSummary=msg;
                                } else if(msg.javaClass.indexOf("DownloadProgress") != -1) {
                                    if(lastUpgradeDownloadProgressMsg!="stop") {
                                        lastUpgradeDownloadProgressMsg=msg;
                                    }
                                } else if(msg.javaClass.indexOf("DownloadComplete") != -1) {
                                    this.upgradesComplete++;
                                    if(!msg.success) {
                                        lastUpgradeDownloadProgressMsg="stop";
                                        startUpgradeMode="stop";
                                        Ext.MessageBox.alert(i18n._("Warning"), i18n._("Error downloading packages. Install Aborted."));
                                    }
                                } else if(msg.javaClass.indexOf("DownloadAllComplete") != -1) {
                                    lastUpgradeDownloadProgressMsg="stop";
                                    startUpgradeMode="stop";
                                    this.stop();
                                    Ext.MessageBox.wait(i18n._("Initializing..."), i18n._("Please wait"), {
                                        interval : 500,
                                        increment : 60,
                                        duration: 60000,
                                        fn: function() {
                                            Ext.MessageBox.alert(
                                                i18n._("Applying Upgrade"),
                                                i18n._("The upgrades have been downloaded and are now being applied.  <strong>DO NOT REBOOT AT THIS TIME.</strong>  Please be patient this process will take a few minutes. After the upgrade is complete you will be able to log in again."),
                                                Ung.Util.goToStartPage);
                                        }
                                    });
                                } 
                            }
                        }
                    }
                    if(lastUpgradeDownloadProgressMsg!=null && lastUpgradeDownloadProgressMsg!="stop") {
                        var msg=lastUpgradeDownloadProgressMsg;
                        var text=Ext.String.format(i18n._("Package: {0}<br/>Progress: {1} kB/{2} kB <br/>Speed: {3}kB/sec"),msg.name, Math.round(msg.bytesDownloaded/1024), Math.round(msg.size/1024), msg.speed);
                        if(this.upgradeSummary) {
                            text+=Ext.String.format(i18n._("<br/>Package {0}/{1}"),this.upgradesComplete+1, this.upgradeSummary.count);
                        }
                        var msgTitle=i18n._("Downloading upgrades... Please wait");
                        if(!Ext.MessageBox.isVisible() || Ext.MessageBox.title!=msgTitle) {
                            Ext.MessageBox.progress(msgTitle, text);
                        }
                        var progressIndex = msg.size!=0?parseFloat(msg.bytesDownloaded/ msg.size):0;
                        Ext.MessageBox.updateProgress(progressIndex, "", text);
                    }
                    if(startUpgradeMode==true) {
                        this.startUpgradeMode();
                    }
                    if(refreshApps && !this.upgradeMode) {
                        main.updateSeparator();
                        main.loadApps();
                    }
                }
                if(!Ung.MessageManager.upgradeMode) {
                  // update system stats
                  main.systemStats.update(messageQueue.systemStats);
                  // upgrade nodes blingers
                  for (var i = 0; i < main.nodes.length; i++) {
                    var nodeCmp = Ung.Node.getCmp(main.nodes[i].nodeId);
                    if (nodeCmp && nodeCmp.isRunning()) {
                      nodeCmp.stats = messageQueue.stats.map[main.nodes[i].nodeId];
                      nodeCmp.updateBlingers();
                    }
                  }
                }
            } catch (err) {
                Ext.MessageBox.alert("Exception in MessageManager", err.message);
            }
        },this), rpc.messageKey, rpc.currentPolicy);
    }
};
Ext.define("Ung.SystemStats", {
	extend: "Ext.Component",
    autoEl : 'div',
    renderTo: "rack-list",
    constructor : function(config) {
        this.id = "system_stats";
        Ung.SystemStats.superclass.constructor.apply(this, arguments);
    },
    onRender : function(container, position) {
        Ung.SystemStats.superclass.onRender.call(this, container, position);
        this.getEl().addCls("system-stats");
        var contentSystemStatsArr=[
            '<div class="label" style="width:100px;left:0px;">'+i18n._("Network")+'</div>',
            '<div class="label" style="width:70px;left:103px;">'+i18n._("Sessions")+'</div>',
            '<div class="label" style="width:70px;left:173px;">'+i18n._("CPU Load")+'</div>',
            '<div class="label" style="width:75px;left:250px;">'+i18n._("Memory")+'</div>',
            '<div class="label" style="width:40px;right:-5px;">'+i18n._("Disk")+'</div>',
            '<div class="network"><div class="tx">'+i18n._("Tx:")+'<div class="tx-value"></div></div><div class="rx">'+i18n._("Rx:")+'<div class="rx-value"></div></div></div>',
            '<div class="sessions"></div>',
            '<div class="cpu"></div>',
            '<div class="memory"><div class="free">'+i18n._("F:")+'<div class="free-value"></div></div><div class="used">'+i18n._("U:")+'<div class="used-value"></div></div></div>',
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
        this.networkToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=network]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extended-stats',
            renderTo: Ext.getBody(),
            html: networkArr.join('')
        });

        // sessions tooltip
        var sessionsArr=[
            '<div class="title">'+i18n._("Total Sessions:")+'</div>',
            '<div class="values"><span name="totalSessions"></span></div>',
            '<div class="title">'+i18n._("TCP Sessions:")+'</div>',
            '<div class="values"><span name="uvmTCPSessions"></span></div>',
            '<div class="title">'+i18n._("UDP Sessions:")+'</div>',
            '<div class="values"><span name="uvmUDPSessions"></span></div>'
        ];
        this.sessionsToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=sessions]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extended-stats',
            renderTo: Ext.getBody(),
            html: sessionsArr.join('')
        });

        // cpu tooltip
        var cpuArr=[
            '<div class="title">'+i18n._("Number of Processors / Type / Speed:")+'</div>',
            '<div class="values"><span name="num_cpus"></span>, <span name="cpu_model"></span>, <span name="cpu_speed"></span></div>',
            '<div class="title">'+i18n._("Load average (1 min , 5 min , 15 min):")+'</div>',
            '<div class="values"><span name="load_average_1_min"></span>, <span name="load_average_5_min"></span>, <span name="load_average_15_min"></span></div>',

            '<div class="title">'+i18n._("Tasks (Processes)")+'</div>',
            '<div class="values"><span name="tasks"></span>'+'</div>',
            '<div class="title">'+i18n._("Uptime:")+'</div>',
            '<div class="values"><span name="uptime"></span></div>'
        ];
        this.cpuToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=cpu]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extended-stats',
            renderTo: Ext.getBody(),
            html: cpuArr.join('')
        });

        // memory tooltip
        var memoryArr=[

            '<div class="title">'+i18n._("Total Memory:")+'</div>',
            '<div class="values"><span name="memory_total"></span> MB</div>',
            '<div class="title">'+i18n._("Memory Used:")+'</div>',
            '<div class="values"><span name="memory_used"></span> MB, <span name="memory_used_percent"></span> %</div>',
            '<div class="title">'+i18n._("Memory Free:")+'</div>',
            '<div class="values"><span name="memory_free"></span> MB, <span name="memory_free_percent"></span> %</div>',
            '<div class="title">'+i18n._("Swap Files:")+'</div>',
            '<div class="values"><span name="swap_total"></span> MB '+i18n._("total swap space")+' (<span name="swap_used"></span> MB '+i18n._("used")+')</div>'
        ];
        this.memoryToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=memory]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extended-stats',
            renderTo: Ext.getBody(),
            html: memoryArr.join('')
        });

        // disk tooltip
        var diskArr=[
            '<div class="title">'+i18n._("Total Disk Space:")+'</div>',
            '<div class="values"><span name="total_disk_space"></span> GB</div>',
            '<div class="title">'+i18n._("Free Disk Space:")+'</div>',
            '<div class="values"><span name="free_disk_space"></span> GB</div>',
            '<div class="title">'+i18n._("Data read:")+'</div>',
            '<div class="values"><span name="disk_reads"></span> MB, <span name="disk_reads_per_second"></span> b/sec</div>',
            '<div class="title">'+i18n._("Data write:")+'</div>',
            '<div class="values"><span name="disk_writes"></span> MB, <span name="disk_writes_per_second"></span> b/sec</div>'
        ];
        this.diskToolTip= Ext.create('Ext.tip.ToolTip',{
            target: this.getEl().down("div[class=disk]"),
            dismissDelay:0,
            hideDelay :400,
            width: 330,
            cls: 'extended-stats',
            renderTo: Ext.getBody(),
            html: diskArr.join('')
        });

    },
    update : function(stats) {
        var sessionsText = '<font color="#55BA47">' + stats.map.uvmSessions + "</font>";
        if (stats.map.uvmSessions > 8000)
            sessionsText = '<font color="orange">' + stats.map.uvmSessions + "</font>";
        if (stats.map.uvmSessions > 9000)
            sessionsText = '<font color="red">' + stats.map.uvmSessions + "</font>";
        this.getEl().down("div[class=sessions]").dom.innerHTML=sessionsText;
        
        this.getEl().down("div[class=cpu]").dom.innerHTML=stats.map.oneMinuteLoadAvg;
        var oneMinuteLoadAvg = stats.map.oneMinuteLoadAvg;
        var oneMinuteLoadAvgAdjusted = oneMinuteLoadAvg - stats.map.numCpus;
        var loadText = '<font color="#55BA47">' + i18n._('low') + '</font>';
        if (oneMinuteLoadAvgAdjusted > 1.0)
            loadText = '<font color="orange">' + i18n._('medium') + '</font>';
        if (oneMinuteLoadAvgAdjusted > 4.0)
            loadText = '<font color="red">' + i18n._('high') + '</font>';
        this.getEl().down("div[class=cpu]").dom.innerHTML=loadText;
            
        var txSpeed=Math.round(stats.map.txBps/10)/100;
        var rxSpeed=Math.round(stats.map.rxBps/10)/100;
        this.getEl().down("div[class=tx-value]").dom.innerHTML=txSpeed+"KB/s";
        this.getEl().down("div[class=rx-value]").dom.innerHTML=rxSpeed+"KB/s";
        var memoryFree=Ung.Util.bytesToMBs(stats.map.MemFree);
        var memoryUsed=Ung.Util.bytesToMBs(stats.map.MemTotal-stats.map.MemFree);
        this.getEl().down("div[class=free-value]").dom.innerHTML=memoryFree+" MB";
        this.getEl().down("div[class=used-value]").dom.innerHTML=memoryUsed+" MB";
        var diskPercent=Math.round((1-stats.map.freeDiskSpace/stats.map.totalDiskSpace)*20 )*5;
        this.getEl().down("div[name=disk_value]").dom.className="disk"+diskPercent;
        if(this.networkToolTip.rendered) {
            var toolTipEl=this.networkToolTip.getEl();
            toolTipEl.down("span[name=tx_speed]").dom.innerHTML=txSpeed;
            toolTipEl.down("span[name=rx_speed]").dom.innerHTML=rxSpeed;
        }
        if(this.sessionsToolTip.rendered) {
            var toolTipEl=this.sessionsToolTip.getEl();
            toolTipEl.down("span[name=totalSessions]").dom.innerHTML=stats.map.uvmSessions /* XXX plus bypassed sessions */ ; 
            toolTipEl.down("span[name=uvmTCPSessions]").dom.innerHTML=stats.map.uvmTCPSessions;
            toolTipEl.down("span[name=uvmUDPSessions]").dom.innerHTML=stats.map.uvmUDPSessions;
        }
        if(this.cpuToolTip.rendered) {
            var toolTipEl=this.cpuToolTip.getEl();
            toolTipEl.down("span[name=num_cpus]").dom.innerHTML=stats.map.numCpus;
            toolTipEl.down("span[name=cpu_model]").dom.innerHTML=stats.map.cpuModel;
            toolTipEl.down("span[name=cpu_speed]").dom.innerHTML=stats.map.cpuSpeed;
            var uptimeAux=Math.round(stats.map.uptime);
            var uptimeSeconds = uptimeAux%60;
            uptimeAux=parseInt(uptimeAux/60);
            var uptimeMinutes = uptimeAux%60;
            uptimeAux=parseInt(uptimeAux/60);
            var uptimeHours = uptimeAux%24;
            uptimeAux=parseInt(uptimeAux/24);
            var uptimeDays = uptimeAux;

            toolTipEl.down("span[name=uptime]").dom.innerHTML=(uptimeDays>0?(uptimeDays+" "+(uptimeDays==1?i18n._("Day"):i18n._("Days"))+", "):"") + ((uptimeDays>0 || uptimeHours>0)?(uptimeHours+" "+(uptimeHours==1?i18n._("Hour"):i18n._("Hours"))+", "):"") + uptimeMinutes+" "+(uptimeMinutes==1?i18n._("Minute"):i18n._("Minutes"));
            toolTipEl.down("span[name=tasks]").dom.innerHTML=stats.map.numProcs;
            toolTipEl.down("span[name=load_average_1_min]").dom.innerHTML=stats.map.oneMinuteLoadAvg;
            toolTipEl.down("span[name=load_average_5_min]").dom.innerHTML=stats.map.fiveMinuteLoadAvg;
            toolTipEl.down("span[name=load_average_15_min]").dom.innerHTML=stats.map.fifteenMinuteLoadAvg;
        }
        if(this.memoryToolTip.rendered) {
            var toolTipEl=this.memoryToolTip.getEl();
            toolTipEl.down("span[name=memory_used]").dom.innerHTML=memoryUsed;
            toolTipEl.down("span[name=memory_free]").dom.innerHTML=memoryFree;
            toolTipEl.down("span[name=memory_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.MemTotal);
            toolTipEl.down("span[name=memory_used_percent]").dom.innerHTML=Math.round((stats.map.MemTotal-stats.map.MemFree)/stats.map.MemTotal*100);
            toolTipEl.down("span[name=memory_free_percent]").dom.innerHTML=Math.round(stats.map.MemFree/stats.map.MemTotal*100);

            toolTipEl.down("span[name=swap_total]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.SwapTotal);
            toolTipEl.down("span[name=swap_used]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.SwapTotal-stats.map.SwapFree);
        }
        if(this.diskToolTip.rendered) {
            var toolTipEl=this.diskToolTip.getEl();
            toolTipEl.down("span[name=total_disk_space]").dom.innerHTML=Math.round(stats.map.totalDiskSpace/10000000)/100;
            toolTipEl.down("span[name=free_disk_space]").dom.innerHTML=Math.round(stats.map.freeDiskSpace/10000000)/100;
            toolTipEl.down("span[name=disk_reads]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.diskReads);
            toolTipEl.down("span[name=disk_reads_per_second]").dom.innerHTML=Math.round(stats.map.diskReadsPerSecond*100)/100;
            toolTipEl.down("span[name=disk_writes]").dom.innerHTML=Ung.Util.bytesToMBs(stats.map.diskWrites);
            toolTipEl.down("span[name=disk_writes_per_second]").dom.innerHTML=Math.round(stats.map.diskWritesPerSecond*100)/100;
        }
    },
    reset : function() {
    }
});

// Activity Blinger Class
Ext.define("Ung.ActivityBlinger", {
	extend: "Ext.Component",
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
        this.getEl().addCls("activity-blinger");
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
                out.push('<div class="blinger-text activity-blinger-text" style="top:' + top + 'px;">' + i18n._(bar) + '</div>');
                out.push('<div class="activity-blinger-bar" style="top:' + top + 'px;width:0px;display:none;" id="activityBar_'
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
Ung.ActivityBlinger.template = new Ext.Template('<div class="blinger-name">{blingerName}</div>',
        '<div class="activity-blinger-box" id="blingerBox_{id}"></div>');
Ung.ActivityBlinger.decayFactor = Math.pow(0.94, Ung.MessageManager.normalFrequency / 1000);
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
Ext.define("Ung.SystemBlinger", {
	extend: "Ext.Component",
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
        this.getEl().addCls("system-blinger");
        var templateHTML = Ung.SystemBlinger.template.applyTemplate({
            'id' : this.getId(),
            'blingerName' : i18n._("system")
        });
        this.getEl().insertHtml("afterBegin", templateHTML);

        var out = [];
        for (var i = 0; i < 4; i++) {
            var top = 1 + i * 15;
            out.push('<div class="blinger-text system-blinger-label" style="top:' + top + 'px;" id="systemName_' + this.getId() + '_' + i + '"></div>');
            out.push('<div class="blinger-text system-blinger-value" style="top:' + top + 'px;" id="systemValue_' + this.getId() + '_' + i + '"></div>');
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
            Ext.MessageBox.alert(i18n._("Warning"), Ext.String.format(i18n._("The node {0} has {1} metrics. The maximum number of metrics is {2}."),nodeCmp.displayName ,activeMetrics.length,4));
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
                            fn : Ext.bind(function(elem, checked) {
                                if(checked && this.tempMetrics.length>=4) {
                                    Ext.MessageBox.alert(i18n._("Warning"),i18n._("Please set up to four items."));
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

                            },this)
                        }
                    }
                });
            }
            this.configWin= Ext.create("Ung.Window", {
                blingerCmp: this,
                modal : true,
                title : i18n._("Set up to four"),
                bodyStyle : "padding: 5px 5px 5px 15px;",
                defaults: {},
                items: configItems,
                autoScroll : true,
                draggable : true,
                resizable : true,
                buttons: [{
                    name : 'Ok',
                    text : i18n._("Ok"),
                    handler : Ext.bind(function() {
                        this.updateActiveMetrics();
                        this.configWin.hide();
                    },this)
                },{
                    name : 'Cancel',
                    text : i18n._("Cancel"),
                    handler : Ext.bind(function() {
                        this.configWin.hide();
                    },this)
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
                var metricItem=this.configWin.items.get(i);
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
        rpc.messageManager.setActiveMetrics(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var nodeCmp = Ext.getCmp(this.parentId);
            nodeCmp.blingers.activeMetrics.list=activeMetrics;
            this.buildActiveMetrics();
        },this),nodeCmp.Tid,{javaClass:"java.util.List", list:activeMetrics});

    },
    update : function(stats) {
        // UPDATE COUNTS
        var nodeCmp = Ext.getCmp(this.parentId);
        var activeMetrics=nodeCmp.blingers.activeMetrics.list;
        for (var i = 0; i < activeMetrics.length; i++) {
            var activeMetric=activeMetrics[i];
            var newValue="&nbsp;";
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
Ung.SystemBlinger.template = new Ext.Template('<div class="blinger-name">{blingerName}</div>',
        '<div class="system-blinger-box" id="blingerBox_{id}"></div>',
        '<div class="systemStatSettings" id="systemStatSettings_{id}"></div>');
Ext.ComponentMgr.registerType('ungSystemBlinger', Ung.SystemBlinger);

// Event Log class
Ext.define("Ung.GridEventLog", {
	extend: "Ext.grid.Panel",
    // the settings component
    settingsCmp : null,
    // refresh on activate Tab (each time the tab is clicked)
    refreshOnActivate : true,
    // Event manager rpc function to call
    // default is getEventQueries() from settingsCmp
    eventQueriesFn : null,
    // Records per page
    recordsPerPage : 25,
    // fields for the Store
    fields : null,
    // columns for the column model
    columns : null,
    enableHdMenu : false,
    enableColumnMove: false,
    // for internal use
    rpc : null,
    helpSource : 'event_log',
    // mask to show during refresh
    //loadMask : {msg: i18n._("Refreshing...")},
    // called when the component is initialized
    constructor: function(config) {
    	 var modelName='Ung.GridEventLog.Store.ImplicitModel-' + Ext.id();
    	 Ext.define(modelName, {
             extend: 'Ext.data.Model',
             fields: config.fields
         });
    	 config.modelName = modelName;

    	this.callParent([config]);
    },
    initComponent : function() {
        this.rpc = {};

        if ( this.title == null ) {
            this.title = i18n._('Event Log');
        }
        if ( this.hasAutoRefresh == null ) {
            this.hasAutoRefresh = true;
        }
        if ( this.autoExpandColumn == null ) {
            this.autoExpandColumn = "timestamp";
        }
        if ( this.name == null ) {
            this.name = "Event Log";
        }
        if ( this.eventQueriesFn == null ) {
            this.eventQueriesFn = this.settingsCmp.node.nodeContext.rpcNode.getEventQueries;
        }
        this.rpc.repository = {};
        this.store=Ext.create('Ext.data.Store', {
            model: this.modelName,
            data: [],
            pageSize : this.recordsPerPage,
            proxy: {
                type: 'pagingmemory',
                reader: {
                    type: 'json',
                    root: 'list'
                }
            },
            autoLoad: false,
			remoteSort:true
        });
        
        this.pagingToolbar = Ext.create('Ext.toolbar.Paging',{
            //y: -2,//TODO: no longer supported in extjs4
            //height: 21,
            //pageSize : this.recordsPerPage,//TODO: move this to store
            width: 250,
            store : this.store,
            style: "border:0; top:1px;"
        });

        this.bbar = [{
            xtype : 'tbtext',
            id: "querySelector_"+this.getId(),
            text : ''
        }, {
            xtype : 'tbtext',
            id: "rackSelector_"+this.getId(),
            text : ''
        }, {
            xtype : 'button',
            id: "refresh_"+this.getId(),
            text : i18n._('Refresh'),
            name : "Refresh",
            tooltip : i18n._('Refresh'),
            iconCls : 'icon-refresh',
            handler : Ext.bind(this.refreshHandler,this, [false])
        }, {
            xtype : 'button',
            id: "flush_"+this.getId(),
            text : i18n._('Full Refresh'),
            name : "Flush",
            tooltip : i18n._('Flush Events from Memory to Database and then Refresh'),
            iconCls : 'icon-refresh',
            handler : Ext.bind(this.flushHandler,this, [true])
        }, {
            xtype : 'button',
            hidden : !this.hasAutoRefresh,
            id: "auto_refresh_"+this.getId(),
            text : i18n._('Auto Refresh'),
            enableToggle: true,
            pressed: false,
            name : "Auto Refresh",
            tooltip : i18n._('Auto Refresh every 5 seconds (does not flush)'),
            iconCls : 'icon-autorefresh',
            handler : Ext.bind(function() {
                var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                if(autoRefreshButton.pressed) {
                    this.startAutoRefresh();
                } else {
                    this.stopAutoRefresh();
                }
            },this)
        }, {
            xtype : 'button',
            id: "export_"+this.getId(),
            text : i18n._('Export'),
            name : "Export",
            tooltip : i18n._('Export Events to File'),
            iconCls : 'icon-export',
            handler : Ext.bind(this.exportHandler,this)
        }, {
            xtype : 'tbtext',
            text : '<div style="width:30px;"></div>'
        }, this.pagingToolbar, {
            xtype : 'tbtext',
            text : '<i><font color="red">' + i18n._('Warning') + ':</font></i> ' + i18n._('Event logs are delayed by a few minutes.')
        }];
        Ung.GridEventLog.superclass.initComponent.call(this);
 
		var cmConfig = this.columns;
        for (i in cmConfig) {
            if (cmConfig[i].sortable == true || cmConfig[i].sortable == null) {
                cmConfig[i].sortable = true;
                cmConfig[i].initialSortable = true;
            } else {
                cmConfig[i].initialSortable = false;
            }
        }
    },
    autoRefreshEnabled:true,
    startAutoRefresh: function(setButton) {
        this.pagingToolbar.hide();
        this.autoRefreshEnabled=true;
        var columnModel=this.columns;
        this.getStore().sort(columnModel[0].dataIndex, "DESC");
        for (i in columnModel) {
            columnModel[i].sortable = false;
        }
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(true);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.disable();
        this.autorefreshList();
    },
    stopAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=false;
        this.pagingToolbar.show();
        var columnModel=this.columns;
        for (i in columnModel) {
            columnModel[i].sortable = columnModel[i].initialSortable;
        }
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(false);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.enable();
    },
    autorefreshCallback : function(result, exception) {
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
        //this.makeSelectable();
        if(this!=null && this.rendered && this.autoRefreshEnabled) {
            if(this==this.settingsCmp.tabs.getActiveTab()) {
            	Ext.Function.defer(this.autorefreshList,5000,this);
            } else {
                this.stopAutoRefresh(true);
            }
        }
    },
    autorefreshList : function() {
        var selQuery = this.getSelectedQuery();
        var selPolicy = this.getSelectedPolicy();
        if (selQuery != null && selPolicy != null) {
            rpc.jsonrpc.UvmContext.getEvents(Ext.bind(this.autoRefreshCallback,this), selQuery, selPolicy, 1000 );
        }
    },
    exportHandler : function() {
        var selQuery = this.getSelectedQuery();
        var selQueryName = this.getSelectedQueryName();
        var selPolicy = this.getSelectedPolicy();
        if (selQuery != null && selPolicy != null) {
            Ext.MessageBox.wait(i18n._("Exporting Events..."), i18n._("Please wait"));
    	    var name = ( (this.name!=null) ? this.name : i18n._("Event Log") ) + " " +selQueryName;
            name=name.trim().replace(/ /g,"_");
            var exportForm = document.getElementById('exportEventLogEvents');
            exportForm["name"].value=name;
            exportForm["query"].value=selQuery;
            exportForm["policyId"].value=selPolicy;
            exportForm["columnList"].value=this.getColumnList();
            //exportForm["data"].value=Ext.encode(rpc.jsonrpc.UvmContext.getEvents( selQuery, selPolicy, 1000000 ));
            exportForm.submit();
            Ext.MessageBox.hide();
        }
    },
    // called when the component is rendered
    onRender : function(container, position) {
        Ung.GridEventLog.superclass.onRender.call(this, container, position);
        //TODO: extjs4 migration find an alternative
        //this.getGridEl().down("div[class*=x-grid3-viewport]").set({'name' : "Table"});
        //this.pagingToolbar.loading.hide();

        if (this.eventQueriesFn != null) {
            this.rpc.eventLogQueries=this.eventQueriesFn();
            var queryList = this.rpc.eventLogQueries;
            var displayStyle;
            var i;
            var out;
            
            displayStyle="";
            // if(repList.length==0) { displayStyle="display:none;"; } // commented out - always show selector
            out = [];
            out.push('<select name="Event Type" id="selectQuery_' + this.getId() + '_' + this.settingsCmp.node.nodeId + '" style="'+displayStyle+'">');
            for (i = 0; i < queryList.length; i++) {
                var queryDesc = queryList[i];
                var selOpt = (i === 0) ? "selected" : "";
                out.push('<option value="' + queryDesc.query + '" ' + selOpt + '>' + this.settingsCmp.i18n._(queryDesc.name) + '</option>');
            }
            out.push('</select>');
            Ext.getCmp('querySelector_' + this.getId()).setText(out.join(""));
            

            displayStyle="";
            if (this.settingsCmp.node.nodeContext.nodeDesc.type == "SERVICE") displayStyle = "display:none;"; //hide rack selector for services
            out = [];
            out.push('<select name="Rack" id="selectPolicy_' + this.getId() + '_' + this.settingsCmp.node.nodeId + '" style="'+displayStyle+'">');
            out.push('<option value="-1" ' + selOpt + '>' + i18n._('All Racks') + '</option>');
            for (i = 0; i < rpc.policies.length; i++) {
                var policyDesc = rpc.policies[i];
                var selOpt = ( policyDesc == rpc.currentPolicy ) ? "selected" : "";
                out.push('<option value="' + policyDesc.id + '" ' + selOpt + '>' + policyDesc.name + '</option>');
            }
            out.push('</select>');
            Ext.getCmp('rackSelector_' + this.getId()).setText(out.join(""));
        }
    },
    // get selected query value
    getSelectedQuery : function() {
        var selObj = document.getElementById('selectQuery_' + this.getId() + '_' + this.settingsCmp.node.nodeId);
        var result = null;
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    // get selected query name
    getSelectedQueryName : function() {
        var selObj = document.getElementById('selectQuery_' + this.getId() + '_' + this.settingsCmp.node.nodeId);
        var result = "";
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].label;
        }
        return result;
    },
    // get selected policy
    getSelectedPolicy : function() {
        var selObj = document.getElementById('selectPolicy_' + this.getId() + '_' + this.settingsCmp.node.nodeId);
        var result = "";
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    // return the list of columns in the event long as a comma separated list
    getColumnList : function() {
        var columnList = "";
        for (var i=0; i<this.fields.length ; i++) {
            if (i != 0)
                columnList += ",";
            if (this.fields[i].mapping != null)
                columnList += this.fields[i].mapping;
            else if (this.fields[i].name != null)
                columnList += this.fields[i].name;
        
        }
        return columnList;
    },
    makeSelectable : function() {
        var elems=Ext.DomQuery.select("div[unselectable=on]", this.dom);
        for(var i=0, len=elems.length; i<len; i++){
            elems[i].unselectable = "off";
        }
    },
    refreshHandler: function (forceFlush) {
        if (!this.isReportsAppInstalled()) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("Event Logs require the Reports application. Please install and enable the Reports application."));
        } else {
            if (!forceFlush) {
            	this.setLoading(i18n._('Refreshing Events...'));
                this.refreshList();
            } else {
            	this.setLoading(i18n._('Syncing events to Database... '));
                this.getUntangleNodeReporting().flushEvents(Ext.bind(function(result, exception) {
                	this.setLoading(i18n._('Refreshing Events...'));
                    this.refreshList();
                },this));
                this.updateFunction = Ext.bind(function(){
                    var statusStr = this.getUntangleNodeReporting().getCurrentStatus();
                    //if the loadMask is no longer shown, stop this task
                    //if the loadMask has moved on to a different phase, stop this task
                    if(this.loadMask ==null || this.loadMask.disabled || this.loadMask.msg.indexOf("Syncing") == -1) { 
                        return;
                    }
                    
                	this.setLoading(i18n._('Syncing events to Database... ') + statusStr);

                    window.setTimeout(this.updateFunction, 1000);
                },this);

                window.setTimeout(this.updateFunction, 2000);
            }
        }
    },
    // Refresh the events list
    refreshCallback : function(result, exception) {
        if (exception != null) {
           Ung.Util.handleException(exception);
        } else {
            var events = result;
            //Add sample events for test
            for(var i=0; i<250; i++) {
            	events.list.push({ id:i+1, timeStamp:{javaClass:"java.util.Date", time: (new Date(i*10000000)).getTime()}, client:"1.1.1."+i, uid:"4"+i,swCookie:i,server:"" });
            }
            if (this.settingsCmp !== null) {
                this.getStore().proxy.data = events;
                this.getStore().loadPage(1);
			}
		}
        this.setLoading(false);
        this.makeSelectable();
    },
    flushHandler: function (forceFlush) {
        if (!this.isReportsAppInstalled()) {
            Ext.MessageBox.alert(i18n._('Warning'), i18n._("Event Logs require the Reports application. Please install and enable the Reports application."));
        } else {
        	this.setLoading(i18n._('Syncing events to Database... '));
            this.getUntangleNodeReporting().flushEvents(Ext.bind(function(result, exception) {
                // refresh after complete
                this.refreshHandler();
            }, this));

            this.updateFunction = Ext.bind( function(){
                var statusStr = this.getUntangleNodeReporting().getCurrentStatus();
                //if the loadMask is no longer shown, stop this task
                //if the loadMask has moved on to a different phase, stop this task
                if(this.loadMask ==null || this.loadMask.disabled || this.loadMask.msg.indexOf("Syncing") == -1) { 
                    return;
                }

                this.setLoading(i18n._('Syncing events to Database... ') + statusStr);
                window.setTimeout(this.updateFunction, 1000);

            }, this);

            window.setTimeout(this.updateFunction, 2000);
        }
    },
    refreshList : function() {
        var selQuery = this.getSelectedQuery();
        var selPolicy = this.getSelectedPolicy();
        if (selQuery != null && selPolicy != null) {
            rpc.jsonrpc.UvmContext.getEvents(Ext.bind(this.refreshCallback,this), selQuery, selPolicy, 1000);
        } else {
            this.setLoading(false);
        }
    },
    // is reports node installed
    isReportsAppInstalled : function(forceReload) {
        if (forceReload || this.reportsAppInstalledAndEnabled === undefined) {
            try {
                var reportsNode = this.getUntangleNodeReporting();
                if (this.untangleNodeReporting == null) {
                    this.reportsAppInstalledAndEnabled = false;
                }
                else {
                    if (reportsNode.getRunState() == "RUNNING") 
                        this.reportsAppInstalledAndEnabled = true;
                    else
                        this.reportsAppInstalledAndEnabled = false;
                }
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.reportsAppInstalledAndEnabled;
    },
    // get untangle node reporting
    getUntangleNodeReporting : function(forceReload) {
        if (forceReload || this.untangleNodeReporting === undefined) {
            try {
                this.untangleNodeReporting = rpc.nodeManager.node("untangle-node-reporting");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.untangleNodeReporting;
    },
    
    listeners: {
        "activate": {
            fn: function() {
                if( this.refreshOnActivate ) {
                	Ext.Function.defer(this.refreshHandler,1, this, [false]);
                }
            }
        },
        "deactivate" : {
            fn: function() {
                if(this.autoRefreshEnabled) {
                    this.stopAutoRefresh(true);
                }
            }
        }
    }
});

// Standard Ung window
Ext.define('Ung.Window', {
	extend: 'Ext.window.Window',
    modal : true,
    // window title
    title : null,
    // breadcrumbs
    breadcrumbs : null,
    // function called by close action
    closeAction : 'cancelAction',
    draggable : false,
    resizable : false,
    // sub componetns - used by destroy function
    subCmps : null,
    // size to rack right side on show
    sizeToRack : true,
    layout : 'anchor',
    defaults: {
        anchor: '100% 100%',
        autoScroll: true,
        autoWidth : true
    },

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
        Ext.QuickTips.init();      
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
        //objSize.width = Math.min(viewportWidth,Math.max(1024,viewportWidth - main.contentLeftWidth));
        objSize.width = viewportWidth - main.contentLeftWidth;
        this.setPosition(viewportWidth-objSize.width, 0);
        this.setSize(objSize);
    },

    // to override if needed
    isDirty : function() {
        return false;
    },
    cancelAction : function() {
        if (this.isDirty()) {
            Ext.MessageBox.confirm(i18n._('Warning'), i18n._('There are unsaved settings which will be lost. Do you want to continue?'),
            		Ext.bind(function(btn) {
                    if (btn == 'yes') {
                        this.closeWindow();
                    }
                },this));
        } else {
            this.closeWindow();
        }
    },
    // the close window action
    // to override
    closeWindow : function() {
        this.hide();
        Ext.destroy(this);
    }
});

Ung.Window.cancelAction = function(dirty, closeWinFn) {
    if (dirty) {
        Ext.MessageBox.confirm(i18n._('Warning'), i18n._('There are unsaved settings which will be lost. Do you want to continue?'),
            function(btn) {
                if (btn == 'yes') {
                    closeWinFn();
                }
            });
    } else {
        closeWinFn();
    }
};

Ext.define("Ung.SettingsWin", {
	extend: "Ung.Window",
    // config i18n
    i18n : null,
    // holds the json rpc results for the settings classes
    rpc : null,
    // tabs (if the window has tabs layout)
    tabs : null,
    // holds the json rpc results for the settings class like baseSettings
    // object, repository, repositoryDesc
    rpc : null,
    layout : 'anchor',
    anchor: '100% 100%',
    // build Tab panel from an array of tab items
    constructor : function(config) {
        config.rpc = {};
        Ung.SettingsWin.superclass.constructor.apply(this, arguments);
    },
    buildTabPanel : function(itemsArray) {
        this.tabs = Ext.create('Ext.tab.Panel',{
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
        this.tabs.on('render', function() {
            this.addNamesToPanels();
        }, this.tabs);
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
    closeWindow : function() {
        this.hide();
        Ext.destroy(this);
    },
    // to override
    saveAction : function() {
        Ung.Util.todo();
    },
    // save function
    applyAction : function() {
        if (this.validate()) {
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
            // If it uses the old getBaseSettings/setBaseSettings
            if (typeof this.getRpcNode().setBaseSettings == 'function') {
                this.getRpcNode().setBaseSettings(Ext.bind(function(result, exception) {
                    this.initialBaseSettings = Ung.Util.clone(this.getBaseSettings());            
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                },this), this.getBaseSettings());
            // If it uses the old getSettings/setSettings
            } else {
                this.getRpcNode().setSettings(Ext.bind(function(result, exception) {
                    this.initialSettings = Ung.Util.clone(this.getSettings());            
                    Ext.MessageBox.hide();
                    if(Ung.Util.handleException(exception)) return;
                },this), this.getSettings());
            }
        }
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
// Node Settings Window
Ext.define("Ung.NodeWin", {
	extend: "Ung.SettingsWin",
    node : null,
    constructor : function(config) {
        var nodeName=config.node.name;
        this.id = "nodeWin_" + nodeName + "_" + rpc.currentPolicy.id;
        // initializes the node i18n instance
        config.i18n = Ung.i18nModuleInstances[nodeName];
        Ung.NodeWin.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() {
        if (this.helpSource == null) {
            this.helpSource = this.node.helpSource;
        };
        this.breadcrumbs = [{
            title : i18n._(rpc.currentPolicy.name),
            action : Ext.bind(function() {
                this.cancelAction(); // TODO check if we need more checking
            },this)
        }, {
            title : this.node.displayName
        }];
        if(this.bbar==null) {
            this.bbar=["-",{
                name : "Remove",
                id : this.getId() + "_removeBtn",
                iconCls : 'node-remove-icon',
                text : i18n._('Remove'),
                handler : Ext.bind(function() {
                    this.removeAction();
                },this)
            },"-",{
                name : 'Help',
                id : this.getId() + "_helpBtn",
                iconCls : 'icon-help',
                text : i18n._('Help'),
                handler : Ext.bind(function() {
                    this.helpAction();
                },this)
            },'->',{
                name : "Save",
                id : this.getId() + "_saveBtn",
                iconCls : 'save-icon',
                text : i18n._('OK'),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.saveAction,1, this,[false]);
                },this)
            },"-",{
                name : "Cancel",
                id : this.getId() + "_cancelBtn",
                iconCls : 'cancel-icon',
                text : i18n._('Cancel'),
                handler : Ext.bind(function() {
                    this.cancelAction();
                },this)
            },"-",{
                name : "Apply",
                id : this.getId() + "_applyBtn",
                iconCls : 'apply-icon',
                text : i18n._('Apply'),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.applyAction,1, this);
                },this)
            },"-"];
        }
        Ung.NodeWin.superclass.initComponent.call(this);
    },
    removeAction : function() {
        this.node.removeAction();
    },
    closeWindow : function() {
        this.hide();
        if(this.node.fnCallback) {
            this.node.fnCallback.call();
        }
        Ext.destroy(this);
    },
    // get nodeContext.rpcNode object
    getRpcNode : function() {
        return this.node.nodeContext.rpcNode;
    },
    // get base settings object
    getBaseSettings : function(forceReload) {
        if (forceReload || this.rpc.baseSettings === undefined) {
            try {
                if (typeof this.getRpcNode().getBaseSettings == 'function') {
                    this.rpc.baseSettings = this.getRpcNode().getBaseSettings();
                }
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.baseSettings;
    },
    // get node settings object
    getSettings : function(forceReload) {
        if (forceReload || this.rpc.settings === undefined) {
            try {
                this.rpc.settings = this.getRpcNode().getSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.settings;
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
    }
});
Ung.NodeWin._nodeScripts = {};

// Dynamically loads javascript file for a node
Ung.NodeWin.loadNodeScript = function(settingsCmp, handler) {
    var scriptFile = Ung.Util.getScriptSrc('settingsNew.js');
    Ung.Util.loadScript('script/' + settingsCmp.name + '/' + scriptFile, Ext.bind(function() {
        this.settingsClassName = Ung.NodeWin.getClassName(this.name);
        if(!Ung.NodeWin.dependency[this.name]) {
            handler.call(this);
        } else {
            var dependencyClassName=Ung.NodeWin.getClassName(Ung.NodeWin.dependency[this.name].name);
            if(!dependencyClassName) {
                Ung.Util.loadScript('script/' + Ung.NodeWin.dependency[this.name].name + '/' + scriptFile, Ext.bind(function() {
                    Ung.NodeWin.dependency[this.name].fn.call(this);
                    handler.call(this);
                },this));
            } else {
                Ung.NodeWin.dependency[this.name].fn.call(this);
                handler.call(this);
            }
        }
    },settingsCmp));
};

Ung.NodeWin.classNames = {};
Ung.NodeWin.dependency = {};
// Static function get the settings class name for a node
Ung.NodeWin.getClassName = function(name) {
    var className = Ung.NodeWin.classNames[name];
    return className === undefined ? null : className;
};
// Static function to register a settings class name for a node
Ung.NodeWin.registerClassName = function(name, className) {
    Ung.NodeWin.classNames[name] = className;
};

// Config Window
Ext.define("Ung.ConfigWin", {
	extend: "Ung.SettingsWin",
    // class constructor
    constructor : function(config) {
        this.id = "configWin_" + config.name;
        // for config elements we have the untangle-libuvm translation map
        this.i18n = i18n;
        Ung.ConfigWin.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() {
        if (!this.name) {
            this.name = "configWin_" + this.name;
        }
        if(this.bbar==null) {
            this.bbar=['-',{
                name : 'Help',
                id : this.getId() + "_helpBtn",
                iconCls : 'icon-help',
                text : i18n._("Help"),
                handler : Ext.bind(function() {
                    this.helpAction();
                },this)
            },'->',{
                name : 'Save',
                id : this.getId() + "_saveBtn",
                iconCls : 'save-icon',
                text : i18n._("OK"),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.saveAction,1, this,[false]);
                },this)
            },"-",{
                name : 'Cancel',
                id : this.getId() + "_cancelBtn",
                iconCls : 'cancel-icon',
                text : i18n._("Cancel"),
                handler : Ext.bind(function() {
                    this.cancelAction();
                },this)
            },"-",{
                name : "Apply",
                id : this.getId() + "_applyBtn",
                iconCls : 'apply-icon',
                text : i18n._("Apply"),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.applyAction,1, this,[true]);
                },this)
            },"-"];
        }
        Ung.ConfigWin.superclass.initComponent.call(this);
    }
});
// update window
// has the content and 3 standard buttons: help, cancel, Update
Ext.define('Ung.UpdateWindow', {
	extend: 'Ung.Window',
    initComponent : function() {
        if(this.bbar==null) {
            this.bbar=[
            '->',
            {
                name : "Save",
                id : this.getId() + "_saveBtn",
                iconCls : 'save-icon',
                text : i18n._('Save'),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.saveAction,1, this);
                },this)
            },'-',{
                name : "Cancel",
                id : this.getId() + "_cancelBtn",
                iconCls : 'cancel-icon',
                text : i18n._('Cancel'),
                handler : Ext.bind(function() {
                    this.cancelAction();
                },this)
            },'-',{
                name : "Apply",
                id : this.getId() + "_applyBtn",
                iconCls : 'apply-icon',
                text : i18n._('Apply'),
                handler : Ext.bind(function() {
                	Ext.Function.defer(this.applyAction,1, this, []);
                },this)
            },'-'];
        }
        Ung.UpdateWindow.superclass.initComponent.call(this);
    },
    // the update actions
    // to override
    updateAction : function() {
        Ung.Util.todo();
    },
    saveAction : function(){
        Ung.Util.todo();
    },
    applyAction : function(){
        Ung.Util.todo();
    }
});

// Manage list popup window
Ext.define("Ung.ManageListWindow", {
	extend: "Ung.UpdateWindow",
    // the editor grid
    grid : null,
    initComponent : function() {
        this.items=this.grid;
        Ung.ManageListWindow.superclass.initComponent.call(this);
    },
    closeWindow : function(skipLoad) {
        if(!skipLoad) {
            this.grid.reloadGrid();
        }
        this.hide();
    },
    isDirty : function() {
        return this.grid.isDirty();
    },
    updateAction : function() {
        this.hide();
    },
    saveAction : function(){
        this.applyAction(Ext.bind(this.hide,this));
    }
});

// Row editor window used by editor grid
Ext.define('Ung.RowEditorWindow', {
	extend:'Ung.UpdateWindow',
    // the editor grid
    grid : null,
    // input lines for standard input lines (text, checkbox, textarea, ..)
    inputLines : null,
    // extra validate function for row editor
    validate: null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,
    // the record currently edit
    record : null,
    // initial record data
    initialRecordData : null,
    sizeToRack : false,
    // size to grid on show
    sizeToGrid : false,
    //size to a given component
    sizeToComponent: null,
    addMode: null,       
    initComponent : function() {
        if (!this.height && !this.width && !this.sizeToComponent) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Edit');
        }
        if (this.rowEditorLabelWidth == null) {
            this.rowEditorLabelWidth = 100;
        }
        if(this.bbar == null){
            this.bbar  = [
                '->',
                {
                    name : "Cancel",
                    id : this.getId() + "_cancelBtn",
                    iconCls : 'cancel-icon',
                    text : i18n._('Cancel'),
                    handler : Ext.bind(function() {
                        this.cancelAction();
                    },this)
                },'-',{
                    name : "Done",
                    id : this.getId() + "_doneBtn",
                    iconCls : 'apply-icon',
                    text : i18n._('Done'),
                    handler : Ext.bind(function() {
                    	Ext.defer(this.updateAction,1, this);
                    },this)
            },'-'];         
        }        
        this.items = Ext.create('Ext.panel.Panel',{
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
        if(this.sizeToComponent==null) {
            this.sizeToComponent=this.grid;
        }
        var objPosition = this.sizeToComponent.getPosition();
        if (this.sizeToGrid || this.height==null || this.width==null) {
            var objSize = this.sizeToComponent.getSize();
            this.setSize(objSize);
            if (objPosition[1] + objSize.height > main.viewport.getSize().height) {
                objPosition[1] = Math.max(main.viewport.getSize().height - objSize.height,0);
            }
        }
        this.setPosition(objPosition);
        if(Ext.isIE) {
            var cancelButton=Ext.getCmp("cancel_"+this.getId());
            if(cancelButton) {
                cancelButton.hide();
                cancelButton.show();
            }
        }
    },
    // populate is called whent a record is edited, tot populate the edit window
    // This function should be deprecated for populateTree.
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
    populateTree : function(record,addMode)
    {
        this.addMode=addMode;
        this.record = record;
        this.initialRecordData = Ext.encode(record.data);

        this.populateChild(this, record);
    },
    populateChild : function(component,record)
    {
        if ( component == null ) {
            return;
        }

        if (component.dataIndex != null && component.setValue ) {
            component.suspendEvents();
            component.setValue(record.get(component.dataIndex));
            component.resumeEvents();
        }

        var items = null;
        if (component.inputLines) {
            items = component.inputLines;
        } else {
            items = component.items;
        }

        if ( items ) {
            for (var i = 0; i < items.length; i++) {
                var item = null;
                if ( items.get != null ) {
                    item = items.get(i);
                } else {
                    item = items[i];
                }
                this.populateChild( item, record);
            }
        }
    },
    // check if the form is valid;
    // this is the default functionality which can be overwritten
    isFormValid : function() {
        var validResult = true;
        for (var i = 0; i < this.inputLines.length; i++) {
            var item = null;
            if ( this.inputLines.get != null ) {
                item = this.inputLines.get(i);
            } else {
                item = this.inputLines[i];
            }
            if ( item == null ) {
                continue;
            }

            if ( item.isValid == null ) {
                continue;
            }
            
            if (!item.isValid()) {
            	validResult=false;
            	break;
            }
        }
        if(validResult==true && this.validate!=null) {
        	validResult = this.validate(this.inputLines);
        }
        if(validResult!=true) {
        	var errMsg = i18n._("The form is not valid!");
        	if(validResult!=false) {
        		errMsg = validResult;
        	}
        	Ext.MessageBox.alert(i18n._('Warning'), errMsg);
        }
        return (validResult == true);
    },
    // updateAction is called to update the record after the edit
    updateAction : function() {
        if (!this.isFormValid()) {
            return;
        }
    	
        if (this.record !== null) {
            if (this.inputLines) {
                for (var i = 0; i < this.inputLines.length; i++) {
                    var inputLine = this.inputLines[i];
                    if(inputLine.dataIndex!=null) {
                        // this.record.data[inputLine.dataIndex] = inputLine.getValue();
                        this.record.set(inputLine.dataIndex, inputLine.getValue());
                    }
                }
            }
            if(this.addMode) {
                if (this.grid.addAtTop) {
                    this.grid.getStore().insert(0, [this.record]);
                    if(this.grid.hasReorder) {
                        this.grid.startEditing(0,0);
                        this.grid.stopEditing();
                    }
                } else {
                    this.grid.getStore().add([this.record]);
                    if(this.grid.hasReorder) {
                        var len = this.grid.getStore().data.length;
                        this.grid.startEditing(len-1,0);
                        this.grid.stopEditing();
                    }
                }
                this.grid.updateChangedData(this.record, "added");
            }
        }
        this.hide();
    },

    updateActionTree : function() {
        if (!this.isFormValid()) {
            return;
        }

        if (this.record !== null) {
            this.updateActionChild(this, this.record);

            if(this.addMode) {
                if (this.grid.addAtTop) {
                    this.grid.getStore().insert(0, [this.record]);
                    if(this.grid.hasReorder) {
                        this.grid.startEditing(0,0);
                        this.grid.stopEditing();
                    }
                } else {
                    this.grid.getStore().add([this.record]);
                    if(this.grid.hasReorder) {
                        var len = this.grid.getStore().data.length;
                        this.grid.startEditing(len-1,0);
                        this.grid.stopEditing();
                    }
                }
                this.grid.updateChangedData(this.record, "added");
            }
        }

        this.hide();        
    },
    updateActionChild : function( component, record )
    {
        if ( component == null ) {
            return;
        }
        
        if (component.dataIndex != null && component.setValue ) {
            this.record.set(component.dataIndex, component.getValue());
        }

        var items = null;
        if (component.inputLines) {
            items = component.inputLines;
        } else {
            items = component.items;
        }

        if ( items ) {
            for (var i = 0; i < items.length; i++) {
                var item = null;
                if ( items.get != null ) {
                    item = items.get(i);
                } else {
                    item = items[i];
                }
                this.updateActionChild( item, record);
            }
        }
    },
    isDirty : function() {
        if (this.record !== null) {
            if (this.inputLines) {
                for (var i = 0; i < this.inputLines.length; i++) {
                    var inputLine = this.inputLines[i];
                    if(inputLine.dataIndex!=null) {
                        if (this.record.get(inputLine.dataIndex) != inputLine.getValue()) {
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    },
    closeWindow : function() {
        this.record.data = Ext.decode(this.initialRecordData);
        this.hide();
    }
});

/*
// RpcProxy
// uses json rpc to get the information from the server
Ung.RpcProxy = function(rpcFn, rpcFnArgs, paginated, modelName ) {
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
	this.model = modelName;
};
*/

Ext.define('Ung.RpcProxy',{
	extend:'Ext.data.Store',
	rpcFn:null,
	rpcFnArgs:null,
	model: null,
	paginated: false,
    constructor : function(config) {
        Ung.RpcProxy.superclass.constructor.apply(this, arguments);
    },
    initComponent : function() { },
    // sets the total number of records
    setTotalRecords : function(totalRecords) {
        this.totalRecords = totalRecords;
    },
    load : function(config) {
        var obj = {};
		if ( typeof config.params != 'undefined') {
			obj.params = config.params;
		} else {
			obj.params = null;
		}
		if ( config.reader != null) {
			obj.reader = config.reader;
		} else {
			obj.reader = this.proxy.getReader();
		}
		if ( config.callback != null) {
			obj.callback = config.callback;
		} else {
			obj.callback = config.callback;
		}
        obj.scope = config.scope;
        obj.arg = config.arg;
        obj.totalRecords = this.totalRecords;
		obj.parent = this;
        var sortColumns = [];
        if (typeof config.params != 'undefined' && config.params.sort) {
            var type = scope.fields.get(config.params.sort).type;
            var sortField = config.params.sort;
            if (type == 'string') {
                sortField = "UPPER("+config.params.sort+")";
            }
            sortColumns.push((config.params.dir == "ASC" ? "+" : "-") + sortField);
        }
        if (this.paginated) {
            if (this.rpcFnArgs == null) {
					start = 0;
					if ( typeof config.params != 'undefined') {
						start = config.params.start ? config.params.start:0;
					}
                    end=this.totalRecords != null ? this.totalRecords : Ung.Util.maxRowCount;
					if ( typeof config.params != 'undefined') {
						if ( config.params.limit) {
							end = config.params.limit;
						}
					}
					this.rpcFn(Ext.bind(this.errorHandler,obj), start,end, sortColumns);
            } else {
                var args = [Ext.bind(this.errorHandler,obj)].
                            concat(this.rpcFnArgs).
                                concat([config.params.start ? config.params.start : 0,
                                    config.params.limit ? config.params.limit : this.totalRecords != null ? this.totalRecords : Ung.Util.maxRowCount,
                                    sortColumns]);
				this.rpcFn.apply(this, args);
            }
        } else {
            if (this.rpcFnArgs == null) {
				this.rpcFn(Ext.bind(this.errorHandler,obj));
            } else {
                var args = [Ext.bind(this.errorHandler,obj)].concat(this.rpcFnArgs);
				this.rpcFn.apply(this, args);
            }
        }
    },
    errorHandler : function(result, exception) {
        if(Ung.Util.handleException(exception, Ext.bind(function() {
			if ( this.callback != null) {
				this.callback.call(this.scope, null, this.arg, false);
			}
        },this),"alert")) return;

        var res = null;
        try {
            res = this.reader.readRecords(result);
			if ( res ) {
				this.parent.loadData(res.records);
			}
			if ( this.callback != null) {
				this.callback.call(this.scope, res, this.arg, true);
			}
        } catch (e) {
			if ( this.callback != null) {
				this.callback.call(this.scope, null, this.arg, false);
			}
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
Ext.define('Ung.grid.CheckColumn', {
	extend:'Ext.ux.CheckColumn',
	alias: 'widget.ung.checkcolumn',
	fixed: true,
    constructor : function(config) {
        Ext.apply(this, config);
        if (!this.id) {
            this.id = Ext.id();
        }
        if (!this.width) {
            this.width = 55;
        }
		Ung.grid.CheckColumn.superclass.constructor.call(this,config);
    },
    init : function(grid) {
        this.grid = grid;
    },
});






// Grid edit column
Ext.define('Ung.grid.IconColumn_Old', {
    constructor : function(config) {
        Ext.apply(this, config);
        if (!this.id) {
            this.id = Ext.id();
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
        this.renderer = Ext.bind(this.renderer,this);
    },
    init : function(grid) {
        this.grid = grid;
        this.grid.on('render', function() {
            var view = this.grid.getView();
            //TODO: extjs4 fix
            //view.mainBody.on('mousedown', this.onMouseDown, this);
        }, this);
    },

    onMouseDown : function(e, t) {
        if (t.className && t.className.indexOf(this.iconClass) != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.handle(record, index);
        }
    },

    renderer : function(value, metadata, record) {
        return '<div class="'+this.iconClass+'">&nbsp;</div>';
    }
});

// Grid edit column
Ext.define('Ung.grid.EditColumn', {
	extend:'Ext.grid.column.Action',
	menuDisabled: true,
	fixed: true,
    iconCls: 'icon-edit-row',
    constructor : function(config) {
        if (!config.header) {
            config.header = i18n._("Edit");
        }
        if (!config.width) {
            config.width = 50;
        }
        Ung.grid.EditColumn.superclass.constructor.call(this,config);
    },
	init:function(grid) {
		this.grid = grid;
	},
	handler: function(view, rowIndex, colIndex) {
		var rec = view.getStore().getAt(rowIndex);
		this.grid.editHandler(rec);
	}
});

// Grid edit column
Ext.define('Ung.grid.DeleteColumn', {
	extend:'Ext.grid.column.Action',
	menuDisabled: true,
	fixed: true,
	iconCls: 'icon-delete-row',
    constructor : function(config) {
        if (!config.header) {
            config.header = i18n._("Delete");
        }
        if (!config.width) {
            config.width = 55;
        }
        Ung.grid.DeleteColumn.superclass.constructor.call(this,config);
    },
	init:function(grid) {
		this.grid=grid;
	},
	handler: function(view, rowIndex, colIndex) {
		var rec = view.getStore().getAt(rowIndex);
		this.grid.deleteHandler(rec);
	}
});

// Grid reorder column
Ext.define('Ung.grid.ReorderColumn', {
	extend:'Ext.grid.column.Action',
	iconCls:'icon-drag',
	menuDisabled:true,
	fixed:true,
    constructor : function(config) {
        if (!config.header) {
            config.header = i18n._("Reorder");
        }
        if (!config.width) {
            config.width = 55;
        }
		Ung.grid.ReorderColumn.superclass.constructor.call(this,config);
    },
    init : function(grid) {
        this.grid = grid;
    },
});


	


// Editor Grid class
Ext.define('Ung.EditorGrid', {
	extend:'Ext.grid.Panel',
	selType: 'rowmodel',
	plugins: [
        Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 2
        })
    ],
    // record per page
    recordsPerPage : 25,
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
    // should add add rows at top or bottom
    addAtTop : true,
    // has Import Export buttons
    hasImportExport: null,    
    // has Edit buton on each record
    hasEdit : true,
    configEdit: null,
    // has Delete buton on each record
    hasDelete : true,
    configDelete: null,
    // the default Empty record for a new row
    hasReorder : false,
    configReorder: null,
    // the default Empty record for a new row
    emptyRow : null,
    // input lines used by the row editor
    rowEditorInputLines : null,
    // extra validate function for row editor
    rowEditorValidate: null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,
    // the default sort field
    sortField : null,
    // the default sort order
    sortOrder : null,
    // the columns are sortable by default, if sortable is not specified
    columnsDefaultSortable : null,
    // paginate the grid by default
    paginated: true,
    // javaClass of the record, used in save function to create correct json-rpc
    // object
    recordJavaClass : null,
    // the map of changed data in the grid
    // used by rendering functions and by save
    dataRoot: null,
    importSettingsWindow: null,    
    changedData : null,
    stripeRows : true,
    //clicksToEdit : 1,
    enableColumnHide : false,
    enableColumnMove: false,
    autoGenerateId: false,
    addedId : 0,
    generatedId:1,
    loadMask: null,
    subCmps:null,
	modelName: null,
    constructor : function(config) {
        this.subCmps=[];
        this.changedData = {};
        this.configReorder={};
        this.configEdit={};
        this.configDelete={};
		this.callParent(arguments);
    },
    initComponent : function() {
        if ( this.dataRoot == null) {
            this.dataRoot='list';
        }
        if(this.loadMask===null) {
           this.loadMask={msg: i18n._("Loading ...")} ;
        }
        for (var i = 0; i < this.columns.length; i++) {
        	var col=this.columns[i];
        	col.menuDisabled= true;
        	if(this.columnsDefaultSortable && col.sortable == null) {
        		col.sortable=true;
        	}
        }        
        if (this.hasReorder) {
            this.enableDragDrop = true;
            this.selType= 'rowmodel';
            this.dropConfig= {
                appendOnly:true
            };

            var reorderColumn = Ext.create('Ung.grid.ReorderColumn',this.configReorder);
            if (!this.plugins) {
                this.plugins = [];
            }
            this.plugins.push(reorderColumn);
            this.columns.push(reorderColumn);
        }
        if (this.hasEdit) {
            if (this.configEdit == null) 
                throw i18n._("Invalid configEdit for Grid with Edit enabled");
            
            var editColumn = Ext.create('Ung.grid.EditColumn',this.configEdit);
            if (!this.plugins) {
                this.plugins = [];
            }
            this.plugins.push(editColumn);
            this.columns.push(editColumn);
        }
        if (this.hasDelete) {
            var deleteColumn = Ext.create('Ung.grid.DeleteColumn',this.configDelete);
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
                convert : Ext.bind(function(val, rec) {
                    return (rec.id !=null)?rec.id:this.generatedId++;
                },this)
            });
        }
		this.modelName='Ung.EditorGrid.Store.ImplicitModel-' + this.id;
		if ( Ext.ModelManager.get(this.modelName) == null) {
			Ext.define(this.modelName, {
				extend: 'Ext.data.Model',
				fields: this.fields
			});
		}
        if (this.proxyRpcFn) {
            this.store = Ext.create('Ung.RpcProxy',{
				rpcFn:this.proxyRpcFn, 
				rpcFnArgs:this.proxyRpcFnArgs,
				paginated:this.paginated,
				model:this.modelName,
				proxy: {
					type: 'pagingmemory',
					reader: {
						type:'json',
						totalProperty : 'totalRecords',
						root : this.dataRoot,
					}
				},
				pageSize : this.recordsPerPage,
                sortInfo : this.sortField ? {
                    field : this.sortField,
                    direction : this.sortOrder ? this.sortOrder : "ASC"
                } : null,
                remoteSort : this.paginated,
                listeners : {
                    "update" : {
                        fn : Ext.bind(function(store, record, operation) {
                            this.updateChangedData(record, "modified");
                        },this)
                    },
                    "load" : {
                        fn : Ext.bind(function(store, records, options) {
                            this.updateFromChangedData(records, options);
                        },this)
                    }
                }
            });
        } else if(this.data) {
    		this.store=Ext.create('Ext.data.Store', {
				data: this.data,
				model:this.modelName,	
				pageSize : this.paginated?this.recordsPerPage:null,
				proxy: {
					type: this.paginated?'pagingmemory':'memory',
					reader: {
						type: 'json',
						root: ''
					}
				},
				autoLoad: false,
				sorters : this.sortField ? {
                    property : this.sortField,
                    direction : this.sortOrder ? this.sortOrder : "ASC"
                } : null,
				remoteSort:this.paginated,
                listeners : {
                    "update" : {
                        fn : Ext.bind(function(store, record, operation) {
                            this.updateChangedData(record, "modified");
                        },this)
                    },
                    "load" : {
                        fn : Ext.bind(function(store, records, options) {
                            this.updateFromChangedData(records, options);
                        },this)
                    }
                }
			});
            this.totalRecords=this.data.length;
        }
        if(this.paginated) {
            this.bbar = Ext.create('Ext.toolbar.Paging',{
                pageSize : this.recordsPerPage,
                store : this.store,
                displayInfo : true,
                displayMsg : i18n._('Displaying topics {0} - {1} of {2}'),
                emptyMsg : i18n._("No topics to display")
            });
        }

        if (this.rowEditor==null && this.rowEditorInputLines != null) {
            this.rowEditor = Ext.create('Ung.RowEditorWindow',{
                grid : this,
                inputLines : this.rowEditorInputLines,
                validate: this.rowEditorValidate,
                rowEditorLabelWidth : this.rowEditorLabelWidth
            });
        }

        if(this.rowEditor!=null) {
            //this.rowEditor.render();
            this.subCmps.push(this.rowEditor);
        }
        if (this.tbar == null) {        
        	this.tbar=[];
        }
        if(this.hasImportExport===null) {
            this.hasImportExport=this.hasAdd;
        }
        if (this.hasAdd) {
            this.tbar.push({
                text : i18n._('Add'),
                tooltip : i18n._('Add New Row'),
                iconCls : 'icon-add-row',
                name : 'Add',
                parentId : this.getId(),
                handler : Ext.bind(this.addHandler,this)
            });
        }
        if (this.hasImportExport) {
        	this.tbar.push('->', {
                text : i18n._('Import'),
                tooltip : i18n._('Import From File'),
                iconCls : 'icon-import',
                name : 'Import',
                parentId : this.getId(),
                handler : Ext.bind(this.importHandler,this)
            }, {
                text : i18n._('Export'),
                tooltip : i18n._('Export To File'),
                iconCls : 'icon-export',
                name : 'export',
                parentId : this.getId(),
                handler : Ext.bind(this.exportHandler,this)
            },'-');    	
        }
        Ung.EditorGrid.superclass.initComponent.call(this);
        /* TODO migration: find an alternate solution
        var columnModel=this.getColumnModel();
        columnModel.getRenderer = function(col){
            if(!this.config[col].renderer){
                return Ung.Util.defaultRenderer;
            }
            return this.config[col].renderer;
        };
        if (this.columnsDefaultSortable !== null) {
            columnModel.defaultSortable = this.columnsDefaultSortable;
        }*/
    },
    stopEditing: function() {
    	//added for compatimbilty
    	//TODO:remove this
    },
    startEditing: function() {
    	//added for compatimbilty
    	//TODO:remove this
    },
    addHandler : function() {
		var record = Ext.create(this.modelName, Ext.decode(Ext.encode(this.emptyRow)));
		record.data.id = this.genAddedId();
        this.stopEditing();
        if (this.rowEditor) {
            this.rowEditor.populate(record, true);
            this.rowEditor.show();
        } else {
            if (this.addAtTop)
                this.getStore().insert(0, [record]);
            else
                this.getStore().add([record]);
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
    importHandler : function() {
        if(this.importSettingsWindow == null) {
            this.importSettingsWindow = Ext.create('Ung.ImportSettingsWindow',{
        	    grid : this
        	});
            this.subCmps.push(this.importSettingsWindow);
        }
        this.stopEditing();
        this.importSettingsWindow.show();
    },
    onImport : function (importMode, importedRows) {
        this.stopEditing();
        this.removePagination(Ext.bind(function() {
        	Ext.Function.defer(this.onImportContinue,1,this,[importMode, importedRows]);
        },this));
    },
    onImportContinue : function (importMode, importedRows) {
        var invalidRecords=0;
        if(importedRows == null) {
            importedRows=[];
        }
        var records=[];
        for (var i = 0; i < importedRows.length; i++) {
            try {
                var record= Ext.create('Ext.data.Record',importedRows[i]);
                if(record.data["javaClass"] == this.recordJavaClass) {
                    record.set("id", this.genAddedId());
                    records.push(record);
                } else {
                    invalidRecords++;
                }
            } catch(e) {
                invalidRecords++;
            }
        }
        var validRecords=records.length;
        if(validRecords > 0) {
            if(importMode=='replace' ) {
                this.deleteAllRecords();
                this.getStore().insert(0, records.reverse());
                this.updateChangedDataOnImport(records, "added");
            } else {
                if(importMode=='append') {
                    this.getStore().add(records);
                } else if(importMode=='prepend'){ //replace or prepend mode
                    this.getStore().insert(0, records.reverse());
                }
                this.updateChangedDataOnImport(records, "added");
            }
        }
        if(validRecords > 0) {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records."), validRecords));
            } else {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records and {1} invalid records."), validRecords, invalidRecords));
            }
        } else {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("Import failed. Imported file has no records."));
            } else {
                Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("Import failed. Imported file contains {0} invalid records and no valid records."), invalidRecords));
            }
        }        
    },
    deleteAllRecords : function () {
        var records=this.getStore().getRange();
        this.updateChangedDataOnImport(records, "deleted");
    },
    exportHandler : function() {
        Ext.MessageBox.wait(i18n._("Exporting Settings..."), i18n._("Please wait"));
    	this.removePagination(Ext.bind(function() {
    	    var gridName=(this.name!=null)?this.name:this.recordJavaClass;
            gridName=gridName.trim().replace(/ /g,"_");
            var exportForm = document.getElementById('exportGridSettings');
            exportForm["gridName"].value=gridName;
            exportForm["gridData"].value="";
            exportForm["gridData"].value=Ext.encode(this.getFullSaveList(true));
            exportForm.submit();
            Ext.MessageBox.hide();
	    }, this ));
    },
    removePagination : function (handler) {
        if(this.isPaginated()) {
            //to remove bottom pagination bar
            this.minPaginateCount = Ung.Util.maxRowCount;
            this.setTotalRecords(this.totalRecords);
    
            //make all cahnged data apear in first page
            for (id in this.changedData) {
                var cd = this.changedData[id];
                cd.pageStart=0;
            }
            //reload grid
            this.loadPage(0, handler, this);
        } else {
            if(handler) {
                handler.call(this);
            }
        }
    },
    getPageStart : function() {
        if (this.store && this.store.lastOptions && this.store.lastOptions.params) {
            return this.store.lastOptions.params.start;
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
        return  this.paginated && (this.totalRecords != null && this.totalRecords >= this.minPaginateCount);
    },
    clearChangedData : function () {
        this.changedData = {};
    },
    reloadGrid : function(options){
        this.clearChangedData();
        if(options && options.data){
            Ung.Util.generateListIds(options.data);                
            this.store.proxy.data = options.data;
            this.setTotalRecords(this.store.proxy.data.length);
        }
        //TODO: find extjs4 solution
        //this.store.reload();           
    },
    beforeDestroy : function() {
        Ext.each(this.subCmps, Ext.destroy);
        Ung.EditorGrid.superclass.beforeDestroy.call(this);
    },
    afterRender : function() {
        Ung.EditorGrid.superclass.afterRender.call(this);
        if(this.hasReorder) {
            var ddrowTarget = Ext.create('Ext.dd.DropTarget',this.container, {
                ddGroup: "GridDD",
                // copy:false,
                notifyDrop : Ext.bind(function(dd, e, data){
                
                    var sm = this.getSelectionModel();
                    var rows = sm.getSelections();
                    var cindex = dd.getDragData(e).rowIndex;    // Here is need

                    var dsGrid = this.getStore();

                    for(i = 0; i < rows.length; i++) {
                        rowData = dsGrid.getById(rows[i].id);
                        dsGrid.remove(dsGrid.getById(rows[i].id));
                        dsGrid.insert(cindex, rowData);
                    }

                    this.getView().refresh();

                    // put the cursor focus on the row of the gridRules which we
                    // just draged
                    this.getSelectionModel().selectRow(cindex);
                },this)
            });
        }
        //TODO: extjs4 migration find an alternative
        //this.getGridEl().down("div[class*=x-grid3-viewport]").set({'name' : "Table"});

        this.getView().getRowClass = function(record, index, rowParams, store) {
            var id = record.get("id");
            if (id == null || id < 0) {
                return "grid-row-added";
            } else {
            	//HACK: this.panel is not standard api 
                var d = this.panel.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        return "grid-row-deleted";
                    } else {
                        return "grid-row-modified";
                    }
                }
            }
            return "";
        };
        
        if ( undefined !== this.header ) {
            var target = this.header.dom;
            var qt = this.tooltip;
        
            if (( undefined !== qt ) && ( undefined !== target )) {
                Ext.QuickTips.register({
                    target: target,
                    title: '',
                    text: qt,
                    enabled: true,
                    showDelay: 20
                });
            }
        }
        Ext.Function.defer(this.initialLoad,1, this);
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
                    if (recIndex >= 0) {
                        var rec = this.store.getAt(recIndex);
                        rec.data = cd.recData;
                        rec.commit();
                    }
                }
            }
        }
    },
    isDirty : function() {
        // Test if there are changed data
        return Ung.Util.hasData(this.changedData);
    },
    disableSorting : function () {
        if (!this.isDirty()) {
        	//TODO: find extjs4 solution
//            var cmConfig = this.getColumnModel().config;
//            for (i in cmConfig) {
//                cmConfig[i].sortable = false;
//            }
        }
    },
    // Update Changed data after an import
    updateChangedDataOnImport : function(records, currentOp) {
        this.disableSorting();
        var recLength=records.length;
        if(currentOp == "added") {
            for (var i = 0; i < recLength; i++) {
                var record=records[i];
                this.changedData[record.get("id")] = {
                    op : currentOp,
                    recData : record.data,
                    pageStart : 0
                };
            }
        } else if (currentOp == "deleted") {
            for(var i=0;i<recLength; i++) {
                this.store.suspendEvents();
                var record=records[i];
                var id = record.get("id");
                var cd = this.changedData[id];
                if (cd == null) {
                    this.changedData[id] = {
                        op : currentOp,
                        recData : record.data,
                        pageStart : 0
                    };
                } else {
                    if ("added" == cd.op) {
                        this.store.remove(record);
                        this.changedData[id] = null;
                        delete this.changedData[id];
                    } else {
                        this.changedData[id] = {
                            op : currentOp,
                            recData : record.data,
                            pageStart : 0
                        };
                    }                    
                }
                this.store.resumeEvents();
            }
            if(records.length > 0) {
                this.getView().refresh(false);
            }
        }
    },
    // Update Changed data after an operation (modifyed, deleted, added)
    updateChangedData : function(record, currentOp) {
        this.disableSorting();
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
                this.getView().refreshNode(index);
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
        /*
         * TODO: find extjs4 solution
        if (this.totalRecords != null) {
            this.getStore().proxy.setTotalRecords(this.totalRecords);
        }
        if(this.paginated) {
            if (this.isPaginated()) {
                this.bbar.show();
                this.bbar.enable();
            } else {
                this.bbar.hide();
                this.bbar.disable();
            }
            if(this.rendered) {
                this.bbar.syncSize();
            }
        }
        */
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
        var cd = this.findFirstChangedDataByFieldValue(field, value);
        if (cd != null) {
            this.loadPage(cd.pageStart, Ext.bind(function(r, options, success) {
                if (success) {
                    this.focusChangedDataField(options.arg, field);
                }
            },this), this, cd);
        }
    },
    editRowChangedDataByFieldValue : function(field, value) {
        var cd = this.findFirstChangedDataByFieldValue(field, value);
        if (cd != null) {
            this.loadPage(cd.pageStart, Ext.bind(function(r, options, success) {
                if (success) {
                     alert("todo");
                }
            },this), this, cd);
        }
    },
    // Get the save list from the changed data
    getSaveList : function() {
        return Ung.Util.getSaveList(this.changedData, this.recordJavaClass);
    },
    // Get the entire list
    // for the unpaginated grids, that send all the records on save
    getFullSaveList : function(forExport) {
        var list=[];
        var records=this.store.getRange();
        for(var i=0; i<records.length;i++) {
            var id = records[i].get("id");
            if (id != null && id >= 0) {
                var d = this.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        continue;
                    }
                }
            }
            if (this.recordJavaClass != null){
                records[i].data["javaClass"] = this.recordJavaClass;
            }
            var recData=Ext.decode(Ext.encode(records[i].data));
            if(recData.id<0 || forExport) {
                delete recData.id;
            }
            list.push(recData);
        }

        return list;
    },
    getGridSaveList : function(handler, skipRepagination) {
        if(this.isPaginated()) {
            var oldSettings=null;
            if(!skipRepagination) {
                oldSettings = {
                    changedData : Ext.decode(Ext.encode(this.changedData)),
                    minPaginateCount: this.minPaginateCount,
                    pageStart : this.getPageStart()
                };
            }
            //to remove bottom pagination bar
            this.minPaginateCount = Ung.Util.maxRowCount;
            if(skipRepagination) {
                this.setTotalRecords(this.totalRecords);
            }
    
            //make all cahnged data apear in first page
            for (id in this.changedData) {
                var cd = this.changedData[id];
                cd.pageStart=0;
            }
            //reload grid
            this.loadPage(0, Ext.bind(function() {
                var result=this.getFullSaveList();
				Ung.Util.generateListIds(result)
                if(!skipRepagination) {
                    this.changedData = oldSettings.changedData;
                    this.minPaginateCount = oldSettings.minPaginateCount;
                    this.loadPage(oldSettings.pageStart, Ext.bind(function() {
                        handler({
                            javaClass : "java.util.LinkedList",
                            list : result
                        });
                    },this), this);
                } else {
                    handler({
                        javaClass : "java.util.LinkedList",
                        list : result
                    });
                };
            },this), this);
        } else {
			var fullSaveList = this.getFullSaveList();
			Ung.Util.generateListIds(fullSaveList);
            handler({
                javaClass : "java.util.LinkedList",
                list :fullSaveList
            });
        }
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
Ext.define('Ung.JsonListReader', {
	extend:'Ext.data.JsonReader',
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
            var fName = (fields && fields.length > 0) ? fields.items[0].name : "name";
            values[fName] = n;
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

Ext.define('Ung.Breadcrumbs', {
	extend:'Ext.Component',
    autoEl : "div",
    // ---Node specific attributes------
    elements : null,
    onRender : function(container, position) {
        Ung.Breadcrumbs.superclass.onRender.call(this, container, position);
        if (this.elements != null) {
            for (var i = 0; i < this.elements.length; i++) {
                if (i > 0) {
                    this.getEl().insertHtml('beforeEnd', '<span class="icon-breadcrumbs-separator">&nbsp;&nbsp;&nbsp;&nbsp;</span>');
                }
                var crumb = this.elements[i];
                if (crumb.action) {
                    var crumbEl = document.createElement("span");;
                    crumbEl.className = 'breadcrumb-link';
                    crumbEl.innerHTML = crumb.title;
                    crumbEl = Ext.get(crumbEl);
                    crumbEl.on("click", crumb.action, this);
                    this.getEl().appendChild(crumbEl);

                } else {
                    this.getEl().insertHtml('beforeEnd', '<span class="breadcrumb-text" >' + crumb.title + '</span>');
                }

            }
        }
    }
});


Ung.grid.ButtonColumn = function(config) {
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
    this.renderer = Ext.bind(this.renderer,this);
};

Ung.grid.ButtonColumn.prototype = {
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
        if (t.className && t.className.indexOf('ung-button') != -1) {
            e.stopEvent();
            var index = this.grid.getView().findRowIndex(t);
            var record = this.grid.store.getAt(index);
            this.handle(record);
        }
    },
    // to override
    handle : function(record) {
    },
    // private
    onMouseOver : function(e,t) {
        if (t.className && t.className.indexOf('ung-button') != -1) {
            t.className="ung-button button-column ung-button-hover";
        }
    },
    // private
    onMouseOut : function(e,t) {
        if (t.className && t.className.indexOf('ung-button') != -1) {
            t.className="ung-button button-column";
        }
    },
    renderer : function(value, metadata, record) {
        return '<div class="ung-button button-column">'+value+'</div>';
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
            this.title = i18n._('Select Users');
        }
        if(this.bbar == null){
            this.bbar =  [
                    '->',
                    {
                        name : "Cancel",
                        id : this.getId() + "_cancelBtn",
                        iconCls : 'cancel-icon',
                        text : i18n._('Cancel'),
                        handler : Ext.bind(function() {
                            this.cancelAction();
                        },this)
                    },'-',{
                        name : "Done",
                        id : this.getId() + "_doneBtn",
                        iconCls : 'apply-icon',
                        text : i18n._('Done'),
                        handler : Ext.bind(function() {
                        	Ext.Function.defer(this.saveAction,1, this);
                        },this)
                    },'-'
            ];         
        }
/*        var selModel = new Ext.grid.CheckboxSelectionModel({
            singleSelect : this.singleSelectUser,
            listeners : {
                rowselect : Ext.bind(this.onSelectRow, this )
            }
        }); */
        this.usersGrid=Ext.create('Ext.grid.Panel',{
           // title: i18n._('Users'),
           height: 210,
           width: 500,
           enableHdMenu : false,
           enableColumnMove: false,
           store: Ext.create('Ext.data.Store',{
                    proxy: {
                        type: 'pagingmemory',
                        reader: {
                            type:'json',
                            totalProperty : 'totalRecords',
                            root : 'list',
                        }
                    },
                     fields : [{
                        name : "lastName"
                    },{
                        name : "sortColumn",
                        type : "string",
                        sortType : Ung.SortTypes.asLastName,
                        convert : function( val,rec) {
                            if ( rec.javaClass == "com.untangle.node.adconnector.GroupEntry" ) {
                                /* This is filth, it is designed so
                                the groups sort after the special
                                matchers (like [all]), but before all
                                of the users. */
                                return "___";
                            } else if ( rec.javaClass == "com.untangle.node.adconnector.UserEntry" ) {
                                return "_" + rec.lastName;
                            }

                            return null;
                        }
                    },{
                        name : "firstName",
                        type : "string"
                    },{
                        name: "UID",
                        type : "string",
                        sortType : Ung.SortTypes.asUID
                    },{
                        name : "javaClass"
                    },{
                        /* Common name for a group, this is used as the display name */
                        name : "CN"
                    },{
                        /* sAMAccountName is the unique identifier for a group. */
                        name : "SAMAccountName"
                    },{
                        name: "type",
                        type : "string",
                        mapping: "UID",
                        convert : function(val, rec) {
                            if ( rec.javaClass == "com.untangle.node.adconnector.UserEntry" ) {
                                return i18n._("User");
                            } else if ( rec.javaClass == "com.untangle.node.adconnector.GroupEntry" ) {
                                return i18n._("Group");
                            }
                            return val;
                        }
                    },{
                        name: "name",
                        type : "string",
                        mapping: "UID",
                        convert : function(val, rec) {
                            if ( rec.javaClass == "com.untangle.node.adconnector.UserEntry" ) {
                                return rec.UID;
                            } else if ( rec.javaClass == "com.untangle.node.adconnector.GroupEntry" ) {
                                return rec.SAMAccountName;
                            }
                            return val;
                        }
                    },{
                        name : "displayName",
                        mapping : "javaClass",
                        convert : function(val, rec) {
                            if ( rec.javaClass == "com.untangle.node.adconnector.UserEntry" ) {
                                var displayName = ( rec.firstName == null )  ? "" : rec.firstName;
                                displayName = displayName + " " + 
                                    (( rec.lastName == null )  ? "" : rec.lastName);
                                return displayName;
                            } else if ( rec.javaClass == "com.untangle.node.adconnector.GroupEntry" ) {
                                return rec.CN;
                            } else {
                                if (rec.displayName != null)
                                    return rec.displayName;
                                else
                                	return "";
                            }
                        }
                    }],
                sortInfo : this.sortField ? {
                    field: this.sortField,
                    direction : this.sortOrder ? this.sortOrder : "ASC"
                }: null,
                remoteSort : false,
            }),
            columns: [//selModel,
            {
                header : i18n._( "Type" ),
                width : 100,
                sortable : true,
                dataIndex : "type"
            },{
                header : i18n._("Name"),
                width: 100,
                sortable : true,
                dataIndex: "name"
            },{
                header : i18n._("Full Name"),
                width: 350,
                sortable : true,
                dataIndex: "displayName"
            }],
            //selModel : selModel
        });
        this.items = Ext.create('Ext.form.Panel',{
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
                    border : false,
                    cls: 'description',
                    html: this.singleSelectUser ? i18n._("Select a User.") : i18n._("Choose Active Directory users and/or groups.")
                }, {
                    xtype : 'fieldset',
                    title : this.singleSelectUser ? i18n._('Select an existing user') : i18n._('Select existing user(s) or group(s)'),
                    autoHeight : true,
                    items: [this.usersGrid]
                }, {
                    border : false,
                    cls: 'description',
                    hidden : this.singleSelectUser,
                    html: i18n._("Or manually specify username using a semicolon separated list. (Example: &quot;alice;bob&quot;)") 
                }, {
                    xtype : "fieldset",
                    title : i18n._("Manually specify username(s)"),
                    autoHeight : true,
                    hidden : this.singleSelectUser,
                    items:[{
                        xtype : "textfield",
                        width : 420,
                        fieldLabel : i18n._( "Username(s)" ),
                        name : "otherUsers"
                    }]
                },{
                    xtype : 'fieldset',
                    title : i18n._('Add a new user'),
                    autoHeight : true,
                    hidden : !this.singleSelectUser,
                    buttonAlign : 'left',
                    buttons:[{
                        xtype: "button",
                        name : 'Open Local Directory',
                        text : i18n._("Open Local Directory"),
                        hidden : (!this.loadLocalDirectoryUsers || !this.singleSelectUser),
                        handler : Ext.bind(function() {
                            Ext.MessageBox.wait(i18n._("Loading Config..."), i18n._("Please wait"));
                            Ext.Function.defer(Ung.Util.loadResourceAndExecute,1,this,["Ung.LocalDirectory",Ung.Util.getScriptSrc("script/config/localDirectory.js"), Ext.bind(function() {
                                main.localDirectoryWin=new Ung.LocalDirectory({"name":"localDirectory",fnCallback: Ext.bind(function() {
                                    this.populate(this.record,this.fnCallback);
                                },this)});
                                main.localDirectoryWin.show();
                                Ext.MessageBox.hide();
                            },this)]);
                        },this)
                    }, {
                        xtype: "button",
                        name : 'Open Active Directory',
                        text : i18n._("Open Active Directory"),
                        hidden : (!this.loadActiveDirectoryUsers || !this.singleSelectUser),
                        disabled : !main.isNodeRunning('untangle-node-adconnector'),
                        handler : Ext.bind(function() {
                            var node = main.getNode('untangle-node-adconnector');
                            if (node != null) {
                                var nodeCmp = Ung.Node.getCmp(node.nodeId);
                                if (nodeCmp != null) {
                                    nodeCmp.onSettingsAction(Ext.bind(function() {
                                        this.populate(this.record,this.fnCallback);
                                    },this));
                                }
                            }
                        },this)
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
        this.userEntries = [];
        if ( !this.singleSelectUser ) {
            this.userEntries.push({ firstName : "", lastName : null, UID: "[any]", displayName: "Any User"});
            this.userEntries.push({ firstName : "", lastName : null, UID: "[authenticated]", displayName: "Any Authenticated User"});
            this.userEntries.push({ firstName : "", lastName : null, UID: "[unauthenticated]", displayName: "Any Unauthenticated/Unidentified User"});
        }

        var loadActiveDirectory = this.loadActiveDirectoryUsers && main.isNodeRunning("untangle-node-adconnector");
        if (loadActiveDirectory){
            rpc.nodeManager.node("untangle-node-adconnector").getActiveDirectoryManager().getActiveDirectoryUserEntries(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception, Ext.bind(function() {
                    Ext.MessageBox.alert(i18n._("Warning"), i18n._("There was a problem refreshing Active Directory users.  Please check your Active Directory settings and then try again."), Ext.bind(function(){
                        this.populateCallback();
                    },this));
                },this),"noAlert")) return;
                this.userEntries=this.userEntries.concat(result.list);
                this.populateCallback();
            },this));
        } else {
            this.populateSemaphore--;
        }

        if (loadActiveDirectory && !this.singleSelectUser){
            rpc.nodeManager.node("untangle-node-adconnector").getActiveDirectoryManager().getActiveDirectoryGroupEntries(Ext.bind(function(result, exception) {
                if( Ung.Util.handleException(exception, Ext.bind(function() {
                    Ext.MessageBox.alert(i18n._("Warning"), i18n._("There was a problem refreshing Active Directory groups.  Please check your Active Directory settings and then try again."), Ext.bind(function(){
                        this.populateCallback();
                    },this));
                },this),"noAlert")) return;
                
                this.userEntries=this.userEntries.concat(result.list);
                this.populateCallback();
            },this),false);
        } else {
            this.populateSemaphore--;
        }
    },
    populateCallback : function () {
        this.populateSemaphore--;
        if (this.populateSemaphore == 0) {
            if (this.settingsCmp !== null) {
                var sm=this.usersGrid.getSelectionModel();
                sm.clearSelections();
                var store=this.usersGrid.getStore();
                store.proxy.data = {list:this.userEntries};
                store.load({
                    params : {
                        start : 0
                    }
                });

                var users = this.record.get(this.userDataIndex),user,group,index;

                if(users ==null) {
                    users = "";
                } else {
                    users=users.split(";");
                }

                var freeForm = [];

                for(var i=0;i<users.length;i++) {
                    user = users[i].trim();
                    group = user.replace( "group::", "" );
                    index = -1;

                    if ( user == group ) {
                        index=store.find("UID",user);
                    } else {
                        index=store.find("SAMAccountName",group);
                    }

                    if(index>=0) {
                        sm.selectRow(index,true);
                    } else {
                        freeForm.push( user );
                    }

                    this.find( "name", "otherUsers" )[0].setValue( freeForm.join( " ;" ));
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
    applyAction : function()
    {
        if (this.isFormValid()) {
            if (this.record !== null) {
                var sm=this.usersGrid.getSelectionModel();
                var users = [];
                var selRecs=sm.getSelections();
                var value, record, i;
                
                /* First throw in the free form values */
                record = this.find( "name", "otherUsers" )[0].getValue().split( ";" );
                for ( i = 0 ; i < record.length ; i++ ) {
                    value = record[i].trim();
                    if ( value.length > 0 ) {
                        users.push( value );
                    }
                }
                
                for( i=0;i<selRecs.length;i++) {
                    record = selRecs[i];
                    if ( record.get("javaClass") == "com.untangle.node.adconnector.GroupEntry" ) {
                        value =  "group::" + record.get("SAMAccountName");
                    } else {
                        value = record.get("UID");
                    }

                    if(value=="[any]") {
                        users=[value];
                        break;
                    } else {
                      users.push(value);
                    }
                }

                /* If no users are selected, pick the any user. */
                if ( users.length == 0 ) {
                    users = ["[any]"];
                }
                this.record.set(this.userDataIndex,users.join(";"));

                /* Update the selection model if necessary */
                if ( users.length == 1 && users[0] == "[any]" ) {
                    sm.clearSelections();
                    sm.selectFirstRow();
                }
                
                if(this.fnCallback) {
                    this.fnCallback.call();
                }
            }
            return true;
        } else {
            Ext.MessageBox.alert(i18n._("Warning"), i18n._("Please choose a user id/login or press Cancel!"));
            return false;
        }
    },

    saveAction : function()
    {
        if ( this.applyAction()) {
            this.hide();
        }
    },

    closeWindow : function() {
        this.hide();
    },
    
    onSelectRow : function( selectionModel, rowIndex, record )
    {
        /* Uncheck any if they select another user. */
        if ( !this.singleSelectUser && record.get("UID") != "[any]" ) {
            selectionModel.suspendEvents();
            selectionModel.deselectRow(0);
            selectionModel.resumeEvents();
        }
    }
});
Ung.ImportSettingsReader = function(meta, recordType){
    meta = meta || {};
    Ung.ImportSettingsReader.superclass.constructor.call(this, meta, recordType || meta.fields);
};
Ext.extend(Ung.ImportSettingsReader, Ext.data.DataReader, {
    read : function(response){
    	var jsonString = Ext.util.Format.htmlDecode(response.responseText);
        var json = eval("("+jsonString+")");
        if(!json) {
        	Ext.MessageBox.alert(i18n._("Warning"), i18n._("Import failed."));
        }
        return {
            success : json
        };
    }
});

//Import Settings window
Ung.ImportSettingsWindow = Ext.extend(Ung.UpdateWindow, {
    // the editor grid
    grid : null,
    height:230,
    width: 450,
    sizeToRack : false,
    // size to grid on show
    sizeToGrid : false,
    //importMode
    // 'replace' = 'Replace current settings'
    // 'prepend' = 'Prepend to current settings'
    // 'append' = 'Append to current settings'
    importMode: 'replace',     
    initComponent : function() {
        if (!this.height && !this.width) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Import Settings');
        }
        if(this.bbar == null){
            this.bbar  = [
                '->',
                {
                    name : "Cancel",
                    id : this.getId() + "_cancelBtn",
                    iconCls : 'cancel-icon',
                    text : i18n._('Cancel'),
                    handler : Ext.bind(function() {
                        this.cancelAction();
                    },this)
                },'-',{
                    name : "Done",
                    id : this.getId() + "_doneBtn",
                    iconCls : 'apply-icon',
                    text : i18n._('Done'),
                    handler : Ext.bind(function() {
                        Ext.getCmp('import_settings_form'+this.getId()).getForm().submit({
                            waitMsg : i18n._('Please wait while the settings are uploaded...'),
                            success : Ext.bind(this.importSettingsSuccess, this ),
                            failure : Ext.bind(this.importSettingsFailure, this )
                        });
                    },this)
            },'-'];         
        }
        this.items = Ext.create('Ext.panel.Panel',{
            anchor: "100% 100%",
            layout:"form",
            buttonAlign : 'right',
            border : false,
            bodyStyle : 'padding:10px 10px 0px 10px;',
            autoScroll: true,
            defaults : {
                selectOnFocus : true,
                msgTarget : 'side'
            },
            items : [{
                xtype : 'radio',
                boxLabel : i18n._('Replace current settings'),
                hideLabel : true,
                name : 'importMode',
                checked : (this.importMode=='replace'),
                listeners : {
                    "check" : {
                        fn : Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'replace';
                            }
                        },this)
                    }
                }
            }, {
                xtype : 'radio',
                boxLabel : i18n._('Prepend to current settings'),
                hideLabel : true,
                name : 'importMode',
                checked : (this.importMode=='prepend'),
                listeners : {
                    "check" : {
                        fn : Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'prepend';
                            }
                        },this)
                    }
                }
            }, {
                xtype : 'radio',
                boxLabel : i18n._('Append to current settings'),
                hideLabel : true,
                name : 'importMode',
                checked : (this.importMode=='append'),
                listeners : {
                    "check" : {
                        fn : Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'append';
                            }
                        },this)
                    }
                }
            }, {
                cls: 'description',
                border : false,
                bodyStyle : 'padding:5px 0px 5px 30px;',
                html : "<i>" + i18n._("with settings from")+ "</i>"
            }, {
                fileUpload : true,
                xtype : 'form',
                id : 'import_settings_form'+this.getId(),
                errorReader: new Ung.ImportSettingsReader(),
                url : 'gridSettings',
                border : false,
                items : [{
                    fieldLabel : i18n._('File'),
                    name : 'import_settings_textfield',
                    inputType : 'file',
                    xtype : 'textfield',
                    allowBlank : false
                },{
                    xtype : 'hidden',
                    name : 'type',
                    value : 'import'
                }]
            }]
        });
        Ung.ImportSettingsWindow.superclass.initComponent.call(this);
    },
    show : function() {
        Ung.UpdateWindow.superclass.show.call(this);
        var objPosition = this.grid.getPosition();
        if (this.sizeToGrid) {
            var objSize = this.grid.getSize();
            this.setSize(objSize);
            if (objPosition[1] + objSize.height > main.viewport.getSize().height) {
                objPosition[1] = Math.max(main.viewport.getSize().height - objSize.height,0);
            }
        }
        this.setPosition(objPosition);
        if(Ext.isIE) {
            var cancelButton=Ext.getCmp("cancel_"+this.getId());
            if(cancelButton) {
                cancelButton.hide();
                cancelButton.show();
            }
        }
    },
    importSettingsSuccess : function (form, action) {
        var result = action.result.success;
        Ext.MessageBox.wait(i18n._("Importing Settings..."), i18n._("Please wait"));
        if(!result) {
            Ext.MessageBox.alert(i18n._("Warning"), i18n._("Import failed."));
        } else if(!result.success) {
            Ext.MessageBox.alert(i18n._("Warning"), result.msg);
        } else {
            this.grid.onImport(this.importMode, result.msg);
            this.closeWindow();
        }
    },
    importSettingsFailure : function (form, action) {
        Ext.MessageBox.alert(i18n._("Warning"), action.result.msg);
    },
    isDirty : function() {
        return false;  
    },
    closeWindow : function() {
        this.hide();
    }
});
