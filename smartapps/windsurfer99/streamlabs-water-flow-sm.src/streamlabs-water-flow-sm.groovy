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
            		input (name: "SL_locName", type: "text", title: "Enter Streamlabs location name assigned to Streamlabs flow meter", multiple: false, required: true)
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
    initSL_Locations()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initialize with settings: ${settings}"

}

//Get desired location from Streamlabs cloud based on user's entered location's name
def initSL_Locations() {
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations',
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
    ]
    //log.debug "params for locations: ${params}"

	try {
		def SL_location
        httpGet(params) {resp ->
            //log.debug "resp: ${resp}"
            def resp_data = resp.data
            //log.debug "resp data: ${resp.data}"
            //log.debug "resp_data: ${resp_data}"
            def SL_locations0 = resp_data.locations[0]
            //log.debug "SL_locations0: ${SL_locations0}"
            def ttl = resp_data.total
            log.debug "Total SL_locations: ${ttl}"
            resp.data.locations.each{ SL_loc->
            	//log.debug "name of SL_location: ${SL_loc.name}"
                if (SL_loc.name == SL_locName) {
                	state.SL_location = SL_loc
                }
            }
            SL_location = state.SL_location
            log.debug "SL_location to use: ${SL_location}"
            //log.debug "# of SL_locations: ${resp.data.total}"
            //log.debug "resp contentType: ${resp.contentType}"
            //def totl = resp.data.locations[0].name
            //log.debug "name of locations[0]: ${totl}"
 
updateAway()
            //log.debug "initSL_Locations resp status: ${resp.status}"
            //if (resp.status == 200){
            //    log.debug "initSL_Locations resp status good"
            //}

        }
    } catch (e) {
        log.error "error in SL_locations: $e"
    }
}

//Set Away status
def updateAway() {
	def newHomeAway
    //log.debug "state.SL_location.homeAway: ${state.SL_location.homeAway}"
    if (state.SL_location.homeAway == "home") {
    	newHomeAway = "away"
    } else {
    	newHomeAway = "home"
    }
    //log.debug "newHomeAway: ${newHomeAway}"
    def cmdBody = [
			"homeAway": newHomeAway
	]
    //log.debug "cmdBody: ${cmdBody}"
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
			body : new groovy.json.JsonBuilder(cmdBody).toString()    
            ]

    log.debug "params for updateAway: ${params}"

	try {
        httpPutJson(params){resp ->
            log.debug "updateAway resp data: ${resp.data}"
            log.debug "updateAway resp status: ${resp.status}"
        }
    } catch (e) {
        log.error "error in updateAway: $e"
    }
}


// TODO: implement event handlers
