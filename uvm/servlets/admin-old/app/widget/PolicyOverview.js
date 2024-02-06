Ext.define('Ung.widget.PolicyOverview', {
    extend: 'Ext.container.Container',
    alias: 'widget.policyoverviewwidget',

    controller: 'widget',

    border: false,
    baseCls: 'widget',
    height: 300,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    viewModel: true,

    refreshIntervalSec: 30,

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        html: '<h1>' + 'Policy Overview'.t() + '</h1>' +
            '<div class="actions"><a class="action-btn"><i class="fa fa-rotate-left fa-lg" data-action="refresh"></i></a></div>'
    }, {
        xtype: 'treepanel',
        flex: 1,
        rootVisible: false,
        displayField: 'name',
        store: 'policiestree',
        header: false,
        border: false,
        animate: false,
        useArrows: true,
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        columns: [{
            xtype: 'treecolumn',
            text: 'Policy Name (id)'.t(),
            flex: 1,
            dataIndex: 'name',
            renderer: function (val, meta, rec) {
                return '<strong>' + rec.get('name') + '</strong> (' + rec.get('policyId') + ')';
            }
        }, {
            xtype: 'widgetcolumn',
            width: 120,
            text: 'Apps Installed'.t() + ',<br/>' + 'Running'.t() + ', ' + 'Inherited'.t(),
            widget: {
                xtype: 'appspicker',
                bind: {
                    apps: '{record.apps}'
                }
            }
        }, {
            width: 80,
            dataIndex: 'stats',
            align: 'right',
            text: 'Sessions'.t(),
            renderer: function(val) {
                return val ? val.sessionCount : '<span style="color: #CCC">0</span>';
            }
        }, {
            width: 80,
            dataIndex: 'stats',
            align: 'right',
            text: 'Traffic'.t() + '<br/>(KB/s)',
            renderer: function(val) {
                return (val && val.totalKbps > 0) ? Renderer.sessionSpeed(val.totalKbps) : '<span style="color: #CCC">0</span>';
            }
        }, {
            xtype: 'actioncolumn',
            iconCls: 'fa fa-external-link-square',
            align: 'center',
            tooltip: 'Go to Policy'.t(),
            width: 30,
            handler: function (view, rowIndex, colIndex, item, e, record) {
                Ung.app.redirectTo('#apps/' + record.get('policyId'));
            }
        }]
    }],

    fetchData: function (cb) {
        var me = this;
        me.down('treepanel').setLoading(true);
        // Ext.getStore('policiestree').build(function () {

        // get policies apps
        Rpc.asyncData('rpc.appManager.getAppsViews')
            .then(function (policies) {

                // get policies stats
                Rpc.asyncData('rpc.sessionMonitor.getPoliciesSessionsStats')
                    .then(function (stats) {

                        // build apps/stats
                        Ext.Array.each(policies, function (policy) {
                            var treePol = Ext.getStore('policiestree').findRecord('policyId', policy.policyId);
                            var apps = [];
                            Ext.Array.each(policy.appProperties.list, function (app) {
                                if (app.type !== 'FILTER') { return; }
                                var instance = Ext.Array.findBy(policy.instances.list, function(instance) { return instance.appName === app.name; });
                                var parentPolicy = null;
                                if (instance) {
                                    // installedAppsNo++;
                                    var inherited = false;
                                    if (instance.policyId && policy.policyId !== instance.policyId) {
                                        parentPolicy = Ext.getStore('policiestree').findRecord('policyId', instance.policyId).get('name');
                                        inherited = true;
                                    }
                                    instance.runState = policy.runStates.map[instance.id];
                                    apps.push({
                                        name: app.name,
                                        displayName: app.displayName,
                                        state: Ext.create('Ung.model.AppState',{instance: instance}),
                                        policyId: instance.policyId,
                                        parentPolicy: parentPolicy,
                                        inherited: inherited
                                    });
                                }
                            });
                            if(treePol){
                                treePol.set('apps', apps);
                                treePol.set('stats', stats[policy.policyId.toString()]);
                            }
                        });

                        Ext.getStore('policiestree').sync();
                        me.down('treepanel').setLoading(false);
                        cb();
                    });
            });
        // });
    }
});
