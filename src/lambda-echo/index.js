// Lambda service for SmartThings - Echo Integration

var http = require('https');
// var smartthingsCustomSkill = require('./smartthingsCustomSkill.js');

const HTTP_GET = "GET";
const HTTP_POST = "POST";

var ST_HOSTNAME = "graph.api.smartthings.com";



// Note the client id and log settings are unique for each different lambda service (development, Internal QA, production)

// ====== kshuk dev Start ========
// ARN: arn:aws:lambda:us-east-1:747017778629:function:amazonEcho
var AMAZON_CLIENT_ID = "ae89baa0-62c7-4bea-8985-1eef12534c99"; // kevin.shuk@smartthings.com
var AMAZON_COHO_SKILL_APP_ID = "amzn1.ask.skill.5c7bce44-5049-4d92-b9a3-f8b1f3a5a496";
var AMAZON_CUSTOM_SKILL_APP_ID = "amzn1.echo-sdk-ams.app.563420d2-8402-41c3-b9a8-f55daca14adb";

var logsEnabled = true;
var networkLogsEnabled = true;
// ====== kshuk dev End ========

/*
 // ====== Production Start ========
 // ARN: arn:aws:lambda:us-east-1:747017778629:function:amazonEcho
var AMAZON_CLIENT_ID = "83ed14bc-e192-47fa-b877-40d1d421086e";

var logsEnabled = true;
var networkLogsEnabled = false;
 // ====== Production End ========
*/

/*
// ====== Development Start ========
// ARN - arn:aws:lambda:us-east-1:344541377886:function:amazonEchoDev

var AMAZON_CLIENT_ID = "e85c63f0-3199-4059-89db-44fbdcaf23aa";

var logsEnabled = true;
var networkLogsEnabled = true;
// ====== Development End ========
*/

/*
// ====== Development Staging Start ========
// ARN - arn:aws:lambda:us-east-1:344541377886:function:amazonEchoDev
var AMAZON_CLIENT_ID = "f80e3825-e378-429c-884a-15e423f3871d";

var logsEnabled = true;
var networkLogsEnabled = true;
ST_HOSTNAME = "sgraph.api.smartthings.com";
// ====== Development Staging End ========
*/

/*
 // ====== Internal QA Start (SA on superuser) ========
 // ARN - arn:aws:lambda:us-east-1:344541377886:function:amazonEcho

var AMAZON_CLIENT_ID = "903c37c8-16fd-4447-8491-b8e9b9ad5a49";

var logsEnabled = true;
var networkLogsEnabled = true;
 // ====== Internal QA End ========
*/

// Version 2.1

// TODO:
// Add support for SmartApp reporting success or fail on control command


// TODO replace this error codes with Alexa V2 codes when the API is final
// Amazon error codes

