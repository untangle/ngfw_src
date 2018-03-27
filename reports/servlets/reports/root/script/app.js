Ext.define('Ung.view.Main', {
    extend: 'Ung.view.reports.Main', // defined in module
    // layout: 'border',
    viewModel: {
        data: { servlet: 'REPORTS' }
    },
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        ui: 'navigation',
        border: false,
        style: {
            background: '#1b1e26'
        },
        items: [{
            xtype: 'button',
            padding: '6 7 2 7',
            border: false,
            html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>',
            hrefTarget: '_self',
            href: '#'
        }]
    }]
});

Ext.define('Ung.controller.Global', {
    extend: 'Ext.app.Controller',

    stores: [
        'Categories',
        'Reports',
        'ReportsTree'
    ],

    refs: {
        reportsView: '#reports',
    },
    config: {
        routes: {
            '': 'onMain',
            ':params': {
                action: 'onMain',
                conditions: {
                    ':params' : '(.*)'
                }
            }
        }
    },

    onMain: function (query) {
        var reportsVm = this.getReportsView().getViewModel(),
            route = {}, conditions = [],
            condsQuery = '', decoded, parts, key, sep, val, fmt;

        if (query) {
            Ext.Array.each(query.replace('?', '').split('&'), function (part) {
                decoded = decodeURIComponent(part);

                if (decoded.indexOf(':') >= 0) {
                    parts = decoded.split(':');
                    key = parts[0];
                    sep = parts[1];
                    val = parts[2];
                    fmt = parseInt(parts[3], 10);
                } else {
                    parts = decoded.split('=');
                    key = parts[0];
                    val = parts[1];
                }

                if (key === 'cat' || key === 'rep') {
                    route[key] = val;
                } else {
                    conditions.push({
                        column: key,
                        operator: sep,
                        value: val,
                        autoFormatValue: fmt === 1 ? true : false,
                        javaClass: 'com.untangle.app.reports.SqlCondition'
                    });
                    condsQuery += '&' + key + ':' + encodeURIComponent(sep) + ':' + encodeURIComponent(val) + ':' + fmt;
                }
            });
        }

        reportsVm.set('query', {
            route: route,
            conditions: conditions,
            string: condsQuery
        });

    }
});

Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    name: 'Ung',
    namespace: 'Ung',
    controllers: ['Global'],
    defaultToken : '',
    mainView: 'Ung.view.Main',
    context: 'REPORTS',
    initialLoad: false,
    launch: function () {
        var me = this;
        try {
            rpc.reportsManager = rpc.ReportsContext.reportsManager();
        } catch (ex) {
            console.error(ex);
        }

        Ext.Deferred.parallel([
            Rpc.asyncPromise('rpc.reportsManager.getReportEntries'),
            Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications')
        ]).then(function (result) {
            Ext.getStore('reports').loadData(result[0].list); // reports store is defined in Reports module
            Ext.getStore('categories').loadData(Ext.Array.merge(Util.baseCategories, result[1].list));
            Ext.getStore('reportstree').build(); // build the reports tree

            me.getMainView().getViewModel().set('reportsAppStatus', {
                installed: true,
                enabled: true
            });

            if (!Ung.app.initialLoad) {
                Ung.app.initialLoad = true;
                Ext.fireEvent('initialload');
            }

        });
    }
});

Ext.define('Ung.view.ChartMain', {
    extend: 'Ung.view.reports.GraphReport',
    itemId: 'fixedChart',
    viewModel: {},
    renderInReports: true
});

Ext.define('Ung.controller.ChartGlobal', {
    extend: 'Ext.app.Controller',

    refs: {
        chartView: '#fixedChart',
    },
    config: {
        routes: {
            '': 'onMain',
        }
    },

    onMain: function (categoryName, reportName) {
        var me = this,
            view = this.getChartView(),
            vm = view.getViewModel();

        var chartReport = Ext.Object.fromQueryString(window.location.search.substring(1));

        try {
            rpc.reportsManager = rpc.ReportsContext.reportsManager();
        } catch (ex) {
            console.error(ex);
        }

        rpc.reportsManager.getReportEntry(Ext.bind(function (result, ex) {
            if (ex) {
                Util.handleException(ex); return;
            }
            if(result){
                var entry = new Ung.model.Report(result);
                vm.set('entry', entry);
                vm.set('f_startdate', new Date( parseInt( chartReport.startDate, 10 ) ) );
                vm.set('f_enddate', new Date( parseInt( chartReport.endDate, 10 ) ) );
            }
        }, this), chartReport.reportUniqueId);
    }
});

Ext.define('Ung.ChartApplication', {
    extend: 'Ext.app.Application',
    name: 'Ung',
    namespace: 'Ung',
    controllers: ['ChartGlobal'],
    defaultToken : '',
    mainView: 'Ung.view.ChartMain',
    launch: function () {
        Ext.fireEvent('init');
    }
});
