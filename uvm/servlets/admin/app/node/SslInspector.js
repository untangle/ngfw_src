Ext.define('Ung.node.SslInspector', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-casing-ssl-inspector',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t()
            }
        }
    }]
});