// The services that the appliance driver depends on are not currently available.
var DEPENDENT_SERVICE_UNAVAILABLE = "DEPENDENT_SERVICE_UNAVAILABLE";
// The access token provided has expired. Use a refresh token to retrieve a new access token.
var EXPIRED_ACCESS_TOKEN = "EXPIRED_ACCESS_TOKEN";
// An error occurred while the appliance driver was handing the request and the error does not fall into any of the specified categories.
var INTERNAL_ERROR = "INTERNAL_ERROR";
// The access token provided is not valid.
var INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";
// The target appliance does not exist. This could happen if the target appliance has been removed by the customer.
var NO_SUCH_TARGET = "NO_SUCH_TARGET";
// The requested operation to the target appliance caused an error because the target appliance is already in the requested state. Do not return an error if the target appliance is already at the requested state and the operation can still be applied.
var TARGET_ALREADY_AT_REQUESTED_STATE = "TARGET_ALREADY_AT_REQUESTED_STATE";
// Connectivity between the bridge to which the target appliance connects, such as a hub, and the cloud is unstable. As a result, the appliance cannot be reliably controlled.
var TARGET_BRIDGE_CONNECTIVITY_UNSTABLE = "TARGET_BRIDGE_CONNECTIVITY_UNSTABLE";
// The bridge, for example a hub, that connects the target appliance to the cloud has an old firmware version which could degrade the experience.
var TARGET_BRIDGE_FIRMWARE_VERSION_OUTDATED = "TARGET_BRIDGE_FIRMWARE_VERSION_OUTDATED";
// The bridge, for example a hub, that connects the target appliance to the cloud has a hardware malfunction.
var TARGET_BRIDGE_HARDWARE_MALFUNCTION = "TARGET_BRIDGE_HARDWARE_MALFUNCTION";
// The bridge, for example a hub, that connects the target appliance to the cloud is not online.
var TARGET_BRIDGE_OFFLINE = "TARGET_BRIDGE_OFFLINE";
// Connectivity between the target appliance and the cloud is unstable. As a result, the appliance cannot be reliably controlled.
var TARGET_CONNECTIVITY_UNSTABLE = "TARGET_CONNECTIVITY_UNSTABLE";
// The target appliance has an old firmware version which could degrade the experience.
var TARGET_FIRMWARE_VERSION_OUTDATED = "TARGET_FIRMWARE_VERSION_OUTDATED";
// The target appliance has a hardware malfunction.
var TARGET_HARDWARE_MALFUNCTION = "TARGET_HARDWARE_MALFUNCTION";
// The target appliance is not online.
var TARGET_OFFLINE = "TARGET_OFFLINE";
// The operation of adjusting settings is supported by the appliance, but the requested adjustment is not within the permitted range of the target appliance.
var TARGET_SETTING_OUT_OF_RANGE = "TARGET_SETTING_OUT_OF_RANGE";
// The information received is not what the appliance driver expected because the directive is malformed or the payload does not conform to the driver directive specification.
var UNEXPECTED_INFORMATION_RECEIVED = "UNEXPECTED_INFORMATION_RECEIVED";
// The operation specified by the namespace and the name in the header of the driver directive is not supported by the appliance driver or the target appliance.
var UNSUPPORTED_OPERATION = "UNSUPPORTED_OPERATION";
// The target appliance is not supported by this appliance driver.
var UNSUPPORTED_TARGET = "UNSUPPORTED_TARGET";
// The operation of adjusting settings is supported by the appliance, but the unit of adjustment does not apply to the target appliance.
var UNSUPPORTED_TARGET_SETTING = "UNSUPPORTED_TARGET_SETTING";


var COHO_PAYLOAD_VERSION = 2;
//
// ------------- HELPERS -----------
//

function getHostName(url) {
	// Remove https:// prefix if exists
	var hostName = url;
	if (hostName.indexOf('http') >= 0) {
		hostName = hostName.substring(hostName.lastIndexOf('//') + 2);
	}

	// Remove :xxxx port if exists
	if (hostName.lastIndexOf(':') >= 0) {
		hostName = hostName.substring(0, hostName.lastIndexOf(':'));
	}
	return hostName;
}

function buildSmartappRequestOptions(httpMethod, smartappInstallId, oauth2AccessToken, baseUrl) {
	var url = '/api/smartapps/installations/' + smartappInstallId;
	var reqOpts = {
		hostname: getHostName(baseUrl),
		path: url,
		port: 443,
		method: httpMethod,
		headers: {
			accept: '*/*',
			Authorization: 'Bearer ' + oauth2AccessToken,
		}
	};
	return reqOpts;
}

function buildCustomSkillOptions(smartappInstallId, oauth2AccessToken, baseUrl) {
	var reqOpts = buildSmartappRequestOptions(HTTP_GET, smartappInstallId, oauth2AccessToken, baseUrl);
	reqOpts.path += '/custom';
	//log("buildCustomSkillOptions", JSON.stringify(reqOpts, null, 2));
	return reqOpts;
}

function buildCustomSkillCommandOptions(smartappInstallId, oauth2AccessToken, baseUrl) {
	var reqOpts = buildCustomSkillOptions(smartappInstallId, oauth2AccessToken, baseUrl);
	reqOpts.method = HTTP_POST;
	//log("buildCustomSkillCommandOptions", JSON.stringify(reqOpts, null, 2));
	return reqOpts;
}

