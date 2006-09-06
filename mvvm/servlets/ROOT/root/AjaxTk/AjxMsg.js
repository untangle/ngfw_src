/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra AJAX Toolkit
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

function AjxMsg() { }

/* formats for calendar use */
AjxMsg["formatCalDate"] = "EEE, MMM d";
AjxMsg["formatCalDateLong"] = "EEEE, MMMM d";
AjxMsg["formatCalDateFull"] = "EEEE, MMMM d, yyyy";
AjxMsg["formatCalDay"] = "M/d";
AjxMsg["formatCalMonth"] = "MMMM yyyy";

/* additional formats for date formatting */
AjxMsg["formatDateMediumNoYear"] = "MMM dd";

/* date parsing information */
AjxMsg["dateParsing2DigitStartYear"] = "1936";

/* relative time keywords */
AjxMsg["ago"] = "ago";
AjxMsg["day"] = "day";
AjxMsg["days"] = "days";
AjxMsg["hour"] = "hour";
AjxMsg["hours"] = "hours";
AjxMsg["minute"] = "minute";
AjxMsg["minutes"] = "minutes";
AjxMsg["month"] = "month";
AjxMsg["months"] = "months";
AjxMsg["second"] = "second";
AjxMsg["seconds"] = "seconds";
AjxMsg["week"] = "week";
AjxMsg["weeks"] = "weeks";
AjxMsg["year"] = "year";
AjxMsg["years"] = "years";

AjxMsg["first"] = "first";
AjxMsg["second"] = "second";
AjxMsg["third"] = "third";
AjxMsg["fourth"] = "fourth";
AjxMsg["last"] = "last";

AjxMsg["yesterday"] = "yesterday";
AjxMsg["today"] = "today";
AjxMsg["tomorrow"] = "tomorrow";

AjxMsg["cancel"] = "Cancel";
AjxMsg["detail"] = "Detail";
AjxMsg["dismiss"] = "Dismiss";
AjxMsg["no"] = "No";
/* bug fix 3004 */
AjxMsg["noResults"] = "No results found.";
AjxMsg["ok"] = "OK";
AjxMsg["yes"] = "Yes";

AjxMsg["addAll"] = "Add All";
AjxMsg["add"] = "Add";
AjxMsg["remove"] = "Remove";
AjxMsg["removeAll"] = "Remove All";

AjxMsg["criticalMsg"] = "Critical";
AjxMsg["infoMsg"] = "Informational";
AjxMsg["warningMsg"] = "Warning";

AjxMsg["workInProgress"] = "Work In Progress";
AjxMsg["cancelRequest"] = "Cancel Request";

/* confirm dialog */
AjxMsg["confirmTitle"] = "Confirmation";

/* wizard dialog */
AjxMsg["_next"] = "Next";
AjxMsg["_prev"] = "Previous";
AjxMsg["_finish"] = "Finish";
AjxMsg["_close"] = "Close";

AjxMsg["weekdaySunShort"] = "S";
AjxMsg["weekdayMonShort"] = "M";
AjxMsg["weekdayTueShort"] = "T";
AjxMsg["weekdayWedShort"] = "W";
AjxMsg["weekdayThuShort"] = "T";
AjxMsg["weekdayFriShort"] = "F";
AjxMsg["weekdaySatShort"] = "S";

AjxMsg["monthJanShort"] = "J";
AjxMsg["monthFebShort"] = "F";
AjxMsg["monthMarShort"] = "M";
AjxMsg["monthAprShort"] = "A";
AjxMsg["monthMayShort"] = "M";
AjxMsg["monthJunShort"] = "J";
AjxMsg["monthJulShort"] = "J";
AjxMsg["monthAugShort"] = "A";
AjxMsg["monthSepShort"] = "S";
AjxMsg["monthOctShort"] = "O";
AjxMsg["monthNovShort"] = "N";
AjxMsg["monthDecShort"] = "D";

AjxMsg["valueIsRequired"] = "Value is required";
AjxMsg["notAString"] = "Value must be a text string.";
AjxMsg["stringLenWrong"] = "Value must be exactly {0} characters long.";
AjxMsg["stringTooShort"] = "Value must be at least {0} characters long.";
AjxMsg["stringTooLong"] = "Value must be no more than {0} characters long.";
AjxMsg["stringMismatch"] = "Value did not match valid values.";


AjxMsg["notANumber"] = "Value must be a number.";
AjxMsg["notAnInteger"] = "Value must be an integer.";
AjxMsg["numberTotalExceeded"] = "Whole digits exceeds total of {0}.";
AjxMsg["numberFractionExceeded"] = "Fractional digits exceeds total of {0}.";
AjxMsg["numberMoreThanMax"] = "Value must be less than or equal to {0}.";
AjxMsg["numberMoreThanEqualMax"] = "Value must be less than {0}.";
AjxMsg["numberLessThanMin"] = "Value must be greater than or equal to {0}.";
AjxMsg["numberLessThanEqualMin"] = "Value must be greater than {0}.";
AjxMsg["numberMustBeNon0Percent"] = "Value must be a percentage between 1 and 100";

/* Xforms versions of messages above. Will be replaced when Xforms moves to new property model */
AjxMsg["xFnumberMoreThanMax"] = "Value must be less than or equal to {0}.";
AjxMsg["XfnumberLessThanMin"] = "Value must be greater than or equal to {0}.";



AjxMsg["invalidDateString"] = 'Date value must be entered in the form: MM/DD/YYYY or "today", "yesterday" or "tomorrow".';
AjxMsg["invalidTimeString"] = "Time value must be entered in the form: HH:MM[:SS] [AM|PM]";
AjxMsg["invalidDatetimeString"] = "Date format not understood.";

AjxMsg["didNotMatchChoice"] = "Value '$0' did not match any values for this type.";

AjxMsg["xformRepeatAdd"] = "+";
AjxMsg["xformRepeatRemove"] = "-";
AjxMsg["xformDateTimeFormat"] = "{0,date} at {0,time}";
