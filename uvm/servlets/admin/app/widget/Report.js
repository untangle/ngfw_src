Ext.define('Ung.widget.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',
    /* requires-start */
    requires: [
        //'Ung.widget.report.ReportController',
        'Ung.widget.ReportModel',
        'Ung.chart.TimeChart',
        'Ung.chart.PieChart',
        'Ung.chart.EventChart'
    ],
    /* requires-end */
    controller: 'widget',
    viewModel: {
        type: 'reportwidget'
    },
    config: {
        widget: null,
        entry: null
    },

    hidden: true,
    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    border: false,
    baseCls: 'widget adding',

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '{title}' +
                '<button class="action-btn"><i class="fa fa-rotate-left" data-action="refresh"></i></button>'
        }
    }]
});
