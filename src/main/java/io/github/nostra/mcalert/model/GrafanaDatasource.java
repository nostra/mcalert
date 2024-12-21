package io.github.nostra.mcalert.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

///  ```
/// {
///     "id": 1,
///     "uid": "P1809F7CD0C75ACF3",
///     "orgId": 1,
///     "name": "prometheus",
///     "type": "prometheus",
///     "typeName": "Prometheus",
///     "typeLogoUrl": "public/app/plugins/datasource/prometheus/img/prometheus_logo.svg",
///     "access": "proxy",
///     "url": "http://prometheus-k8s.monitoring.svc:9090",
///     "user": "",
///     "database": "",
///     "basicAuth": false,
///     "isDefault": false,
///     "jsonData": {},
///     "readOnly": true
///   }
///  ```
@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaDatasource(
        String name,
        String uid
) {

}
