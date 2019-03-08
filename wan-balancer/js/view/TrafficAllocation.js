Ext.define('Ung.apps.wan-balancer.view.TrafficAllocation', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wan-balancer-trafficallocation',
    itemId: 'traffic-allocation',
    title: 'Traffic Allocation'.t(),
    viewModel: true,
    scrollable: true,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px', fontWeight: 600 },
        html: 'Allocate traffic across WAN interfaces'.t()
    }],

    items: [{
        xtype: 'displayfield',
        padding: '10 10 0 10',
        value: 'Traffic allocation across WAN interfaces is controlled by assigning a relative weight (1-100) to each interface.'.t() + '<BR>' +
               'After entering the weight of each interface the resulting allocation is displayed.'.t() + '<BR>' +
               'If all WAN interfaces have the same bandwidth it is best to assign the same weight to all WAN interfaces.'.t() + '<BR>' +
               'If the WAN interfaces vary in bandwidth, enter numbers that correlate the relative available bandwidth.'.t() + '<BR>' +
               'For example: 15 for a 1.5Mbit/sec T1, 60 for a 6 mbit link, and 100 for a 10mbit link.'.t()
    },{
        xtype: 'app-wan-balancer-weight-grid',
        padding: '10 10 10 10'
    }]

});

Ext.define('Ung.apps.wan-balancer.view.WeightGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wan-balancer-weight-grid',
    itemId: 'weight-grid',
    title: 'Interface Weights'.t(),
    bind: '{interfaceWeightList}',

    emptyText: 'No WAN Interfaces defined'.t(),

    columns: [{
        header: 'Interface'.t(),
        dataIndex: 'name',
        width: Renderer.idWidth,
    }, {
        header: 'Weight'.t(),
        dataIndex: 'weight',
        width: Renderer.sizeWidth,
        editor: {
            xtype: 'numberfield',
            allowDecimals: false,
            minValue: 0,
            maxValue: 100
        }
    }, {
        header: 'Resulting Traffic Allocation'.t(),
        dataIndex: 'description',
        width: Renderer.messageWidth,
        flex: 1
    }]
});
