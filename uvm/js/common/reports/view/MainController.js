Ext.define('Ung.view.reports.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': {
            deactivate: 'resetView'
        }
    },

    listen: {
        global: {
            initialload: 'onInitialLoad'
        }
    },

    onInitialLoad: function () {
        var me = this, vm = me.getViewModel(),
            view = me.getView(),
            tree = me.lookup('tree');

        // me.getView().setLoading(false);
        // set the reports view context (ADMIN or REPORTS)
        vm.set('context', Ung.app.context);
        me.buildTablesStore();

        /**
         * When Reports App is installed/removed or enabled/disabled
         * show appropriate card
         */
        vm.bind('{reportsAppStatus}', function (status) {
            if(Util.isDestroyed(me, view)){
                return;
            }
            view.remove(view.down('#loader'));
            if (status.installed && status.enabled) {
                view.getLayout().setActiveItem(1);
                me.buildStats();
            } else {
                view.getLayout().setActiveItem(0);
            }
        });

        /**
         * While fetching report data show loading indicators
         */
        vm.bind('{fetching}', function (val) {
            if (!val) { Ext.MessageBox.hide(); } // hide any loading message box
        });


        /**
         * When query route changes select report path
         */
        vm.bind('{query.route}', function (route) {
            if(Util.isDestroyed(me)){
                return;
            }
            var path = (route.cat || '') + (route.rep ? ('/' + route.rep) : '');
            if (!path) {
                tree.collapseAll();
                tree.setSelection(null);
                me.showNode(tree.getStore().getRoot());
            }

            me.lookup('tree').selectPath(path, 'slug', '/', function (success, lastNode) {
                if (!success) {
                    if (!success) {
                        // the route do not match any cat/report selection
                        if (path !== '') { Ext.fireEvent('invalidquery'); }
                        return;
                    }
                }
                if (!lastNode.isLeaf()) {
                    lastNode.expand();
                }
            }, me);
        });

        /**
         * When conditions query string changes,
         * enable/disable reports tree nodes for which conditions do not apply
         */
        vm.bind('{query.conditions}', function (conditions) {
            if(Util.isDestroyed(me)){
                return;
            }
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

    /**
     * Redirects (updates route) based on selected reports tree node or breadcrumb node
     */
    onSelectReport: function (el, node) {
        var me = this, vm = me.getViewModel(), condsQuery = '';

        if (!node.get('url')) { return; }

        Ext.Array.each(vm.get('query.conditions'), function (c) {
            condsQuery += '&' + c.column + ':' + encodeURIComponent(c.operator) + ':' + encodeURIComponent(c.value) + ':' + (c.autoFormatValue === true ? 1 : 0);
        });

        if (Ung.app.context === 'REPORTS') {
            Ung.app.redirectTo(node.get('url') + condsQuery);
        } else {
            Ung.app.redirectTo('#reports?' + node.get('url') + condsQuery);
        }
        me.showNode(node);
    },

    /**
     * Adds table confinguration to the view model,
     * and it's used in editing report settings
     */
    buildTablesStore: function () {
        if (!rpc.reportsManager) { return; }
        var me = this, vm = me.getViewModel();
        Rpc.asyncData('rpc.reportsManager.getTables').then(function (result) {
            vm.set('tables', result);
        });
    },

    /**
     * Applies node selection to the entry
     */
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
     * Tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value) {
        return this.rendererRegExp ? value.replace(this.rendererRegExp, '<span style="font-weight: bold; background: #EEE; color: #000; border-bottom: 1px #000 solid;">$1</span>') : value;
    },

    /**
     * Filters the tree
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
     * Resets the view to an initial state
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
     * Builds statistics for categories, based on global conditions
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

    // get the entry controller and edit a new blank entry
    newReport: function () {
        var me = this, entryCtrl = me.getView().down('entry').getController();
        if (entryCtrl) {
            entryCtrl.editEntry(true); // true = new report entry
            // activate the report card if not yet activated
            me.lookup('cards').setActiveItem('report');
        }
    },

    // show import dialog on import
    newImport: function () {
        var me = this;
        var dialog = me.getView().add({
            xtype: 'importdialog'
        });
        dialog.show();
    },

    // exports categories
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
