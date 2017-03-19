Ext.define('Ung.view.reports.EventReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventreport',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            deactivate: 'onDeactivate'
        }
    },

    onBeforeRender: function () {
        var me = this, vm = this.getViewModel();
        console.log('here');
        vm.bind('{entry}', function (entry) {
            if (entry.get('type') !== 'EVENT_LIST') {
                return;
            }
            me.fetchData();
        });
    },

    onDeactivate: function () {
        // this.getView().setHtml('');
    },

    fetchData: function () {
        var me = this, vm = this.getViewModel();
        me.entry = vm.get('entry');
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                        vm.get('entry').getData(),
                        vm.get('startDate'),
                        vm.get('tillNow') ? null : vm.get('endDate'), -1)
            .then(function(result) {
                me.getView().setLoading(false);

                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').down('#currentData').setLoading(false);
                }
            });
    }

});
