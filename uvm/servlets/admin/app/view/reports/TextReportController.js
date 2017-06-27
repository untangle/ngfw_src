Ext.define('Ung.view.reports.TextReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.textreport',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            deactivate: 'onDeactivate'
        }
    },

    onBeforeRender: function () {
        var me = this, vm = this.getViewModel();

        vm.bind('{entry}', function (entry) {
            if (entry.get('type') !== 'TEXT') {
                return;
            }
            if (!me.getView().up('reportwidget')) {
                me.fetchData();
            } else {
                me.isWidget = true;
            }
        });
    },

    onDeactivate: function () {
        this.getView().setHtml('');
    },

    fetchData: function (reset, cb) {
        var me = this, vm = this.getViewModel();
        me.entry = vm.get('entry');

        var treeNav;
        if (me.getView().up('#reports')) {
            treeNav = me.getView().up('#reports').down('treepanel');
        }

        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            vm.set('startDate', new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600 * 24) * 1000));
            vm.set('endDate', new Date(rpc.systemManager.getMilliseconds()));
        }

        if (treeNav) { treeNav.setDisabled(true); } // disable reports tree while data is fetched
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            vm.get('entry').getData(), // entry
            vm.get('startDate'), // start date
            vm.get('tillNow') ? null : vm.get('endDate'), // end date
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function(result) {
                me.getView().setLoading(false);
                if (treeNav) { treeNav.setDisabled(false); }
                me.processData(result.list);
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').getController().formatTextData(result.list);
                }

                if (cb) { cb(); }

            });
    },

    processData: function (data) {

        var v = this.getView(),
            vm = this.getViewModel(),
            textColumns = vm.get('entry.textColumns'), i, columnName, values = [];

        if (data.length > 0 && textColumns && textColumns.length > 0) {
            Ext.Array.each(textColumns, function (column) {
                columnName = column.split(' ').splice(-1)[0];
                values.push(data[0][columnName] || 0);
            });

            v.setHtml(Ext.String.format.apply(Ext.String.format, [vm.get('entry.textString')].concat(values)));
            // todo: send data to the datagrid for TEXT report
        }
    }
});
