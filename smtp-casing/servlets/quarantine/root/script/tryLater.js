var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.TryLater", {
    singleton: true,
    viewport: null,
    init: function(config) {
        Ext.apply(this, config);
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
                    html: "Quarantine Service Error"
                }
            ]}, {
                xtype: 'panel',
                region:'center',
                flex: 1,
                items: [{
                    xtype : 'component',
                    html : 'The '+this.companyName+' Server has encountered an error. Please try later. Thanks and sorry.',
                    style: 'font-size:16px;',
                    margin : 10
                }]
            }
        ]});
    }
});