// function getCustomSkillCommand(installationId, accessToken, baseUrl) {
//	 var customSkillPath = '/api/smartapps/installations/' + installationId + '/custom';
//	 return {
//		 hostname: getHostName(baseUrl),
//		 port: 443,
//		 path: customSkillPath,
//		 method: 'GET',
//		 headers: {accept: '*/*',  Authorization: 'Bearer ' + accessToken}
//	 };
// }

function getDiscoveryOptions(installationId, accessToken, baseUrl) {
	var discoveryPath = '/api/smartapps/installations/' + installationId + '/discovery';
	return {
		hostname: getHostName(baseUrl),
		port: 443,
		path: discoveryPath,
		method: 'GET',
		headers: {accept: '*/*',  Authorization: 'Bearer ' + accessToken}
	};
}

function getControlOptions(installationId, accessToken, controlCommand, applianceId, value, baseUrl) {
	var controlPath = '/api/smartapps/installations/' + installationId + '/control/' + applianceId + '/' + controlCommand;
	if (value !== null) {
		controlPath = controlPath + '/' + value;
	}
	return {
		hostname: getHostName(baseUrl),
		port: 443,
		path: controlPath,
		method: 'POST',
		headers: {accept: '*/*',  Authorization: 'Bearer ' + accessToken}
	};
}

function getSmartAppPathOptions(accessToken) {
	var smartAppPath = '/api/smartapps/endpoints/' + AMAZON_CLIENT_ID;
	return {
		hostname: ST_HOSTNAME,
		port: 443,
		path: smartAppPath,
		method: 'GET',
		headers: {accept: '*/*', Authorization: 'Bearer ' + accessToken}
	};
}

exports.handler = function (event, context) {
	log("handler", "");
	logNetwork("echo->lambda handler", event);

	// Protocol sanity check
	if (isEventCustomSkill(event)) {
		// this is a custom skill!!
		log("Info", "session: " + event.session + "; request: " + event.request + "; version: " + event.version);

		// punt the request wholesale to the custom skill handler
		//smartthingsCustomSkill.handler(event, context);

		getSmartAppPath(event, context, handleCustomSkill);
	} else if (event.header === null) {
		sendErrorResponseV1(context, event, UNEXPECTED_INFORMATION_RECEIVED, "Missing header", "Unknown", "Request");
	} else if (event.payload === null) {
		sendErrorResponseV1(context, event, UNEXPECTED_INFORMATION_RECEIVED, "Missing payload");
	} else if (parseInt(event.header.payloadVersion) != COHO_PAYLOAD_VERSION) {
		sendErrorResponseV1(context, event, UNSUPPORTED_OPERATION, "Unsupported version");
	} else {
		// Handle different namespaces
		switch (event.header.namespace) {
			case "Alexa.ConnectedHome.Discovery":
				// Find the correct SmartApp instance and then call handleDiscovery
				getSmartAppPath(event, context, handleDiscovery);
				break;

			case "Alexa.ConnectedHome.Control":
				// Find the correct SmartApp instance and then call handleControl
				getSmartAppPath(event, context, handleControl);
				break;

			case "Alexa.ConnectedHome.System":
				// Send back current SmartThings cloud health
				handleSystem(event, context);
				break;

			default:
				log("Error", "Not supported namespace " + event.header.namespace);
				sendErrorResponseV1(context, event, UNSUPPORTED_OPERATION, "Unsupported namespace");
				break;
		}
	}
};

/**
 Handles any custom skill request and brokers it based on intent
 */
