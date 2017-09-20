Ext.define('Ung.view.Main', {
    extend: 'Ung.view.reports.Reports', // defined in module
    // layout: 'border',
    viewModel: {
        data: { servlet: 'REPORTS' }
    },
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        border: false,
        style: {
            background: '#1b1e26'
        },
        items: [{
            xtype: 'button',
            html: '<img src="' + '/images/BrandingLogo.png" style="height: 40px;"/>',
            hrefTarget: '_self',
            href: '#'
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        style: {
            zIndex: 9997
        },

        padding: 5,
        plugins: 'responsive',
        items: [{
            xtype: 'breadcrumb',
            reference: 'breadcrumb',
            store: 'reportstree',
            useSplitButtons: false,
            listeners: {
                selectionchange: function (el, node) {
                    if (!node.get('slug')) { return; }
                    if (node) {
                        if (node.get('url')) {
                            Ung.app.redirectTo('#' + node.get('url'));
                        } else {
                            Ung.app.redirectTo('#');
                        }
                    }
                }
            }
        }],
        responsiveConfig: {
            wide: { hidden: true },
            tall: { hidden: false }
        }
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
            ':category': 'onMain',
            ':category/:entry': 'onMain'
        }
    },

    onMain: function (categoryName, reportName) {
        var reportsVm = this.getReportsView().getViewModel();
        var hash = ''; // used to handle reports tree selection

        if (categoryName) {
            hash += categoryName;
        }
        if (reportName) {
            hash += '/' + reportName;
        }
        reportsVm.set('hash', hash);
    }
});

Ext.define('Ung.Application', {
    extend: 'Ext.app.Application',
    name: 'Ung',
    namespace: 'Ung',
    controllers: ['Global'],
    defaultToken : '',
    mainView: 'Ung.view.Main',
    launch: function () {
        try {
            rpc.reportsManager = rpc.appManager.app('reports').getReportsManager();
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
            Ext.fireEvent('init');
        });
    }
});

Ext.define('Ung.view.ChartMain', {
    extend: 'Ung.view.reports.GraphReport',
    itemId: 'chart',
    viewModel: {},
    renderInReports: true
});

Ext.define('Ung.controller.ChartGlobal', {
    extend: 'Ext.app.Controller',

    refs: {
        chartView: '#chart',
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
            rpc.reportsManager = rpc.appManager.app('reports').getReportsManager();
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
                vm.set('startDate', new Date( parseInt( chartReport.startDate, 10 ) ) );
                vm.set('endDate', new Date( parseInt( chartReport.endDate, 10 ) ) );
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

