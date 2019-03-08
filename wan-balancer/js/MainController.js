Ext.define('Ung.apps.wan-balancer.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wan-balancer',

    control: {
        '#': {
            beforerender: 'getSettings'
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings')
        ])
        .then( function(result){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }

            vm.set('settings', result[0]);
            me.calculateNetwork(result[1]);

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

    calculateNetwork: function( networkSettings ) {
        var interfaceWeightData = [];
        var destinationWanData = [];
        var trafficAllocation = "";
        var total = 0, i, intf, weight, intfCount;

        var vm = this.getViewModel();
        var weightArray = vm.get('settings.weights');

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
        var vm = this.getViewModel();

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
    },

    statics: {
        destinationWanRenderer: function(value){
            var wanlist = this.getViewModel().get('destinationWanList');
            var dstname = 'Unknown'.t();
            wanlist.each(function(record) { if (record.get('index') == value) dstname = record.get('name'); });
            return(dstname);
        }
    }

});
