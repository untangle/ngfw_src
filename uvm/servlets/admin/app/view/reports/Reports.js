Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports',
    itemId: 'reports',

    layout: 'border',

    /* requires-start */
    requires: [
        'Ung.view.reports.ReportsController',
        'Ung.view.reports.ReportsModel',
        'Ung.model.Category'
    ],
    /* requires-end */
    controller: 'reports',

    // tbar: [{
    //     xtype: 'component',
    //     bind: {
    //         html: '{categoryName} | {categories.selection} | {reportName} | {report}'
    //     }
    // }],

    viewModel: {
        data: {
            categoryName: null, // as set in route
            category: null, // the category object from grid
            reportName: null, // as coming from route
            report: null, // as coming from route
            activeCard: 'allCategories', // category, report
        },

        stores: {
            categoryReports: {
                source: 'reports',
                filters: [{
                    property: 'category',
                    value: '{categoryName}',
                    exactMatch: true
                }]
            }
        }
    },

    items: [{
        xtype: 'grid',
        reference: 'categories',
        region: 'west',
        width: 200,
        split: true,
        border: false,
        collapsible: true,
        animCollapse: false,
        titleCollapse: true,
        title: 'Select Category'.t(),
        layout: 'fit',
        hidden: true,
        bind: {
            selection: '{category}',
            hidden: '{!categories.selection}',
            collapsed: '{reports.selection}'
        },
        store: 'categories', // this is a global store
        columns: [{
            dataIndex: 'icon',
            width: 20,
            renderer: function (value, meta) {
                meta.tdCls = 'app-icon';
                return '<img src="' + value + '"/>';
            }
        }, {
            dataIndex: 'displayName',
            flex: 1
        }],
        listeners: {
            rowclick: function (el, category) { // better than select
                Ung.app.redirectTo('#reports/' + category.get('url'));
                return false;
            }
        }
    }, {
        xtype: 'container',
        region: 'center',
        layout: 'border',
        items: [{
            xtype: 'grid',
            reference: 'reports',
            region: 'west',
            width: 200,
            border: false,
            split: true,
            collapsible: true,
            animCollapse: false,
            titleCollapse: true,
            title: 'Select Report'.t(),
            layout: 'fit',
            hidden: true,
            bind: {
                selection: '{report}',
                store: '{categoryReports}',
                hidden: '{!reports.selection}'
            },
            columns: [{
                dataIndex: 'title',
                width: 25,
                renderer: function (value, meta, record) {
                    // meta.tdCls = 'app-icon';
                    // return Util.iconReportTitle(record);
                    return '<i class="fa ' + record.get('icon') + ' fa-lg"></i>';
                }
            }, {
                dataIndex: 'title',
                flex: 1,
                renderer: function (value, meta, record) {
                    return record.get('readOnly') ? value.t() : value;
                }
            }],
            listeners: {
                rowclick: function (el, report) {
                    Ung.app.redirectTo('#reports/' + report.get('category').replace(/ /g, '-').toLowerCase() + '/' + report.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());
                    return false;
                }
            }
        }, {
            region: 'center',
            border: false,
            layout: 'card',
            bind: {
                activeItem: '{activeCard}',
            },
            defaults: {
                border: false
            },
            items: [{
                xtype: 'dataview',
                itemId: 'allCategories',
                store: 'categories',
                tpl: '<p class="apps-title">' + 'System'.t() + '</p>' +
                    '<tpl for=".">' +
                        '<tpl if="type === \'system\'">' +
                        '<a href="#reports/{url}" class="app-item">' +
                        '<img src="{icon}" width=80 height=80/>' +
                        '<span class="app-name">{displayName}</span>' +
                        '</a>' +
                        '</tpl>' +
                    '</tpl>' +
                    '<p class="apps-title">' + 'Apps'.t() + '</p>' +
                    '<tpl for=".">' +
                        '<tpl if="type === \'app\'">' +
                        '<a href="#reports/{url}" class="app-item">' +
                        '<img src="{icon}" width=80 height=80/>' +
                        '<span class="app-name">{displayName}</span>' +
                        '</a>' +
                        '</tpl>' +
                    '</tpl>',
                itemSelector: 'a'
            }, {
                itemId: 'category',
                scrollable: true,
                items: [{
                    xtype: 'component',
                    cls: 'headline',
                    margin: '50 0',
                    bind: {
                        html: '<img src="{category.icon}" style="width: 80px; height: 80px;"/><br/>{category.displayName}'
                    }
                }, {
                    xtype: 'dataview',
                    bind: '{categoryReports}',

                    cls: 'cat-reports',
                    tpl: '<tpl for=".">' +
                            '<a href="#reports/{url}"><i class="fa {icon} fa-3x"></i><span>{title}</span><br/>{description}</a>' +
                        '</tpl>',
                    itemSelector: 'a'
                }]
            }, {
                xtype: 'reports-entry',
                itemId: 'report',
                // bodyPadding: 10,
                // bind: {
                //     html: '<h3>{selectedReport.localizedTitle}</h3>'
                // }
            }]
        }]
    }]
});
