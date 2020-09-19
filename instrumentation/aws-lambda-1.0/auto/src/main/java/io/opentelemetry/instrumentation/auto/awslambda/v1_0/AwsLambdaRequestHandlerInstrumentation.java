/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.awslambda.v1_0;

import static io.opentelemetry.instrumentation.auto.awslambda.v1_0.AwsLambdaInstrumentationHelper.FUNCTION_TRACER;
import static io.opentelemetry.instrumentation.auto.awslambda.v1_0.AwsLambdaInstrumentationHelper.MESSAGE_TRACER;
import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.auto.service.AutoService;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.auto.api.OpenTelemetrySdkAccess;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class AwsLambdaRequestHandlerInstrumentation extends AbstractAwsLambdaInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("com.amazonaws.services.lambda.runtime.RequestHandler");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return implementsInterface(named("com.amazonaws.services.lambda.runtime.RequestHandler"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    // Order of transformation matters. A bit fragile, but our unit tests will catch breakage
    // easily.
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new LinkedHashMap<>();

    // First instrument the function invocation itself.
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(named("handleRequest"))
            .and(takesArgument(1, named("com.amazonaws.services.lambda.runtime.Context"))),
        AwsLambdaRequestHandlerInstrumentation.class.getName() + "$HandleRequestAdvice");

    // Then instrument message handling if appropriate.
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(named("handleRequest"))
            .and(takesArgument(0, named("com.amazonaws.services.lambda.runtime.events.SQSEvent")))
            .and(takesArgument(1, named("com.amazonaws.services.lambda.runtime.Context"))),
        AwsLambdaRequestHandlerInstrumentation.class.getName() + "$HandleSQSEventAdvice");

    return transformers;
  }

  public static class HandleRequestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(1) Context context,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope) {
      span = FUNCTION_TRACER.startSpan(context, Kind.SERVER);
      scope = FUNCTION_TRACER.startScope(span);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope) {
      scope.close();

      if (throwable != null) {
        FUNCTION_TRACER.endExceptionally(span, throwable);
      } else {
        FUNCTION_TRACER.end(span);
      }
      OpenTelemetrySdkAccess.forceFlush(1, TimeUnit.SECONDS);
    }
  }

  public static class HandleSQSEventAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) SQSEvent event,
        @Advice.Argument(1) Context context,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope) {
      span = MESSAGE_TRACER.startSpan(context, event);
      scope = MESSAGE_TRACER.startScope(span);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope) {
      scope.close();

      if (throwable != null) {
        MESSAGE_TRACER.endExceptionally(span, throwable);
      } else {
        MESSAGE_TRACER.end(span);
      }
    }
  }
}