Ext.namespace('Ung');
// The location of the blank pixel image
Ext.BLANK_IMAGE_URL = 'ext/resources/images/default/s.gif';
// the main internationalization object
var i18n=null;
// the main json rpc object
var rpc=null;

Ung.Setup =  {
	init: function() {
        /*
		rpc = {};
		// get JSONRpcClient
		rpc.jsonrpc = new JSONRpcClient("/webui/JSON-RPC");
        this.viewport = new Ext.Viewport({
            layout:'border',
            items:[{
                    region:'center',
                    id: 'center',
                    html: "TODO",                    
                    border: false,
                    cls: 'centerRegion',
                    bodyStyle: 'background-color: transparent;',
                    autoScroll: true
                }
             ]
        });
        */

        this.wizard = new Ung.Wizard({
            height : 400,
            width : 800,
            cards : [{
                title : "Card 1",
                panel : new Ext.Panel({  defaults : { border : false }, items : [{ html : "one foo" }] } )
                    },{
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