function handleCustomSkill(event, context, smartappInstallId, baseUrl) {
	log("handleCustomSkill", "");
	logNetwork("echo->lambda handleCustomSkill", event);

	var amazonJsonVersion = event.version;
	var reqOptions;
	var httpRequest;
	var httpPostBody = null;

	// define an anonymous function to process the SmartApp's response and store it in the callback var
	var processSmartAppResponse = function (httpResponse) {
		var responseBodyStr = '';

		httpResponse.on('data', function (chunk) {
			responseBodyStr += chunk;
		});
		httpResponse.on('error', function (e) {
			log("handleCustomSkill", e.message);
			sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Unknown error while receiving SmartApp httpResponse: " + e.message);
		});
		httpResponse.on('end', function () {
			log("handleCustomSkill", "httpResponse statusCode: " + httpResponse.statusCode);
			if (httpResponse.statusCode == 200 || httpResponse.statusCode == 204 || httpResponse.statusCode == 201) {
				var json = parseResponseCheckForError(event, context, responseBodyStr);
				json.version = amazonJsonVersion; // Response format version must match request
				sendCustomSkillResponse(context, event, json);
			} else if (httpResponse.statusCode == 429) {
				sendCustomSkillErrorResponse(context, event, DEPENDENT_SERVICE_UNAVAILABLE, "Rate limit exceeded, too many requests received");
			} else {
				sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Unknown error");
			}
		});
	};

	switch (event.request.type) {
		case "SessionEndedRequest":
			log("handleCustomSkill", "Session ended: " + event.request.reason);
			context.succeed();
			break;
		case "LaunchRequest":
			reqOptions = buildCustomSkillOptions(smartappInstallId, getAccessTokenFromEvent(event), baseUrl);
			break;
		case "IntentRequest":
			reqOptions = buildCustomSkillCommandOptions(smartappInstallId, getAccessTokenFromEvent(event), baseUrl);
			httpPostBody = event;
			break;
		default:
			// The request type or specific intent was not handled, so respond with an error
			log("handleCustomSkill", "ERROR: Invalid request type '" + event.request.type + "'");
			sendCustomSkillErrorResponse(context, event, UNSUPPORTED_OPERATION, "Unsupported namespace or name");
	}

	// dispatch the handled request to the SmartApp for processing,
	// and provide the callback funtion to process the response
	logNetwork("lambda->SmartApp handleCustomSkill open request", reqOptions);
	httpRequest = http.request(reqOptions, processSmartAppResponse);
	if (httpPostBody !== null) {
		logNetwork("lambda->SmartApp handleCustomSkill posting data", httpPostBody);
		httpRequest.write(JSON.stringify(httpPostBody));
	}
	httpRequest.end();
}

function handleDiscovery(event, context, installationId, baseUrl) {
	log("handleDiscovery", "");
	logNetwork("echo->lambda handleDiscovery", event);

	// these need to be in the function's scope.
	var options;
	var callback;

	if (event.header.namespace == "Alexa.ConnectedHome.Discovery" && event.header.name == "DiscoverAppliancesRequest") {
		var headers = {namespace: "Alexa.ConnectedHome.Discovery", name: "DiscoverAppliancesResponse", payloadVersion: ""+COHO_PAYLOAD_VERSION};
		var appliances = [];

		options = getDiscoveryOptions(installationId, event.payload.accessToken.trim(), baseUrl);
		callback = function (response) {
			var str = '';

			response.on('data', function (chunk) {
				str += chunk;
			});
			response.on('error', function (e) {
				log("handleDiscovery", e.message);
				sendErrorResponseV1(context, event, INTERNAL_ERROR, "Unknown error while receiving SmartApp response: " + e.message);
			});
			response.on('end', function () {
				log("handleDiscovery", "response statusCode: " + response.statusCode);
				if (response.statusCode == 200 || response.statusCode == 204) {
					var json = parseResponseCheckForError(event, context, str);
					sendResponse(context, event, json, true);
				} else if (response.statusCode == 429) {
					sendErrorResponseV1(context, event, DEPENDENT_SERVICE_UNAVAILABLE, "Rate limit exceeded, too many requests received");
				} else {
					sendErrorResponseV1(context, event, INTERNAL_ERROR, "Unknown error");
				}
			});
		};
	}
	else {
		sendErrorResponseV1(context, event, UNSUPPORTED_OPERATION, "Unsupported namespace or name");
	}

	logNetwork("lambda->SmartApp handleDiscovery", options);
	http.get(options, callback).end();
}

