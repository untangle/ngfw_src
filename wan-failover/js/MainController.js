Ext.define('Ung.apps.wan-failover.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wan-failover',

    control: {
        '#': {
            beforerender: 'getSettings'
        },
        '#status': {
            activate: 'getWanStatus'
        },
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            var testList = result.tests.list;

            // convert all milliseconds to seconds after load
            for (var i = 0 ; i < testList.length;i++) {
                test = testList[i];
                test.timeoutMilliseconds = (test.timeoutMilliseconds / 1000);
                test.delayMilliseconds = (test.delayMilliseconds / 1000);
            }

            vm.set('settings', result);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        // convert all seconds to milliseconds before save
        var testStore = v.query('app-wan-failover-test-grid')[0].getStore();
        var tval,dval;

        testStore.each(function(record) {
            tval = record.get('timeoutMilliseconds');
            dval = record.get('delayMilliseconds');
            record.set('timeoutMilliseconds', tval * 1000);
            record.set('delayMilliseconds', dval * 1000);
        });

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getWanStatus: function (cmp) {
        var v = this.getView(), vm = this.getViewModel(),
            grid = cmp.down('grid[itemId=wanStatus]') || cmp.up('grid[itemId=wanStatus]');

        grid.setLoading(true);
        Rpc.asyncData(v.appManager, 'getWanStatus')
        .then(function(result){
            if(Util.isDestroyed(vm, grid)){
                return;
            }
            var i;
            var list = [];
            for (i=0;i<result.list.length;i++) {
                if (result.list[i].systemName != null)
                    list.push(result.list[i]);
            }
            vm.set('wanStatusData', list);

            var testList = vm.get('settings.tests.list');
            var wanWarnings = [];
            var testMap = {};
            var test;

            // first build a map of all enabled tests
            if (testList) for (i = 0;i < testList.length;i++) {
                test = testList[i];
                if (test.enabled) testMap[test.interfaceId] = true;
            }

            // now make sure each wan interface has an enabled test
            Ext.Array.each(list, function (wan) {
                if (!testMap[wan.interfaceId]) {
                    wanWarnings.push('<li>'  + Ext.String.format('Warning: Interface <i>{0}</i> needs a test configured!'.t(), wan.interfaceName) + '</li>');
                }
            });

            vm.set('wanWarnings', wanWarnings.join(''));
            grid.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(vm, grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    }
});

Ext.define('Ung.apps.wan-failover.SpecialController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-wan-failover-special',

    generateSuggestions: function(btn) {
        var parent = btn.up('#tests');
        var vm = parent.getViewModel();
        var faceCombo = parent.down("[fieldIndex='interfaceCombo']");
        var pingCombo = parent.down("[fieldIndex='pingCombo']");
        var form = btn.up('form');

        form.setLoading('Generating Suggestions...'.t() );
        Rpc.asyncData(btn.up('apppanel').appManager, 'getPingableHosts', faceCombo.getValue() )
        .then(function(result){
            if(Util.isDestroyed(form, pingCombo, vm)){
                return;
            }

            var pingData = [];
            for(var i = 0 ; i < result.list.length ; i++) {
                pingData.push([result.list[i],result.list[i]]);
            }
            vm.set('pingListData', pingData);
            pingCombo.getStore().loadData(pingData);
            pingCombo.select(pingData[0][0]);

            form.setLoading(false);
        }, function(ex){
            if(!Util.isDestroyed(form)){
                form.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    runWanTest: function(btn) {
        var record = btn.up('panel').ownerCt.record.data;

        Rpc.asyncData(btn.up('apppanel').appManager, 'runTest', record )
        .then(function(result){
            Ext.MessageBox.alert('Test Results'.t(), result.t());
        }, function(ex){
            Util.handleException(ex);
        });
    }

});
