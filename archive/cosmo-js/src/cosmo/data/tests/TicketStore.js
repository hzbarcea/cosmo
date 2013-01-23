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
dojo.provide("cosmo.data.tests.TicketStore");

dojo.require("cosmo.data.TicketStore");
dojo.require("dojox.data.dom");
dojo.require("dojox.uuid.generateTimeBasedUuid");
dojo.require("cosmo.tests.util");
dojo.require("cosmo.util.auth");

cosmo.data.tests.ticketmfns = {xhtml :"http://www.w3.org/1999/xhtml"};
cosmo.data.tests.ticketmf = dojox.data.dom.createDocument(
    '<?xml version="1.0" encoding="utf-8"?>' +
    '<div xmlns="http://www.w3.org/1999/xhtml">' +
    '<div class="ticket">Key: ' +
    '<span class="key">xvm7udwf30</span>Type:' +
    '<span class="type" title="read-only">read-only</span>' +
    '</div>' +
    '</div>');

cosmo.data.tests.ticketFeed1 = dojox.data.dom.createDocument(
    "<?xml version='1.0' encoding='UTF-8'?>" +
    '<feed xmlns="http://www.w3.org/2005/Atom" xml:base="http://localhost:8080/chandler/atom/">' +
    '<id>urn:uuid:8ea99c40-0bfb-11dd-8b67-f61a36a4de45</id>' +
    '<title type="text">Tickets on Untitled</title>' +
    '<updated>2008-04-16T23:38:04.965Z</updated>' +
    '<generator uri="http://cosmo.osafoundation.org/" version="0.15-SNAPSHOT">Chandler Server</generator>' +
    '<author>' +
    '<name>travis</name>' +
    '<uri>user/travis</uri>' +
    '</author>' +
    '<link rel="self" type="application/atom+xml" href="collection/8ea99c40-0bfb-11dd-8b67-f61a36a4de45/tickets" />' +
    '<entry xmlns:app="http://www.w3.org/2007/app">' +
    '<id>urn:uuid:xvm7udwf30</id>' +
    '<title type="text">xvm7udwf30</title>' +
    '<updated>2008-04-16T21:25:19.111Z</updated>' +
    '<app:edited>2008-04-16T21:25:19.111Z</app:edited>' +
    '<published>2008-04-16T21:25:19.111Z</published>' +
    '<link rel="self" type="application/atom+xml" href="collection/8ea99c40-0bfb-11dd-8b67-f61a36a4de45/ticket/xvm7udwf30" />' +
    '<link rel="edit" type="application/atom+xml" href="collection/8ea99c40-0bfb-11dd-8b67-f61a36a4de45/ticket/xvm7udwf30" />' +
    '<content type="xhtml">' +
    '<div xmlns="http://www.w3.org/1999/xhtml">' +
    '<div class="ticket">Key:' +
    '<span class="key">xvm7udwf30</span>Type:' +
    '<span class="type" title="read-only">read-only</span>' +
    '</div>' +
    '</div>' +
    '</content>' +
    '</entry>' +
    '</feed>');

cosmo.data.tests.ticketEntry1 = cosmo.atompub.query("atom:entry", cosmo.data.tests.ticketFeed1.documentElement)[0];

doh.register("cosmo.data.tests.TicketStore",
	[
        function testProcessor(t){
            var p = new cosmo.data.TicketProcessor();
            var doc = cosmo.data.tests.ticketmf.documentElement;
            t.is("read-only", p.getValues(doc, "type")[0]);
            t.is("xvm7udwf30", p.getValues(doc, "key")[0]);
        },

        function testStore(t){
            var s = new cosmo.data.TicketStore();
            var e = cosmo.data.tests.ticketEntry1;
            t.is("xvm7udwf30", s.getValue(e, "key"));
            t.is("read-only", s.getValue(e, "type"));
        },

        {
            name: "integrationTests",
            timeout: 10000,
            setUp: function(){
                this.serverRunning = cosmo.tests.util.serverRunning();
            },
            runTest: function(){
                if (this.serverRunning){
                    var d = cosmo.tests.util.setupTestUser();
                    d.addCallback(dojo.hitch(this, this.getTicketsUrl));
                    d.addCallback(dojo.hitch(this, this.initStore));
                    d.addCallback(dojo.hitch(this, this.newTicket));

                    d.addErrback(function(e){console.log(e); return e;});
                    return cosmo.tests.util.defcon(d);
                }
            },

            getTicketsUrl: function(user){
                var d = cosmo.tests.util.getCollection(user, "/details");
                d.addCallback(function(cXml){
                    var attr = cosmo.atompub.query("atom:link[@rel='ticket']/@href", cXml.documentElement)[0];
                    return attr? cosmo.xml.getBaseUri(attr) + attr.value : null;
                });
                return d;
            },

            initStore: function(url){
                this.store = new cosmo.data.TicketStore(
                    {
                        iri: url,
                        xhrArgs: cosmo.util.auth.getAuthorizedRequest()
                    });
                return this.store;
            },

            newTicket: function(){
                this.store.newItem({type: "read-only", key: dojox.uuid.generateTimeBasedUuid().slice(0,8)});
                var d = new dojo.Deferred();
                this.store.save({onComplete: dojo.hitch(d, d.callback), onError: dojo.hitch(d, d.errback)});
                return d;
            }
        }
    ]);


