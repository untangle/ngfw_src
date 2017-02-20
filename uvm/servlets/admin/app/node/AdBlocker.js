Ext.define('Ung.node.AdBlocker', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-ad-blocker',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t()
            }
        }
    }]
});
