# Metrics

## Reporter

```java
.interceptors(registry -> {
  registry.forConnection(new MetricsRsConnectionInterceptor(Metrics.REGISTRY));
  registry.forResponder(new MetricsRsResponderInterceptor(Metrics.REGISTRY));
}) //
```

## Collector

```bash
cd metrics 
docker compose up
open http://localhost:3000 # admin/admin
```



