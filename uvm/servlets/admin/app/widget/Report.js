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
            html: '<h1>{entry.localizedTitle}</h1><p>{entry.localizedDescription}</p>' +
                '<div class="actions">' +
                    '<a class="action-btn"><i class="fa fa-cog fa-lg" data-action="settings"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-download fa-lg" data-action="download"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-area-chart fa-lg" data-action="style"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-external-link-square fa-lg" data-action="redirect"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a>' +
                '</div>'
        }
    }]
});
