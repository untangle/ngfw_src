
var PageName = 'Web Filter - Host Summary';
var PageId = 'p379e9f8f63f342838a36d82f8c8c02a1'
document.title = 'Web Filter - Host Summary';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u20 = document.getElementById('u20');
gv_vAlignTable['u20'] = 'top';
var u36 = document.getElementById('u36');

var u31 = document.getElementById('u31');

var u11 = document.getElementById('u11');
gv_vAlignTable['u11'] = 'top';
var u27 = document.getElementById('u27');
gv_vAlignTable['u27'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u4 = document.getElementById('u4');

var u2 = document.getElementById('u2');

var u10 = document.getElementById('u10');
gv_vAlignTable['u10'] = 'top';
var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u26 = document.getElementById('u26');
gv_vAlignTable['u26'] = 'top';
var u35 = document.getElementById('u35');

var u29 = document.getElementById('u29');

var u8 = document.getElementById('u8');
gv_vAlignTable['u8'] = 'top';
var u34 = document.getElementById('u34');

var u14 = document.getElementById('u14');
gv_vAlignTable['u14'] = 'top';
var u28 = document.getElementById('u28');

var u33 = document.getElementById('u33');

var u22 = document.getElementById('u22');

var u13 = document.getElementById('u13');

var u12 = document.getElementById('u12');
gv_vAlignTable['u12'] = 'top';
var u21 = document.getElementById('u21');
gv_vAlignTable['u21'] = 'top';
var u37 = document.getElementById('u37');

u37.style.cursor = 'pointer';
if (bIE) u37.attachEvent("onclick", Clicku37);
else u37.addEventListener("click", Clicku37, true);
function Clicku37(e)
{

if (true) {

	self.location.href="Web Filter - Host Incident.html" + GetQuerystring();

}

}
gv_vAlignTable['u37'] = 'top';
var u7 = document.getElementById('u7');
gv_vAlignTable['u7'] = 'top';
var u17 = document.getElementById('u17');

var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u15 = document.getElementById('u15');
gv_vAlignTable['u15'] = 'top';
var u3 = document.getElementById('u3');

var u1 = document.getElementById('u1');

var u25 = document.getElementById('u25');

var u16 = document.getElementById('u16');

var u19 = document.getElementById('u19');
gv_vAlignTable['u19'] = 'top';
var u9 = document.getElementById('u9');
gv_vAlignTable['u9'] = 'top';
var u30 = document.getElementById('u30');

var u24 = document.getElementById('u24');
gv_vAlignTable['u24'] = 'top';
var u18 = document.getElementById('u18');
gv_vAlignTable['u18'] = 'top';
var u32 = document.getElementById('u32');

var u23 = document.getElementById('u23');
gv_vAlignTable['u23'] = 'top';
if (window.OnLoad) OnLoad();
