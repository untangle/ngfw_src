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

    /**
     * handleThreatLookup is the click event handler to retrieve threat prevention data on a URL or IP Address from the getUrlHistory API
     * 
     */
    handleThreatLookup: function() {
        var v = this.getView(), vm = this.getViewModel();
        var lookupInput = vm.get('threatLookupInfo.inputVal');
        if(!lookupInput) {return;}

        v.setLoading(true);
        Ext.Deferred.sequence([Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'getUrlHistory', [lookupInput])], this)
        .then(function(result){           
            if(Util.isDestroyed(v, vm)){
                return;
            }
            v.setLoading(false);

            for(var i in result) {
                for(var j in result[i]) {
                    if(result[i][j].hasOwnProperty('queries')) {
                            //Parse the getrepinfo data
                            if(result[i][j].queries.hasOwnProperty('getrepinfo')) {

                                vm.set('threatLookupInfo.address', result[i][j].hasOwnProperty('url') ? result[i][j].url : result[i][j].ip);
                                vm.set('threatLookupInfo.score', result[i][j].queries.getrepinfo.reputation);
                                vm.set('threatLookupInfo.popularity', result[i][j].queries.getrepinfo.popularity);
                                vm.set('threatLookupInfo.age', result[i][j].queries.getrepinfo.age);
                                vm.set('threatLookupInfo.country', result[i][j].queries.getrepinfo.country);
                                vm.set('threatLookupInfo.level', result[i][j].queries.getrepinfo.reputation);
                                vm.set('threatLookupInfo.recentCount', result[i][j].queries.getrepinfo.threathistory);

                            }

                            //parse the geturlhistory or getiphistory data
                            if(result[i][j].queries.hasOwnProperty('geturlhistory')) {
                                //current category info
                                if(result[i][j].queries.geturlhistory.hasOwnProperty('current_categorization')) {
                                    if(result[i][j].queries.geturlhistory.current_categorization.hasOwnProperty('categories')) {
                                        vm.set('threatLookupInfo.categories', result[i][j].queries.geturlhistory.current_categorization.categories);
                                    }
                                }

                                //security history
                                if(result[i][j].queries.geturlhistory.hasOwnProperty('security_history')) {
                                    vm.set('threatLookupInfo.history', result[i][j].queries.geturlhistory.security_history);
                                }
                            }
                        }
                    }
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
