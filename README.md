# wgc-mockup

## Develop
First terminal:
```
./mvnw spring-boot:run -Penv-dev -pl *-server -am
```

Second terminal:
```
./mvnw gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:
```
./mvnw gwt:codeserver -pl *-client -am -nsu
```

## Todo
- Settings mit Env Vars
- map.set(settings), damit nicht in Popup hardcodiert werden muss
- Vektorlayer für Highlighting (wie automatisch entfernen beim Schliessen des Popups?)
- URL interface 