/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleValueRecorder;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.common.Labels;
import io.opentelemetry.api.metrics.common.LabelsBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpServerRequestMetrics implements RequestMetrics {

  private static final ContextKey<State> HTTP_SERVER_REQUEST_METRICS_STATE =
      ContextKey.named("http-server-request-metrics-state");

  private static final Logger logger = LoggerFactory.getLogger(HttpServerRequestMetrics.class);

  public static RequestMetricsFactory factory() {
    return HttpServerRequestMetrics::new;
  }

  private final LongUpDownCounter activeRequests;
  private final DoubleValueRecorder duration;

  private HttpServerRequestMetrics(Meter meter) {
    activeRequests =
        meter
            .longUpDownCounterBuilder("http.server.active_requests")
            .setUnit("requests")
            .setDescription("The number of concurrent HTTP requests that are currently in-flight")
            .build();

    duration =
        meter
            .doubleValueRecorderBuilder("http.server.duration")
            .setUnit("milliseconds")
            .setDescription("The duration of the inbound HTTP request")
            .build();
  }

  @Override
  public Context start(Context context, Attributes attributes) {
    long startTimeNanos = System.nanoTime();
    Labels activeRequestLabels = activeRequestLabels(attributes);
    Labels durationLabels = durationLabels(attributes);
    activeRequests.add(1, activeRequestLabels);

    return context.with(
        HTTP_SERVER_REQUEST_METRICS_STATE,
        new AutoValue_HttpServerRequestMetrics_State(
            activeRequestLabels, durationLabels, startTimeNanos));
  }

  @Override
  public void end(Context context) {
    State state = context.get(HTTP_SERVER_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.debug(
          "No state present when ending context {}. Cannot reset HTTP request metrics.", context);
      return;
    }
    Labels labels = state.activeRequestLabels();
    activeRequests.add(-1, labels);
    duration.record(System.nanoTime() - state.startTimeNanos(), labels);
  }

  private static Labels activeRequestLabels(Attributes attributes) {
    LabelsBuilder labels = Labels.builder();
    attributes.forEach(
        (key, value) -> {
          if (key.getType() != AttributeType.STRING) {
            return;
          }
          switch (key.getKey()) {
            case "http.method":
            case "http.host":
            case "http.scheme":
            case "http.flavor":
            case "http.server_name":
              labels.put(key.getKey(), (String) value);
              break;
            default:
              // fall through
          }
        });
    return labels.build();
  }

  private static Labels durationLabels(Attributes attributes) {
    LabelsBuilder labels = Labels.builder();
    attributes.forEach(
        (key, value) -> {
          if (key.getType() != AttributeType.STRING) {
            return;
          }
          switch (key.getKey()) {
            case "http.method":
            case "http.host":
            case "http.scheme":
            case "http.status_code":
            case "http.flavor":
            case "http.server_name":
            case "net.host.name":
            case "net.host.port":
              labels.put(key.getKey(), (String) value);
              break;
            default:
              // fall through
          }
        });
    return labels.build();
  }

  @AutoValue
  abstract static class State {

    abstract Labels activeRequestLabels();

    abstract Labels durationLabels();

    abstract long startTimeNanos();
  }
}
