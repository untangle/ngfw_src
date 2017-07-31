Ext.define('Ung.widget.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',

    controller: 'widget',
    viewModel: true,
    config: {
        widget: null,
        entry: null
    },

    lastFetchTime: null,

    hidden: true,
    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    border: false,
    baseCls: 'widget',

    visible: false,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '<h1>{entry.localizedTitle}</h1><p>{entry.category}</p>' +
                '<div class="actions">' +
                    '<a class="action-btn"><i class="fa fa-info-circle fa-lg" data-action="info"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-cog fa-lg" data-action="settings"></i></a>' +
                    '<a class="action-btn" style="display: {entry.type === "TIME_GRAPH" || entry.type === "TIME_GRAPH_DYNAMIC" || entry.type === "PIE_GRAPH" ? "auto" : "none"};"><i class="fa fa-download fa-lg" data-action="download"></i></a>' +
                    // '<a class="action-btn"><i class="fa fa-area-chart fa-lg" data-action="style"></i></a>' +
                    '<a href="#reports/{entry.url}" class="action-btn"><i class="fa fa-external-link-square fa-lg"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a>' +
                '</div>'
        }
    }]
});
