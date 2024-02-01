Ext.define('Ung.cmp.AppReports', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.appreports',
    title: '<i class="fa fa-pie-chart"></i> ' + 'Reports'.t(),

    viewModel: {
        stores: {
            appReports: {
                source: 'reports',
                filters: [{
                    property: 'category',
                    value: '{props.displayName}',
                    exactMatch: true
                }]
            }
        }
    },

    padding: 10,
    margin: '20 0',
    cls: 'app-section',

    collapsed: true,
    disabled: true,
    hidden: true,
    bind: {
        collapsed: '{!state.on}',
        disabled: '{!state.on}',
        hidden: '{!reportsAppStatus.enabled}'
    },

    items: [{
        xtype: 'dataview',
        bind: '{appReports}',
        cls: 'app-reports',
        tpl: '<tpl for=".">' +
                '<a href="#reports?{url}"><i class="fa {icon}"></i> {localizedTitle}</a>' +
            '</tpl>',
        itemSelector: 'a'
    }]

});
