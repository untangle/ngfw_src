
var PageName = 'Main Page';
var PageId = 'p11967de5847a41b5b22e542e39c81563'
document.title = 'Main Page';

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

var u62 = document.getElementById('u62');

u62.style.cursor = 'pointer';
if (bIE) u62.attachEvent("onclick", Clicku62);
else u62.addEventListener("click", Clicku62, true);
function Clicku62(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Protocol Control - User List.html" + GetQuerystring();

}

}

var u45 = document.getElementById('u45');
gv_vAlignTable['u45'] = 'top';
var u28 = document.getElementById('u28');

var u142 = document.getElementById('u142');

u142.style.cursor = 'pointer';
if (bIE) u142.attachEvent("onclick", Clicku142);
else u142.addEventListener("click", Clicku142, true);
function Clicku142(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Configuration Backup - Global Incident.html" + GetQuerystring();

}

}

var u53 = document.getElementById('u53');
gv_vAlignTable['u53'] = 'top';
var u59 = document.getElementById('u59');
gv_vAlignTable['u59'] = 'top';
var u71 = document.getElementById('u71');
gv_vAlignTable['u71'] = 'top';
var u104 = document.getElementById('u104');

var u93 = document.getElementById('u93');
gv_vAlignTable['u93'] = 'top';
var u12 = document.getElementById('u12');

var u73 = document.getElementById('u73');
gv_vAlignTable['u73'] = 'top';
var u150 = document.getElementById('u150');

var u128 = document.getElementById('u128');

u128.style.cursor = 'pointer';
if (bIE) u128.attachEvent("onclick", Clicku128);
else u128.addEventListener("click", Clicku128, true);
function Clicku128(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Email List.html" + GetQuerystring();

}

}

var u56 = document.getElementById('u56');

var u91 = document.getElementById('u91');
gv_vAlignTable['u91'] = 'top';
var u173 = document.getElementById('u173');
gv_vAlignTable['u173'] = 'top';
var u90 = document.getElementById('u90');

u90.style.cursor = 'pointer';
if (bIE) u90.attachEvent("onclick", Clicku90);
else u90.addEventListener("click", Clicku90, true);
function Clicku90(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Phish Blocker - Email List.html" + GetQuerystring();

}

}

var u40 = document.getElementById('u40');

var u125 = document.getElementById('u125');

var u23 = document.getElementById('u23');
gv_vAlignTable['u23'] = 'top';
var u147 = document.getElementById('u147');
gv_vAlignTable['u147'] = 'top';
var u132 = document.getElementById('u132');

u132.style.cursor = 'pointer';
if (bIE) u132.attachEvent("onclick", Clicku132);
else u132.addEventListener("click", Clicku132, true);
function Clicku132(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Global Incident.html" + GetQuerystring();

}

}

var u88 = document.getElementById('u88');

u88.style.cursor = 'pointer';
if (bIE) u88.attachEvent("onclick", Clicku88);
else u88.addEventListener("click", Clicku88, true);
function Clicku88(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Phish Blocker - Summary.html" + GetQuerystring();

}

}

var u130 = document.getElementById('u130');

u130.style.cursor = 'pointer';
if (bIE) u130.attachEvent("onclick", Clicku130);
else u130.addEventListener("click", Clicku130, true);
function Clicku130(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Host List.html" + GetQuerystring();

}

}

var u108 = document.getElementById('u108');

u108.style.cursor = 'pointer';
if (bIE) u108.attachEvent("onclick", Clicku108);
else u108.addEventListener("click", Clicku108, true);
function Clicku108(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="AD Connector - Global Incident.html" + GetQuerystring();

}

}

var u54 = document.getElementById('u54');

u54.style.cursor = 'pointer';
if (bIE) u54.attachEvent("onclick", Clicku54);
else u54.addEventListener("click", Clicku54, true);
function Clicku54(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spam Blocker - Global Incident.html" + GetQuerystring();

}

}

var u100 = document.getElementById('u100');

u100.style.cursor = 'pointer';
if (bIE) u100.attachEvent("onclick", Clicku100);
else u100.addEventListener("click", Clicku100, true);
function Clicku100(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Intrusion Prevention - Summary.html" + GetQuerystring();

}

}

var u37 = document.getElementById('u37');
gv_vAlignTable['u37'] = 'top';
var u3 = document.getElementById('u3');
gv_vAlignTable['u3'] = 'top';
var u105 = document.getElementById('u105');

var u68 = document.getElementById('u68');

u68.style.cursor = 'pointer';
if (bIE) u68.attachEvent("onclick", Clicku68);
else u68.addEventListener("click", Clicku68, true);
function Clicku68(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Summary.html" + GetQuerystring();

}

}

