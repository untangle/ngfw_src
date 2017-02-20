Ext.define('Ung.node.PhishBlocker', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-phish-blocker',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Phish Blocker detects and blocks phishing emails using signatures.'.t()
            }
        }
    }]
});
