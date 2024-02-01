Ext.define('Ung.widget.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',

    controller: 'widget',
    viewModel: {
        formulas: {
            f_size: function (get) {
                switch(get('widget.size')) {
                case 'SMALL': return 300;
                case 'MEDIUM': return 450;
                case 'LARGE': return 600;
                case 'XLARGE': return 750;
                }
            }
        }
    },
    config: {
        widget: null,
        entry: null
    },

    lastFetchTime: null,


    bind: {
        width: '{f_size}'
    },

    border: false,
    baseCls: 'widget',

    visible: false,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '<h1><a href="#reports?{entry.url}{query.string}">{entry.localizedTitle}</a></h1><p><a href="#reports?cat={entry.categorySlug}{query.string}">{entry.category}</a></p>' +
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
            html: '<p>{entry.description}</p>' +
                '<div class="options">' +
                '<i class="fa fa-times fa-lg" data-action="closemenu"></i>' +
                '<a data-action="download" style="display: {entry.type === "TIME_GRAPH" || entry.type === "TIME_GRAPH_DYNAMIC" || entry.type === "PIE_GRAPH" ? "auto" : "none"};"><i class="fa fa-download fa-lg" data-action="download"></i> ' + 'Download (image)'.t() + '</a>' +
                '<a data-action="export" style="display: {entry.type === "EVENT_LIST" ? "auto" : "none"};"><i class="fa fa-download fa-lg" data-action="export"></i> ' + 'Export (CSV)'.t() + '</a>' +
                '<a data-action="reports" data-url="#reports?{entry.url}{query.string}"><i class="fa fa-external-link-square fa-lg"></i> ' + 'Open in Reports'.t() + '</a>' +
                '<hr/>' +
                '<label style="color: #999; margin: 10px 0; display: block;">' + 'Settings'.t() + '</label>' +
                '<label style="float: left; width: 20%; padding: 5px 0;">' + 'Size'.t() +':</label> <a data-action="size-SMALL" class="size {widget.size === "SMALL" ? "selected" : ""}">S</a>' +
                '<a data-action="size-MEDIUM" class="size {widget.size === "MEDIUM" ? "selected" : ""}">M</a>' +
                '<a data-action="size-LARGE" class="size {widget.size === "LARGE" ? "selected" : ""}">L</a>' +
                '<a data-action="size-XLARGE" class="size {widget.size === "XLARGE" ? "selected" : ""}">XL</a>' +
                '<br/>' +
                '<label>' + 'Auto Refresh'.t() + ':</label> <select>' +
                    '<option value=10 {widget.refreshIntervalSec === 10 ? "selected" : ""}>10 ' + 'seconds'.t() + '</option>' +
                    '<option value=30 {widget.refreshIntervalSec === 30 ? "selected" : ""}>30 ' + 'seconds'.t() + '</option>' +
                    '<option value=60 {widget.refreshIntervalSec === 60 ? "selected" : ""}>1 ' + 'minute'.t() + '</option>' +
                    '<option value=120 {widget.refreshIntervalSec === 120 ? "selected" : ""}>2 ' + 'minutes'.t() + '</option>' +
                    '<option value=300 {widget.refreshIntervalSec === 300 ? "selected" : ""}>5 ' + 'minutes'.t() + '</option>' +
                    '<option value=0 {widget.refreshIntervalSec === 0 ? "selected" : ""}>' + 'never'.t() + '</option>' +
                '</select><br/>' +
                '<button data-action="save">' + 'Save'.t() + '</button>' +
                '</div>'
        }
    }]
});
