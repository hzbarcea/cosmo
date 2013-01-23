/*
 * Copyright 2006 Open Source Applications Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

dojo.provide("cosmo.datetime.tests.timezone");
dojo.require("cosmo.tests.jum");
dojo.require("cosmo.datetime");
dojo.require("cosmo.datetime.Date");
dojo.require("cosmo.datetime.timezone");
dojo.require("cosmo.datetime.timezone.LazyCachingTimezoneRegistry");
dojo.require("cosmo.env");
//Initialization.
//TODO - once Dojo implements setUp() and tearDown() move this code there.

(function(){
var registry = new cosmo.datetime.timezone.LazyCachingTimezoneRegistry(cosmo.env.getBaseUrl() + "/js/olson-tzdata/");
cosmo.datetime.timezone.setTimezoneRegistry(registry);

function getNyTz(){
    var timezone = cosmo.datetime.timezone.getTimezone("America/New_York");
    return timezone;
}

function getUsRs(){
    var rs = cosmo.datetime.timezone.getRuleSet("US");
    return rs;
}

doh.register("cosmo.datetime.test.timezone", [
                 function getTimezone(){
                     var timezone = getNyTz();
                     jum.assertTrue(timezone != null);
                 },

                 function getDateField(){
                     var getDateField = cosmo.datetime.timezone._getDateField;
                     var cosmoDate = new cosmo.datetime.Date(2006, 11, 10, 12, 33, 30);
                     jum.assertEquals(2006, getDateField(cosmoDate, "year"));
                     jum.assertEquals(11, getDateField(cosmoDate, "month"));
                     jum.assertEquals(10, getDateField(cosmoDate, "date"));
                     jum.assertEquals(12, getDateField(cosmoDate, "hours"));
                     jum.assertEquals(33, getDateField(cosmoDate, "minutes"));
                     jum.assertEquals(30, getDateField(cosmoDate, "seconds"));

                     var jsDate = new Date(2006, 11, 10, 12, 33, 30);
                     jum.assertEquals(2006, getDateField(jsDate, "year"));
                     jum.assertEquals(11, getDateField(jsDate, "month"));
                     jum.assertEquals(10, getDateField(jsDate, "date"));
                     jum.assertEquals(12, getDateField(jsDate, "hours"));
                     jum.assertEquals(33, getDateField(jsDate, "minutes"));
                     jum.assertEquals(30, getDateField(jsDate, "seconds"));

                     var fullHashDate = { year: 2006,
                                          month: 11,
                                          date: 10,
                                          hours: 12,
                                          minutes: 33,
                                          seconds: 30};

                     jum.assertEquals(2006, getDateField(fullHashDate, "year"));
                     jum.assertEquals(11, getDateField(fullHashDate, "month"));
                     jum.assertEquals(10, getDateField(fullHashDate, "date"));
                     jum.assertEquals(12, getDateField(fullHashDate, "hours"));
                     jum.assertEquals(33, getDateField(fullHashDate, "minutes"));
                     jum.assertEquals(30, getDateField(fullHashDate, "seconds"));

                     var sparseHashDate = { year: 2006,
                                            month: 11 };

                     jum.assertEquals(2006, getDateField(sparseHashDate, "year"));
                     jum.assertEquals(11, getDateField(sparseHashDate, "month"));
                     jum.assertEquals(1, getDateField(sparseHashDate, "date"));
                     jum.assertEquals(0, getDateField(sparseHashDate, "hours"));
                     jum.assertEquals(0, getDateField(sparseHashDate, "minutes"));
                     jum.assertEquals(0, getDateField(sparseHashDate, "seconds"));
                 },

                 function compareDates(){
                     var compareDates = cosmo.datetime.timezone._compareDates;
                     var jsDate1 = new Date(2006, 11, 10, 12, 33, 30);
                     var jsDate2 = new Date(2007, 11, 10, 12, 33, 30);
                     jum.assertTrue(compareDates(jsDate1, jsDate2) < 0);

                     jsDate1 = new Date(2006, 11, 10, 12, 33, 30);
                     jsDate2 = new Date(2006, 11, 10, 12, 33, 30);
                     jum.assertTrue(compareDates(jsDate1, jsDate2) == 0);

                     jsDate1 = new Date(2006, 11, 10, 12, 33, 31);
                     jsDate2 = new Date(2006, 11, 10, 12, 33, 30);
                     jum.assertTrue(compareDates(jsDate1, jsDate2)  > 0);

                     jsDate1 = new Date(2006, 11, 10, 13, 33, 31);
                     jsDate2 = new Date(2006, 11, 10, 12, 33, 31);
                     jum.assertTrue(compareDates(jsDate1, jsDate2)  > 0);

                     var sparseHashDate = { year: 2006,
                                            month: 11 };
                     jsDate2 = new Date(2006, 11, 1, 1, 1, 1, 1);
                     jum.assertTrue(compareDates(sparseHashDate, jsDate2) < 0);
                 },

                 function getZoneItemForDate(){
                     var tz = getNyTz();
                     var date = new Date(2006, 1, 1);
                     var zoneItem = tz._getZoneItemForDate(date);
                     jum.assertEquals(null, zoneItem.untilDate);

                     date = new Date(1966, 11, 31);
                     zoneItem = tz._getZoneItemForDate(date);
                     jum.assertEquals(1967, zoneItem.untilDate.year);

                     date = new Date(1800, 1, 1);
                     zoneItem = tz._getZoneItemForDate(date);
                     jum.assertEquals(1883, zoneItem.untilDate.year);

                     date = new Date(1920, 1, 1);
                     zoneItem = tz._getZoneItemForDate(date);
                     jum.assertEquals(1942, zoneItem.untilDate.year);
                 },

                 function getRulesForYear(){
                     var rs = getUsRs();
                     var rules = rs._getRulesForYear(1999);
                     jum.assertEquals(2, rules.length);
                     jum.assertEquals(1967, rules[0].startYear);
                 },

                 function DayGreateThanNForMonthAndYear(){
                     var func = cosmo.datetime.timezone._getDayGreaterThanNForMonthAndYear;

                     //"get me the date of the first thursday that is greater than or equal to the 8th in November"
                     var date = func(8, 4, 10, 2006);
                     jum.assertEquals(9, date);

                     //"get me the date of the first wednesday that is greater than or equal to the 8th in November"
                     date = func(8, 3, 10, 2006);
                     jum.assertEquals(8, date);

                     //"get me the date of the first tuesday that is greater than or equal to the 8th in November"
                     date = func(8, 2, 10, 2006);
                     jum.assertEquals(14, date);
                 },

                 function DayLessThanNForMonthAndYear(){
                     var func = cosmo.datetime.timezone._getDayLessThanNForMonthAndYear;

                     //"get me the date of the last thursday that is less than or equal to the 8th in November"
                     var date = func(8,4,10,2006);
                     jum.assertEquals(2, date);

                     //"get me the date of the last wednesday that is less than or equal to the 8th in November"
                     date = func(8,3,10,2006);
                     jum.assertEquals(8, date);

                     //"get me the date of the last tuesday that is less than or equal to the 8th in November"
                     date = func(8,2,10,2006);
                     jum.assertEquals(7, date);
                 },

                 function getStartDateForYear(){
                     //to test: cosmo.datetime.timezone.Rule.prototype._getStartDateForYear = function(year)
                     var rs = getUsRs();
                     var sorter = function(a,b){return a.startMonth - b.startMonth;};

                     var rules = rs._getRulesForYear(1967);
                     rules.sort(sorter);
                     var startDate = rules[0]._getStartDateForYear(2006);

                     //for sanity's sake, make sure it's APR
                     jum.assertEquals(3, startDate.month);

                     //rule says Apr, lastSun - last sunday in april which is the 30th
                     jum.assertEquals(30, startDate.date);

                     rules = rs._getRulesForYear(1974);
                     rules.sort(sorter);
                     startDate = rules[0]._getStartDateForYear(1974);

                     //rule says "jan 6"
                     jum.assertEquals(0, startDate.month);
                     jum.assertEquals(6, startDate.date);

                     rules = rs._getRulesForYear(2007);
                     rules.sort(sorter);
                     startDate = rules[0]._getStartDateForYear(2007);

                     //rule sun>=8 - first sunday after or on the eighth which is the 11th
                     jum.assertEquals(2, startDate.month);
                     jum.assertEquals(11, startDate.date);
                 },

                 function getOffsetInMinutes(){
                     var timezone = getNyTz();
                     var date;
                     var offset;

                     date = new cosmo.datetime.Date(2006, 1, 1, 0, 0, 0, 0, "America/Los_Angeles");
                     offset = timezone.getOffsetInMinutes(date);
                     jum.assertEquals(-300, offset);

                     date = new cosmo.datetime.Date(2006, 3, 1, 0, 0, 0, 0, "America/Los_Angeles");
                     offset = timezone.getOffsetInMinutes(date);
                     jum.assertEquals(-300, offset);

                     date = new cosmo.datetime.Date(2006, 3, 2, 1, 59, 59, 0, "America/Los_Angeles");
                     offset = timezone.getOffsetInMinutes(date);
                     jum.assertEquals(-300, offset);

                     date = new cosmo.datetime.Date(2006, 3, 2, 3, 0, 0, 0, "America/Los_Angeles");
                     offset = timezone.getOffsetInMinutes(date);
                     jum.assertEquals(-240, offset);
                 },

                 function getRuleForDate(){
                     //var tz = cosmo.datetime.timezone._timezoneRegistry.getTimezone("America/Barbados");
                     var date = new Date(2006, 1, 1);
                     var ruleSet = cosmo.datetime.timezone.getRuleSet("Barb");
                     var rule = ruleSet._getRuleForDate(date);
                     jum.assertTrue(rule != null);
                     jum.assertTrue(rule.startYear == 1980);
                     jum.assertTrue(rule.letter == "S");

                 }
             ]);

 })();