# mcalert

Created with:

```shell
quarkus create app io.github.nostra:mcalert --java=21 --no-code
```
```shell
quarkus extension add picocli
quarkus extension add resteasy-reactive-jackson
quarkus extension add rest-client-reactive-jackson
quarkus extension add quarkus-scheduler
```

## Build with

```shell
./mvnw -B package
```

## Run with

```shell
java -jar target/quarkus-app/quarkus-run.jar 
```

## Configure

Create a file in your home directory named `$HOME/.mcalert.properties` and
configure endpoints. You can have as many endpoints as you like:
```
mcalert.prometheus.endpoints.<NAME>.uri=http://prometheus.somewhere.local.gd:9090/api/v1/alerts
mcalert.prometheus.endpoints.<NAME>.ignore-alerts=CPUThrottlingHigh,KubeControllerManagerDown
mcalert.prometheus.endpoints.<NAME>.watchdog-alerts=disabled
```
You can have different endpoints with different `NAME`. Fill in the relevant alerts for each environment.

You can supply headers. The example illustrates what to do when you want an `Authorization` header.
By increasing the index, you can add more headers.

```
mcalert.prometheus.endpoints<NAME>.header[0].name=Authorization
mcalert.prometheus.endpoints<NAME>.header[0].content=Basic dXNlcm5hbWU6cGFzc3dvcmQ=
```

## Create a Mac DMG image

```shell
mvn -B clean package
echo "Create dmg"
cd target
jpackage --verbose --name mcalert --input quarkus-app \
    --description "Read Prometheus endpoint and show status as toolbar icon" \
    --icon ../mcalert.icns \
    --main-jar quarkus-run.jar 
```

## Icons

Icons downloaded from
- https://remixicon.com/icon/cloud-off-fill
- https://remixicon.com/icon/bug-line
- https://remixicon.com/icon/circle-line
- https://remixicon.com/icon/pulse-line
- https://remixicon.com/icon/shut-down-line
- https://remixicon.com/icon/information-off-line