var u159 = document.getElementById('u159');
gv_vAlignTable['u159'] = 'top';
var u21 = document.getElementById('u21');
gv_vAlignTable['u21'] = 'top';
var u113 = document.getElementById('u113');
gv_vAlignTable['u113'] = 'top';
var u129 = document.getElementById('u129');
gv_vAlignTable['u129'] = 'top';
var u156 = document.getElementById('u156');

u156.style.cursor = 'pointer';
if (bIE) u156.attachEvent("onclick", Clicku156);
else u156.addEventListener("click", Clicku156, true);
function Clicku156(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Web Filter - User List.html" + GetQuerystring();

}

}

var u35 = document.getElementById('u35');
gv_vAlignTable['u35'] = 'top';
var u18 = document.getElementById('u18');

var u126 = document.getElementById('u126');

u126.style.cursor = 'pointer';
if (bIE) u126.attachEvent("onclick", Clicku126);
else u126.addEventListener("click", Clicku126, true);
function Clicku126(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Summary.html" + GetQuerystring();

}

}

var u49 = document.getElementById('u49');

var u131 = document.getElementById('u131');
gv_vAlignTable['u131'] = 'top';
var u109 = document.getElementById('u109');
gv_vAlignTable['u109'] = 'top';
var u97 = document.getElementById('u97');
gv_vAlignTable['u97'] = 'top';
var u63 = document.getElementById('u63');
gv_vAlignTable['u63'] = 'top';
var u84 = document.getElementById('u84');

u84.style.cursor = 'pointer';
if (bIE) u84.attachEvent("onclick", Clicku84);
else u84.addEventListener("click", Clicku84, true);
function Clicku84(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Firewall - Global Incident.html" + GetQuerystring();

}

}

var u106 = document.getElementById('u106');

u106.style.cursor = 'pointer';
if (bIE) u106.attachEvent("onclick", Clicku106);
else u106.addEventListener("click", Clicku106, true);
function Clicku106(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="AD Connector - Summary.html" + GetQuerystring();

}

}

var u9 = document.getElementById('u9');

var u46 = document.getElementById('u46');

var u81 = document.getElementById('u81');
gv_vAlignTable['u81'] = 'top';
var u144 = document.getElementById('u144');

var u172 = document.getElementById('u172');

u172.style.cursor = 'pointer';
if (bIE) u172.attachEvent("onclick", Clicku172);
else u172.addEventListener("click", Clicku172, true);
function Clicku172(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Host List.html" + GetQuerystring();

}

}

var u151 = document.getElementById('u151');

var u30 = document.getElementById('u30');

var u77 = document.getElementById('u77');

var u152 = document.getElementById('u152');

u152.style.cursor = 'pointer';
if (bIE) u152.attachEvent("onclick", Clicku152);
else u152.addEventListener("click", Clicku152, true);
function Clicku152(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Web Filter - Summary.html" + GetQuerystring();

}

}

var u168 = document.getElementById('u168');

u168.style.cursor = 'pointer';
if (bIE) u168.attachEvent("onclick", Clicku168);
else u168.addEventListener("click", Clicku168, true);
function Clicku168(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Administration - Global Incident.html" + GetQuerystring();

}

}

var u7 = document.getElementById('u7');
gv_vAlignTable['u7'] = 'top';
var u13 = document.getElementById('u13');
gv_vAlignTable['u13'] = 'top';
var u95 = document.getElementById('u95');
gv_vAlignTable['u95'] = 'top';
var u61 = document.getElementById('u61');
gv_vAlignTable['u61'] = 'top';
var u137 = document.getElementById('u137');
gv_vAlignTable['u137'] = 'top';
var u123 = document.getElementById('u123');
gv_vAlignTable['u123'] = 'top';
var u44 = document.getElementById('u44');

var u165 = document.getElementById('u165');
gv_vAlignTable['u165'] = 'top';
var u27 = document.getElementById('u27');
gv_vAlignTable['u27'] = 'top';
var u4 = document.getElementById('u4');
gv_vAlignTable['u4'] = 'top';
var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'top';
var u75 = document.getElementById('u75');
gv_vAlignTable['u75'] = 'top';
var u170 = document.getElementById('u170');

var u148 = document.getElementById('u148');

u148.style.cursor = 'pointer';
if (bIE) u148.attachEvent("onclick", Clicku148);
else u148.addEventListener("click", Clicku148, true);
function Clicku148(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="OpenVPN - Global Incident.html" + GetQuerystring();

}

}

