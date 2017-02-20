Ext.define('Ung.view.node.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'nodereports',

    requires: [
        //'Ung.chart.NodeChart'
    ],

    layout: 'border',

    border: false,

    title: 'Reports'.t(),
    //scrollable: true,
    items: [{
        region: 'west',
        xtype: 'grid',
        width: 300,
        border: false,
        bodyBorder: false,
        header: false,
        hideHeaders: true,
        trackMouseOver: false,
        viewConfig: {
            stripeRows: false
        },
        bind: {
            store: '{reports}'
        },
        columns: [{
            dataIndex: 'title',
            flex: 1
        }]
    }, {
        region: 'center',
        html: 'center'
    }]

});
