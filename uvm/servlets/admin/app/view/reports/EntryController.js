Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports-entry',

    control: {
        '#': {
            afterrender: 'onAfterRender',
        }
    },

    colorPalette: [
        // red
        'B71C1C', 'C62828', 'D32F2F', 'E53935', 'F44336', 'EF5350', 'E57373', 'EF9A9A',
        // pink
        '880E4F', 'AD1547', 'C2185B', 'D81B60', 'E91E63', 'EC407A', 'F06292', 'F48FB1',
        // purple
        '4A148C', '6A1B9A', '7B1FA2', '8E24AA', '9C27B0', 'AB47BC', 'BA68C8', 'CE93D8',
        // blue
        '0D47A1', '1565C0', '1976D2', '1E88E5', '2196F3', '42A5F5', '64B5F6', '90CAF9',
        // teal`
        '004D40', '00695C', '00796B', '00897B', '009688', '26A69A', '4DB6AC', '80CBC4',
        // green
        '1B5E20', '2E7D32', '388E3C', '43A047', '4CAF50', '66BB6A', '81C784', 'A5D6A7',
        // limE
        '827717', '9E9D24', 'AFB42B', 'C0CA33', 'CDDC39', 'D4E157', 'DCE775', 'E6EE9C',
        // yellow
        'F57F17', 'F9A825', 'FBC02D', 'FDD835', 'FFEB3B', 'FFEE58', 'FFF176', 'FFF59D',
        // orange
        'E65100', 'EF6C00', 'F57C00', 'FB8C00', 'FF9800', 'FFA726', 'FFB74D', 'FFCC80',
        // brown
        '3E2723', '4E342E', '5D4037', '6D4C41', '795548', '8D6E63', 'A1887F', 'BCAAA4',
        // grey
        '212121', '424242', '616161', '757575', '9E9E9E', 'BDBDBD', 'E0E0E0', 'EEEEEE',
    ],

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();
        // watch when the selected report is changed

        vm.bind('{entry}', function (entry) {
            if (!entry) { return; }
            console.log(entry);
            me.addChart();

            this.set('_defaultColors', entry.get('colors') ? false : true);
        });

        vm.bind('{_defaultColors}', function (val) {
            var colors, colorBtns = [];

            if (val) {
                vm.set('entry.colors', null);
            } else {
                colors = vm.get('entry.colors') || Util.defaultColors;
                me.getView().down('#colors').removeAll();
                colors.forEach(function (color, i) {
                    colorBtns.push({
                        xtype: 'button',
                        margin: '0 1',
                        idx: i,
                        arrowVisible: false,
                        menu: {
                            plain: true,
                            xtype: 'colormenu',
                            colors: me.colorPalette,
                            height: 200,
                            listeners: {
                                select: 'updateColor'
                            },
                            dockedItems: [{
                                xtype: 'toolbar',
                                dock: 'bottom',
                                // ui: 'footer',
                                items: [{
                                    // text: 'Remove'.t(),
                                    iconCls: 'fa fa-ban',
                                    tooltip: 'Remove'.t()
                                }, {
                                    text: 'OK'.t(),
                                    iconCls: 'fa fa-check',
                                    listeners: {
                                        click: function (btn) {
                                            btn.up('button').hideMenu();
                                        }
                                    }
                                }]

                            }]
                        },
                        text: '<i class="fa fa-square" style="color: ' + color + '"></i>',
                    });
                });
                me.getView().down('#colors').add(colorBtns);

            }
        });

    },

    addChart: function () {
        var me = this,
            vm = this.getViewModel(),
            chartContainer = me.getView().down('#chartContainer'),
            entry = vm.get('entry');

        chartContainer.remove('chart'); // remove any existing chart

        if (entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
            chartContainer.add({
                xtype: 'timechart',
                itemId: 'chart',
            });
        }

        if (entry.get('type') === 'PIE_GRAPH') {
            chartContainer.add({
                xtype: 'piechart',
                itemId: 'chart'
            });
        }

        if (entry.get('type') === 'EVENT_LIST') {
            // chartContainer.add({
            //     xtype: 'eventchart',
            //     itemId: 'chart',
            //     entry: this.entry
            // });
        }

        if (entry.get('type') === 'TEXT') {
            // chartContainer.add({
            //     xtype: 'component',
            //     itemId: 'chart',
            //     html: 'Not Implemented'
            // });
        }
    },

    updateColor: function (menu, color) {
        var vm = this.getViewModel(),
            newColors = vm.get('entry.colors') ? Ext.clone(vm.get('entry.colors')) : Ext.clone(Util.defaultColors);

        menu.up('button').setText('<i class="fa fa-square" style="color: #' + color + ';"></i>');
        newColors[menu.up('button').idx] = '#' + color;
        vm.set('entry.colors', newColors);
        return false;
    },

    // addColor: function (btn) {
    //     btn.up('grid').getStore().add({color: 'FF0000'});
    //     // var vm = this.getViewModel();
    //     // var colors = vm.get('report.colors');
    //     // colors.push('#FF0000');
    //     // vm.set('report.colors', colors);
    // },

    refreshData: function () {
        this.getView().down('#chart').getController().fetchData();
    },

    updateReport: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                vm.get('report').copyFrom(entry);
                vm.get('report').commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> report updated!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());
            });
    },

    saveNewReport: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        entry.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        entry.set('readOnly', false);

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                Ext.getStore('reports').add(entry);
                entry.commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + entry.get('title') + ' report added!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());
            });
    }



});
