Ext.define('Ung.view.reports.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': { afterrender: 'onAfterRender', deactivate: 'resetView' }
    },

    listen: {
        global: {
            init: 'onInit'
        }
    },

    onAfterRender: function () {
        this.getView().setLoading(true);
    },

    onInit: function () {
        var me = this, vm = me.getViewModel(),
            tree = me.lookup('tree');

        // set the context (ADMIN or REPORTS)
        vm.set('context', Ung.app.context);

        me.getView().setLoading(false);

        me.buildTablesStore();

        vm.bind('{fetching}', function (val) {
            if (!val) { Ext.MessageBox.hide(); } // hide any loading message box
        });

        /**
         * When route changes select report path
         */
        vm.bind('{query.route}', function (route) {
            var path = (route.cat || '') + (route.rep ? ('/' + route.rep) : '');
            if (!path) {
                tree.collapseAll();
                tree.setSelection(null);
                me.showNode(tree.getStore().getRoot());
            }

            me.lookup('tree').selectPath(path, 'slug', '/', function (success, lastNode) {
                if (!success) { return; }
                if (!lastNode.isLeaf()) {
                    lastNode.expand();
                }
            }, me);
            me.buildStats();
        });

        // When conditions query string changes, update disble state of the reports nodes in the tree
        vm.bind('{query.conditions}', function (conditions) {
            var root = Ext.getStore('reportstree').getRoot(), conds = [], disabledCategory;
            Ext.Array.each(conditions, function (c) {
                conds.push(c.column);
            });

            root.eachChild(function (catNode) {
                disabledCategory = true;
                catNode.eachChild(function (repNode) { // report node
                    if (conds.length > 0) {
                        if (TableConfig.containsColumns(repNode.get('table'), conds)) {
                            repNode.set('disabled', false);
                            disabledCategory = false;
                        } else {
                            repNode.set('disabled', true);
                        }
                    } else {
                        repNode.set('disabled', false);
                        disabledCategory = false;
                    }
                });
                catNode.set('disabled', disabledCategory);
            });
            me.buildStats();
        });
    },

    // Redirect on selecting report from the tree
    onSelectReport: function (el, node) {
        var me = this, vm = me.getViewModel(), condsQuery = '';

        console.log(vm.get('query.conditions'));

        Ext.Array.each(vm.get('query.conditions'), function (c) {
            condsQuery += '&' + c.column + ':' + encodeURIComponent(c.operator) + ':' + encodeURIComponent(c.value) + ':' + (c.autoFormatValue === true ? 1 : 0);
        });

        if (Ung.app.context === 'REPORTS') {
            Ung.app.redirectTo('#' + node.get('url'));
        } else {
            Ung.app.redirectTo('#reports?' + node.get('url') + condsQuery);
        }
        // Ung.app.redirectTo(Ung.app.context === 'REPORTS' ? '#' : '#reports?' + node.get('url') + condsQuery);
        me.showNode(node);
    },


    buildTablesStore: function () {
        if (!rpc.reportsManager) { return; }
        var me = this, vm = me.getViewModel();
        Rpc.asyncData('rpc.reportsManager.getTables').then(function (result) {
            vm.set('tables', result); // used in advanced report settings table name
        });
    },

    showNode: function (node) {
        var me = this, record;

        me.getViewModel().set('selection', node.isRoot() ? null : node);

        if (node.isLeaf()) {
            // report node
            record = Ext.getStore('reports').findRecord('url', node.get('url'), 0, false, true, true);
            if (record) {
                me.getView().down('entry').getViewModel().set({
                    entry: record
                });
            }
            me.lookup('cards').setActiveItem('report');
        } else {
            me.lookup('cards').setActiveItem('category');
            // me.buildStats(node);
            node.expand();
        }
    },


    /**
     * the tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value, meta, record) {
        // if (!record.isLeaf()) {
        //     meta.tdCls = 'x-tree-category';
        // }
        // if (!record.get('readOnly') && record.get('uniqueId')) {
        //     meta.tdCls = 'x-tree-custom-report';
        // }
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
        var me = this, tree = me.lookup('tree'), breadcrumb = me.lookup('breadcrumb');
        tree.collapseAll();
        tree.getSelectionModel().deselectAll();
        tree.getStore().clearFilter();
        tree.down('textfield').setValue('');

        breadcrumb.setSelection('root');

        if (me.getViewModel().get('hash') !== 'create') {
            me.buildStats();
            me.lookup('cards').setActiveItem('category');
            me.getViewModel().set('selection', null);
            me.getViewModel().set('hash', null);
        }
    },

    /**
     * Builds statistics for categories, based on global conditions too
     */
    buildStats: function () {
        var me = this, vm = me.getViewModel(),
            node = vm.get('selection'),
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

        if (!node) { node = Ext.getStore('reportstree').getRoot(); }

        node.cascade(function (n) {
            if (n.isRoot() || n.get('disabled')) { return; }
            if (n.isLeaf()) {
                stats.reports.total += 1;
                if (!n.get('readOnly')) { stats.reports.custom += 1; }
                switch(n.get('type')) {
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
                if (n.get('type') === 'app') {
                    stats.categories.app += 1;
                }
            }
        });
        vm.set('stats', stats);
    },

    // on new report just redirect to proper route
    newReport: function () {
        Ung.app.redirectTo('#reports/create');
    },

    newImport: function () {
        var me = this;
        var dialog = me.getView().add({
            xtype: 'importdialog'
        });
        dialog.show();
    },


    exportCategoryReports: function () {
        var me = this, vm = me.getViewModel(), reportsArr = [], category, reports;

        if (vm.get('selection')) {
            category = vm.get('selection.text'); // selected category
            reports = Ext.getStore('reports').query('category', category, false, true, true);
        } else {
            // export all
            reports = Ext.getStore('reports').getData();
        }

        Ext.Array.each(reports.items, function (report) {
            var rep = report.getData();
            // remove extra custom fields
            delete rep._id;
            delete rep.localizedTitle;
            delete rep.localizedDescription;
            delete rep.slug;
            delete rep.categorySlug;
            delete rep.url;
            delete rep.icon;
            reportsArr.push(rep);
        });

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());
        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = 'AllReports'.t() + (category ? '_' + category.replace(/ /g, '_') : ''); // used in exported file name
        exportForm.gridData.value = Ext.encode(reportsArr);
        exportForm.submit();
        Ext.MessageBox.hide();
    }


});