var u11 = document.getElementById('u11');
gv_vAlignTable['u11'] = 'top';
var u58 = document.getElementById('u58');

u58.style.cursor = 'pointer';
if (bIE) u58.attachEvent("onclick", Clicku58);
else u58.addEventListener("click", Clicku58, true);
function Clicku58(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Protocol Control - Summary.html" + GetQuerystring();

}

}

var u140 = document.getElementById('u140');

var u42 = document.getElementById('u42');

var u145 = document.getElementById('u145');

var u103 = document.getElementById('u103');
gv_vAlignTable['u103'] = 'top';
var u25 = document.getElementById('u25');
gv_vAlignTable['u25'] = 'top';
var u153 = document.getElementById('u153');
gv_vAlignTable['u153'] = 'top';
var u169 = document.getElementById('u169');
gv_vAlignTable['u169'] = 'top';
var u127 = document.getElementById('u127');
gv_vAlignTable['u127'] = 'top';
var u160 = document.getElementById('u160');

var u39 = document.getElementById('u39');
gv_vAlignTable['u39'] = 'top';
var u70 = document.getElementById('u70');

u70.style.cursor = 'pointer';
if (bIE) u70.attachEvent("onclick", Clicku70);
else u70.addEventListener("click", Clicku70, true);
function Clicku70(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Email List.html" + GetQuerystring();

}

}

var u120 = document.getElementById('u120');

u120.style.cursor = 'pointer';
if (bIE) u120.attachEvent("onclick", Clicku120);
else u120.addEventListener("click", Clicku120, true);
function Clicku120(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Attack Blocker - Summary.html" + GetQuerystring();

}

}

var u87 = document.getElementById('u87');

var u166 = document.getElementById('u166');

u166.style.cursor = 'pointer';
if (bIE) u166.attachEvent("onclick", Clicku166);
else u166.addEventListener("click", Clicku166, true);
function Clicku166(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Platform-Server.html" + GetQuerystring();

}

}

var u8 = document.getElementById('u8');

var u36 = document.getElementById('u36');

var u171 = document.getElementById('u171');

var u149 = document.getElementById('u149');
gv_vAlignTable['u149'] = 'top';
var u20 = document.getElementById('u20');

var u67 = document.getElementById('u67');

var u52 = document.getElementById('u52');

u52.style.cursor = 'pointer';
if (bIE) u52.attachEvent("onclick", Clicku52);
else u52.addEventListener("click", Clicku52, true);
function Clicku52(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spam Blocker - Email List.html" + GetQuerystring();

}

}

var u141 = document.getElementById('u141');

var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u85 = document.getElementById('u85');
gv_vAlignTable['u85'] = 'top';
var u146 = document.getElementById('u146');

u146.style.cursor = 'pointer';
if (bIE) u146.attachEvent("onclick", Clicku146);
else u146.addEventListener("click", Clicku146, true);
function Clicku146(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="OpenVPN - Summary.html" + GetQuerystring();

}

}

var u34 = document.getElementById('u34');

var u154 = document.getElementById('u154');

u154.style.cursor = 'pointer';
if (bIE) u154.attachEvent("onclick", Clicku154);
else u154.addEventListener("click", Clicku154, true);
function Clicku154(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Web Filter - Host List.html" + GetQuerystring();

}

}

var u99 = document.getElementById('u99');

var u17 = document.getElementById('u17');
gv_vAlignTable['u17'] = 'top';
var u1 = document.getElementById('u1');

var u65 = document.getElementById('u65');
gv_vAlignTable['u65'] = 'top';
var u48 = document.getElementById('u48');

var u121 = document.getElementById('u121');
gv_vAlignTable['u121'] = 'top';
var u96 = document.getElementById('u96');

u96.style.cursor = 'pointer';
if (bIE) u96.attachEvent("onclick", Clicku96);
else u96.addEventListener("click", Clicku96, true);
function Clicku96(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Phish Blocker - Global Web Incident.html" + GetQuerystring();

}

}

var u79 = document.getElementById('u79');
gv_vAlignTable['u79'] = 'top';
var u32 = document.getElementById('u32');

var u80 = document.getElementById('u80');

u80.style.cursor = 'pointer';
if (bIE) u80.attachEvent("onclick", Clicku80);
else u80.addEventListener("click", Clicku80, true);
function Clicku80(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Firewall - Host List.html" + GetQuerystring();

}

}

var u134 = document.getElementById('u134');

var u51 = document.getElementById('u51');
gv_vAlignTable['u51'] = 'top';
var u143 = document.getElementById('u143');
gv_vAlignTable['u143'] = 'top';
var u76 = document.getElementById('u76');

