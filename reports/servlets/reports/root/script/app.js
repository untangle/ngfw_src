/** main.js and util.js are old UI resources */

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