function handleControl(event, context, installationId, baseUrl) {
	log("handleControl", "");
	if (event.header.namespace != "Alexa.ConnectedHome.Control") {
		sendErrorResponseV1(context, event, UNSUPPORTED_OPERATION, "Unsupported namespace or name");
	} else {
		var additionalApplianceDetailsMap = event.payload.appliance.additionalApplianceDetails;
		var applianceId = event.payload.appliance.applianceId;
		var accessToken = event.payload.accessToken.trim();

		var command = event.header.name;
		var value = null;

		var errorCode = null;
		var errorMessage = null;

		if (applianceId === null) {
			errorCode = UNEXPECTED_INFORMATION_RECEIVED;
			errorMessage = "Missing appliance ID";
		} else if (command == "TurnOnRequest" || command == "TurnOffRequest") {

		} else if (command == "SetPercentageRequest" || command == "IncrementPercentageRequest" || command == "DecrementPercentageRequest") {
			if (command == "SetPercentageRequest")
				value = event.payload.percentageState.value;
			else
				value = event.payload.deltaPercentage.value;

			if (value == null) {
				errorCode = UNEXPECTED_INFORMATION_RECEIVED;
				errorMessage = "Adjustment value is missing"
			} else if (isNaN(value)) {
				errorCode = UNEXPECTED_INFORMATION_RECEIVED;
				errorMessage = "Adjustment value is not a number"
			}
		} else if (command == "SetTargetTemperatureRequest" || command == "IncrementTargetTemperatureRequest" || command == "DecrementTargetTemperatureRequest") {
			if (command == "SetTargetTemperatureRequest" || event.payload.deltaTemperature == null)
				value = event.payload.targetTemperature.value;
			else
				value = event.payload.deltaTemperature.value;

			if (value == null) {
				errorCode = UNEXPECTED_INFORMATION_RECEIVED;
				errorMessage = "Adjustment value is missing"
			} else if (isNaN(value)) {
				errorCode = UNEXPECTED_INFORMATION_RECEIVED;
				errorMessage = "Adjustment value is not a number"
			}
		} else {
			errorCode = UNSUPPORTED_OPERATION;
			errorMessage = "Unsupported command";
		}

		if (errorCode != null) {
			sendErrorResponseV1(context, event, errorCode, errorMessage);
		} else {
			var options = getControlOptions(installationId, accessToken, command, applianceId, value, baseUrl);

			callback = function (response) {
				var str = '';

				response.on('data', function (chunk) {
					str += chunk;
				});
				response.on('error', function (e) {
					log("handleControl", e.message);
					sendErrorResponseUnknown(context, event);
				});
				response.on('end', function (body) {
					log("handleControl", "response statusCode: " + response.statusCode+" response: "+response+" body: "+str);
					logNetwork("lambda<-SmartApp handleControl", event);
					if (response.statusCode == 200 || response.statusCode == 204 || response.statusCode == 201) {
						var body = JSON.parse(str);
						if (body.error) {
							sendErrorResponseV2(context, event, body.error, body.payload);
						} else {
							sendResponse(context, event, body, false);
						}
					} else {
						sendErrorResponseUnknown(context, event);
					}
				});
			};

			logNetwork("lambda->echo handleControl", options);
			http.get(options, callback).end();
		}
	}
}


function handleSystem(event, context) {
	if (event.header.namespace == "System" && event.header.name == "HealthCheckRequest") {
		var payloads = {isHealthy: true, description: "The system is currently healthy"};
		sendResponse(context, event, payloads, true);
	} else {
		sendErrorResponseV1(context, event, UNSUPPORTED_OPERATION, "Unsupported namespace or name");
	}
}

/**
 * If request event has top level header, and payload elements, it's a smart home skill rewqest
 * @param  Map  event unmarshaled JSON event into javascript map stucture
 * @return Boolean	 true if event is a Smart Home event, false otherwise
 */
