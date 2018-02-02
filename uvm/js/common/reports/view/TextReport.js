Ext.define('Ung.view.reports.TextReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.textreport',

    viewModel: true,

    border: false,
    bodyBorder: false,

    padding: '50 10',

    bodyStyle: {
        fontFamily: 'Roboto Condensed, Arial, sans-serif',
        textAlign: 'center',
        fontSize: '16px'
    },

    config: {
        widget: null
    },

    controller: {
        control: {
            '#': {
                afterrender: 'onAfterRender',
                deactivate: 'onDeactivate'
            }
        },

        onAfterRender: function (view) {
            var me = this, vm = this.getViewModel();

            // find and set the widget component if report is rendered inside a widget
            view.setWidget(view.up('reportwidget'));

            // if it's a widget, than fetch data after the report entry is binded to it
            vm.bind('{entry}', function (entry) {
                if (!entry || entry.get('type') !== 'TEXT') { return; }

                // if rendered as widget, add to dashboard queue
                // if (view.getWidget()) {
                //     DashboardQueue.addFirst(view.getWidget());
                // }

                // if rendered in creating new widget dialog, fetch data
                if (view.up('new-widget')) {
                    me.fetchData(true);
                }
            });

            // });

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

            // date range setup
            if (!me.getView().renderInReports) {
                // if not rendered in reports than treat as widget so from server startDate is extracted the timeframe
                startDate = new Date(Util.getMilliseconds() - (Ung.dashboardSettings.timeframe * 3600 || 3600) * 1000);
                endDate = null;
            } else {
                // if it's a report, convert UI client start date to server date
                startDate = Util.clientToServerDate(vm.get('f_startdate'));
                endDate = Util.clientToServerDate(vm.get('f_enddate'));
            }

            me.getView().setLoading(true);
            Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                entry.getData(), // entry
                startDate,
                endDate,
                vm.get('globalConditions'), -1) // sql filters
                .then(function(result) {
                    if (!me.getView()) { return; }
                    me.getView().setLoading(false);
                    if (reps) { reps.getViewModel().set('fetching', false); }
                    me.processData(result.list);

                    if (cb) { cb(result.list); }

                })
                .always(function() {
                    if (!me.getView()) { return; }
                    me.getView().setLoading(false);
                    if (reps) { reps.getViewModel().set('fetching', false); }
                });
        },

        processData: function (data) {

            var v = this.getView(),
                vm = this.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'),
                textColumns = entry.get('textColumns'), columnName, values = [];

            if (data.length > 0 && textColumns && textColumns.length > 0) {
                Ext.Array.each(textColumns, function (column) {
                    columnName = column.split(' ').splice(-1)[0];
                    values.push(data[0][columnName] || 0);
                });

                v.setHtml(Ext.String.format.apply(Ext.String.format, [entry.get('textString')].concat(values)));
                // todo: send data to the datagrid for TEXT report
            }
        }
    }
});
