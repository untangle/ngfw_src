Ext.define('Ung.apps.threatprevention.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-threat-prevention',

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

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

        var threatList = [];
        Ung.common.threatprevention.references.threats.each( function(threat){
            threatList.push(threat.get('description'));
        });
        vm.set('threatList', "Match may be one or more of the following categories:".t()+'<br>'+'<i>'+threatList.join(", ")+'</i>');
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
        Ung.common.threatprevention.references.reputations.each( function(threat){
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
        var me = this, vm = this.getViewModel();

        var viewLabelComponent = this.up().down('[itemId='+this.viewLabel+']');
        var viewRangeComponent = this.up().down('[itemId='+this.rangeLabel+']');
        var matched = false;
        var rangeArguments = [slider.rangeTpl];
        Ung.common.threatprevention.references.reputations.each( function(threat){
            if(matched == false && newValue > 0){
                rangeArguments.push(threat.get('color'));
            }else{
                rangeArguments.push('dddddd');
            }
            if(newValue >= threat.get('rangeBegin') && newValue <= threat.get('rangeEnd')){
                matched = true;
            }
        });
        viewRangeComponent.setHtml( Ext.String.format.apply(null, rangeArguments) );
        me.updateDescription();
        me.up('panel').down('fieldset').items.each( 
            function(item){
                if(item.itemId != 'threatReputation'){
                    if(newValue == 0){
                        item.disable();
                    }else{
                        item.enable();
                    }
                }
            }
        );
    },

    updateDescription: function(){
        var me = this,
            vm = me.up('panel').getViewModel(),
            viewLabelComponent = this.up().down('[itemId='+this.viewLabel+']');
        if(vm == null){
            return;
        }
        var sliderValue = me.getValue();
        var actionValue = this.up('panel').down('[itemId=action]').getValue();
        var thresholdWarning = me.thresholdWarning;
        Ung.common.threatprevention.references.reputations.each( function(threat){
            if(sliderValue == 0){
                viewLabelComponent.setHtml('No matches'.t());
            }else{
                if(sliderValue >= threat.get('rangeBegin') && sliderValue <= threat.get('rangeEnd')){
                    viewLabelComponent.setHtml(
                        Ext.String.format(me.labelTpl, threat.get('description')) +
                        ( ( actionValue == 'block' && sliderValue >= thresholdWarning.maxBlockValue ) ? Ext.String.format(thresholdWarning.labelTpl) : '' ) +
                        ( ( actionValue == 'pass' && sliderValue <= thresholdWarning.minPassValue ) ? Ext.String.format(thresholdWarning.labelTpl) : '' )
                    );
                }
            }
        });
    }
});

Ext.define('Ung.ThreatComboAction', {
    extend: 'Ext.form.field.ComboBox',
    alias: 'widget.threatcomboaction',
    listeners:{
        change: function(combo, newValue, oldValue){
            var me = this;
            me.up('panel').down('threatslider').updateDescription();
        }
    }
});