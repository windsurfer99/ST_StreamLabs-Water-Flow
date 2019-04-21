/**
 *  StreamLabs Water Flow SM
 *
 *  Copyright 2019 Bruce Andrews
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "StreamLabs Water Flow SM",
    namespace: "windsurfer99",
    author: "windsurfer99",
    description: "Service Manager for cloud-based API for StreamLabs Water Flow meter",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
 singleInstance: true) {
    appSetting "api_key"
}


preferences {
	page(name: "pageOne", title: "Options", uninstall: true, install: true) {
		section("Inputs") {
        		paragraph ("Set the API Key via App Settings in IDE")
            		label (title: "Assign a name for Service Manager", required: false, multiple: true)
            		input (name: "modes", type: "mode", title: "Enter SmartThings modes when water meter should be Away", multiple: true, required: false)
            		input (name: "locName", type: "text", title: "Enter location name assigned to Streamlabs meter", multiple: false, required: true)
		}
	}
}
	
def installed() {
	log.debug "Installed with settings: ${settings}"
	// get the value of api key
	def mySecret = appSettings.api_key
    //log.debug "APi Key: $mySecret"
    //log.debug "Modes: $modes"

	initialize()
    GetLocations()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initialize with settings: ${settings}"

}

def GetLocations() {
    def params = [
        uri:  'https://api.streamlabswater.com/v1/locations',
        headers: ['Authorization': 'Bearer ' + appSettings.api_key],
        contentType: 'application/json',
    ]
    log.debug "params for locations: ${params}"
//    def TotalLocations
try {
		def location
        httpGet(params) {resp ->
            //log.debug "resp: ${resp}"
            def resp_data = resp.data
            //log.debug "resp data: ${resp.data}"
            log.debug "resp_data: ${resp_data}"
            def locations0 = resp_data.locations[0]
            log.debug "locations0: ${locations0}"
            def ttl = resp_data.total
            log.debug "Total locations: ${ttl}"
            resp.data.locations.each{ loc->
            	log.debug "name of location: ${loc.name}"
                if (loc.name == locName) {
                	state.location = loc
                }
            }
            location = state.location
            log.debug "location to use: ${location}"
            //log.debug "# of locations: ${resp.data.total}"
            //log.debug "resp contentType: ${resp.contentType}"
            //def totl = resp.data.locations[0].name
            //log.debug "name of locations[0]: ${totl}"
 
            log.debug "resp status: ${resp.status}"
            if (resp.status == 200){
                log.debug "resp status good"
            }

        }
    } catch (e) {
        log.error "error in locations: $e"
    }
}

// TODO: implement event handlers
