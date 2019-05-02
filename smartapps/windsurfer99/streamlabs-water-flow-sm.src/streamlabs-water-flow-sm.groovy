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
            		input (name: "SL_awayModes", type: "mode", title: "Enter SmartThings modes when water meter should be Away", multiple: true, required: false)
            		input (name: "SL_locName", type: "text", title: "Enter Streamlabs location name assigned to Streamlabs flow meter", multiple: false, required: true)
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	// get the value of api key
	def mySecret = appSettings.api_key
    //log.debug "APi Key: $mySecret"
    //log.debug "Modes: $SL_awayModes"
    subscribe(location, "mode", modeChangeHandler)
	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initialize with settings: ${settings}"
    initSL_Locations() //determine Streamlabs location to use
    runEvery1Minute(pollSLAlert) //Poll Streamlabs cloud for leak alert
}

//Handler for runEvery; determine if there are any alerts
def pollSLAlert() {
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
            ]
	try {
        httpGet(params) {resp ->
            log.debug "pollSLAlert resp.data: ${resp.data}"
            def resp_data = resp.data
            def SL_locationsAlert = resp_data.alerts[0]
            if (SL_locationsAlert) {
            	log.debug "pollSLAlert Alert0 received: ${SL_locationsAlert}"
                //send event to child device handler
            }
        }
    } catch (e) {
        log.error "error in pollSLAlert: $e"
    }
}

//Get desired location from Streamlabs cloud based on user's entered location's name
def initSL_Locations() {
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations',
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
    ]
    //log.debug "params for locations: ${params}"
    state.SL_location = null

	try {
		//def SL_location
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
            if (!state.SL_location) {
            	log.error "SmartLabs location name: ${SL_locName} not found!"
            }
            //SL_location = state.SL_location
            log.debug "SL_location to use: ${state.SL_location}"
            //log.debug "# of SL_locations: ${resp.data.total}"
            //log.debug "resp contentType: ${resp.contentType}"
            //def totl = resp.data.locations[0].name
            //log.debug "name of locations[0]: ${totl}"
 
//updateAway()
			//log.debug "location.currentMode: ${location.currentMode}"
            //log.debug "initSL_Locations resp status: ${resp.status}"
            //if (resp.status == 200){
            //    log.debug "initSL_Locations resp status good"
            //}

        }
    } catch (e) {
        log.error "error in SL_locations: $e"
    }
}

//Method to set Streamlabs homeAway status; called with 'home' or 'away'
def updateAway(newHomeAway) {
	//def newHomeAway
    //log.debug "state.SL_location.homeAway: ${state.SL_location.homeAway}"
    //if (state.SL_location.homeAway == "home") {
    //	newHomeAway = "away"
    //} else {
    //	newHomeAway = "home"
    //}
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

//handler for when SmartThings mode changes
//if new mode is one of the ones specified for a StreamLabs away mode, change Streamlabs to away
def modeChangeHandler(evt) {
    log.debug "mode changed to ${evt.value}"
    //log.debug "SL_awayModes: ${SL_awayModes}"
    //log.debug "location.currentMode: ${location.currentMode}"
/*    if (SL_awayModes?.find{it == location.currentMode} != null) {
        //change to away
        updateAway("away")
    }  else {
        //change to home
        updateAway("home")
    }
*/
	def foundmode = false
	SL_awayModes?.each{ awayModes->
        if (location.currentMode == awayModes) {
        	foundmode = true //new mode is one to set Streamlabs to away
        }
    }
    if (foundmode) {
        //change to away
        updateAway("away")
    } else {
        //change to home; new mode isn't one specified for Streamlabs away
        updateAway("home")
    }
}
// TODO: implement event handlers
