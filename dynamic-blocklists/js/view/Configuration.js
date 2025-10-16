Ext.define('Ung.apps.dynamic-blocklists.view.Configuration', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-dynamic-blocklists-configuration',
    itemId: 'configuration',
    title: 'Configuration'.t(),
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 0,

        layout: {
            type: 'fit',
            align: 'stretch'
        },
        scrollable: false,  
        items: [
         {
            xtype: 'component',
            html: '',
            flex: 1, // Makes the iframe fill the available space
            listeners: {
                afterrender: function (view) {
                    view.setHtml('<iframe src="/console/settings/services/dynamic-blocklist" style="width: 100%; height: 100%; border: none;"></iframe>');
                }
            }
        }]
    }, 
]
});
