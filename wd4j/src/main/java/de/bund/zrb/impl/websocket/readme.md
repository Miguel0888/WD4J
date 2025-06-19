# Local end definition
This are the incoming messages from the remote end.

```
Message = (
CommandResponse /
ErrorResponse /
Event
)
```
```
CommandResponse = {
type: "success",
id: js-uint,
result: ResultData,
Extensible
}
```
```
ErrorResponse = {
type: "error",
id: js-uint / null,
error: ErrorCode,
message: text,
? stacktrace: text,
Extensible
}
```
```
ResultData = (
BrowsingContextResult /
EmptyResult /
NetworkResult /
ScriptResult /
SessionResult /
StorageResult /
WebExtensionResult
)
```
```
EmptyResult = {
Extensible
}
```
```
Event = {
type: "event",
EventData,
Extensible
}
```

see: https://w3c.github.io/webdriver-bidi/#local-end-definition