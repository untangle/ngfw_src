Ext.define('Ung.apps.wan-balancer.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wan-balancer',

    control: {
        '#': {
            beforerender: 'getSettings'
        }
    },

    getSettings: function () {
        var me = this, vm = this.getViewModel();
        this.getView().appManager.getSettings(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
            vm.set('settings', result);
            me.afterGetSettings();
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            if (grid.listProperty != null) {
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
            }
        });

        me.setWeights();

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    afterGetSettings: function() {
        var interfaceWeightData = [];
        var destinationWanData = [];
        var trafficAllocation = "";
        var total = 0, i, intf, weight, intfCount;

        var vm = this.getViewModel();
        var weightArray = vm.get('settings.weights');
        var networkSettings = rpc.networkManager.getNetworkSettings();

        for (i = 0 ; i < networkSettings.interfaces.list.length ; i++) {
            intf = networkSettings.interfaces.list[i];
            if (intf.configType != 'ADDRESSED' || !intf.isWan ) continue;
            weight = weightArray[intf.interfaceId-1];
            interfaceWeightData.push({
                interfaceId: intf.interfaceId,
                name: intf.name,
                weight: weight,
                description: ""
            });
            total += weight;
        }

        trafficAllocation += "<TABLE WIDTH=600 BORDER=1 CELLSPACING=0 CELLPADDING=5 STYLE=border-collapse:collapse;>";

        intfCount = interfaceWeightData.length;
        for(i = 0 ; i < intfCount ; i++) {
            interfaceWeightData[i].description = this.getDescription(intfCount, total, interfaceWeightData[i].weight);
            var item = interfaceWeightData[i];
            trafficAllocation += "<TR><TD>" + Ext.String.format("{0} interface".t(), item.name) + "</TD><TD>" + item.description + "</TD></TR>";
        }

        trafficAllocation += "</TABLE>";

        destinationWanData.push([0, 'Balance'.t()]);

        for (var c = 0 ; c < networkSettings.interfaces.list.length ; c++) {
            intf = networkSettings.interfaces.list[c];
            var name = intf.name;
            var key = intf.interfaceId;
            if ( intf.configType == 'ADDRESSED' && intf.isWan) {
                destinationWanData.push( [ key, name ] );
            }
        }

        vm.set('interfaceWeightData', interfaceWeightData);
        vm.set('trafficAllocation', trafficAllocation);
        vm.set('destinationWanData', destinationWanData);
    },

    getDescription: function(intfCount, total, weight) {
        var percent = ( total == 0 ) ? Math.round(( 1 / intfCount ) * 1000) / 10 : Math.round(( weight / total ) * 1000) / 10;
        return Ext.String.format("{0}% of Internet traffic.".t(), percent);
    },

    setWeights: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        // JSON serializes out empty elements
        var weights = [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0 ];

        var iwd = vm.get('interfaceWeightData');

        iwd.forEach(function(record) {
            weights[record.interfaceId - 1] = record.weight;
        });

        vm.set('settings.weights', weights);
    }

});
