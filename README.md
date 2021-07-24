# wgc-mockup

## Develop
First terminal:
```
./mvnw spring-boot:run -Penv-dev -pl *-server -am
```

Second terminal:
```
./mvnw gwt:codeserver -pl *-client -am
````

Or without downloading all the snapshots again:
```
./mvnw gwt:codeserver -pl *-client -am -nsu
```