var u155 = document.getElementById('u155');
gv_vAlignTable['u155'] = 'top';
var u158 = document.getElementById('u158');

u158.style.cursor = 'pointer';
if (bIE) u158.attachEvent("onclick", Clicku158);
else u158.addEventListener("click", Clicku158, true);
function Clicku158(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Web Filter - Global Incident.html" + GetQuerystring();

}

}

var u101 = document.getElementById('u101');
gv_vAlignTable['u101'] = 'top';
var u94 = document.getElementById('u94');

u94.style.cursor = 'pointer';
if (bIE) u94.attachEvent("onclick", Clicku94);
else u94.addEventListener("click", Clicku94, true);
function Clicku94(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Phish Blocker - Global Incident.html" + GetQuerystring();

}

}

var u60 = document.getElementById('u60');

u60.style.cursor = 'pointer';
if (bIE) u60.attachEvent("onclick", Clicku60);
else u60.addEventListener("click", Clicku60, true);
function Clicku60(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Protocol Control - Host List.html" + GetQuerystring();

}

}

var u117 = document.getElementById('u117');
gv_vAlignTable['u117'] = 'top';
var u43 = document.getElementById('u43');
gv_vAlignTable['u43'] = 'top';
var u114 = document.getElementById('u114');

u114.style.cursor = 'pointer';
if (bIE) u114.attachEvent("onclick", Clicku114);
else u114.addEventListener("click", Clicku114, true);
function Clicku114(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spyware Blocker - Host List.html" + GetQuerystring();

}

}

var u112 = document.getElementById('u112');

u112.style.cursor = 'pointer';
if (bIE) u112.attachEvent("onclick", Clicku112);
else u112.addEventListener("click", Clicku112, true);
function Clicku112(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spyware Blocker - Summary.html" + GetQuerystring();

}

}

var u174 = document.getElementById('u174');

u174.style.cursor = 'pointer';
if (bIE) u174.attachEvent("onclick", Clicku174);
else u174.addEventListener("click", Clicku174, true);
function Clicku174(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="User List.html" + GetQuerystring();

}

}

var u26 = document.getElementById('u26');

var u124 = document.getElementById('u124');

var u74 = document.getElementById('u74');

u74.style.cursor = 'pointer';
if (bIE) u74.attachEvent("onclick", Clicku74);
else u74.addEventListener("click", Clicku74, true);
function Clicku74(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Global Incident.html" + GetQuerystring();

}

}

var u122 = document.getElementById('u122');

u122.style.cursor = 'pointer';
if (bIE) u122.attachEvent("onclick", Clicku122);
else u122.addEventListener("click", Clicku122, true);
function Clicku122(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Attack Blocker - Global Incident.html" + GetQuerystring();

}

}

var u138 = document.getElementById('u138');

u138.style.cursor = 'pointer';
if (bIE) u138.attachEvent("onclick", Clicku138);
else u138.addEventListener("click", Clicku138, true);
function Clicku138(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Remote Access Portal - Global Incident.html" + GetQuerystring();

}

}

var u10 = document.getElementById('u10');

var u57 = document.getElementById('u57');

var u163 = document.getElementById('u163');
gv_vAlignTable['u163'] = 'top';
var u92 = document.getElementById('u92');

u92.style.cursor = 'pointer';
if (bIE) u92.attachEvent("onclick", Clicku92);
else u92.addEventListener("click", Clicku92, true);
function Clicku92(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Phish Blocker - Host List.html" + GetQuerystring();

}

}

var u5 = document.getElementById('u5');

var u41 = document.getElementById('u41');
gv_vAlignTable['u41'] = 'top';
var u135 = document.getElementById('u135');

var u24 = document.getElementById('u24');

var u89 = document.getElementById('u89');
gv_vAlignTable['u89'] = 'top';
var u102 = document.getElementById('u102');

u102.style.cursor = 'pointer';
if (bIE) u102.attachEvent("onclick", Clicku102);
else u102.addEventListener("click", Clicku102, true);
function Clicku102(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Intrusion Prevention - Global Incident.html" + GetQuerystring();

}

}

var u167 = document.getElementById('u167');
gv_vAlignTable['u167'] = 'top';
var u118 = document.getElementById('u118');

var u0 = document.getElementById('u0');

var u55 = document.getElementById('u55');
gv_vAlignTable['u55'] = 'top';
var u38 = document.getElementById('u38');

var u83 = document.getElementById('u83');
gv_vAlignTable['u83'] = 'top';
var u110 = document.getElementById('u110');

var u50 = document.getElementById('u50');