function isEventSmartHome(event) {
	if (event.header == null || event.payload == null) {
		return false;
	}
	return true;
}

/**
 * If request event has top level session, request, and version elements, it's a custom skill rewqest
 * @param  Map  event unmarshaled JSON event into javascript map stucture
 * @return Boolean	 true if event is a Custom Skill event, false otherwise
 */
function isEventCustomSkill(event) {
	if (event.session == undefined || event.request == undefined || event.version == undefined) {
		return false;
	}
	return true;
}

/**
 * Returns the OAUTH2 session Access Token from the appropriate path for the request type
 * @param  Map event unmarshaled JSON event into javascript map stucture
 * @return String	  the access token
 */
function getAccessTokenFromEvent(event) {
	var accessToken = null;
	if (isEventCustomSkill(event)) {
		accessToken = event.session.user.accessToken.trim();
	}
	else if (isEventSmartHome(event)) {
		accessToken = event.payload.accessToken.trim();
	}
	else {
		log("getAccessTokenFromEvent ERROR", "request object has no accessToken at payload.accessToken or user.accessToken\n" + event);
	}
	return accessToken;
}



// Resolve the correct SmartApp URL using the client id and access token
// Each user's Amazon Echo SmartApp has its own URL
// When successful, call the resultCallback(event, context, URL) function
function getSmartAppPath(event, context, resultCallback) {
	log("getSmartAppPath", "");
	//var accessToken = event.payload.accessToken.trim();
	var accessToken = getAccessTokenFromEvent(event);

	var options = getSmartAppPathOptions(accessToken);

	callback = function (response) {
		var str = '';

		response.on('data', function (chunk) {
			str += chunk;
		});
		response.on('error', function (e) {
			log("getSmartAppPath", e.message);
			if (resultCallback == handleCustomSkill) {
				sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Unable to find your SmartThings Amazon Echo SmartApp, please disable and renable the SmartThings skill from the Alexa Skills Marketplace.");
			} else {
				sendErrorResponseV1(context, event, INTERNAL_ERROR, "Unknown error when identifying the SmartApp: " + e.message);
			}
		});
		response.on('end', function () {
			log("getSmartAppPath", "response statusCode: " + response.statusCode);

			var json = parseResponseCheckForError(event, context, str);
			if (json !== null) {
				log("getSmartAppPath", "Got "+json.length+" SmartApps");
				var index = json.length - 1;
				// TODO Deal with multiple locations!!
				if (json[index] != null && json[index].url != null) {
					// TODO change to 0?
					log("getSmartAppPath", "SmartApp URL is " + json[index].url);

					// Url will be in format '/api/smartapps/installations/5a2843e2-49e1-4be5-9564-39db029ec961'
					// extract the id
					resultCallback(event, context, json[index].url.substring(json[index].url.lastIndexOf('/') + 1), json[index].base_url);
				} else if (str = "[]") {
					// No endpoints found but token was valid, this most likely means that the SmartApp has been uninstalled
					// Would it make more sense to send INTERNAL_ERROR?
					log("getSmartAppPath", "SmartApp might have been uninstalled");
					if (resultCallback == handleCustomSkill) {
						sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Unable to find your SmartThings Amazon Echo SmartApp, please disable and renable the SmartThings skill from the Alexa Skills Marketplace.");
					} else {
						sendErrorResponseV1(context, event, INVALID_ACCESS_TOKEN, "SmartApp has been uninstalled, user should relink with SmartThings");
					}
				} else {
					log("getSmartAppPath", "Unexpected response");
					if (resultCallback == handleCustomSkill) {
						sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Unable to find your SmartThings Amazon Echo SmartApp, please disable and renable the SmartThings skill from the Alexa Skills Marketplace.");
					} else {
						sendErrorResponseV1(context, event, INTERNAL_ERROR, "Unexpected response received while identifying SmartApp");
					}
				}
			} else {
				log("getSmartAppPath", "response (expected JSON but did not get it): " + str);
			}
		});
	};
	logNetwork("lambda->SmartApp getSmartAppPath", options);
	var req = http.get(options, callback).end();
}

