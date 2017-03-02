Ext.define('Ung.cmp.AppReports', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.appreports',
    title: '<i class="fa fa-pie-chart"></i> ' + 'Reports'.t(),

    viewModel: {
        stores: {
            appReports: {
                source: '{reports}',
                filters: [{
                    property: 'category',
                    value: '{appName}',
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
    bind: {
        collapsed: '{instance.targetState !== "RUNNING"}',
        disabled: '{instance.targetState !== "RUNNING"}'
    },

    items: [{
        xtype: 'dataview',
        bind: '{appReports}',
        cls: 'app-reports',
        tpl: '<tpl for=".">' +
                '<p><i class="fa {icon}"></i> <a href="#reports/{uniqueId}">{localizedTitle}</a></p>' +
            '</tpl>',
        itemSelector: 'p'
    }]

});
