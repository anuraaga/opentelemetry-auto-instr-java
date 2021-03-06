/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.internal.urlclassloader;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class UrlClassLoaderInstrumentationModule extends InstrumentationModule {
  public UrlClassLoaderInstrumentationModule() {
    super("internal-url-class-loader");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new UrlClassLoaderInstrumentation());
  }
}
