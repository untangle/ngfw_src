Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports-entry',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        },
        '#chart': {
            // afterrender: 'fetchReportData'
        }
    },

    updateColors: function (store) {
        var vm = this.getViewModel(),
            newColors = [];
        store.each(function (rec) {
            newColors.push('#' + rec.get('color'));
        });
        vm.set('report.colors', newColors);
    },

    addColor: function (btn) {
        btn.up('grid').getStore().add({color: 'FF0000'});
        // var vm = this.getViewModel();
        // var colors = vm.get('report.colors');
        // colors.push('#FF0000');
        // vm.set('report.colors', colors);
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();

        // watch when the selected report is changed
        vm.bind('{report}', function (report) {
            console.log(report);
            if (report) {
                me.addChart(report);
            }
        });
        vm.bind('{report.colors}', function (entryColors) {
            var colors = entryColors || Util.defaultColors,
                // colorsCmp = [],
                colorsStoreData = [];
            colors.forEach(function (color) {
                // colorsCmp.push({
                //     bind: color,
                // });
                colorsStoreData.push({
                    color: color.replace('#', '')
                });
            });
            // me.getView().down('#colors').removeAll();
            // me.getView().down('#colors').add(colorsCmp);

            vm.set('_colorsData', colorsStoreData);

            // console.log(colors);
            // if (report) {
            //     me.addChart(report);
            // }
        });
    },

    addChart: function (report) {
        var holder = this.getView().down('#chartHolder');
        holder.remove('chart');
        if (report.get('type') === 'TIME_GRAPH' || report.get('type') === 'TIME_GRAPH_DYNAMIC') {
            holder.add({
                xtype: 'timechart',
                itemId: 'chart',
                entry: report
            });
        }

        if (report.get('type') === 'PIE_GRAPH') {
            holder.add({
                xtype: 'piechart',
                itemId: 'chart',
                // entry: report,
                viewModel: {
                    data: {
                        entry: report
                    }
                }
            });
        }

        if (report.get('type') === 'EVENT_LIST') {
            holder.add({
                xtype: 'eventchart',
                itemId: 'chart',
                entry: report
            });
        }

        if (report.get('type') === 'TEXT') {
            holder.add({
                xtype: 'component',
                itemId: 'chart',
                html: 'Not Implemented'
            });
        }
    },

    fetchReportData: function () {
        var me = this,
            vm = me.getViewModel(),
            chart = me.getView().down('#chart');

        if (!chart) { return; }

        chart.fireEvent('beginfetchdata');

        if (vm.get('report.type') !== 'EVENT_LIST') {

            Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                           chart.getViewModel().get('entry').getData(),
                           vm.get('startDate'),
                           vm.get('tillNow') ? null : vm.get('endDate'), -1)
                .then(function(result) {
                    chart.fireEvent('setseries', result.list);
                });
        } else {
            var extraCond = null, limit = 100;
            Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                           chart.getEntry().getData(),
                           extraCond,
                           limit,
                           null,
                           null)
                .then(function(result) {
                    result.getNextChunk(function (result2, ex2) {
                        if (ex2) { Util.exceptionToast(ex2); return false; }
                        // console.log(result2);
                        chart.fireEvent('setdata', result2.list);
                    }, 100);

                });
        }
    },

    updateReport: function () {
        var vm = this.getViewModel();

        Rpc.asyncData('rpc.reportsManager.saveReportEntry', vm.get('report').getData())
            .then(function(result) {
                vm.get('report').commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('report.title') + '</span> report updated!');
            });
    },

    saveNewReport: function () {
        // var me = this, report,
        var report, vm = this.getViewModel();

        report = vm.get('report').copy(null);
        report.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        report.set('readOnly', false);

        console.log(report);

        // Rpc.asyncData('rpc.reportsManager.saveReportEntry', report.getData())
        //     .then(function(result) {
        //         vm.get('report').reject();
        //         Ext.getStore('reports').add(report);
        //         report.commit();
        //         // me.getView().down('#reportsGrid').getSelectionModel().select(report);
        //         Util.successToast('<span style="color: yellow; font-weight: 600;">' + report.get('title') + ' report added!');
        //     });
    }



});
