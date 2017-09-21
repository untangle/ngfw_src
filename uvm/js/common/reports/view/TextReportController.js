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
        var me = this, vm = this.getViewModel(), reps = me.getView().up('#reports');
        me.entry = vm.get('entry');

        if (reps) { reps.getViewModel().set('fetching', true); }

        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            vm.set('startDate', new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600) * 1000 + (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset));
        }

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            vm.get('entry').getData(), // entry
            Ext.Date.add(vm.get('startDate'), Ext.Date.MINUTE, -(new Date().getTimezoneOffset() + rpc.timeZoneOffset/60000)), // start date
            (vm.get('tillNow') || !me.getView().renderInReports) ? null : Ext.Date.add(vm.get('endDate'), Ext.Date.MINUTE, -(new Date().getTimezoneOffset() + rpc.timeZoneOffset/60000)), // end date
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function(result) {
                me.getView().setLoading(false);
                if (reps) { reps.getViewModel().set('fetching', false); }
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
