/*
 * Copyright 2008 Open Source Applications Foundation
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
dojo.provide("cosmo.ui.widget.NotificationPane");

dojo.require("cosmo.util.html");
dojo.require("dijit._Templated");
dojo.require("cosmo.ui.widget.TimezonePicker");
dojo.require("cosmo.app.pim");

dojo.requireLocalization("cosmo.ui.widget", "NotificationPane");

dojo.declare("cosmo.ui.widget.NotificationPane", [dijit._Widget, dijit._Templated], {
    templatePath: dojo.moduleUrl("cosmo", "ui/widget/templates/NotificationPane.html"),
    widgetsInTemplate: true,

    title: null,
    preferences: null,
    checkboxes: null,

    // Attach points
    collectionsContainer: null,
    sendFrequencySelector: null,

    constructor: function(preferences){
        this.l10n = dojo.i18n.getLocalization("cosmo.ui.widget", "NotificationPane");
        this.title = this.l10n.title;
        this.preferences = preferences;
    },

    makeCollectionCheckbox: function(uuid, name){
        var dict = {};
        dict.div = document.createElement('div');
        dojo.addClass(dict.div, "collectionChoice");
        dict.input = document.createElement('input');
        dict.label = document.createElement('label');
        dict.input.type = "checkbox";
        var id = "collection_" + uuid;
        dict.input.id = id;
        dict.input.name = uuid;
        dict.label.setAttribute('for', id);
        dict.label.innerHTML = name;
        dict.div.appendChild(dict.input);
        dict.div.appendChild(dict.label);
        return dict;
    },

    postCreate: function(){
        var collections = cosmo.app.pim.collections;
        this.checkboxes = [];
        for (var i = 0; i < collections.length; i++){
            var collection = collections.getAtPos(i);
            var dict = this.makeCollectionCheckbox(collection.getUid(), collection.getDisplayName());

            dict.input.onchange = this.checkboxOnChange;
            this.checkboxes.push(dict.input);
            if (i % 3 == 0)
                dojo.addClass(dict.div, "clear");
            this.collectionsContainer.appendChild(dict.div);
        }

        // set up timezone default
    },

    // event handlers
    selectAllOnChange: function(e){
        debugger;
        //pass
    },

    checkboxOnChange: function(e){
        debugger;
    }
});