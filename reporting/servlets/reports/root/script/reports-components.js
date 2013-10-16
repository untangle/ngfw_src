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
    }

};
