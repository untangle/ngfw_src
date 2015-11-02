var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.Request", {
    singleton: true,
    viewport: null,
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
                        width: 400
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
        Ext.MessageBox.wait( i18n._( "Requesting Digest" ), i18n._( "Please Wait" ));
        rpc.requestDigest( Ext.bind(function( result, exception ) {
            Ext.MessageBox.hide();
            var message;
            if ( exception ) {
                message = exception.message;
                if (message == null || message == "Unknown") {
                    message = i18n._("Please Try Again");
                }
                Ext.MessageBox.alert("Failed", message);
                return;
            }

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
    }
});