u50.style.cursor = 'pointer';
if (bIE) u50.attachEvent("onclick", Clicku50);
else u50.addEventListener("click", Clicku50, true);
function Clicku50(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spam Blocker - Summary.html" + GetQuerystring();

}

}

var u86 = document.getElementById('u86');

var u72 = document.getElementById('u72');

u72.style.cursor = 'pointer';
if (bIE) u72.attachEvent("onclick", Clicku72);
else u72.addEventListener("click", Clicku72, true);
function Clicku72(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Virus Blocker - Host List.html" + GetQuerystring();

}

}

var u115 = document.getElementById('u115');
gv_vAlignTable['u115'] = 'top';
var u69 = document.getElementById('u69');
gv_vAlignTable['u69'] = 'top';
var u22 = document.getElementById('u22');

var u161 = document.getElementById('u161');

var u139 = document.getElementById('u139');
gv_vAlignTable['u139'] = 'top';
var u177 = document.getElementById('u177');
gv_vAlignTable['u177'] = 'top';
var u66 = document.getElementById('u66');

var u133 = document.getElementById('u133');
gv_vAlignTable['u133'] = 'top';
var u19 = document.getElementById('u19');

var u136 = document.getElementById('u136');

u136.style.cursor = 'pointer';
if (bIE) u136.attachEvent("onclick", Clicku136);
else u136.addEventListener("click", Clicku136, true);
function Clicku136(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Remote Access Portal - Summary.html" + GetQuerystring();

}

}

var u157 = document.getElementById('u157');
gv_vAlignTable['u157'] = 'top';
var u15 = document.getElementById('u15');
gv_vAlignTable['u15'] = 'top';
var u33 = document.getElementById('u33');
gv_vAlignTable['u33'] = 'top';
var u107 = document.getElementById('u107');
gv_vAlignTable['u107'] = 'top';
var u98 = document.getElementById('u98');

var u16 = document.getElementById('u16');

var u119 = document.getElementById('u119');

var u64 = document.getElementById('u64');

u64.style.cursor = 'pointer';
if (bIE) u64.attachEvent("onclick", Clicku64);
else u64.addEventListener("click", Clicku64, true);
function Clicku64(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Protocol Control - Global Incident.html" + GetQuerystring();

}

}

var u164 = document.getElementById('u164');

u164.style.cursor = 'pointer';
if (bIE) u164.attachEvent("onclick", Clicku164);
else u164.addEventListener("click", Clicku164, true);
function Clicku164(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Platform-Traffic.html" + GetQuerystring();

}

}

var u111 = document.getElementById('u111');

var u176 = document.getElementById('u176');

u176.style.cursor = 'pointer';
if (bIE) u176.attachEvent("onclick", Clicku176);
else u176.addEventListener("click", Clicku176, true);
function Clicku176(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Email List.html" + GetQuerystring();

}

}

var u47 = document.getElementById('u47');
gv_vAlignTable['u47'] = 'top';
var u175 = document.getElementById('u175');
gv_vAlignTable['u175'] = 'top';
var u82 = document.getElementById('u82');

u82.style.cursor = 'pointer';
if (bIE) u82.attachEvent("onclick", Clicku82);
else u82.addEventListener("click", Clicku82, true);
function Clicku82(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Firewall - User List.html" + GetQuerystring();

}

}

var u116 = document.getElementById('u116');

u116.style.cursor = 'pointer';
if (bIE) u116.attachEvent("onclick", Clicku116);
else u116.addEventListener("click", Clicku116, true);
function Clicku116(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Spyware Blocker - Global Incident.html" + GetQuerystring();

}

}

var u78 = document.getElementById('u78');

u78.style.cursor = 'pointer';
if (bIE) u78.attachEvent("onclick", Clicku78);
else u78.addEventListener("click", Clicku78, true);
function Clicku78(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Firewall - Summary.html" + GetQuerystring();

}

}

var u31 = document.getElementById('u31');
gv_vAlignTable['u31'] = 'top';
var u162 = document.getElementById('u162');

u162.style.cursor = 'pointer';
if (bIE) u162.attachEvent("onclick", Clicku162);
else u162.addEventListener("click", Clicku162, true);
function Clicku162(e)
{

if (true) {

	objIframe = document.getElementById("u0");
	objIframe.src="Platform - Summary.html" + GetQuerystring();

}

}

var u29 = document.getElementById('u29');
gv_vAlignTable['u29'] = 'top';
var u178 = document.getElementById('u178');
gv_vAlignTable['u178'] = 'top';
var u14 = document.getElementById('u14');

if (window.OnLoad) OnLoad();
