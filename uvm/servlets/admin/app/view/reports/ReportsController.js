Ext.define('Ung.view.reports.ReportsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': {
            beforerender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    listen: {
        store: {
            '#reports': {
                datachanged: 'onReportsLoad'
            }
        }
    },

    onAfterRender: function () {
        var me = this, app, i, categGrid = this.lookupReference('categories');
        var vm = this.getViewModel();
        var categories = [
            { categoryName: 'Hosts', type: 'system', url: 'hosts', displayName: 'Hosts'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_hosts.png' },
            { categoryName: 'Devices', type: 'system', url: 'devices', displayName: 'Devices'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_devices.png' },
            { categoryName: 'Network', type: 'system', url: 'network', displayName: 'Network'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_network.png' },
            { categoryName: 'Administration', type: 'system', url: 'administration', displayName: 'Administration'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_admin.png' },
            { categoryName: 'System', type: 'system', url: 'system', displayName: 'System'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_system.png' },
            { categoryName: 'Shield', type: 'shield', url: 'shield', displayName: 'Shield'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_shield.png' }
        ];

        try {
            rpc.reportsManager = rpc.appManager.app('reports').getReportsManager();
        } catch (ex) {
            console.log(ex);
            return;
        }

        Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
            for (i = 0; i < result.list.length; i += 1) {
                app = result.list[i];
                if (app.name !== 'branding-manager' && app.name !== 'live-support') {
                    categories.push({
                        categoryName: app.displayName,
                        type: 'app',
                        url: app.name,
                        displayName: app.displayName, // t()
                        icon: '/skins/modern-rack/images/admin/apps/' + app.name + '_80x80.png'
                    });
                }
            }
            Ext.getStore('categories').loadData(categories);
            vm.set('category', categGrid.getStore().findRecord('categoryName', vm.get('categoryName')));
        });

        // Rpc.asyncData('rpc.reportsManager.getConditionQuickAddHints').then(function (result) {
        //     console.log(result);
        // });


        vm.bind('{categoryName}', function (categoryName) {
            var categStore = me.lookupReference('categories').getStore();
            if (categStore) {
                vm.set('category', categStore.findRecord('categoryName', vm.get('categoryName')));
            }
        });

        vm.bind('{reportName}', function (reportName) {
            if (!reportName) {
                vm.set('report', null);
                return;
            }
            // remove the last space - hack for when reports are finished to load
            // so the specific report item can be selected
            reportName = reportName.replace(/ /g, '');

            var reportsStore = me.lookupReference('reports').getStore(), report;
            if (reportsStore.data.length) {
                report = reportsStore.queryBy(function (entry) {
                    return entry.get('category') === vm.get('category.categoryName') &&
                        entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase() === reportName;
                });
                if (report && report.length > 0) {
                    // console.log(report);
                    // main reference from the store
                    vm.set('report', report.getAt(0));
                    // report reference copy on which modifications are made
                    vm.set('entry', report.getAt(0).copy(null));
                }
            }
        });
    },

    onDeactivate: function () {
        var vm = this.getViewModel();
        vm.set({
            categoryName: null,
            category: null,
            reportName: null,
            report: null,
            activeCard: 'category'
        });
    },

    /**
     * this is used when the entire app loads directly on report page
     * in this case, after reports are loaded, the reportName vm prop is updated just to triger the binding
     */
    onReportsLoad: function () {
        var vm = this.getViewModel();
        if (vm.get('reportName')) {
            vm.set('reportName', vm.get('reportName') + ' ');
        }
    }

});
