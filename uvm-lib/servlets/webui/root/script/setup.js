Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc = {};

Ung.SetupWizard.welcome = function()
{
    var panel = new Ext.FormPanel({
        defaultType : 'textfield',
        items : [{
            xtype : 'label',
            html : i18n._( 'This wizard will guide you through the inital setup and configuration of your Untangle Server. Click <b>Next</b> to get started.' )
        }]
    });

    return {
        title : i18n._( "Welcome" ),
        contentTitle : i18n._( "Thanks for using Untangle" ),
        panel : panel
    };
}

Ung.Setup =  {
	init: function() {
        rpc = {};
                
        rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        
        rpc.languageManager = rpc.jsonrpc.RemoteUvmContext.languageManager();

        var result = rpc.languageManager.getTranslations( "main" );
        i18n = new Ung.I18N( { "map": result.map })

        this.wizard = new Ung.Wizard({
            height : 400,
            width : 800,
            cards : [Ung.SetupWizard.welcome(),{
                title : "Card 2",
                panel : new Ext.Panel({ defaults : { border : false }, items : [{ html : "two foo" }] } )
            },{
                title : "Card 3",
                panel : new Ext.Panel({ defaults : { border : false }, items : [{ html : "The final countdown" }] } )
            }],
            el : "container"
        });

        this.wizard.render();

        this.wizard.goToPage( 0 );
	}
};	
