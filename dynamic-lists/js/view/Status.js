Ext.define('Ung.apps.dynamic-lists.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-dynamic-lists-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/dynamic-lists.svg" width="80" height="80"/>' +
                '<h3>Dynamic Lists</h3>' +
                '<p>' + 'Dynamic Lists provides functionality to block IPS from URL Lists.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        },
        {
            xtype: 'appreports'
        }
        //  {
        //     xtype: 'component',
        //     html: '',
        //     flex: 1, 
        //     listeners: {
        //         afterrender: function (view) {
        //             view.setHtml('<iframe src="/vue/DynamicBlockLists" style="width: 100%; height: 100%; border: 1px solid red;"></iframe>');
        //         }
        //     }
        // }
    ]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        layout: 'fit',
        items: [{
            xtype: 'appmetrics',
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
