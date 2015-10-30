var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.Request", {
    singleton: true,
    viewport: null,
    init: function(config) {
        Ext.apply(this, config);
        rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
        
        this.startApplication();
    },
    startApplication: function() {
        document.title = Ext.String.format(i18n._("{0} | Request Quarantine Digest"), this.companyName)
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
                    html: '<img src="/images/BrandingLogo.png?'+(new Date()).getTime()+'" border="0" height="60"/>',
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
                xtype: 'tabpanel',
                region:'center',
                activeTab: 0,
                deferredRender: false,
                border: false,
                plain: true,
                flex: 1,
                html: "TODO"
            }
        ]});
    }
});