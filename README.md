# mcalert

This application gives you a nifty icon in the Mac Taskbar 
"![pulse-line.png](src%2Fmain%2Fresources%2Fimages%2Fpulse-line.png)" which indicates
the status of your Kubernetes cluster by examining the Prometheus status.

When clicking on the icon, you get a dropdown list which allows you to identify the cluster
which reports a problem:

![All active Prometheus instances are happy](mcalert-green.png)
![One or more unhappy Prometheus instances](mcalert-red.png)

If you click on one of the configurations in the dropdown, you toggle 
whether it gets checked or not. In the example images "local-prometheus" is
deactivated. You decide your own names in the configuration.

You need jdk-21 with jpackage. The rest of the dependencies should get pulled in.

If running on Gnome, you need the
[Tray Icons: Reloaded](https://extensions.gnome.org/extension/2890/tray-icons-reloaded/)
extension.

# Create a Mac DMG image

```shell
./mvnw -B clean package
cd target
jpackage --verbose --name mcalert --input quarkus-app \
    --description "Read Prometheus endpoint and show status as toolbar icon" \
    --icon ../mcalert.icns \
    --main-jar quarkus-run.jar 
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

### Dev mode configuration:

Dev mode configuration in [application.properties](src%2Fmain%2Fresources%2Fapplication.properties),
expects a port-forward proxy to Prometheus similar to:
```
kubectl port-forward -n monitoring svc/prometheus-k8s 9090
 ```


## Details

This is a Quarkus based application created with:

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

## Icons

Icons downloaded from
- https://remixicon.com/icon/cloud-off-fill
- https://remixicon.com/icon/bug-line
- https://remixicon.com/icon/circle-line
- https://remixicon.com/icon/pulse-line
- https://remixicon.com/icon/shut-down-line
- https://remixicon.com/icon/information-off-line
