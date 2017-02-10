Ext.define('Ung.view.reports.ReportsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    init: function () {
        this.getAvailableTables();
    },

    control: {
        '#': { activate: 'onBeforeActivate', beforedeactivate: 'onBeforeDeactivate' },
        '#categoriesGrid': { selectionchange: 'onCategorySelect' },
        '#reportsGrid': { selectionchange: 'onReportSelect' },
        '#chart': { afterrender: 'fetchReportData' },

        '#startDate': { select: 'onSelectStartDate' },
        '#startTime': { select: 'onSelectStartTime' },
        '#startDateTimeBtn': { click: 'setStartDateTime' },
        '#endDate': { select: 'onSelectEndDate' },
        '#endTime': { select: 'onSelectEndTime' },
        '#endtDateTimeBtn': { click: 'setEndDateTime' },

        '#refreshBtn': { click: 'fetchReportData' },
        '#applyBtn': { click: 'fetchReportData' },
        '#chartStyleBtn': { toggle: 'fetchReportData' },
        '#timeIntervalBtn': { toggle: 'fetchReportData' },

        '#saveNewBtn': { click: 'saveReport' },
        '#updateBtn': { click: 'updateReport' },
        '#removeBtn': { click: 'removeReport' },

        '#dashboardBtn': { click: 'toggleDashboardWidget' }
    },

    listen: {
        global: {
            init: 'getCurrentApplications',
        },
        store: {
            '#categories': {
                datachanged: 'onCategoriesLoad'
            }
        }
    },

    /*
    listen: {
        store: {
            '#reports': {
                remove: 'onRemoveReport'
            }
        }
    },
    */

    onCategoriesLoad: function () {
        // reports are already loaded
        // console.log('cat load');
        var vm = this.getViewModel();
        if (vm.get('category')) {
            var cat = Ext.getStore('categories').findRecord('categoryName', vm.get('category'));
            this.getView().down('#categoriesGrid').getSelectionModel().select(cat);
        }
    },

    onBeforeActivate: function () {
        var vm = this.getViewModel();

        // if Reports inside Node settings
        if (this.getView().getInitCategory()) {
            vm.set({
                isNodeReporting: true,
                activeCard: 'categoryCard',
                category: null,
                report: null,
                categoriesData: null,
                startDateTime: null,
                endDateTime: null
            });

            this.getView().down('#categoriesGrid').setCollapsed(false); // expand categories panel if collapsed

            // filter reports based on selected category
            Ext.getStore('reports').filter({
                property: 'category',
                value: this.getView().getInitCategory().categoryName,
                exactMatch: true
            });
            this.buildReportsList();
            return;
        }

        // if main Reports view
        vm.set({
            isNodeReporting: false,
            activeCard: 'allCategoriesCard',
            // category: null,
            report: null,
            categoriesData: null,
            startDateTime: null,
            endDateTime: null
        });

        var me = this;
        vm.bind('{category}', function (category) {
            if (category) {
                var cat = Ext.getStore('categories').findRecord('categoryName', category);
                me.getView().down('#categoriesGrid').getSelectionModel().select(cat);
            } else {
                me.onBeforeDeactivate();
            }
        });
    },

    onBeforeDeactivate: function () {
        this.getView().down('#categoriesGrid').getSelectionModel().deselectAll();
        this.getView().down('#reportsGrid').getSelectionModel().deselectAll();
        this.getViewModel().set('activeCard', 'allCategoriesCard');
        Ext.getStore('reports').clearFilter();
    },

    getCurrentApplications: function () {
        var app, i, vm = this.getViewModel(), me = this;
        var categories = [
            { categoryName: 'Hosts', type: 'system', url: 'hosts', displayName: 'Hosts'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_hosts.png' },
            { categoryName: 'Devices', type: 'system', url: 'devices', displayName: 'Devices'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_devices.png' },
            { categoryName: 'Network', type: 'system', url: 'network', displayName: 'Network'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_network.png' },
            { categoryName: 'Administration', type: 'system', url: 'administration', displayName: 'Administration'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_admin.png' },
            { categoryName: 'System', type: 'system', url: 'system', displayName: 'System'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/config/icon_config_system.png' },
            { categoryName: 'Shield', type: 'system', url: 'shield', displayName: 'Shield'.t(), icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/untangle-node-shield_17x17.png' }
        ];

        rpc.reportsManager.getCurrentApplications(function (result, ex) {

            if (ex) { Ung.Util.exceptionToast(ex); return false; }

            for (i = 0; i < result.list.length; i += 1) {
                app = result.list[i];
                if (app.name !== 'untangle-node-branding-manager' && app.name !== 'untangle-node-live-support') {
                    categories.push({
                        categoryName: app.displayName,
                        type: 'app',
                        url: app.name.replace('untangle-node-', ''),
                        displayName: app.displayName, // t()
                        icon: resourcesBaseHref + '/skins/modern-rack/images/admin/apps/' + app.name + '_80x80.png'
                    });
                }
            }
            Ext.getStore('categories').loadData(categories);
            // vm.set('categoriesData', Ext.getStore('categories').getRange());
            // //me.getView().down('#categoriesGrid').getSelectionModel().select(0);

            // var allCategItems = [];
            // categories.forEach(function (category, idx) {
            //     allCategItems.push({
            //         xtype: 'button',
            //         baseCls: 'category-btn',
            //         html: '<img src="' + category.icon + '"/><br/><span>' + category.displayName + '</span>',
            //         index: idx,
            //         handler: function () {
            //             me.getView().down('#categoriesGrid').getSelectionModel().select(this.index);
            //         }
            //     });
            // });
            // me.getView().down('#allCategoriesList').removeAll();

            // if (me.getView().down('#categoriesLoader')) {
            //     me.getView().down('#categoriesLoader').destroy();
            // }

            // me.getView().down('#allCategoriesList').add(allCategItems);
        });
    },

    getAvailableTables: function() {
        var me = this;
        if (rpc.reportsManager) {
            rpc.reportsManager.getTables(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return false; }
                me.getViewModel().set('tableNames', result);
            });
        }
    },

    onCategorySelect: function (selModel, records) {
        console.log('here');
        if (records.length === 0) {
            return false;
        }

        this.getViewModel().set('activeCard', 'categoryCard'); // set category view card as active
        this.getViewModel().set('category', records[0]);
        this.getViewModel().set('report', null);
        this.getView().down('#categoriesGrid').setCollapsed(false); // expand categories panel if collapsed

        this.getView().down('#reportsGrid').getSelectionModel().deselectAll();

        Ung.app.redirectTo('#reports/' + records[0].get('url'));

        // filter reports based on selected category
        console.log(records[0].get('categoryName'));
        Ext.getStore('reports').filter({
            property: 'category',
            value: records[0].get('categoryName'),
            exactMatch: true
        });
        this.buildReportsList();
    },

    buildReportsList: function () {
        var me = this;
        var entries = [], entryHtml = '';

        // add reports list in category view card
        this.getView().down('#categoryReportsList').removeAll();
        Ext.getStore('reports').getRange().forEach(function (report) {

            // entryHtml = Ung.Util.iconReportTitle(report);
            entryHtml = '<i class="fa ' + report.get('icon') + ' fa-lg"></i><span class="ttl">' + (report.get('readOnly') ? report.get('title').t() : report.get('title')) + '</span><p>' +
                          (report.get('readOnly') ? report.get('description').t() : report.get('description')) + '</p>';
            entries.push({
                xtype: 'button',
                html: entryHtml,
                baseCls: 'entry-btn',
                //cls: (!entries[i].readOnly && entries[i].type !== 'EVENT_LIST') ? 'entry-btn custom' : 'entry-btn',
                border: false,
                textAlign: 'left',
                item: report,
                handler: function () {
                    //console.log('handler');
                    me.getView().down('#reportsGrid').getSelectionModel().select(this.item);
                    //_that.entryList.getSelectionModel().select(this.item);
                }
            });
        });
        this.getView().down('#categoryReportsList').add(entries);
    },

    onReportSelect: function (selModel, records) {
        if (records.length === 0) {
            this.getViewModel().set({
                activeCard: 'categoryCard'
            });
            return;
        }
        var report = records[0],
            chartContainer = this.getView().down('#report');

        this.getViewModel().set({
            activeCard: 'reportCard',
            report: report
        });

        this.getView().down('#customization').setActiveItem(0);
        chartContainer.remove('chart');

        if (report.get('type') === 'TIME_GRAPH' || report.get('type') === 'TIME_GRAPH_DYNAMIC') {
            chartContainer.add({
                xtype: 'timechart',
                itemId: 'chart',
                entry: report
            });
        }

        if (report.get('type') === 'PIE_GRAPH') {
            chartContainer.add({
                xtype: 'piechart',
                itemId: 'chart',
                entry: report
            });
        }

        if (report.get('type') === 'EVENT_LIST') {
            chartContainer.add({
                xtype: 'eventchart',
                itemId: 'chart',
                entry: report
            });
        }

        if (report.get('type') === 'TEXT') {
            chartContainer.add({
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

        chart.fireEvent('beginfetchdata');

        if (vm.get('report.type') !== 'EVENT_LIST') {
            rpc.reportsManager.getDataForReportEntry(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return false; }
                chart.fireEvent('setseries', result.list);
            }, chart.getEntry().getData(), vm.get('startDateTime'), vm.get('endDateTime'), -1);
        } else {
            var extraCond = null, limit = 100;
            rpc.reportsManager.getEventsForDateRangeResultSet(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return false; }
                //console.log(result);
                //this.loadResultSet(result);
                result.getNextChunk(function (result2, ex2) {
                    if (ex2) { Ung.Util.exceptionToast(ex2); return false; }
                    console.log(result2);
                    chart.fireEvent('setdata', result2.list);
                }, 100);

            }, chart.getEntry().getData(), extraCond, limit,  vm.get('startDateTime'), vm.get('endDateTime'));
        }
    },

    onSelectStartDate: function (picker, date) {
        var vm = this.getViewModel(), _date;
        if (!vm.get('startDateTime')) {
            this.getViewModel().set('startDateTime', date);
        } else {
            _date = new Date(vm.get('startDateTime'));
            _date.setDate(date.getDate());
            _date.setMonth(date.getMonth());
            vm.set('startDateTime', _date);
        }
    },

    onSelectStartTime: function (combo, record) {
        var vm = this.getViewModel(), _date;
        if (!vm.get('startDateTime')) {
            _date = new Date();
            //_date = _date.setDate(_date.getDate() - 1);
        } else {
            _date = new Date(vm.get('startDateTime'));
        }
        _date.setHours(record.get('date').getHours());
        _date.setMinutes(record.get('date').getMinutes());
        vm.set('startDateTime', _date);
    },

    setStartDateTime: function () {
        var view = this.getView();
        console.log(this.getViewModel().get('startDateTime'));
        view.down('#startDateTimeMenu').hide();
    },

    onSelectEndDate: function (picker, date) {
        var vm = this.getViewModel(), _date;
        if (!vm.get('endDateTime')) {
            this.getViewModel().set('endDateTime', date);
        } else {
            _date = new Date(vm.get('endDateTime'));
            _date.setDate(date.getDate());
            _date.setMonth(date.getMonth());
            vm.set('endDateTime', _date);
        }
    },

    onSelectEndTime: function (combo, record) {
        var vm = this.getViewModel(), _date;
        if (!vm.get('endDateTime')) {
            _date = new Date();
            //_date = _date.setDate(_date.getDate() - 1);
        } else {
            _date = new Date(vm.get('endDateTime'));
        }
        _date.setHours(record.get('date').getHours());
        _date.setMinutes(record.get('date').getMinutes());
        vm.set('endDateTime', _date);
    },

    setEndDateTime: function () {
        var view = this.getView();
        view.down('#endDateTimeMenu').hide();
    },

    saveReport: function () {
        var me = this, report,
            vm = this.getViewModel();

        report = vm.get('report').copy(null);
        report.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        report.set('readOnly', false);

        rpc.reportsManager.saveReportEntry(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return false; }
            vm.get('report').reject();
            Ext.getStore('reports').add(report);
            report.commit();
            me.getView().down('#reportsGrid').getSelectionModel().select(report);
            Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + report.get('title') + ' report added!');
        }, report.getData());
    },

    updateReport: function () {
        var vm = this.getViewModel();

        rpc.reportsManager.saveReportEntry(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return false; }
            vm.get('report').commit();
            Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('report.title') + '</span> report updated!');
        }, vm.get('report').getData());
    },

    removeReport: function () {
        var me = this,
            vm = this.getViewModel();

        Ext.MessageBox.confirm('Warning'.t(),
            'This will remove also the Widget from Dashboard'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
            function (btn) {
                if (btn === 'yes') {
                    rpc.reportsManager.removeReportEntry(function (result, ex) {
                        if (ex) { Ung.Util.exceptionToast(ex); return false; }

                        Ext.getStore('reports').remove(vm.get('report'));
                        me.buildReportsList();
                        me.getView().down('#reportsGrid').getSelectionModel().deselectAll();

                        Ung.Util.successToast('Report removed!');

                        me.toggleDashboardWidget();
                    }, vm.get('report').getData());
                }
            });
    },

    toggleDashboardWidget: function () {
        var vm = this.getViewModel(), record, me = this;
        if (vm.get('isWidget')) {
            // remove from dashboard
            record = Ext.getStore('widgets').findRecord('entryId', vm.get('report.uniqueId'));
            if (record) {
                Ext.getStore('widgets').remove(record);
                Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
                rpc.dashboardManager.setSettings(function (result, ex) {
                    if (ex) { Ung.Util.exceptionToast(ex); return; }
                    Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('report.title') + '</span> was removed from dashboard!');
                    Ext.GlobalEvents.fireEvent('removewidget', vm.get('report.uniqueId'));
                    vm.set('isWidget', !vm.get('isWidget'));
                    me.getView().down('#reportsGrid').getView().refresh();
                }, Ung.dashboardSettings);
            } else {
                Ung.Util.exceptionToast('<span style="color: yellow; font-weight: 600;">' + vm.get('report.title') + '</span> was not found on Dashboard!');
            }
        } else {
            // add to dashboard
            record = Ext.create('Ung.model.Widget', {
                displayColumns: vm.get('report.displayColumns'),
                enabled: true,
                entryId: vm.get('report.uniqueId'),
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: 3600,
                type: 'ReportEntry'
            });
            Ext.getStore('widgets').add(record);

            Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
            rpc.dashboardManager.setSettings(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return; }
                Ung.Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('report.title') + '</span> was added to dashboard!');
                Ext.GlobalEvents.fireEvent('addwidget', record, vm.get('report'));
                vm.set('isWidget', !vm.get('isWidget'));
                me.getView().down('#reportsGrid').getView().refresh();
            }, Ung.dashboardSettings);
        }
    }

});
