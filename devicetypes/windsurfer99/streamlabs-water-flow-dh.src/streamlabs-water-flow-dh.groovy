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
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2)  {
		standardTile("water", "device.water", width: 6, height: 4) {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC"
		}
		main "water"
		details(["water"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'water' attribute

}