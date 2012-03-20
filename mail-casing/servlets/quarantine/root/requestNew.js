Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/ext/resources/images/default/s.gif';

var i18n;
var qr;
Ung.QuarantineRequest = function() {
}

Ung.QuarantineRequest.prototype = {
    init : function()
    {
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

        items = items.concat([{
            xtype : 'fieldset',
            layout: {
                type:'hbox',
                align:'left'
            },
            width:450,
            title : this.singleSelectUser ? i18n._('Select User') : i18n._('Enter the email address for which you would like the Quarantine Digest'),
            autoHeight : true,
            items : [
                {
                    xtype:'textfield',
                    fieldLabel : i18n._( "Email Address" ),
                    name : "email_address",
                    width: '300',
                    flex:1,
                },
                {
                    xtype:'button',
                    text : i18n._( "Request" ),
                    cls:'quarantine-left-indented-2',
                    handler : function() {
                        var field = this.requestForm.query('textfield[name="email_address"]')[0];
                        var email = field.getValue();
                        field.disable();
                        this.rpc.requestDigest( Ext.bind(this.requestEmail,this ), email );
                    },
                    scope : this
                }]
            }]);

        this.requestForm  = Ext.create('Ext.form.Panel',{
            border : false,
            autoScroll: true,
            defaults : {
                selectOnFocus : true,
                msgTarget : 'side'
            },
        //title:'Request Quarantine Digest Email',
            //border: true,
        //frame:true,
            items : items
        });
    },

    completeInit : function()
    {
        this.init();
        this.requestForm.render( "quarantine-request-digest" );
    },

    requestEmail : function( result, exception )
    {
        if ( exception ) {
            var message = exception.message;
            if (message == null || message == "Unknown") {
                message = i18n._("Please Try Again");
            }
            Ext.MessageBox.alert("Failed",message);
            return;
        }

        var field = this.requestForm.query('textfield[name="email_address"]')[0];

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

        field.enable();
    }
};

Ext.onReady( function(){
    qr = new Ung.QuarantineRequest();

    // Initialize the I18n
    Ext.Ajax.request({
        url : 'i18n',
            success : function( response, options ) {
            i18n = new Ung.I18N({ map : Ext.decode( response.responseText )});
            qr.completeInit();
        },
        method : "GET",
        failure : function() {
            Ext.MessageBox.alert("Error", "Unable to load the language pack." );
        },
      params : { module : 'untangle-casing-mail' }
    });
});
