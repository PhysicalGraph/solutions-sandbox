// Our Lambda function fle is required
var lambdafy = require('./index.js');

// The Lambda context "done" function is called when complete with/without error
var context = {
	done: function (err, result) {
		console.log('------------');
		console.log('Context done');
		console.log('   error:', err);
		console.log('   result:', result);
	},
	succeed: function (result) {
		console.log('------------');
		console.log('Context succeed');
		console.log('   result:', result);
	},
	fail: function (err) {
		console.log('------------');
		console.log('Context fail');
		console.log('   error:', err);
	}
};

var smartHomeDiscoveryEvent = {
	"header": {
		"namespace": "Alexa.ConnectedHome.Discovery",
		"name": "DiscoverAppliancesResponse",
		"payloadVersion": "2"
	},
	"payload": {
		"accessToken": "sampleAccessToken",
		"discoveredAppliances": [
			{
				"applianceId": "testApplianceId1",
				"manufacturerName": "testManufacturerName",
				"modelName": "testModelName",
				"version": "testVersion",
				"friendlyName": "testFriendlyName",
				"friendlyDescription": "testFriendlyDescription",
				"isReachable": true,
				"additionalApplianceDetails": {}
			},
			{
				"applianceId": "testApplianceId2",
				"manufacturerName": "testManufacturerName",
				"modelName": "testModelName",
				"version": "testVersion",
				"friendlyName": "testFriendlyName",
				"friendlyDescription": "testFriendlyDescription",
				"isReachable": true,
				"additionalApplianceDetails": {
					"key2": "value2",
					"key1": "value1"
				}
			}
		]
	}
};

var smartHomeControlEvent = {
  "header": {
	"payloadVersion": "2",
	"namespace": "Alexa.ConnectedHome.Control",
	"name": "SwitchOnOffRequest"
  },
  "payload": {
	"switchControlAction": "TURN_ON",
	"appliance": {
	  "additionalApplianceDetails": {
		"key2": "value2",
		"key1": "value1"
	  },
	  "applianceId": "sampleId"
	},
	"accessToken": "sampleAccessToken"
  }
};

var customSkillEvent = {
  "session": {
	"new": false,
	"sessionId": "session1234",
	"attributes": {},
	"user": {
	  "userId": null,
	  "accessToken": "sampleAccessToken"
	},
	"application": {
	  "applicationId": "amzn1.echo-sdk-ams.app.f1fc7835-a6ed-4ada-bb1a-26c251c91ab8"
	}
  },
  "version": "1.0",
  "request": {
	"intent": {
	  "slots": {
		"LockOperation": {
		  "name": "LockOperation",
		  "value": "lock"
		},
		"WhichLock": {
			"name": "WhichLock",
			"value": "front door"
		}
	  },
	  "name": "DialogSmartDoorIntent"
	},
	"type": "IntentRequest",
	"requestId": "request5678"
  }
};

// event to send to the lambda function
var event = customSkillEvent;
// var event = smartHomeControlEvent;
// ar event = smartHomeDiscoveryEvent;

// Call the Lambda function
lambdafy.handler(event, context);
