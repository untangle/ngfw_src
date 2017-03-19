Ext.define('Ung.view.reports.GraphReport', {
    extend: 'Ext.container.Container',
    alias: 'widget.graphreport',

    controller: 'graphreport',
    viewModel: true,

    bodyBorder: false,

    items: [{
        xtype: 'component',
        reference: 'graph',
        cls: 'chart'
    }]
});
