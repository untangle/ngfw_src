Ext.define('Ung.apps.ipreputation.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ip-reputation',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);
            // console.log(result);

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

        console.log(vm.get('settings'));

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
    }

});

Ext.define('Ung.ThreatSlider', {
    extend: 'Ext.slider.Single',
    alias: 'widget.threatslider',
    useTips: true,
    tipText: function(thumb){
        var matchingThreat = null;
        Ung.common.ipreputation.references.reputations.each( function(threat){
            if(thumb.value >= threat.get('rangeBegin') && thumb.value <= threat.get('rangeEnd')){
                matchingThreat = threat;
            }
        });
        if(matchingThreat){
            return Ext.String.format(thumb.ownerCt.tipTpl, matchingThreat.get('description'), matchingThreat.get('details'));
        }
    },
    initComponent: function() {
        this.callParent();
        this.on({
            change: this.showValue
        });
    },
    showValue: function(slider, newValue){
        var viewLabelComponent = this.up().down('[itemId='+this.viewLabel+']');
        var viewRangeComponent = this.up().down('[itemId='+this.rangeLabel+']');
        var matched = false;
        var rangeArguments = [slider.rangeTpl];
        Ung.common.ipreputation.references.reputations.each( function(threat){
            if(matched == false && newValue > 0){
                rangeArguments.push(threat.get('color'));
            }else{
                rangeArguments.push('dddddd');
            }
            if(newValue == 0){
                viewLabelComponent.setHtml('No matches'.t());
            }else{
                if(newValue >= threat.get('rangeBegin') && newValue <= threat.get('rangeEnd')){
                    matched = true;
                    viewLabelComponent.setHtml(Ext.String.format(slider.labelTpl, threat.get('description')));
                }
            }
        });
        viewRangeComponent.setHtml( Ext.String.format.apply(null, rangeArguments) );
    }

});

Ext.define('Ung.Threats', {
    extend: 'Ext.form.CheckboxGroup',
    alias: 'widget.threats',

    constructor: function(config) {
        var me = this;
        var items = [];
        Ung.common.ipreputation.references.threats.each( function(threat){
            items.push({
                inputValue: threat.get('bit'),
                boxLabel: threat.get('description'),
                autoEl: {
                    tag: 'div',
                    // 'data-qtip': condition.storeTip(record.get(condition.storeValue), null, record)
                    'data-qtip': threat.get('details')
                }
            });
        });
        config.items = items;
        me.callParent(arguments);
    }
    // initComponent: function(){
    //     // Build list of checkboxes
    //     // 
    //     this.initialConfig.items = items;

    //     console.log(this);
    //     this.callParent();
    // }

});

