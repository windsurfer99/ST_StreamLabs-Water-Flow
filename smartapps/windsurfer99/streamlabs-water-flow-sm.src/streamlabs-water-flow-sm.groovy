/**
 *  StreamLabs Water Flow SM
 *  Smart App/ Service Manager for StreamLabs Water Flow Meter
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
	log.debug "StreamLabs SM installed with settings: ${settings}"
	// get the value of api key
	def mySecret = appSettings.api_key
    //log.debug "Modes: $SL_awayModes"
	initialize()
}


def updated() {
	log.debug "StreamLabs SM updated with settings: ${settings}"

	unsubscribe()
    cleanup()
	initialize()
}

def initialize() {
	log.debug "StreamLabs SM initialize with settings: ${settings}"
    state.SL_location = null
    state.childDevice = null
    state.inAlert = false
    subscribe(location, "mode", modeChangeHandler)
    initSL_Locations() //determine Streamlabs location to use
    if (state.SL_location) { 
    	//we have a device; put it into initial state
        def eventData = [name: "water", value: "dry"]
    	def existingDevice = getChildDevice(state.SL_location?.locationId)
        existingDevice?.generateEvent(eventData)
        state.inAlert =  false
	    runEvery1Minute(pollSLAlert) //Poll Streamlabs cloud for leak alert
        determineFlows() //get current flow totals from cloud
    }
}

//remove things
def cleanup() {
	log.debug "StreamLabs SM cleanup called"
    def SL_Devices = getChildDevices()
    SL_Devices.each {
		log.debug "StreamLabs SM deleting SL deviceNetworkID: ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
    state.SL_location = null
    state.childDevice = null
    state.inAlert = false
}

//Handler for runEvery; determine if there are any alerts
def pollSLAlert() {
    //log.debug "pollSLAlert state: ${state}"
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	if (state.SL_location){
        def params = [
                uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
                headers: ['Authorization': 'Bearer ' + appSettings.api_key],
                contentType: 'application/json',
                ]
        try {
            httpGet(params) {resp ->
                log.debug "StreamLabs SM pollSLAlert resp.data: ${resp.data}"
                def resp_data = resp.data
                def SL_locationsAlert = resp_data.alerts[0]
                if (SL_locationsAlert) {
                    if (!state.inAlert){
                        //new alert, send wet event to child device handler
                        log.debug "StreamLabs SM new pollSLAlert Alert0 received: ${SL_locationsAlert}; send wet event"
                        def eventData = [name: "water", value: "wet"]
                        existingDevice?.generateEvent(eventData)
                        state.inAlert =  true
                    }
                } else {
                    if (state.inAlert){
                        //alert removed, send dry event to child device handler
                        log.debug "StreamLabs SM pollSLAlert Alert0 deactivated ; send dry event"
                        def eventData = [name: "water", value: "dry"]
                        existingDevice?.generateEvent(eventData)
                        state.inAlert =  false
                    }
                }
            }
        } catch (e) {
            log.error "StreamLabs SM error in pollSLAlert: $e"
        }
    }
}

//determine flow totals from cloud
def determineFlows() {
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	if (state.SL_location){
        def params = [
                uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId + '/readings/water-usage/summary',
                headers: ['Authorization': 'Bearer ' + appSettings.api_key],
                contentType: 'application/json',
                ]
        try {
            httpGet(params) {resp ->
                log.debug "StreamLabs SM retrieveFlows resp.data: ${resp.data}"
                def resp_data = resp.data
                state.todayFlow = resp_data?.today
                state.thisMonthFlow = resp_data?.thisMonth
                state.thisYearFlow = resp_data?.thisYear
                state.unitsFlow = resp_data?.units
            }
        } catch (e) {
            log.error "StreamLabs SM error in retrieveFlows: $e"
        }
    }
}

// return current low totals
Map retrieveFlows() {
	log.debug "StreamLabs SM retrieveFlows called"
	return ["todayFlow":state.todayFlow, "thisMonthFlow":state.thisMonthFlow, "thisYearFlow":state.thisYearFlow]
}

//Get desired location from Streamlabs cloud based on user's entered location's name
def initSL_Locations() {
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations',
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
    ]
    state.SL_location = null

	try {
        httpGet(params) {resp ->
            def resp_data = resp.data
            def SL_locations0 = resp_data.locations[0]
            def ttl = resp_data.total
            log.debug "StreamLabs SM total SL_locations: ${ttl}"
            resp.data.locations.each{ SL_loc->
                if (SL_loc.name == SL_locName) {
                	state.SL_location = SL_loc
                }
            }
            if (!state.SL_location) {
            	log.error "StreamLabs SM location name: ${SL_locName} not found!"
            } else {
            //load device handler for this location (device)
                def existingDevice = getChildDevice(state.SL_location.locationId)
                if(!existingDevice) {
                    //def childDevice = addChildDevice("windsurfer99", "StreamLabs Water Flow", state.SL_location.locationId, null, [name: "Device.${deviceId}", label: device.name, completedSetup: true])
                    def childDevice = addChildDevice("windsurfer99", "StreamLabs Water Flow DH", state.SL_location.locationId, null, [name: "Streamlabs Water Flow", label: "Streamlabs Water Flow", completedSetup: true])
            		log.debug "StreamLabs SM device created: ${childDevice}"
                }
            
            }
            log.debug "StreamLabs SM SL_location to use: ${state.SL_location}"
        }
    } catch (e) {
        log.error "StreamLabs SM error in initSL_locations: $e"
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

    log.debug "StreamLabs SM params for updateAway: ${params}"

	try {
        httpPutJson(params){resp ->
            log.debug "StreamLabs SM updateAway resp data: ${resp.data}"
            log.debug "StreamLabs SM updateAway resp status: ${resp.status}"
        }
    } catch (e) {
        log.error "StreamLabs SM error in updateAway: $e"
    }
}

//handler for when SmartThings mode changes
//if new mode is one of the ones specified for a StreamLabs away mode, change Streamlabs to away
//Do nothing if the user hasn't selected any modes defined as being Streamlabs away.
def modeChangeHandler(evt) {
    log.debug "StreamLabs SM SmartThings mode changed to ${evt.value}"
    //log.debug "SL_awayModes: ${SL_awayModes}"
    //log.debug "location.currentMode: ${location.currentMode}"
	def foundmode = false
    log.debug "StreamLabs SM SL_awayModes: ${SL_awayModes}; size: ${SL_awayModes?.size}"
    if (SL_awayModes?.size() > 0) {//only do something if user specified some modes
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
}
// TODO: implement event handlers