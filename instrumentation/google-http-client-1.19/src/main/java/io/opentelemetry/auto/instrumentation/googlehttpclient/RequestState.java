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

package io.opentelemetry.auto.instrumentation.googlehttpclient;

import io.opentelemetry.trace.Span;
import java.util.Objects;

public class RequestState {

  private Span span;

  public RequestState(Span span) {
    setSpan(span);
  }

  public Span getSpan() {
    return span;
  }

  public void setSpan(Span span) {
    Objects.requireNonNull(span);
    this.span = span;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RequestState that = (RequestState) o;
    return Objects.equals(span, that.span);
  }

  @Override
  public int hashCode() {
    return Objects.hash(span);
  }
}