function sendResponse(context, event, payload, response) {
	var tempName = "" + event.header.name;
	if (response)
		tempName = tempName.replace("Request", "Response");
	else
		tempName = tempName.replace("Request", "Confirmation");

	var headers = {"namespace": event.header.namespace, "name": tempName, "payloadVersion": ""+COHO_PAYLOAD_VERSION};
	var result = {"header": headers, "payload": payload};

	logNetwork("echo<-lambda sendResponse", result);
	context.succeed(result);
}

function sendCustomSkillResponse(context, event, jsonResponse) {
	// jsonResponse is the fully-formatted  alexa custom skill response
	logNetwork("echo<-lambda sendResponse", jsonResponse);
	context.succeed(jsonResponse);
}

// Utility function for sending DriverInternalError according to Alexa COHO V2 protocol
function sendErrorResponseUnknown(context, event) {
	sendErrorResponseV2(context, event, "DriverInternalError", "{}");
}

// TODO remove this function and old error codes when list of Alexa V2 error codes is final
// Send errors according to old Alexa COHO V1 protocol (will be converted to V2 error response)
// namespace and name are optional, if excluded the values will be read from the event
function sendErrorResponseV1(context, event, errorCode, errorDescription, namespace, name) {
	if (typeof namespace === 'undefined') {
		namespace = event.header.namespace;
	}

	var tempName = "" + name;
	var payload = {};

	switch (errorCode) {
		case DEPENDENT_SERVICE_UNAVAILABLE:
			tempName = "DependentServiceUnavailableError";
			payload = "";
			break;
		case NO_SUCH_TARGET:
			tempName = "NoSuchTargetError";
			break;
		case TARGET_ALREADY_AT_REQUESTED_STATE:
			tempName = "AlreadySetToTargetError";
			break;
		case TARGET_OFFLINE:
			tempName = "TargetOfflineError";
			break;
		case TARGET_SETTING_OUT_OF_RANGE:
			tempName = "ValueOutOfRangeError";
			payload = {minimumValue: "0", maximumValue: "100"};
			break;
		default:
			tempName = "DriverInternalError";
			break;
	}

	var headers = {"namespace": namespace, "name": tempName, "payloadVersion": ""+COHO_PAYLOAD_VERSION};
	//var payload = {"success": false, "exception": {"code": errorCode, "description": errorDescription}};

	var result = {"header": headers, "payload": payload};

	logNetwork("echo<-lambda sendErrorResponse "+errorDescription, result);
	context.succeed(result);
}

// Send errors according to Alexa COHO V2 protocol
// namespace and name are optional, if excluded the values will be read from the event
function sendErrorResponseV2(context, event, error, payload, namespace, name) {
	if (typeof namespace === 'undefined') {
		namespace = event.header.namespace;
	}
	if (typeof name === 'undefined') {
		name = event.header.name;
	}

	var headers = {"namespace": namespace, "name": error, "payloadVersion": ""+COHO_PAYLOAD_VERSION};
	// var payload = payload; //{"success": false, "exception": {"code": errorCode, "description": errorDescription}};
	//var payload = {};
	var result = {"header": headers, "payload": payload};

	logNetwork("echo<-lambda sendErrorResponse", result);
	context.succeed(result);
}


function sendCustomSkillErrorResponse(context, event, errorCode, errorDescription) {
	var cardTitle = "SmartThings Error: " + errorCode;
	// Setting this to true ends the session and exits the skill.
	var shouldEndSession = true;
	var alexaResponse = buildResponse({}, buildSpeechletResponse(cardTitle, errorDescription, null, shouldEndSession));
	context.succeed(alexaResponse); // succeed? Yeah...
}



