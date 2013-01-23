/*
 * Copyright 2007-2008 Open Source Applications Foundation
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

/**
 * summary:
 *      This module provides convenience functions for user preferences.
 * description:
 *      This module provides convenience functions for creating,
 *      editing and deleting server side user preferences.
 */
dojo.provide("cosmo.account.preferences");

dojo.require("cosmo.app.pim");
dojo.require("dojo.cookie");

cosmo.account.preferences = new function () {
	this.SHOW_ACCOUNT_BROWSER_LINK = 'UI.Show.AccountBrowserLink';
	this.LOGIN_URL = 'Login.Url';
	this.DEFAULT_VIEW = 'Pim.Default.View';
	this.DEFAULT_COLLECTION = 'Pim.Default.Collection';

    this.getCookiePreference = function(key){
        return dojo.cookie(key);
    };

    this.setCookiePreference = function(key, val){
        return dojo.cookie(key, val, {path: "/"});
    };

    this.getPreference = function(key, kwArgs){
		return cosmo.app.pim.serv.getPreference(key, kwArgs);
    };

    this.setPreference = function(key, val, kwArgs){
		var deferred = cosmo.app.pim.serv.setPreference(key, val, kwArgs);
		var preferences = {};
		preferences[key] = val;
        cosmo.topics.publish(cosmo.topics.PreferencesUpdatedMessage, [preferences])
        return deferred;
    };

    this.deletePreference = function(key, kwArgs){
		return cosmo.app.pim.serv.deletePreferences(key, kwArgs);
    };

    this.getPreferences = function(kwArgs){
		return cosmo.app.pim.serv.getPreferences(kwArgs);
    };

};
