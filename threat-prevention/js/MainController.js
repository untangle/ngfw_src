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

        // set the enabled custom block page based on URL existance
        vm.bind('{settings.customBlockPageUrl}', function (url) {
            vm.set('settings.customBlockPageEnabled', url.length > 0);
        });

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'localNetworks'),
        ], this)
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result[0]);
            vm.set('localNetworks', result[1]);

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
        var lookupTarget = vm.get('threatLookupInfo.target');
        vm.set( 'threatLookupInfo.address', '');
        vm.set( 'threatLookupInfo.recentCount', '');
        vm.set( 'threatLookupInfo.level', 0);
        vm.set( 'threatLookupInfo.popularity', 0);
        vm.set( 'threatLookupInfo.history', '');
        vm.set( 'threatLookupInfo.country', '');
        vm.set( 'threatLookupInfo.age', 0);

        if(!lookupInput) {
            return;
        }

        // Don't perform lookup if local
        var local = false;
        vm.get('localNetworks').forEach( function(network){
            if(Util.ipMatchesNetwork(lookupInput, network['address'], network['netmask'])){
                local = true;
            }
        });

        vm.set( 'threatLookupInfo.local', local);
        if(local){
            return;
        }

        promises = [];
        // if(lookupInput.match(Ext.form.field.VTypes.mask.ip4AddrMaskRe) != null && lookupTarget == 'client'){
        if(lookupTarget == 'client'){
                promises.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'getIpInfo', [lookupInput]));
            promises.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'getIpHistory', [lookupInput]));
        }else{
            promises.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'getUrlInfo', [lookupInput]));
            promises.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", -1, 'getUrlHistory', [lookupInput]));
        }
        v.setLoading(true);
        Ext.Deferred.sequence(promises, this)
        .then(function(results){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            v.setLoading(false);
            var result = null;

            if(results[0] != null){
                result = results[0][0];
                vm.set('threatLookupInfo.level', result.reputation);
                vm.set('threatLookupInfo.address', result.hasOwnProperty('url') ? result.url : result.ip);
            }
            if( results[1] != null ){
                results[1].forEach(function(result){
                    if(result.hasOwnProperty('queries')) {
                        if(result.queries.hasOwnProperty('getrepinfo')) {
                            if(result.queries.getrepinfo.hasOwnProperty('popularity')){
                                vm.set('threatLookupInfo.popularity', result.queries.getrepinfo.popularity);
                            }
                            if(result.queries.getrepinfo.hasOwnProperty('age')){
                                vm.set('threatLookupInfo.age', result.queries.getrepinfo.age);
                            }
                            if(result.queries.getrepinfo.hasOwnProperty('country')){
                                vm.set('threatLookupInfo.country', result.queries.getrepinfo.country);
                            }
                            if(result.queries.getrepinfo.hasOwnProperty('threathistory')){
                                vm.set('threatLookupInfo.recentCount', result.queries.getrepinfo.threathistory);
                            }
                        }else if(result.queries.hasOwnProperty('geturlhistory')) {
                            //parse the geturlhistory or getiphistory data
                            //current category info
                            if(result.queries.geturlhistory.hasOwnProperty('current_categorization')) {
                                if(result.queries.geturlhistory.current_categorization.hasOwnProperty('categories')) {
                                    vm.set('threatLookupInfo.categories', result.queries.geturlhistory.current_categorization.categories);
                                }
                            }else if(result.queries.geturlhistory.hasOwnProperty('security_history')) {
                                //security history
                                vm.set('threatLookupInfo.history', result.queries.geturlhistory.security_history);
                            }
                        }
                    }
            });
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