// If data contains valid json not including an error message, then the json data will be returned.
// If there is a problem, then execution will stop, an error will be sent back to the requester and
// function returns null.
function parseResponseCheckForError(event, context, data) {
	var json = null;
	if (isEventSmartHome(event)) {
		json = parseSmartHomeResponseCheckForError(event, context, data);
	}
	else if (isEventCustomSkill(event)) {
		json = parseCustomSkillResponseCheckForError(event, context, data);
	}
	else {
		log("parseResponseCheckForError FATAL", "This doesnt look like a Smart Home or Custom skill: ");
		context.fail();
	}
	return json;
}

function parseCustomSkillResponseCheckForError(event, context, data) {
	var json = null;
	if (data === null || data.length === 0) {
		log("parseCustomSkillResponseCheckForError", "No response received from SmartApp");
		sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Did not receive an expected response from SmartApp");
	}

	log("parseCustomSkillResponseCheckForError", "Data received from SmartApp: " + data);

	json = JSON.parse(data);

	if (json === null) {
		log("parseCustomSkillResponseCheckForError", "Non-JSON response received from SmartApp");
		sendCustomSkillErrorResponse(context, event, INTERNAL_ERROR, "Did not receive an expected response from SmartApp");
	}
	else if (json.error != undefined && json.error_description != undefined) {
		log("parseCustomSkillResponseCheckForError", "Error message received from SmartApp " + json.error + " " + json.error_description);
		var amazonError = "INTERNAL_ERROR";
		if (json.error == "invalid_token") {
			amazonError = "INVALID_ACCESS_TOKEN";
		}
		logNetwork("lambda<-SmartApp", json);
		sendCustomSkillErrorResponse(context, event, amazonError, "" + json.error_description);
		json = null;
	} else {
		log("parseCustomSkillResponseCheckForError", "No errors found");
		logNetwork("lambda<-SmartApp", json);
	}
	return json;
}


function parseSmartHomeResponseCheckForError(event, context, data) {
	var json = null;
	if (data === null || data.length === 0) {
		log("parseSmartHomeResponseCheckForError", "No response received from SmartApp");
		sendErrorResponseV1(context, event, INTERNAL_ERROR, "Did not receive an expected response from SmartApp");
	}

	log("parseSmartHomeResponseCheckForError", "Data received from SmartApp: " + data);

	json = JSON.parse(data);

	if (json === null) {
		log("parseSmartHomeResponseCheckForError", "Non-JSON response received from SmartApp");
		sendErrorResponseV1(context, event, INTERNAL_ERROR, "Did not receive an expected response from SmartApp");
	}
	else if (json.error != undefined && json.error_description != undefined) {
		log("parseSmartHomeResponseCheckForError", "Error message received from SmartApp " + json.error + " " + json.error_description);
		var amazonError = "INTERNAL_ERROR";
		if (json.error == "invalid_token") {
			amazonError = "INVALID_ACCESS_TOKEN";
		}
		logNetwork("lambda<-SmartApp", json);
		sendErrorResponseV1(context, event, amazonError, "" + json.error_description);
		json = null;
	} else {
		log("parseSmartHomeResponseCheckForError", "No errors found");
		logNetwork("lambda<-SmartApp", json);
	}
	return json;
}

function log(tag, msg) {
	if (logsEnabled) {
		console.log(tag + " : " + msg);
	}
}

function logNetwork(tag, msg) {
	if (networkLogsEnabled) {
		console.log('*************** ' + tag + ' *************');
		console.log("\n" + JSON.stringify(msg, null, '  '));
		console.log('*************** ' + tag + ' End*************');
	}
}

// --------------- Helpers that build all of custom skill responses -----------------------

function buildSpeechletResponse(titleText, outputText, repromptText, shouldEndSession) {
	return {
		outputSpeech: {
			type: "PlainText",
			text: outputText
		},
		card: {
			type: "Simple",
			title: titleText,
			content: outputText
		},
		reprompt: {
			outputSpeech: {
				type: "PlainText",
				text: repromptText
			}
		},
		shouldEndSession: shouldEndSession
	};
}

function buildResponse(sessionAttributes, speechletResponse) {
	return {
		version: "1.0",
		sessionAttributes: sessionAttributes,
		response: speechletResponse
	};
}
