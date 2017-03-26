Ext.define('Ung.view.reports.ReportsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': { afterrender: 'onAfterRender', deactivate: 'onDeactivate' }
    },

    listen: {
        store: {
            '#reports': { datachanged: 'onReportsLoad' }
        }
    },

    onAfterRender: function () {
        var me = this, vm = me.getViewModel();

        me.getView().setLoading(true);

        me.buildCategories();
        vm.bind('{hash}', function (hash) {
            if (!hash || !vm.get('viewReady')) {
                me.resetView();
                return;
            }
            me.lookup('tree').collapseAll();
            me.lookup('tree').selectPath(window.location.hash.replace('#', '/'), 'slug', '/', me.selectPath, me);
        });
    },

    buildCategories: function () {
        var me = this, vm = me.getViewModel();
        var categories = [
            { categoryName: 'Hosts', type: 'system', slug: 'hosts', displayName: 'Hosts'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_hosts.png' },
            { categoryName: 'Devices', type: 'system', slug: 'devices', displayName: 'Devices'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_devices.png' },
            { categoryName: 'Network', type: 'system', slug: 'network', displayName: 'Network'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_network.png' },
            { categoryName: 'Administration', type: 'system', slug: 'administration', displayName: 'Administration'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_admin.png' },
            { categoryName: 'System', type: 'system', slug: 'system', displayName: 'System'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_system.png' },
            { categoryName: 'Shield', type: 'system', slug: 'shield', displayName: 'Shield'.t(), icon: '/skins/modern-rack/images/admin/config/icon_config_shield.png' }
        ];

        try {
            rpc.reportsManager = rpc.appManager.app('reports').getReportsManager();
        } catch (ex) {
            console.log(ex);
            return;
        }

        Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function (result) {
            Ext.Array.each(result.list, function (app) {
                if (app.name !== 'branding-manager' && app.name !== 'live-support') {
                    categories.push({
                        categoryName: app.displayName,
                        type: 'app',
                        url: app.name,
                        displayName: app.displayName, // t()
                        icon: '/skins/modern-rack/images/admin/apps/' + app.name + '_80x80.png'
                    });
                }
            });
            Ext.getStore('categories').loadData(categories);
            if (Ext.getStore('reports').getCount() > 0) {
                me.buildTree();
            }
        });
    },

    /**
     * this is used when the entire app loads directly on report page
     * in this case, after reports are loaded, the reportName vm prop is updated just to triger the binding
     */
    onReportsLoad: function () {
        var vm = this.getViewModel();
        if (Ext.getStore('categories').getCount() > 0) {
            this.buildTree();
        }
    },

    /**
     * builds the reports tree
     * this is called only after reports and categories stores are set
     */
    buildTree: function () {
        var me = this, vm = me.getViewModel(), nodes = [], storeCat, category;

        me.getView().setLoading(false);

        // the reports store is grouped by category
        Ext.Array.each(Ext.getStore('reports').getGroups().items, function (group) {
            storeCat = Ext.getStore('categories').findRecord('categoryName', group._groupKey);

            if (!storeCat) { return; }

            // create category node
            category = {
                text: group._groupKey,
                slug: storeCat.get('slug'),
                type: storeCat.get('type'), // app or system
                icon: storeCat.get('icon'),
                cls: 'category-icon-tree',
                url: storeCat.get('slug'),
                children: [],
                // expanded: group._groupKey === vm.get('category.categoryName')
            };
            // add reports to each category
            Ext.Array.each(group.items, function (entry) {
                category.children.push({
                    text: entry.get('title'),
                    slug: entry.get('slug'),
                    url: entry.get('url'),
                    uniqueId: entry.get('uniqueId'),
                    type: entry.get('type'),
                    readOnly: entry.get('readOnly'),
                    iconCls: 'fa ' + entry.get('icon'),
                    leaf: true
                    // selected: uniqueId === vm.get('entry.uniqueId')
                });
            });
            nodes.push(category);
        });

        me.lookup('tree').setRootNode({
            text: 'All reports',
            slug: 'reports',
            expanded: true,
            children: nodes
        });

        vm.set('viewReady', true);

        // me.lookup('breadcrumb').setStore(me.lookup('tree').getStore());

        if (vm.get('hash')) {
            me.lookup('tree').selectPath(window.location.hash.replace('#', '/'), 'slug', '/', this.selectPath, this);
        } else {
            me.buildStats();
        }

    },

    /**
     * selects a tree node (report or category) based on location hash
     * and updates viewmodel with the report entry
     */
    selectPath: function (success, node) {
        var me = this, vm = me.getViewModel(), record;

        if (!success) { console.log('error'); return; }

        if (node.isLeaf()) {
            // report node
            record = Ext.getStore('reports').findRecord('url', window.location.hash.replace('#reports/', ''));
            if (record) {
                vm.set({
                    report: record, // main reference from the store
                    entry: record.copy(null) // report reference copy on which modifications are made
                });
            }
            me.lookup('cards').setActiveItem('report');
        } else {
            me.lookup('cards').setActiveItem('category');
            me.buildStats();
            node.expand();
        }

        // me.lookup('breadcrumb').setSelection(node);
    },

    /**
     * the tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value, meta, record) {
        if (!record.get('readOnly') && record.get('uniqueId')) {
            meta.tdCls = 'x-tree-custom-report';
        }
        return this.rendererRegExp ? value.replace(this.rendererRegExp, '<span style="font-weight: bold; background: #EEE; color: #000; border-bottom: 1px #000 solid;">$1</span>') : value;
    },

    /**
     * filters reports tree
     */
    filterTree: function (field, value) {
        var me = this, tree = me.lookup('tree');
        me.rendererRegExp = new RegExp('(' + value + ')', 'gi');

        if (!value) {
            tree.getStore().clearFilter();
            tree.collapseAll();
            field.getTrigger('clear').hide();
            return;
        }

        tree.getStore().getFilters().replaceAll({
            property: 'text',
            value: new RegExp(Ext.String.escape(value), 'i')
        });
        tree.expandAll();
        field.getTrigger('clear').show();
    },

    onTreeFilterClear: function () {
        this.lookup('tree').down('textfield').setValue();
    },

    /**
     * resets the view to an initial state
     */
    resetView: function () {
        var me = this, tree = me.lookup('tree');
        tree.collapseAll();
        tree.getSelectionModel().deselectAll();
        tree.getStore().clearFilter();
        tree.down('textfield').setValue('');

        me.buildStats();
        me.lookup('cards').setActiveItem('category');

        // me.lookup('breadcrumb').setSelection('root');
    },

    /**
     * builds statistics for categories
     */
    buildStats: function () {
        var me = this, vm = me.getViewModel(), tree = me.lookup('tree'), selection,
        stats = {
            set: false,
            reports: {
                total: 0,
                custom: 0,
                chart: 0,
                event: 0,
                info: 0
            },
            categories: {
                total: 0,
                app: 0
            }
        };

        if (tree.getSelection().length === 0) {
            selection = tree.getRootNode();
        } else {
            selection = tree.getSelection()[0];
        }

        selection.cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            if (node.isLeaf()) {
                stats.reports.total += 1;
                if (!node.get('readOnly')) { stats.reports.custom += 1; }
                switch(node.get('type')) {
                    case 'TIME_GRAPH':
                    case 'TIME_GRAPH_DYNAMIC':
                    case 'PIE_GRAPH':
                        stats.reports.chart += 1; break;
                    case 'EVENT_LIST':
                        stats.reports.event += 1; break;
                    case 'TEXT':
                        stats.reports.info += 1; break;
                }
            } else {
                stats.categories.total += 1;
                if (node.get('type') === 'app') {
                    stats.categories.app += 1;
                }
            }
        });
        vm.set('stats', stats);
        // vm.notify();
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

    // breadcrumbSelection: function (el, node) {
    //     console.log(node);
    // }


});
