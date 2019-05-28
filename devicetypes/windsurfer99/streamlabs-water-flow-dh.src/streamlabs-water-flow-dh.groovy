/**
 *  StreamLabs Water Flow DH
 *  Device Handler for StreamLabs Water Flow Meter: Cloud Connected Device
 *
 *  Copyright 2019 windsurfer99
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "StreamLabs Water Flow DH", namespace: "windsurfer99", author: "windsurfer99", cstHandler: true) {
		capability "Water Sensor"
        capability "Sensor"
        capability "Health Check"
        capability "refresh"
        command "changeToAway"
        command "changeToHome"
        command "changeToPause"
        command "changeToMonitor"
        attribute "todayFlow", "string"
        attribute "monthFlow", "string"
        attribute "yearFlow", "string"
        attribute "suspend", "enum", ["pause", "monitor"] //tracks if user has requested to suspend leak alerts
        attribute "homeAway", "enum", ["home", "away"]
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2)  {
		standardTile("water", "device.water", width: 6, height: 4) {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC"
		}
		standardTile("refresh", "capability.refresh", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
			state "default", label:"Refresh", icon:"st.secondary.refresh", action:"refresh.refresh"
		}
		standardTile("suspend", "device.suspend", decoration: "flat", width: 2, height: 2) {
//			state "pause", label: "pause", icon:"st.sonos.play-btn", action:"changeToMonitor", backgroundColor:"#e86d13"
//			state "monitor", label: "monitoring", defaultState:true, icon:"st.sonos.pause-btn", action:"changeToPause", backgroundColor:"#00A0DC"
			state "pause", label: "Now paused", icon:"st.sonos.play-btn", action:"changeToMonitor"
			state "monitor", label: "Now monitoring", defaultState:true, icon:"st.sonos.pause-btn", action:"changeToPause"
		}		
		standardTile("homeAway", "device.homeAway", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
			state "home", icon:"st.nest.nest-home", defaultState:true, action:"changeToAway", backgroundColor:"#00A0DC", nextstate: "changingToAway"
			state "away", icon:"st.nest.nest-away", action:"changeToHome", backgroundColor:"#CCCCCC", nextstate: "changingToHome"
    		state "changingToAway", label:'Changing', icon:"st.nest.nest-away", backgroundColor:"#CCCCCC", nextState: "changingToHome"
    		state "changingToHome", label:'Changing', icon:"st.nest.nest-home", backgroundColor:"#00A0DC", nextState: "changingToAway"
		}
		
		valueTile("todayFlow", "device.todayFlow", decoration: "flat", height: 2, width: 2) {
			state "todayFlow", label: 'Usage Today ${currentValue} Gal.'
		}

//		valueTile("todayFlow", "device.todayFlow", decoration: "flat", height: 2, width: 2) {
//			state "todayFlow", label: 'Usage Today\n\r ${currentValue} Gal.'
//		}

		valueTile("monthFlow", "device.monthFlow", decoration: "flat", height: 2, width: 2) {
			state "monthFlow", label: 'Usage this month ${currentValue} Gal.'
		}
 
		valueTile("yearFlow", "device.yearFlow", decoration: "flat", height: 2, width: 2) {
			state "yearFlow", label: 'Usage this year ${currentValue} Gal.'
		}

		main "water"
		details(["water", "homeAway", "suspend", "refresh", "todayFlow", "monthFlow", "yearFlow"])
    }
    preferences {
        input name: "pauseDelay", type: "number", title: "Number", description: "# of minutes for max. pause:", required: false
	}
}
//required implementations
def installed() {
	log.debug "StreamLabs DH installed; state.init: ${state.init}"
//    if (state.init != true){
//    	state.init = true
//		runIn(2,"initialize")
		initialize()
//    }
//    refresh()
}

def initialize() {
	log.debug "StreamLabs DH initialize with pause timeout: ${pauseDelay}"
    schedule("0 0/10 * * * ?", poll) //refresh every 10 minutes
	sendEvent(name: "suspend", value: "monitor")
    state.wetDry = "dry"
//   	state.init = true
//    device.suspend = "monitor"
//    refresh()
}

def updated(){
	log.debug "StreamLabs DH updated"
	unschedule("poll")
    initialize()
}
/*
def uninstalled() {
	log.debug "StreamLabs DH uninstalled called for ${device.deviceNetworkId}"
    //delete me from parent Service Manager; this will leave in a strange state until parent recreates me
//    if (state.init == true){
	parent.deleteSmartLabsDevice(device.deviceNetworkId)
//    }
//    state.init = false
}
*/

// parse events into attributes; not really used with this type of Device Handler
def parse(String description) {
	log.debug "StreamLabs DH- parse called with '${description}'"

}
//poll for changes and update locally; framework is supposed to call every 10 minutes but doesn't seem to
//so scheduled internally, this is the handler for that
def poll() {
	log.debug "StreamLabs DH poll called"
    refresh()
	//sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
}

def refresh(){
	def cloudData = parent.retrievecloudData() 
	log.debug "StreamLabs DH refresh- cloudData: ${cloudData}"
    state.todayFlow = cloudData.todayFlow
    state.thisMonthFlow = cloudData.thisMonthFlow
    state.thisYearFlow = cloudData.thisYearFlow
    state.homeAway = cloudData.homeAway
    state.wetDry = cloudData.inAlert ? "wet" : "dry"

	sendEvent(name: "todayFlow", value: Math.round(cloudData.todayFlow))
	sendEvent(name: "monthFlow", value: Math.round(cloudData.thisMonthFlow))
	sendEvent(name: "yearFlow", value: Math.round(cloudData.thisYearFlow))
	sendEvent(name: "water", value: state.wetDry)
	sendEvent(name: "homeAway", value: cloudData.homeAway)

	//sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "commandResponse")
	//runIn(2, getPower)
}

//actions

//Tile action to change StreamLabs to home
def changeToHome() {
	log.debug "StreamLabs DH changeToHome called"
	parent.updateAway("home")
}

//Tile action to change StreamLabs to away
def changeToAway() {
	log.debug "StreamLabs DH changeToAway called"
	parent.updateAway("away")
}

//Tile action (& suspend time limit action) to re-enble monitoring StreamLabs for alerts
def changeToMonitor() {
	log.debug "StreamLabs DH changeToMonitor called"
	sendEvent(name: "suspend", value: "monitor")
	sendEvent(name: "water", value: state.wetDry) //update real status in case it had been suspended
}

//Tile action to pause monitoring StreamLabs for alerts
def changeToPause() {
	log.debug "StreamLabs DH changeToPause called"
	sendEvent(name: "suspend", value: "pause")
    if (pauseDelay > 0) {//if user wants a time limit on suspending alerts
	    runIn (pauseDelay*60, "changeToMonitor")
    }
}

//handle Events sent from Service Manager; typically wet & dry
def generateEvent(Map results) {
	log.debug "StreamLabs DH generateEvent parameters: '${results}'"
	sendEvent(results)
	return null
}

//Typically called by parent: update to "wet"
def changeWaterToWet() {
	log.debug "StreamLabs DH changeWaterToWet called"
    state.wetDry = "wet"
    if (suspend == "monitor") { //update only if not paused
		sendEvent(name = "water" , value = "wet")
	}
}

//Typically called by parent: update to "dry"
def changeWaterToDry() {
	log.debug "StreamLabs DH changeWaterToDry called"
    state.wetDry = "dry"
    //update even if paused
	sendEvent(name = "water" , value = "dry")
}