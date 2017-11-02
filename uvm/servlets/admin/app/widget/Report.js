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

    // layout: {
    //     type: 'vbox',
    //     align: 'stretch'
    // },
    border: false,
    baseCls: 'widget',

    visible: false,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '<h1><a href="#reports/{entry.url}">{entry.localizedTitle}</a></h1><p><a href="#reports/{entry.categorySlug}">{entry.category}</a></p>' +
                '<div class="actions">' +
                    '<a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a>' +
                    '<a class="action-btn"><i class="fa fa-bars fa-lg" data-action="menu"></i></a>' +
                '</div>'
        }
    }, {
        xtype: 'component',
        cls: 'menu',
        itemId: 'menu',
        bind: {
            html: '<p>{entry.description}<br/><br/> <span style="font-size: 12px; color: #CCC;">reloads every {widget.refreshIntervalSec} seconds</span></p>' +
                '<div class="options">' +
                '<i class="fa fa-times fa-lg" data-action="closemenu"></i>' +
                '<a data-action="download" style="display: {entry.type === "TIME_GRAPH" || entry.type === "TIME_GRAPH_DYNAMIC" || entry.type === "PIE_GRAPH" ? "auto" : "none"};"><i class="fa fa-download fa-lg" data-action="download"></i> ' + 'Download (image)'.t() + '</a>' +
                '<a data-action="export" style="display: {entry.type === "EVENT_LIST" ? "auto" : "none"};"><i class="fa fa-download fa-lg" data-action="export"></i> ' + 'Export (CSV)'.t() + '</a>' +
                '<a href="#reports/{entry.url}"><i class="fa fa-external-link-square fa-lg"></i> ' + 'Open in Reports'.t() + '</a>' +
                '<a data-action="settings"><i class="fa fa-cog fa-lg" data-action="settings"></i> ' + 'Settings'.t() + '</a>' +
                '</div>'
        }
    }]
});
