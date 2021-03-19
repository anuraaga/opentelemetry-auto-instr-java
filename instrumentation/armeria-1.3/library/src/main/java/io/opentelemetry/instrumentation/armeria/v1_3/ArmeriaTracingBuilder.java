/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.StatusExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ArmeriaTracingBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.armeria-1.3";

  private final InstrumenterBuilder<ClientRequestContext, RequestLog> clientInstrumenterBuilder;
  private final InstrumenterBuilder<ServiceRequestContext, RequestLog> serverInstrumenterBuilder;

  private Function<
          SpanNameExtractor<RequestContext>, ? extends SpanNameExtractor<? super RequestContext>>
      spanNameExtractorTransformer = Function.identity();
  private Function<
          StatusExtractor<RequestContext, RequestLog>,
          ? extends StatusExtractor<? super RequestContext, ? super RequestLog>>
      statusExtractorTransformer = Function.identity();
  private final List<AttributesExtractor<? super RequestContext, ? super RequestLog>>
      additionalExtractors = new ArrayList<>();

  ArmeriaTracingBuilder(OpenTelemetry openTelemetry) {
    clientInstrumenterBuilder =
        Instrumenter.<ClientRequestContext, RequestLog>newBuilder(
                openTelemetry, INSTRUMENTATION_NAME)
            .setClientSpanKind(ClientRequestContextSetter.INSTANCE);
    serverInstrumenterBuilder =
        Instrumenter.<ServiceRequestContext, RequestLog>newBuilder(
                openTelemetry, INSTRUMENTATION_NAME)
            .setServerSpanKind(RequestContextGetter.INSTANCE);
  }

  public ArmeriaTracingBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<RequestContext>,
              ? extends SpanNameExtractor<? super RequestContext>>
          spanNameExtractor) {
    this.spanNameExtractorTransformer = spanNameExtractor;
    return this;
  }

  public ArmeriaTracingBuilder setStatusExtractor(
      Function<
              StatusExtractor<RequestContext, RequestLog>,
              ? extends StatusExtractor<? super RequestContext, ? super RequestLog>>
          statusExtractor) {
    this.statusExtractorTransformer = statusExtractor;
    return this;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  public ArmeriaTracingBuilder addAttributeExtractor(
      AttributesExtractor<? super RequestContext, ? super RequestLog> attributesExtractor) {
    additionalExtractors.add(attributesExtractor);
    return this;
  }

  public ArmeriaTracing build() {
    ArmeriaHttpAttributesExtractor httpAttributesExtractor = new ArmeriaHttpAttributesExtractor();
    ArmeriaNetAttributesExtractor netAttributesExtractor = new ArmeriaNetAttributesExtractor();

    SpanNameExtractor<? super RequestContext> spanNameExtractor =
        spanNameExtractorTransformer.apply(SpanNameExtractor.http(httpAttributesExtractor));
    StatusExtractor<? super RequestContext, ? super RequestLog> statusExtractor =
        statusExtractorTransformer.apply(StatusExtractor.http(httpAttributesExtractor));

    Stream.of(clientInstrumenterBuilder, serverInstrumenterBuilder)
        .forEach(
            instrumenter ->
                instrumenter
                    .setSpanNameExtractor(spanNameExtractor)
                    .setSpanStatusExtractor(statusExtractor)
                    .addAttributesExtractor(httpAttributesExtractor)
                    .addAttributesExtractor(netAttributesExtractor)
                    .addAttributesExtractors(additionalExtractors));

    return new ArmeriaTracing(clientInstrumenterBuilder.build(), serverInstrumenterBuilder.build());
  }
}
