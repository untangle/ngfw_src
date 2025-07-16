Ext.define('Ung.config.network.view.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-interfaces',
    itemId: 'interfaces',
    scrollable: true,
    withValidation: false,
    title: 'Interfaces'.t(),
    layout: 'border',

    listeners: {
        afterrender: function () {
            if (Ung.AppIframe) {
                Ung.AppIframe.updateIframe('/console/settings/network/interfaces', false);
                // Ung.AppIframe.updateIframeUrl('/console/settings/network/interfaces');
            }
        }
    },

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Interface configuration'.t() + '</strong> <br/>' +  'Use this page to configure each interface\'s configuration and its mapping to a physical network card.'.t()
    }, {
        xtype: 'tbtext',
        padding: '0 5',
        style: { fontSize: '12px' },
        hidden: true,
        bind: {
            hidden: '{allowAddInterfaces}'
        }, 
        html: '<br/><strong style="color:#FF0000">' + 'Maximum number of interfaces reached.'.t() + '</strong>'
    }],

    // items: [{
    //     region: 'center',
    //     xtype: 'container',
    //     itemId: 'iframeWrapper',
    //     layout: 'fit',
    //     items: [Ung.AppIframe]
    // }]
});
