Ext.BLANK_IMAGE_URL = '/ext4/resources/themes/images/gray/tree/s.gif'; // The location of the blank pixel image
Ung.Util= {
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
            req.open("GET",Ung.Util.addBuildStampToUrl(sScriptSrc),false);
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
    loadModuleTranslations : function(appName, i18n, handler) {
        if(!Ung.i18nModuleInstances[appName]) {
            rpc.languageManager.getTranslations(Ext.bind(function(result, exception, opt, appName, i18n, handler) {
                if (exception) {
                    var message = exception.message;
                    if (message == null || message == "Unknown") {
                        message = i18n._("Please Try Again");
                    }
                    
                    Ext.MessageBox.alert("Failed", message);
                    return;
                };
                var moduleMap=result.map;
                Ung.i18nModuleInstances[appName] = new Ung.ModuleI18N({
                        "map" : i18n.map,
                        "moduleMap" : moduleMap
                });
                handler.call(this);
            },this,[appName, i18n, handler],true), appName);
        } else {
            handler.call(this);
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
                message = i18n._("An error has occurred.");
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
    }
};
