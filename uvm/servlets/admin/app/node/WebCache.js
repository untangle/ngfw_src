Ext.define('Ung.node.WebCache', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-web-cache',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t()
            }
        }
    }]
});
