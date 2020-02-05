Ext.define('Ung.common.Converter.threatprevention', {
    singleton: true,

    ruleIdMap: {},
    ruleId: function(value) {
        console.log(this);
        if(value == 0){
            return;
        }
        var policyId = record && record.get && record.get('policy_id') ? record.get('policy_id') : 1;
        if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId] ||
            !Ung.common.Renderer.threatprevention.ruleIdMap[policyId] ||
            (value != Ung.common.Renderer.threatprevention.listKey && !(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId]))){
            if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId]){
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId] = {};
            }
            var ruleInfo = Renderer.getReportInfo(record, ["threat-prevention"], "rules");

            if(ruleInfo){
                ruleInfo.forEach( function(rule){
                    Ung.common.Renderer.threatprevention.ruleIdMap[policyId][rule["ruleId"]] = rule["description"];
                });
            }
            if(!(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId]) && (value != Ung.common.Renderer.threatprevention.listKey)){
                // If category cannot be found, don't just keep coming back for more.
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId][value] = 'Unknown'.t();
            }
        }
        if(value == Renderer.listKey){
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId];
        }else{
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId][value];
        }
    },

    categoryMap:{
        0: 'Spam Sources'.t(),
        1: 'Windows Exploits'.t(),
        2: 'Web Attacks'.t(),
        3: 'Botnets'.t(),
        4: 'Scanners'.t(),
        5: 'Denial of Service'.t(),
        6: 'Reputation'.t(),
        7: 'Phishing'.t(),
        8: 'Proxy'.t(),
        11: 'Mobile Threats'.t(),
        13: 'Tor Proxy'.t(),
        16: 'Keyloggers'.t(),
        17: 'Malware'.t(),
        18: 'Spyware'.t()
    },
    category: function(value){
        if (value == 0){
            return '';
        }
        var descriptions = [];
        var threatBits = Object.keys(Ung.common.Renderer.threatprevention.categoryMap);
        for(var i = 0; i < threatBits.length; i++){
            if(Math.pow(2,threatBits[i]) & value){
                descriptions.push(Ung.common.Renderer.threatprevention.categoryMap[threatBits[i]]);
            }
        }
        return descriptions.join(', ');
    },

    /**
     * converts the reputation value to readable string
     * @param {int} value - the reputation score
     */
    reputation: function(value) {
        return value ? Ung.common.threatprevention.references.getReputationLevel(value).get('description') : '';
    },
});
