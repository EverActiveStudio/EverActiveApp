var ScriptVars = Java.type("org.zaproxy.zap.extension.script.ScriptVars");
var token = "TOKEN_PLACEHOLDER";

if (token === "") {
    token = null;
}

ScriptVars.setGlobalVar("zast_token", token);
print("Auth Token switched to: " + (token ? "User Token" : "None"));
