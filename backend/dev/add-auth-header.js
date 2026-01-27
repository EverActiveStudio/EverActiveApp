var ScriptVars = Java.type("org.zaproxy.zap.extension.script.ScriptVars");

function sendingRequest(msg, initiator, helper) {
    var token = ScriptVars.getGlobalVar("zast_token");
    if (token && token !== "null" && token.length > 0) {
        msg.getRequestHeader().setHeader("Authorization", "Bearer " + token);
    }
}

function responseReceived(msg, initiator, helper) {
    // Nothing to do here
}
