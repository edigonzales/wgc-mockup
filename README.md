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

## Build
### JVM
```
./mvnw clean package -Penv-prod
```

Dockerimage:
```
docker build -t edigonzales/wgc-mockup-jvm -f wgc-mockup-server/src/main/docker/Dockerfile.jvm .
```

### Native image
```
./mvnw -Penv-prod,native package
```

Ohne Tests:
```
./mvnw -Penv-prod,native -DskipTests package
```

Mit Tests dauert es doppelt solange, weil das Image doppelt erstellt wird (?). Es gibt die "native tests", die noch nicht funktionieren: gleiche Fehlermeldung wegen "unpack before package...".

Dockerimage:
```
docker build -t edigonzales/wgc-mockup-native -f wgc-mockup-server/src/main/docker/Dockerfile.native .
```

### Native image with Docker:
```
docker build -t edigonzales/wgc-mockup-native -f wgc-mockup-server/src/main/docker/Dockerfile.native-build .
```

## Todo
- Settings mit Env Vars
- map.set(settings), damit nicht in Popup hardcodiert werden muss
- Farben etc. als globale Variable. Popup-Settings-Klasse, damit man das überschreiben kann.
- URL interface 
- mindestens ÖREB-Kataster und der "andere Service" mittels Settings veränderbar.