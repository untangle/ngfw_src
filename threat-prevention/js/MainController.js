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
        vm.set('threatList', "Match may contain one or more of the following categories:".t()+'<br>'+'<i>'+threatList.join(", ")+'</i>');
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
    },

    /**
     * showSliderInfo will display the Threat Temperature Gauge above the slider, as well as the label below the slider with the current threat description information.
     * @param {Ung.ThreatSlider} slider - The threat reputation slider object 
     */
    showSliderInfo: function(slider) {
        var me = this, vm = this.getViewModel(), view = this.getView();

        var sliderVal = slider.getValue();

        var rangeArguments = [slider.rangeTpl];
        var matchingThreats = [];
        Ung.common.threatprevention.references.reputations.each( function(threat, index){
            rangeArguments.push(threat.get('color'));
            if(sliderVal > 0 &&
                ( sliderVal >= threat.get('rangeBegin') ) ){
                matchingThreats.push(threat.get('description'));
            }
        });

        var vLabel = Ext.String.format(slider.labelTpl, matchingThreats.length == 0 ? "None" : matchingThreats.join(", ") ) +
        ( matchingThreats.length == 0 ? Ext.String.format(slider.labelNoneTpl) : '') +
        ( matchingThreats.length == Ung.common.threatprevention.references.reputations.getCount() ? Ext.String.format(slider.labelAllTpl) : '' );
        var vRange =  Ext.String.format.apply(null, rangeArguments);

        vm.set('threatMeter', vRange);
        vm.set('currentThreatDescription', vLabel);
    },
    handleThreatLookup: function() {
        var v = this.getView(), vm = this.getViewModel();
        var lookupInput = vm.get('threatLookupInput');
        if(!lookupInput) {return;}

        vm.set('threatLookupAddress', lookupInput);

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'threatLookup', lookupInput)
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            v.setLoading(false);

            for(var i in result) {

                vm.set('threatLookupReputationScore', result[i].reputation);
                var repLevel = Ung.common.threatprevention.references.getReputationLevel(result[i].reputation);
                if(repLevel) {
                    vm.set('threatLookupReputationLevel', repLevel.get('description') + " - " + repLevel.get('details'));
                }
  
                var currentCategories = [];
                for(var j in result[i].cats) {
                    var threatShortDesc = Renderer.webCategory(result[i].cats[j].catid);
                    currentCategories.push(result[i].cats[j].conf + "% Confidence : " + threatShortDesc);
                }
                
                vm.set('threatLookupCategory', currentCategories.join(", "));
            }
        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
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
    },
    /**
     * Override to support reverse slider.
     */
    calculateThumbPosition: function(v) {
        var me = this,
            minValue = me.minValue,
            maxValue = me.maxValue,
            pos = ((maxValue - v) / me.getRange() * 100);
        if (isNaN(pos)) {
            pos = 0;
        }
        return pos;
    },
    /**
     * Override to support reverse slider.
     */
    reversePixelValue : function(pos) {
        return this.maxValue - (pos / this.getRatio());
    },
    /**
     * Override to support reverse slider.
     */
    onKeyDown: function(e) {
        var me = this,
            ariaDom = me.ariaEl.dom,
            k, val;
        
        k = e.getKey();
 
        /*
         * The behaviour for keyboard handling with multiple thumbs is currently undefined.
         * There's no real sane default for it, so leave it like this until we come up
         * with a better way of doing it.
         */
        if (me.disabled || me.thumbs.length !== 1) {
            // Must not mingle with the Tab key!
            if (k !== e.TAB) {
                e.preventDefault();
            }
            
            return;
        }
 
        switch (k) {
            case e.UP:
            case e.RIGHT:
                val = e.ctrlKey ? me.minValue : me.getValue(0) - me.keyIncrement;
                break;
            
            case e.DOWN:
            case e.LEFT:
                val = e.ctrlKey ? me.maxValue : me.getValue(0) + me.keyIncrement;
                break;
            
            case e.HOME:
                val = me.minValue;
                break;
            
            case e.END:
                val = me.maxValue;
                break;
            
            case e.PAGE_UP:
                val = me.getValue(0) + me.pageSize;
                break;
            
            case e.PAGE_DOWN:
                val = me.getValue(0) - me.pageSize;
                break;
        }
        
        if (val !== undefined) {
            e.stopEvent();
            
            val = me.normalizeValue(val);
            
            me.setValue(0, val, undefined, true);
            
            if (ariaDom) {
                ariaDom.setAttribute('aria-valuenow', val);
            }
        }
    }
});
