Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext4/resources/themes/images/gray/tree/s.gif';

var i18n;
var qr;

Ext.define("Ung.QuarantineRequest", {
    init: function() {
        // Initialize the I18n
        Ext.Ajax.request({
            url : 'i18n',
            success : Ext.bind(function( response, options ) {
                i18n = Ext.create('Ung.I18N',{ map : Ext.decode( response.responseText )});
                this.completeInit();
            }, this),
            method : "GET",
            failure : function() {
                Ext.MessageBox.alert("Error", "Unable to load the language pack." );
            },
            params : { module : 'untangle-casing-smtp' }
        });
        
    },
    completeInit : function() {
        this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        /* Dynamically add the items to conditionally add the warning message. */
        var items = [];

        if ( requestMessageCode == 'INVALID_PORTAL_EMAIL' ) {
            items.push({
                xtype:'label',
                text : i18n._( 'Your Remote Access Portal login is not configured with a valid email address.' ),
                region : "north",
                cls : 'message-2',
                ctCls : 'message-container',
                margins:'4 4 4 4'
            });
        }

        items.push({
            xtype : 'fieldset',
            layout: {
                type:'hbox',
                align:'left'
            },
            width:450,
            title : this.singleSelectUser ? i18n._('Select User') : i18n._('Enter the email address for which you would like the Quarantine Digest'),
            autoHeight : true,
            items: [{
                xtype:'textfield',
                fieldLabel : i18n._( "Email Address" ),
                name : "email_address",
                width: '300',
                flex: 1
            }, {
                xtype:'button',
                text : i18n._( "Request" ),
                cls:'quarantine-left-indented-2',
                handler : function() {
                    var email = this.requestForm.down('textfield[name="email_address"]').getValue();
                    Ext.MessageBox.wait( i18n._( "Requesting Digest" ), i18n._( "Please Wait" ));
                    this.rpc.requestDigest( Ext.bind(this.requestEmail,this ), email );
                },
                scope : this
            }]
        });

        this.requestForm  = Ext.create('Ext.form.Panel',{
            renderTo: "quarantine-request-digest",
            border : false,
            autoScroll: true,
            bodyStyle: "padding: 10px 5px 5px 15px;",
            defaults : {
                selectOnFocus : true,
                msgTarget : 'side'
            },
            items : items
        });
    },
    requestEmail : function( result, exception ) {
        Ext.MessageBox.hide();
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            Ext.MessageBox.alert("Failed",message);
            return;
        }

        var field = this.requestForm.down('textfield[name="email_address"]');

        if ( result == true ) {
            message = Ext.String.format( i18n._( "Successfully sent digest to '{0}'" ),  field.getValue());
        }  else {
            message = Ext.String.format( i18n._( "A quarantine does not exist for '{0}'" ), field.getValue());
        }

        Ext.MessageBox.show({
            title : i18n._( "Quarantine Request" ),
            msg : message,
            buttons : Ext.MessageBox.OK,
            icon : Ext.MessageBox.INFO
        });
    }
});

Ext.onReady( function(){
    qr = Ext.create('Ung.QuarantineRequest',{});
    qr.init();
});
