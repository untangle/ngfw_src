i18n={
	//thousands_sep: ',',
	map: null,
	
	_: function (s) {
		if (typeof(this.map)!== null && this.map[s]) {
			return this.map[s];
		}
		return s;
	},
	
	pluralise: function (s, p, n) {
		if (n != 1) return _(p);
		return _(s);
	},
	
	sprintf: function (s) {
		var bits = s.split('%');
		var out = bits[0];
		var re = /^([ds])(.*)$/;
		for (var i=1; i<bits.length; i++) {
			p = re.exec(bits[i]);
			if (!p || arguments[i]==null) continue;
			if (p[1] == 'd') {
				out += parseInt(arguments[i], 10);
			} else if (p[1] == 's') {
				out += arguments[i];
			}
			out += p[2];
		}
		return out;
	}	
};
