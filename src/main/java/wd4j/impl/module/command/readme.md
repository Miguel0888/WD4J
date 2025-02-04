# Remote end definition
This are the outgoing messages (=commands) to the remote end.

```
Command = {
id: js-uint,
CommandData,
Extensible,
}
```

```
CommandData = (
BrowserCommand //
BrowsingContextCommand //
InputCommand //
NetworkCommand //
ScriptCommand //
SessionCommand //
StorageCommand //
WebExtensionCommand
)
```

```
EmptyParams = {
Extensible
}
```

see: https://w3c.github.io/webdriver-bidi/#remote-end-definition