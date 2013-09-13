Ext.namespace('Ung');

Ung.Util = {
    showWarningMessage:function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: i18n._('Warning'),
            modal: true,
            closable: false,
            layout: "fit",
            setSizeToRack: function () {
                var objSize = Ext.get("container").getSize();
                var objXY = Ext.get("container").getXY();
                this.setPosition(objXY);
                this.setSize(objSize);
            },
            items: {
                xtype: "panel",
                minWidth: 350,
                defaults: {
                    border: false
                },
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
                            var detailsComp = wnd.query('fieldset[name="details"]')[0];
                            var detailsButton = wnd.query('button[name="details_button"]')[0];
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
                    autoScroll: true,
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
    rpcExHandler: function(exception, message, continueExecution) {
        Ung.Util.handleException(exception, message);
        if(!continueExecution) {
            if(exception) {
                throw exception;
            }
            else {
                throw i18n._("Error making rpc request to server");
            }
        }
    },
    handleException: function(exception, message, handler) {
        if(exception) {
            console.error("handleException:", exception);
            if(exception.message == null) {
                exception.message = "";
            }
            if(message !=null ) {
                message += "<br/><br/>";
            } else {
                message="";
            }
            var gotoStartPage=false;
            /* special text for apt error */
            if (exception.name == "java.lang.Exception" && ( exception.message.indexOf("exited with") != -1 || exception.message.indexOf("timed out") != -1 )) {
                message += i18n._("The server is unable to properly communicate with the app store.") + "<br/>";
                message += i18n._("Check internet connectivity and the network/DNS configuration.") + "<br/>";
            }
            /* special text for rack error */
            if (exception.name == "java.lang.Exception" && (exception.message.indexOf("already exists in Policy") != -1)) {
                message += i18n._("This application already exists in this policy/rack.") + ":<br/>";
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
                message += i18n._("The connection to the server has been lost.") + "<br/>";
            }
            /* worst case - just say something */
            if (message == "") {
                message = i18n._("An error has occurred.");
                message += "<br/>";
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
            } else {
                handler(message, details);
            }
            return true;
        }
        return false;
    }
};