Ext.namespace('Ung');

//The location of the blank pixel image
Ext.BLANK_IMAGE_URL = '/webui/ext/resources/images/default/s.gif';

var i18n;
var qr;

Ung.QuarantineRequest = function() {
}

Ung.QuarantineRequest.prototype =  {
    init : function()
    {
		this.rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;

        this.requestForm  = new Ext.FormPanel({
            defaults : {
                xtype : "textfield"
            },
            border: false,
            bodyStyle : 'background-color: transparent;',
            items : [{
                fieldLabel : i18n._( "Email Address" ),
                name : "email_address",
                width: '350'
            }],
            buttonAlign: 'left', 
            buttons : [{
                text : i18n._( "Request" ),
                handler : function() {
                    var field = this.requestForm.find( "name", "email_address" )[0];
                    var email = field.getValue();
                    field.disable();
                    this.rpc.requestDigest( this.requestEmail.createDelegate( this ), 
                                            email );
                },
                scope : this
            }]
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
            Ext.MessageBox.alert("Failed",exception); 
            return;
        }

        var field = this.requestForm.find( "name", "email_address" )[0];

        if ( result == true ) {
            alert( "Sent a digest to : " + field.getValue());
            field.setValue( "" );
        }
        
        field.enable();
    }
};

Ext.onReady( function(){
    qr = new Ung.QuarantineRequest();

    // Initialize the I18n
    Ext.Ajax.request({
        url : '/webui/i18n',
            success : function( response, options ) {
            i18n = new Ung.I18N({ map : Ext.decode( response.responseText )});
            qr.completeInit();
        },
        method : "GET",
        failure : function() {
            Ext.MessageBox.alert("Error", "Unable to load the language pack." );
        },
      params : { module : 'mail-casing' }
    });
});




