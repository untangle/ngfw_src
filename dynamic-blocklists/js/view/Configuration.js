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
            // type: 'vbox',
            align: 'stretch'
        },
        scrollable: false,  
        // scrollable: 'y',
        items: [
        //     {
        //     xtype: 'component',
        //     cls: 'app-desc',
        //     html: '<img src="/icons/apps/dynamic-lists.svg" width="80" height="80"/>' +
        //         '<h3>Dynamic Lists</h3>' +
        //         '<p>' + 'Dynamic Lists provides functionality to block IPS from URL Lists.'.t() + '</p>'
        // }, 
        // {
        //     xtype: 'applicense',
        //     hidden: true,
        //     bind: {
        //         hidden: '{!license || !license.trial}'
        //     }
        // }, 
        // {
        //     xtype: 'appstate',
        // },
         {
            xtype: 'component',
            html: '',
            flex: 1, // Makes the iframe fill the available space
            listeners: {
                afterrender: function (view) {
                    view.setHtml('<iframe src="/vue/DynamicBlockLists" style="width: 100%; height: 100%; border: 1px solid red;"></iframe>');
                }
            }
        }]
    }, 
        // {
        //     region: 'west',
        //     border: false,
        //     width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        //     split: true,
        //     layout: 'fit',
        //     items: [{
        //         xtype: 'appmetrics',
        //     }],
        //     bbar: [{
        //         xtype: 'appremove',
        //         width: '100%'
        //     }]
        // }
]
});
