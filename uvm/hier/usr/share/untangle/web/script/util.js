Ext.define('Ung.Util', {
    singleton: true,
    toasts: [],
    isDirty: function (item, depth) {
        if(depth==null) {
            depth=0;
        } else if(depth>30) {
            console.log("Ung.Util.isDirty depth>30");
            return false;
        }
        if(item==null) {
            return false;
        }
        if(Ext.isFunction(item.isDirty)) {
            var isDirty = item.isDirty();
            //if(isDirty) { console.log(item); } //for debugging
            return isDirty;
        }
        if(item.items!=null && item.items.length>0) {
            var hasGet=Ext.isFunction(item.items.get);
            for (var i = 0; i < item.items.length; i++) {
                var subItem = hasGet?item.items.get(i):item.items[i];
                if(Ung.Util.isDirty(subItem, depth+1)) {
                    return true;
                }
            }
        }
        return false;
    },
    clearDirty: function(item, depth) {
        if(depth==null) {
            depth=0;
        } else if(depth>30) {
            console.log("Ung.Util.clearDirty depth>30");
            return;
        }
        if(item==null) {
            return;
        }
        if(Ext.isFunction(item.isDirty)) {
            if(!item.isDirty()) {
                return;
            }
        }
        if(Ext.isFunction(item.clearDirty)) {
            item.clearDirty();
            return;
        }
        if(item.items!=null && item.items.length>0) {
            var hasGet=Ext.isFunction(item.items.get);
            for (var i = 0; i < item.items.length; i++) {
                var subItem = hasGet?item.items.get(i):item.items[i];
                Ung.Util.clearDirty(subItem, depth+1);
            }
        }
    },
    isValid: function (item, depth) {
        if(depth==null) {
            depth=0;
        } else if(depth>30) {
            console.log("Ung.Util.isValid depth>30");
            return true;
        }
        if(item==null) {
            return true;
        }
        if(Ext.isFunction(item.isValid)) {
            return item.isValid();
        }
        if(item.items!=null && item.items.length>0) {
            var hasGet=Ext.isFunction(item.items.get);
            for (var i = 0; i < item.items.length; i++) {
                var subItem = hasGet?item.items.get(i):item.items[i];
                if(!Ung.Util.isValid(subItem, depth+1)) {
                    return false;
                }
            }
        }
        return true;
    },
    goToStartPage: function () {
        Ext.MessageBox.wait(i18n._("Redirecting to the start page..."), i18n._("Please wait"));
        window.location.href="/";
    },
    showWarningMessage:function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: i18n._('Warning'),
            modal:true,
            closable:false,
            layout: "fit",
            setSizeToRack: function () {
                if(Ung.Main && Ung.Main.viewport) {
                    var objSize = Ung.Main.viewport.getSize();
                    objSize.height = objSize.height - 66;
                    this.setPosition(0, 66);
                    this.setSize(objSize);
                } else {
                    this.maximize();
                }
            },
            doSize: function() {
                var detailsComp = this.down('fieldset[name="details"]');
                if(!detailsComp.isHidden()) {
                    this.setSizeToRack();
                } else {
                    this.center();
                }
            },
            items: {
                xtype: "panel",
                minWidth: 350,
                autoScroll: true,
                items: [{
                    xtype: "fieldset",
                    items: [{
                        xtype: "label",
                        html: message
                    }]
                }, {
                    xtype: "fieldset",
                    hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                    items: [{
                        xtype: "button",
                        border: false,
                        name: "details_button",
                        text: i18n._("Show details"),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                wnd.initialHeight = wnd.getHeight();
                                wnd.initialWidth = wnd.getWidth();
                                detailsComp.show();
                                detailsButton.setText(i18n._('Hide details'));
                                wnd.setSizeToRack();
                            } else {
                                detailsComp.hide();
                                detailsButton.setText(i18n._('Show details'));
                                wnd.restore();
                                wnd.setHeight(wnd.initialHeight);
                                wnd.setWidth(wnd.initialWidth);
                                wnd.center();
                            }
                        },
                        scope : this
                    }]
                }, {
                    xtype: "fieldset",
                    name: "details",
                    hidden: true,
                    html: details!=null ? details : ''
                }]
            },
            buttons: [{
                text: i18n._('OK'),
                hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                handler: function() {
                    if ( errorHandler) {
                        errorHandler();
                    } else {
                        wnd.close();
                    }
                }
            }]
        });
        wnd.show();
        if(Ext.MessageBox.rendered) {
            Ext.MessageBox.hide();
        }
    },
    rpcExHandler: function(exception, continueExecution) {
        Ung.Util.handleException(exception);
        if(!continueExecution) {
            if(exception) {
                throw exception;
            }
            else {
                throw i18n._("Error making rpc request to server");
            }
        }
    },
    handleException: function(exception, handler, type, continueExecution) { //type: alertCallback, alert, noAlert
        if(exception) {
            console.error("handleException:", exception);
            if(exception.message == null) {
                exception.message = "";
            }
            var message = null;
            var gotoStartPage=false;
            /* special text for rack error */
            if (exception.name == "java.lang.Exception" && (exception.message.indexOf("already exists in Policy") != -1)) {
                message  = i18n._("This application already exists in this policy/rack.") + ":<br/>";
                message += i18n._("Each application can only be installed once in each policy/rack.") + "<br/>";
            }
            /* handle connection lost */
            if( exception.code==550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
                /* handle connection lost (this happens on windows only for some reason) */
                (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
                /* special text for "method not found" and "Service Temporarily Unavailable" */
                exception.message.indexOf("method not found") != -1 ||
                exception.message.indexOf("Service Unavailable") != -1 ||
                exception.message.indexOf("Service Temporarily Unavailable") != -1 ||
                exception.message.indexOf("This application is not currently available") != -1) {
                message  = i18n._("The connection to the server has been lost.") + "<br/>";
                message += i18n._("Press OK to return to the login page.") + "<br/>";
                if (type !== "noAlert") {
                    handler = Ung.Util.goToStartPage; //override handler
                }
            }
            /* worst case - just say something */
            if (message == null) {
                if ( exception && exception.message ) {
                    message = i18n._("An error has occurred") + ":" + "<br/>"  + exception.message;
                } else {
                    message = i18n._("An error has occurred.");
                }
            }
            var details = "";
            if ( exception ) {
                if ( exception.javaStack )
                    exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
                if ( exception.name )
                    details += "<b>" + i18n._("Exception name") +":</b> " + exception.name + "<br/><br/>";
                if ( exception.code )
                    details += "<b>" + i18n._("Exception code") +":</b> " + exception.code + "<br/><br/>";
                if ( exception.message )
                    details += "<b>" + i18n._("Exception message") + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
                if ( exception.javaStack )
                    details += "<b>" + i18n._("Exception java stack") +":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
                if ( exception.stack )
                    details += "<b>" + i18n._("Exception js stack") +":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
                if ( rpc.fullVersionAndRevision != null )
                    details += "<b>" + i18n._("Build") +":&nbsp;</b>" + rpc.fullVersionAndRevision + "<br/><br/>";
                details +="<b>" + i18n._("Timestamp") +":&nbsp;</b>" + (new Date()).toString() + "<br/>";
            }
            if (handler==null) {
                Ung.Util.showWarningMessage(message, details);
            } else if(type==null || type== "alertCallback") {
                Ung.Util.showWarningMessage(message, details, handler);
            } else if (type== "alert") {
                Ung.Util.showWarningMessage(message, details);
                handler();
            } else if (type== "noAlert") {
                handler(message, details);
            }
            return !continueExecution;
        }
        return false;
    },

    handleExceptionToast: function(exception, handler, type, continueExecution) {
        var toast;
        if (exception) {
            console.error("handleException:", exception);
            if (!exception.message) {
                exception.message = i18n._('An error has occurred');
            }
            toast = Ext.toast({
                html: '<h3><i class="material-icons" style="font-size: 16px;">warning</i><span style="vertical-align: middle;">' + i18n._('Warning') + '</span></h3><span>' + i18n._('Exception') + ': ' + exception.message + '</span>',
                border: false,
                bodyBorder: false,
                cls: 'toast-exception',
                autoCloseDelay: 5000,
                align: 'br',
                bodyCls: 'content',
                width: 300
            });
            if (this.toasts.length >= 3) {
                this.toasts[0].close();
                this.toasts.shift();
            }
            this.toasts.push(toast);
            return !continueExecution;
        }
        return false;
    },

    userActionToast: function(message, handler, type, continueExecution) {
        var toast;
        toast = Ext.toast({
            html: '<i class="material-icons" style="font-size: 16px;">info</i> <span style="vertical-align: middle; color: #CCC;">' + message + '!</span>',
            border: false,
            bodyBorder: false,
            cls: 'toast-exception',
            autoCloseDelay: 3000,
            align: 'br',
            bodyCls: 'content',
            width: 300
        });
        if (this.toasts.length >= 3) {
            this.toasts[0].close();
            this.toasts.shift();
        }
        this.toasts.push(toast);
    },

    addBuildStampToUrl: function(url) {
        var scriptArgs = "s=" + Ung.Main.debugMode ? (new Date()).getTime(): Ung.Main.buildStamp;
        if (url.indexOf("?") >= 0) {
            return url + "&" + scriptArgs;
        } else {
            return url + "?" + scriptArgs;
        }
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
            var req;
            if(window.XMLHttpRequest) {
                req = new XMLHttpRequest();
            } else {
                req = new ActiveXObject("Microsoft.XMLHTTP");
            }
            req.open("GET", Ung.Util.addBuildStampToUrl(sScriptSrc), false);
            req.send(null);
            if( window.execScript) {
                window.execScript(req.responseText);
            } else {
                eval(req.responseText);
            }
        } catch (e) {
            error=e;
            console.log("Failed loading script: ", sScriptSrc, e);
        }
        if(handler) {
            handler.call(this);
        }
        return error;
    },
    todo: function() {
        Ext.MessageBox.alert(i18n._("TODO"),"TODO: implement this.");
    },
    getDayOfWeekList: function() {
        var data = [];
        var datacount = 0;
        data[datacount++] = [1, i18n._("Sunday")];
        data[datacount++] = [2, i18n._("Monday")];
        data[datacount++] = [3, i18n._("Tuesday")];
        data[datacount++] = [4, i18n._("Wednesday")];
        data[datacount++] = [5, i18n._("Thursday")];
        data[datacount++] = [6, i18n._("Friday")];
        data[datacount++] = [7, i18n._("Saturday")];
        return data;
    },
    getV4NetmaskList: function( includeNull ) {
        var data = [];
        if (includeNull) data.push( [null,"\u00a0"] );
        data.push( [32,"/32 - 255.255.255.255"] );
        data.push( [31,"/31 - 255.255.255.254"] );
        data.push( [30,"/30 - 255.255.255.252"] );
        data.push( [29,"/29 - 255.255.255.248"] );
        data.push( [28,"/28 - 255.255.255.240"] );
        data.push( [27,"/27 - 255.255.255.224"] );
        data.push( [26,"/26 - 255.255.255.192"] );
        data.push( [25,"/25 - 255.255.255.128"] );
        data.push( [24,"/24 - 255.255.255.0"] );
        data.push( [23,"/23 - 255.255.254.0"] );
        data.push( [22,"/22 - 255.255.252.0"] );
        data.push( [21,"/21 - 255.255.248.0"] );
        data.push( [20,"/20 - 255.255.240.0"] );
        data.push( [19,"/19 - 255.255.224.0"] );
        data.push( [18,"/18 - 255.255.192.0"] );
        data.push( [17,"/17 - 255.255.128.0"] );
        data.push( [16,"/16 - 255.255.0.0"] );
        data.push( [15,"/15 - 255.254.0.0"] );
        data.push( [14,"/14 - 255.252.0.0"] );
        data.push( [13,"/13 - 255.248.0.0"] );
        data.push( [12,"/12 - 255.240.0.0"] );
        data.push( [11,"/11 - 255.224.0.0"] );
        data.push( [10,"/10 - 255.192.0.0"] );
        data.push( [9,"/9 - 255.128.0.0"] );
        data.push( [8,"/8 - 255.0.0.0"] );
        data.push( [7,"/7 - 254.0.0.0"] );
        data.push( [6,"/6 - 252.0.0.0"] );
        data.push( [5,"/5 - 248.0.0.0"] );
        data.push( [4,"/4 - 240.0.0.0"] );
        data.push( [3,"/3 - 224.0.0.0"] );
        data.push( [2,"/2 - 192.0.0.0"] );
        data.push( [1,"/1 - 128.0.0.0"] );
        data.push( [0,"/0 - 0.0.0.0"] );

        return data;
    },
    getInterfaceListSystemDev: function( wanMatchers, anyMatcher, systemDev ) {
        var data = [];
        var networkSettings = Ung.Main.getNetworkSettings();
        for ( var c = 0 ; c < networkSettings.interfaces.list.length ; c++ ) {
            var intf = networkSettings.interfaces.list[c];
            var name = intf.name;
            var key = systemDev?intf.systemDev:intf.interfaceId;
            data.push( [ key, name ] );
        }
        if (systemDev) {
            data.push( [ "tun0", "OpenVPN" ] );
        } else {
            data.push( [ 250, "OpenVPN" ] ); // 0xfa
            data.push( [ 251, "L2TP" ] ); // 0xfb
            data.push( [ 252, "Xauth" ] ); // 0xfc
            data.push( [ 253, "GRE" ] ); // 0xfd
        }
        if (wanMatchers) {
            data.unshift( ["wan",i18n._("Any WAN")] );
            data.unshift( ["non_wan",i18n._("Any Non-WAN")] );
        }
        if (anyMatcher) {
            data.unshift( ["any",i18n._("Any")] );
        }

        return data;
    },
    getInterfaceList: function( wanMatchers, anyMatcher){
        return Ung.Util.getInterfaceListSystemDev( wanMatchers, anyMatcher, false);
    },
    getInterfaceAddressedList: function() {
        var data = [];
        var networkSettings = Ung.Main.getNetworkSettings();
        for ( var c = 0 ; c < networkSettings.interfaces.list.length ; c++ ) {
            var intf = networkSettings.interfaces.list[c];
            var name = intf.name;
            var key = intf.interfaceId;
            if ( intf.configType == 'ADDRESSED' ) {
                data.push( [ key, name ] );
            }
        }
        return data;
    },
    getInterface: function( networkSettings, interfaceId ) {
        if ( ! networkSettings )
            return null;
        var intfs = networkSettings.interfaces;
        for( var x = 0 ; x < intfs.list.length ; x++) {
            var intf = intfs.list[x];
            if ( intf['interfaceId'] === interfaceId )
                return intf;
        }
        return null;
    },
    getWanList: function() {
        var data = [];
        var networkSettings = Ung.Main.getNetworkSettings();
        for ( var c = 0 ; c < networkSettings.interfaces.list.length ; c++ ) {
            var intf = networkSettings.interfaces.list[c];
            var name = intf.name;
            var key = intf.interfaceId;
            if ( intf.configType == 'ADDRESSED' && intf.isWan) {
                data.push( [ key, name ] );
            }
        }
        return data;
    },
    // Test if there is data in the specified object
    hasData: function(obj) {
        var hasData = false;
        for (var id in obj) {
            hasData = true;
            break;
        }
        return hasData;
    },
    bytesToMBs: function(value) {
        return Math.round(value/10000)/100;
    },
    bytesRenderer: function(bytes, perSecond) {
        var units = (!perSecond) ? [i18n._("bytes"), i18n._("Kbytes"), i18n._("Mbytes"), i18n._("Gbytes")] :
            [i18n._("bytes/s"), i18n._("Kbytes/s"), i18n._("Mbytes/s"), i18n._("Gbytes/s")];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + " " + units[units_itr];
    },
    bytesRendererCompact: function(bytes) {
        var units = ["", "K", "M", "G"];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + " " + units[units_itr];
    },
    resizeWindows: function() {
        Ext.WindowMgr.each(Ung.Util.doSize);
    },
    doSize: function (win) {
        if(win && win.doSize) {
            win.doSize();
        }
    },
    // use this field converter to prevent issues with grid grouping.
    preventEmptyValueConverter: function(value) {
        return (value == null || value === "")?" ":value;
    },
    defaultRenderer: function (value) {
        return (typeof value == 'string') ?
            value.length<1? "&#160;": Ext.util.Format.htmlEncode(value):
        value;
    },
    getQueryStringParam: function(name) {
        name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
        var regexS = "[\\?&]"+name+"=([^&#]*)";
        var regex = new RegExp( regexS );
        var results = regex.exec( window.location.href );
        if( results == null )
            return null;
        else
            return results[1];
    },
    getGenericRuleFields: function(settingsCmp) {
        return [{
            name: 'id'
        }, {
            name: 'name',
            defaultValue:  undefined
        }, {
            name: 'string',
            defaultValue:  undefined
        }, {
            name: 'description',
            defaultValue:  undefined
        }, {
            name: 'category',
            defaultValue:  undefined
        }, {
            name: 'enabled',
            defaultValue: 'true'
        }, {
            name: 'blocked',
            defaultValue:  undefined
        }, {
            name: 'flagged',
            defaultValue:  undefined
        }];
    },
    buildJsonListFromStrings: function(stringsList, propertyName) {
        var jsonList=[];
        if(stringsList != null && stringsList.length>0) {
            for(var i=0; i<stringsList.length; i++) {
                var el={};
                el[propertyName]=stringsList[i];
                jsonList.push(el);
            }
        }
        return jsonList;
    },
    createStoreMap : function(pairArray) {
        var map = {};
        for(var i=0; i<pairArray.length; i++) {
            map[pairArray[i][0]] = pairArray[i][1];
        }
        return map;
    },
    createRecordsMap : function(recList, property) {
        var map = {};
        for(var i=0; i<recList.length; i++) {
            map[recList[i][property]] = recList[i];
        }
        return map;
    },
    keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
    btoa: function (input) {
        if (typeof(btoa) === 'function') {
            return btoa(input);
        }
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;
        input = Ung.Util.utf8Encode(input);
        while (i < input.length) {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);
            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;
            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }
            output = output +
            Ung.Util.keyStr.charAt(enc1) + Ung.Util.keyStr.charAt(enc2) +
            Ung.Util.keyStr.charAt(enc3) + Ung.Util.keyStr.charAt(enc4);
        }
        return output;
    },
    utf8Encode : function (string) {
        string = string.replace(/\r\n/g,"\n");
        var utftext = "";
        for (var n = 0; n < string.length; n++) {
            var c = string.charCodeAt(n);
            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }
        }
        return utftext;
    },
    download: function(content, fileName, mimeType) {
        var a = document.createElement('a');
        mimeType = mimeType || 'application/octet-stream';

        if (navigator.msSaveBlob) { // IE10
            return navigator.msSaveBlob(new Blob([ content ], {
                type : mimeType
            }), fileName);
        } else if ('download' in a) { // html5 A[download]
            a.href = 'data:' + mimeType + ',' + encodeURIComponent(content);
            a.setAttribute('download', fileName);
            document.body.appendChild(a);
            setTimeout(function() {
                a.click();
                document.body.removeChild(a);
            }, 100);
            return true;
        } else { //do iframe dataURL download (old ch+FF):
            var f = document.createElement('iframe');
            document.body.appendChild(f);
            f.src = 'data:' + mimeType + ',' + encodeURIComponent(content);
            setTimeout(function() {
                document.body.removeChild(f);
            }, 400);
            return true;
        }
    },
    hideDangerous: true
});

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
    retry: function( fn, fnScope, params, callback, timeout, count ) {
        var input = {
            "fn": fn,
            "fnScope": fnScope,
            "params": params,
            "callback": callback,
            "timeout": timeout,
            "count": count
        };
        this.callFunction( input );
    },

    completeRetry: function( result, exception, input ) {
        var handler = Ext.bind(this.tryAgain, this, [ exception, input ] );
        var type = "noAlert";

        var count = input.count;

        // Do not retry any more
        if (( count == null ) || ( count < 1 )) {
            handler = null;
            type = null;
        } else {
            input.count--;
        }

        if ( Ung.Util.handleException( exception, handler, type )) {
            return;
        }

        input.callback( result, exception );
    },

    tryAgain: function( exception, input ) {
        if( exception.code == 500 ) {
            // If necessary try calling the function again.
            window.setTimeout( Ext.bind(this.callFunction, this, [ input ] ), input.timeout );
            return;
        }

        var message = exception.message;
        if (message == null || message == "Unknown" || message === "") {
            message = i18n._("Please Try Again");
            if (exception.javaStack != null)
                message += "<br/><br/>" + exception.javaStack;
        }
        Ext.MessageBox.alert(i18n._("Warning"), message);
    },

    callFunction: function( input ) {
        var d = Ext.bind(this.completeRetry, this, [ input ], 2 );
        var fn = input.fn;
        var fnScope = input.fnScope;
        var params = [ d ];

        if ( input.params ) {
                params = params.concat( input.params );
        }

        fn.apply( fnScope, params );
    }

};
