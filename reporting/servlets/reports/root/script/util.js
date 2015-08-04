Ext.define('Ung.Util', {
    singleton: true,
    // Load css file Dynamically
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
    loadModuleTranslations: function(moduleName, handler) {
        if(!Ung.i18nModuleInstances[moduleName]) {
            rpc.languageManager.getTranslations(Ext.bind(function(result, exception, opt, moduleName, handler) {
                if(Ung.Util.handleException(exception)) return;
                var moduleMap=result.map;
                Ung.i18nModuleInstances[moduleName] = Ext.create('Ung.ModuleI18N',{
                    map: i18n.map,
                    moduleMap: moduleMap
                });
                handler.call(this);
            }, this,[moduleName, handler],true), moduleName);
        } else {
            handler.call(this);
        }
    },
    goToStartPage: function () {
        Ext.MessageBox.wait(i18n._("Redirecting to the start page..."), i18n._("Please wait"));
        window.location.reload(true);
    },
    handleException: function(exception) {
        if(exception) {
            console.error("handleException:", exception);
            if(exception.message == null) {
                exception.message = "";
            }
            var message = null;
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
                Ung.Util.showWarningMessage(message, details, Ung.Util.goToStartPage);
                return true;
            }
            /* worst case - just say something */
            if ( exception && exception.message ) {
                message = i18n._("An error has occurred") + ":" + "<br/>"  + exception.message;
            } else {
                message = i18n._("An error has occurred.");
            }
            var details = "";
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
            details +="<b>" + i18n._("Timestamp") +":&nbsp;</b>" + (new Date()).toString() + "<br/>";
            Ung.Util.showWarningMessage(message, details);
            return true;
        }
        return false;
    },
    showWarningMessage:function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: i18n._('Warning'),
            modal:true,
            closable:false,
            layout: "fit",
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
                    items: [{
                        xtype: "button",
                        name: "details_button",
                        text: i18n._("Show details"),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                if(!wnd.initialHeight) {
                                    wnd.initialHeight = wnd.getHeight();
                                    wnd.initialWidth = wnd.getWidth();
                                }
                                detailsComp.show();
                                detailsButton.setText(i18n._('Hide details'));
                                if(!wnd.expandedHeight) {
                                    wnd.expandedHeight = wnd.getHeight();
                                    wnd.expandedWidth = wnd.getWidth()+20;
                                } else {
                                    wnd.setHeight(wnd.expandedHeight);
                                    wnd.setWidth(wnd.expandedWidth);
                                }
                                wnd.center();
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
    bytesRenderer: function(bytes) {
        var units = [i18n._("bytes"), i18n._("Kbytes"), i18n._("Mbytes"), i18n._("Gbytes")];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + " " + units[units_itr];
    },
    timestampFieldWidth: 135,
    ipFieldWidth: 100,
    portFieldWidth: 70,
    hostnameFieldWidth: 120,
    uriFieldWidth: 200,
    usernameFieldWidth: 120,
    booleanFieldWidth: 60,
    emailFieldWidth: 150
});