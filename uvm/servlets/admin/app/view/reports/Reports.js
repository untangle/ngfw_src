Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports-vue',
    itemId: 'reports',

    layout: 'fit',
    border: false,

    viewModel: {
        data: {
            query: {}
        }
    },

    listeners: {
        activate: function (panel) {
            panel.loadReport();
        }
    },

    loadReport: function () {
        var target = this.down('#iframeHolder');
        var route = this.getViewModel().get('query.route') || {};
        var url = '/console/reports';
        if (route.cat) {
            url += '/' + route.cat;
            if (route.rep) {
                url += '/' + route.rep;
            }
            history.replaceState(null, '', window.location.pathname + '#reports');
            this.getViewModel().set('query.route', {});
        }
        Util.attachIframeToTarget(target, url, false);
    },

    items: [{
        xtype: 'container',
        itemId: 'iframeHolder',
        layout: 'fit'
    }]
});
