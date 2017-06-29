Ext.define('Ung.apps.wanfailover.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wan-failover',

    control: {
        '#': {
            beforerender: 'getSettings'
        },
        '#wanStatus': {
            afterrender: 'getWanStatus'
        },
        '#tests': {
            afterrender: 'getWanStatus'
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        this.getView().appManager.getSettings(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            console.log(result);

            var testList = result.tests.list;

            // convert all milliseconds to seconds after load
            for (var i = 0 ; i < testList.length;i++) {
                test = testList[i];
                test.timeoutMilliseconds = (test.timeoutMilliseconds / 1000);
                test.delayMilliseconds = (test.delayMilliseconds / 1000);
            }

            vm.set('settings', result);
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
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    getWanStatus: function (cmp) {
        var vm = this.getViewModel();
        var grid;

        if (cmp) grid = (cmp.getXType() === 'gridpanel') ? cmp : cmp.up('grid');
        if (grid) grid.setLoading(true);

        this.getView().appManager.getWanStatus(function (result, ex) {
            if (grid) grid.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set('wanStatusData', result.list);

            var testList = vm.get('settings.tests.list');
            var wanWarnings = [];
            var testMap = {};
            var test;

            // first build a map of all enabled tests
            if (testList) for (var i = 0 ; i < testList.length;i++) {
                test = testList[i];
                if (test.enabled) testMap[test.interfaceId] = true;
            }

            // now make sure each wan interface has an enabled test
            Ext.Array.each(result.list, function (wan) {
                if (!testMap[wan.interfaceId]) {
                    wanWarnings.push('<li>'  + Ext.String.format('Warning: Interface <i>{0}</i> needs a test configured!'.t(), wan.interfaceName) + '</li>');
                }
            });

            vm.set('wanWarnings', wanWarnings.join('<br/>'));
        });
    }
});

Ext.define('Ung.apps.wanfailover.SpecialController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-wanfailover-special',

    generateSuggestions: function(btn) {
        var parent = btn.up('#tests');
        var vm = parent.getViewModel();
        var faceCombo = parent.down("[fieldIndex='interfaceCombo']");
        var pingCombo = parent.down("[fieldIndex='pingCombo']");

        var wanApp = rpc.appManager.app('wan-failover');
        wanApp.getPingableHosts(Ext.bind(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            var pingData = [];
            for(var i = 0 ; i < result.list.length ; i++) {
                pingData.push([result.list[i],result.list[i]]);
            }
            vm.set('pingListData', pingData);
            pingCombo.getStore().loadData(pingData);
            pingCombo.select(pingData[0][0]);
        }, this), faceCombo.getValue());
    },

    runWanTest: function(btn) {
        var record = btn.up('panel').ownerCt.record.data;
        var wanApp = rpc.appManager.app('wan-failover');
        wanApp.runTest(Ext.bind(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }
            Ext.MessageBox.alert('Test Results'.t(), result.t());
        }, this), record);
    }

});
