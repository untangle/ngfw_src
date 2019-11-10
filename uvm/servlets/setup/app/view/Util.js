Ext.define('Ung.Setup.Util', {
    alternateClassName: 'Util',
    singleton: true,

    setRpcJsonrpc: function(){
        var setupInfo;
        rpc.jsonrpc = new JSONRpcClient('/admin/JSON-RPC');
        try {
            setupInfo = rpc.jsonrpc.UvmContext.getSetupStartupInfo();
        } catch (e) {
            Util.handleException(e);
            // Ung.Util.handleException(e);
        }
        Ext.applyIf(rpc, setupInfo);

    },

    authenticate: function (password, cb) {
        // Ung.app.loading('Authenticating...'.t());
        Ext.Ajax.request({
            url: '/auth/login?url=/admin&realm=Administrator',
            params: {
                username: 'admin',
                password: password
            },
            // If it uses the default type then this will not work
            // because the authentication handler does not like utf8
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            success: function (response) {
                // Ung.app.loading(false);
                if (response.responseText && response.responseText.indexOf('loginPage') != -1) {
                    Ext.MessageBox.alert('Authentication failed'.t(), 'Invalid password.'.t());
                    return;
                }

                Util.setRpcJsonrpc();

                rpc.tolerateKeepAliveExceptions = false;
                rpc.keepAlive = function() {
                    rpc.jsonrpc.UvmContext.getFullVersion(function (result, exception) {
                        if (!rpc.tolerateKeepAliveExceptions) {
                            if (exception) { Util.handleException(exception); return; }
                            // if (Ung.Util.handleException(exception)) { return; }
                        }
                        Ext.defer(rpc.keepAlive, 300000);
                    });
                };
                rpc.keepAlive();
                cb();
            },
            failure: function (response) {
                // Ung.app.loading(false);
                console.log(response);
                Ext.MessageBox.alert('Authenticatication failed'.t(), 'The authentication request has failed.'.t());
            }
        });
    },

    v4NetmaskList: [
        [32, '/32 - 255.255.255.255'],
        [31, '/31 - 255.255.255.254'],
        [30, '/30 - 255.255.255.252'],
        [29, '/29 - 255.255.255.248'],
        [28, '/28 - 255.255.255.240'],
        [27, '/27 - 255.255.255.224'],
        [26, '/26 - 255.255.255.192'],
        [25, '/25 - 255.255.255.128'],
        [24, '/24 - 255.255.255.0'],
        [23, '/23 - 255.255.254.0'],
        [22, '/22 - 255.255.252.0'],
        [21, '/21 - 255.255.248.0'],
        [20, '/20 - 255.255.240.0'],
        [19, '/19 - 255.255.224.0'],
        [18, '/18 - 255.255.192.0'],
        [17, '/17 - 255.255.128.0'],
        [16, '/16 - 255.255.0.0'],
        [15, '/15 - 255.254.0.0'],
        [14, '/14 - 255.252.0.0'],
        [13, '/13 - 255.248.0.0'],
        [12, '/12 - 255.240.0.0'],
        [11, '/11 - 255.224.0.0'],
        [10, '/10 - 255.192.0.0'],
        [9, '/9 - 255.128.0.0'],
        [8, '/8 - 255.0.0.0'],
        [7, '/7 - 254.0.0.0'],
        [6, '/6 - 252.0.0.0'],
        [5, '/5 - 248.0.0.0'],
        [4, '/4 - 240.0.0.0'],
        [3, '/3 - 224.0.0.0'],
        [2, '/2 - 192.0.0.0'],
        [1, '/1 - 128.0.0.0'],
        [0, '/0 - 0.0.0.0']
    ],

    handleException: function (exception) {
        if (Util.ignoreExceptions)
            return;

        var message = null;
        var details = '';

        if ( !exception ) {
            console.error('Null Exception!');
            return;
        } else {
            console.error(exception);
        }

        if ( exception.javaStack )
            exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
        if ( exception.name )
            details += '<b>' + 'Exception name'.t() +':</b> ' + exception.name + '<br/><br/>';
        if ( exception.code )
            details += '<b>' + 'Exception code'.t() +':</b> ' + exception.code + '<br/><br/>';
        if ( exception.message )
            details += '<b>' + 'Exception message'.t() + ':</b> ' + exception.message.replace(/\n/g, '<br/>') + '<br/><br/>';
        if ( exception.javaStack )
            details += '<b>' + 'Exception java stack'.t() +':</b> ' + exception.javaStack.replace(/\n/g, '<br/>') + '<br/><br/>';
        if ( exception.stack )
            details += '<b>' + 'Exception js stack'.t() +':</b> ' + exception.stack.replace(/\n/g, '<br/>') + '<br/><br/>';
        if ( rpc.fullVersionAndRevision != null )
            details += '<b>' + 'Build'.t() +':&nbsp;</b>' + rpc.fullVersionAndRevision + '<br/><br/>';
        details +='<b>' + 'Timestamp'.t() +':&nbsp;</b>' + (new Date()).toString() + '<br/><br/>';
        if ( exception.response )
            details += '<b>' + 'Exception response'.t() +':</b> ' + Ext.util.Format.stripTags(exception.response).replace(/\s+/g,'<br/>') + '<br/><br/>';

        /* handle authorization lost */
        if( exception.response && exception.response.includes('loginPage') ) {
            message  = 'Session timed out.'.t() + '<br/>';
            message += 'Press OK to return to the login page.'.t() + '<br/>';
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        /* handle connection lost */
        if( exception.code==550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
            /* handle connection lost (this happens on windows only for some reason) */
            (exception.name == 'JSONRpcClientException' && exception.fileName != null && exception.fileName.indexOf('jsonrpc') != -1) ||
            /* special text for "method not found" and "Service Temporarily Unavailable" */
            (exception.message && exception.message.indexOf('method not found') != -1) ||
            (exception.message && exception.message.indexOf('Service Unavailable') != -1) ||
            (exception.message && exception.message.indexOf('Service Temporarily Unavailable') != -1) ||
            (exception.message && exception.message.indexOf('This application is not currently available') != -1)) {
            message  = 'The connection to the server has been lost.'.t() + '<br/>';
            message += 'Press OK to return to the login page.'.t() + '<br/>';
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        if (typeof exception === 'string') {
            Util.showWarningMessage(exception, '', Util.goToStartPage);
        } else {
            Util.showWarningMessage(exception.message, details, Util.goToStartPage);
        }
    },

    showWarningMessage: function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: 'Warning'.t(),
            modal:true,
            closable:false,
            layout: 'fit',
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
                xtype: 'panel',
                minWidth: 350,
                autoScroll: true,
                defaults: {
                    border: false
                },
                items: [{
                    xtype: 'fieldset',
                    padding: 10,
                    items: [{
                        xtype: 'label',
                        html: message,
                    }]
                }, {
                    xtype: 'fieldset',
                    hidden: (typeof(interactiveMode) != 'undefined' && interactiveMode == false),
                    items: [{
                        xtype: 'button',
                        name: 'details_button',
                        text: 'Show details'.t(),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                wnd.initialHeight = wnd.getHeight();
                                wnd.initialWidth = wnd.getWidth();
                                detailsComp.show();
                                detailsButton.setText('Hide details'.t());
                                wnd.setSizeToRack();
                            } else {
                                detailsComp.hide();
                                detailsButton.setText('Show details'.t());
                                wnd.restore();
                                wnd.setHeight(wnd.initialHeight);
                                wnd.setWidth(wnd.initialWidth);
                                wnd.center();
                            }
                        },
                        scope : this
                    }]
                }, {
                    xtype: 'fieldset',
                    name: 'details',
                    hidden: true,
                    html: details!=null ? details : ''
                }]
            },
            buttons: [{
                text: 'OK'.t(),
                hidden: (typeof(interactiveMode) != 'undefined' && interactiveMode == false),
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

    goToStartPage: function () {
        Ext.MessageBox.wait('Redirecting to the start page...'.t(), 'Please wait'.t());
        location.reload();
    },
});
