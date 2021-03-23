/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.ClientSpan;
import io.opentelemetry.instrumentation.api.tracer.ServerSpan;
import java.util.List;

// TODO(anuraaga): Need to define what are actually useful knobs, perhaps even providing a
// base-class
// for instrumentation library builders.
/**
 * An instrumenter of the start and end of a request/response lifecycle. Almost all instrumentation
 * of libraries falls into modeling start and end, generating observability signals from these such
 * as a tracing {@link Span}, or metrics such as the duration taken, active requests, etc. When
 * instrumenting a library, there will generally be three steps.
 *
 * <ul>
 *   <li>Create an {@link Instrumenter} using {@link InstrumenterBuilder}. Use the builder to
 *       configure any library-specific customizations, and also expose useful knobs to your user.
 *   <li>Call {@link Instrumenter#start(Context, Object)} at the beginning of a request.
 *   <li>Call {@link Instrumenter#end(Context, Object, Object, Throwable)} at the end of a request.
 * </ul>
 */
public class Instrumenter<REQUEST, RESPONSE> {

  public static <REQUEST, RESPONSE> InstrumenterBuilder<REQUEST, RESPONSE> newBuilder(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      SpanNameExtractor<? super REQUEST> spanNameExtractor) {
    return new InstrumenterBuilder<>(openTelemetry, instrumentationName, spanNameExtractor);
  }

  private final Tracer tracer;
  private final SpanNameExtractor<? super REQUEST> spanNameExtractor;
  private final SpanKindExtractor<? super REQUEST> spanKindExtractor;
  private final StatusExtractor<? super REQUEST, ? super RESPONSE> statusExtractor;
  private final List<? extends AttributesExtractor<? super REQUEST, ? super RESPONSE>> extractors;
  private final ErrorCauseExtractor errorCauseExtractor;

  Instrumenter(
      Tracer tracer,
      SpanNameExtractor<? super REQUEST> spanNameExtractor,
      SpanKindExtractor<? super REQUEST> spanKindExtractor,
      StatusExtractor<? super REQUEST, ? super RESPONSE> statusExtractor,
      List<? extends AttributesExtractor<? super REQUEST, ? super RESPONSE>> extractors,
      ErrorCauseExtractor errorCauseExtractor) {
    this.tracer = tracer;
    this.spanNameExtractor = spanNameExtractor;
    this.spanKindExtractor = spanKindExtractor;
    this.statusExtractor = statusExtractor;
    this.extractors = extractors;
    this.errorCauseExtractor = errorCauseExtractor;
  }

  public Context start(Context parentContext, REQUEST request) {
    SpanKind spanKind = spanKindExtractor.extract(request);
    SpanBuilder spanBuilder =
        tracer
            .spanBuilder(spanNameExtractor.extract(request))
            .setSpanKind(spanKind)
            .setParent(parentContext);

    AttributesBuilder attributes = Attributes.builder();
    for (AttributesExtractor<? super REQUEST, ? super RESPONSE> extractor : extractors) {
      extractor.onStart(attributes, request);
    }
    attributes.build().forEach((key, value) -> spanBuilder.setAttribute((AttributeKey) key, value));

    Span span = spanBuilder.startSpan();
    Context context = parentContext.with(span);
    switch (spanKind) {
      case SERVER:
        return ServerSpan.with(context, span);
      case CLIENT:
        return ClientSpan.with(context, span);
      default:
        return context;
    }
  }

  public void end(Context context, REQUEST request, RESPONSE response, Throwable error) {
    Span span = Span.fromContext(context);

    AttributesBuilder attributes = Attributes.builder();
    for (AttributesExtractor<? super REQUEST, ? super RESPONSE> extractor : extractors) {
      extractor.onEnd(attributes, request, response);
    }
    attributes.build().forEach((key, value) -> span.setAttribute((AttributeKey) key, value));

    if (error != null) {
      error = errorCauseExtractor.extractCause(error);
      span.recordException(error);
    }

    span.setStatus(statusExtractor.extract(request, response, error));

    span.end();
  }
}
