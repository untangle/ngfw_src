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
            Rpc.asyncPromise(v.appManager, 'getReportInfo', 'localNetworks', null)
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

        if (!Util.validateForms(v)) {
            return;
        }

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
     */
    handleThreatLookup: function() {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel(),
            lookupInput = vm.get('threatLookupInfo.inputVal');

        // Clear current values.
        vm.set( 'threatLookupInfo.resultAddress', '');
        vm.set( 'threatLookupInfo.resultServerReputation', '');
        vm.set( 'threatLookupInfo.resultClientReputation', '');

        if(!lookupInput) {
            return;
        }

        var urlParts = lookupInput.match(Ext.form.field.VTypes.mask.urlAddrRe);

        // Don't perform lookup if local
        var local = false;
        vm.get('localNetworks').forEach( function(network){
            if(Util.ipMatchesNetwork(urlParts[3] ? urlParts[3] : urlParts[0], network['address'], network['netmask'])){
                local = true;
            }
        });

        if(urlParts[3] != undefined){
            // User specified url so pick the host.
            lookupInput = urlParts[3];
            if(urlParts[5] != undefined){
                // Path also specified
                lookupInput += urlParts[5];
            }
        }

        vm.set( 'threatLookupInfo.local', local);
        if(local){
            return;
        }

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getReportInfo', 'getIpInfo', [lookupInput]),
            Rpc.asyncPromise(v.appManager, 'getReportInfo', 'getUrlInfo', [lookupInput]),
            Rpc.asyncPromise(v.appManager, 'getReportInfo', 'getIpHistory', [lookupInput]),
            Rpc.asyncPromise(v.appManager, 'getReportInfo', 'getUrlHistory', [lookupInput])
        ], this)
        .then(function(results){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }
            v.setLoading(false);

            urlAddress = lookupInput;
            ipAddress = null;

            var ipResult = {};
            var ipOccurances = null;
            if(results[0] != null){
                // If path specified, we won't get an ip result
                ipResult = results[0][0];
                if(ipResult.hasOwnProperty('ip')){
                    ipAddress = ipResult.ip;
                }
                ipOccurances = results[2][0]['queries']['getrephistory']['history_count'];
                ipOccurances = (ipOccurances > 0) ? Ext.String.format('{0} occurrences', ipOccurances) : null;
            }

            var urlResult = {};
            if(results[1] != null){
                urlResult = results[1][0];
                if(urlResult.hasOwnProperty('url')){
                    urlAddress = urlResult.url;
                }
            }
            var urlOccurances = results[3][1]['queries']['getrepinfo']['threathistory'];
            urlOccurances = (urlOccurances > 0) ? Ext.String.format('{0} occurrences', urlOccurances) : null;

            if(ipAddress != null && ipAddress != urlAddress){
                vm.set( 'threatLookupInfo.resultAddress', Ext.String.format("{0} ({1})", urlAddress, ipAddress));
            }else{
                vm.set( 'threatLookupInfo.resultAddress', urlAddress);
            }
            if(urlResult.hasOwnProperty('reputation')){
                vm.set( 'threatLookupInfo.resultServerReputation', me.getReputationStatus(urlResult.reputation, urlOccurances));
            }
            if(ipResult.hasOwnProperty('reputation')){
                vm.set( 'threatLookupInfo.resultClientReputation', me.getReputationStatus(ipResult.reputation, ipOccurances));
            }

        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
            }
            Util.handleException(ex);
        });
    },

    /**
     * Get formatted reputation with description and detail
     * @param  reputation Integer value of reputation
     * @param  extra Extra values to show in parens after the reputation level.
     */
    getReputationStatus: function(reputation, extra){
        var reputationStatus = "";
        Ung.common.threatprevention.references.reputations.each(function(record){
            if(reputation >= record.get('rangeBegin') &&
               reputation <= record.get('rangeEnd')){
                if(extra){
                    reputationStatus = Ext.String.format("<b>{0}</b> ({1}) - {2}", record.get('description'), extra, record.get('details'));
                }else{
                    reputationStatus = Ext.String.format("<b>{0}</b> - {1}", record.get('description'), record.get('details'));
                }
            }
        });
        return reputationStatus;
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
