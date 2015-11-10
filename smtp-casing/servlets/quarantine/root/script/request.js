var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.Request", {
    singleton: true,
    viewport: null,
    emailRegEx: /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,63}$/,
    init: function(config) {
        Ext.apply(this, config);
        Ext.Ajax.request({
            url : 'i18n',
            success : Ext.bind(function( response, options ) {
                i18n = Ext.create('Ung.I18N',{ map : Ext.decode( response.responseText )});
                rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
                this.startApplication();
            }, this),
            method : "GET",
            failure : function() {
                Ext.MessageBox.alert("Error", "Unable to load the language pack." );
            },
            params : { module : 'untangle' }
        });
    },
    startApplication: function() {
        document.title = Ext.String.format(i18n._("{0} | Request Quarantine Digest"), this.companyName);
        this.viewport = Ext.create('Ext.container.Viewport',{
            layout:'border',
            items:[{
                region: 'north',
                padding: 5,
                height: 70,
                xtype: 'container',
                layout: { type: 'hbox', align: 'stretch' },
                items: [{
                    xtype: 'container',
                    html: '<img src="/images/BrandingLogo.png" border="0" height="60"/>',
                    width: 100,
                    flex: 0
                }, {
                    xtype: 'component',
                    padding: '27 10 0 10',
                    style: 'text-align:right; font-family: sans-serif; font-weight:bold;font-size:18px;',
                    flex: 1,
                    html: i18n._("Request Quarantine Digest Emails")
                }
            ]}, {
                xtype: 'panel',
                region:'center',
                flex: 1,
                items: [{
                    xtype : 'component',
                    html : i18n._('Enter the email address for which you would like the Quarantine Digest'),
                    border : true,
                    margin : 10
                }, {
                    xtype : 'container',
                    margin : 10,
                    layout: { type:'hbox', align:'left'},
                    items: [{
                        xtype:'textfield',
                        fieldLabel : i18n._( "Email Address" ),
                        name : "email_address",
                        allowBlank: false,
                        blankText: i18n._( "Please enter a valid email address" ),
                        vtype: 'email',
                        validateOnChange: false,
                        validateOnBlur: false,
                        msgTarget: 'under',
                        width: 400,
                        listeners: {
                            specialkey: {
                                fn: function(field, e) {
                                    console.log(e);
                                    if (e.getKey() == e.ENTER) {
                                        this.requestEmail();
                                    }
                                },
                                scope: this
                            }
                        }
                    }, {
                        xtype:'button',
                        text : i18n._( "Request" ),
                        margin: '0 0 0 30',
                        handler : this.requestEmail,
                        scope : this
                    }]
                }]
            }
        ]});
    },
    requestEmail: function() {
        var email = this.viewport.down('textfield[name="email_address"]');
        if(!email.isValid()) {
            Ext.MessageBox.alert(i18n._("Error"), i18n._( "Please enter a valid email address" ));
            return;
        }
        Ext.MessageBox.wait( i18n._("Requesting Digest"), i18n._( "Please Wait" ));
        rpc.requestDigest( Ext.bind(function( result, exception ) {
            if (this.handleException(exception)) {
                return;
            }
            var message;
            if ( result ) {
                message = Ext.String.format( i18n._( "Successfully sent digest to '{0}'" ),  email.getValue());
                email.setValue("");
            }  else {
                message = Ext.String.format( i18n._( "A quarantine does not exist for '{0}'" ), email.getValue());
            }

            Ext.MessageBox.show({
                title : i18n._( "Quarantine Request" ),
                msg : message,
                buttons : Ext.MessageBox.OK,
                icon : Ext.MessageBox.INFO
            });
        }, this), email.getValue());
    },
    handleException : function(exception) {
        if (exception) {
            if (console) {
                console.error("handleException:", exception);
            }
            if (exception.message == null) {
                exception.message = "";
            }
            var message = null;

            // handle connection lost
            if (exception.code == 550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
            // handle connection lost (this happens on windows only for some
            // reason)
            (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
            // special text for "method not found" and "Service Temporarily
            // Unavailable"
            exception.message.indexOf("method not found") != -1 || exception.message.indexOf("Service Unavailable") != -1 || exception.message.indexOf("Service Temporarily Unavailable") != -1 || exception.message.indexOf("This application is not currently available") != -1) {
                message = i18n._("The connection to the server has been lost.") + "<br/>";

            }
            // worst case - just say something
            if (message == null) {
                if (exception && exception.message) {
                    message = i18n._("An error has occurred") + ":" + "<br/>" + exception.message;
                } else {
                    message = i18n._("An error has occurred.");
                }
            }

            var details = "";
            if (exception.javaStack)
                // override poor jsonrpc.js naming
                exception.name = exception.javaStack.split('\n')[0];
            if (exception.name)
                details += "<b>" + i18n._("Exception name") + ":</b> " + exception.name + "<br/><br/>";
            if (exception.code)
                details += "<b>" + i18n._("Exception code") + ":</b> " + exception.code + "<br/><br/>";
            if (exception.message)
                details += "<b>" + i18n._("Exception message") + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
            if (exception.javaStack)
                details += "<b>" + i18n._("Exception java stack") + ":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
            if (exception.stack)
                details += "<b>" + i18n._("Exception js stack") + ":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
            details += "<b>" + i18n._("Timestamp") + ":&nbsp;</b>" + (new Date()).toString() + "<br/>";
            Ext.MessageBox.alert(message, details);
            return true;
        }
        return false;
    }
});


Ext.apply(Ext.form.VTypes, {
    email: function (v) {
        return Ung.Request.emailRegEx.test(v);
    },
    emailText: i18n._("Please enter a valid email address")
});