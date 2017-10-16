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

        // needed on Create New

        // vm.bind('{eEntry.type}', function (type) {
        //     if (type !== 'TEXT') { return; }
        //     Ext.defer(function () {
        //         me.fetchData(true);
        //     }, 300);
        // });

    },

    onDeactivate: function () {
        this.getView().setHtml('');
    },

    fetchData: function (reset, cb) {
        var me = this, vm = this.getViewModel(), reps = me.getView().up('#reports'), startDate, endDate;
        var entry = vm.get('eEntry') || vm.get('entry');

        if (reps) { reps.getViewModel().set('fetching', true); }

        // startDate
        if (!me.getView().renderInReports) {
            // if not rendered in reports than treat as widget so from server startDate is extracted the timeframe
            startDate = new Date(rpc.systemManager.getMilliseconds() - (Ung.dashboardSettings.timeframe * 3600 || 3600) * 1000);
        } else {
            // if it's a report, convert UI client start date to server date
            startDate = Util.clientToServerDate(vm.get('startDate'));
        }

        // endDate
        if (vm.get('tillNow') || !me.getView().renderInReports) {
            // if showing reports till current time
            endDate = null;
        } else {
            // otherwise, in reports, convert UI client end date to server date
            endDate = Util.clientToServerDate(vm.get('endDate'));
        }

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            entry.getData(), // entry
            startDate,
            endDate,
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function(result) {
                me.getView().setLoading(false);
                if (reps) { reps.getViewModel().set('fetching', false); }
                me.processData(result.list);
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').getController().formatTextData(result.list);
                }

                if (cb) { cb(); }

            })
            .always(function() {
                if (reps) { reps.getViewModel().set('fetching', false); }
            });
    },

    processData: function (data) {

        var v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('eEntry') || vm.get('entry'),
            textColumns = entry.get('textColumns'), i, columnName, values = [];

        if (data.length > 0 && textColumns && textColumns.length > 0) {
            Ext.Array.each(textColumns, function (column) {
                columnName = column.split(' ').splice(-1)[0];
                values.push(data[0][columnName] || 0);
            });

            v.setHtml(Ext.String.format.apply(Ext.String.format, [entry.get('textString')].concat(values)));
            // todo: send data to the datagrid for TEXT report
        }
    }
});
