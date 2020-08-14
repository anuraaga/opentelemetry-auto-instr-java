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

package io.grpc.override;

import com.linecorp.armeria.common.RequestContext;
import io.grpc.Context;
import io.grpc.Context.Storage;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextStorageOverride extends Storage {

  private static final Logger logger = LoggerFactory.getLogger(ContextStorageOverride.class);

  private static final ThreadLocal<Context> LOCAL_CONTEXT = new ThreadLocal<>();

  private static final AttributeKey<Context> CONTEXT =
      AttributeKey.valueOf(ContextStorageOverride.class, "CONTEXT");

  @Override
  public Context doAttach(Context toAttach) {
    RequestContext armeriaCtx = RequestContext.currentOrNull();
    Context current = current(armeriaCtx);
    if (armeriaCtx != null) {
      armeriaCtx.setAttr(CONTEXT, toAttach);
    } else {
      LOCAL_CONTEXT.set(toAttach);
    }
    return current;
  }

  @Override
  public void detach(Context toDetach, Context toRestore) {
    RequestContext armeriaCtx = RequestContext.currentOrNull();
    Context current = current(armeriaCtx);
    if (current != toDetach) {
      // Log a warning instead of throwing an exception as the context to attach is assumed
      // to be the correct one and the unbalanced state represents a coding mistake in a lower
      // layer in the stack that cannot be recovered from here.
      if (logger.isWarnEnabled()) {
        logger.warn("Context was not attached when detaching", new Throwable().fillInStackTrace());
      }
    }

    if (toRestore == Context.ROOT) {
      toRestore = null;
    }
    if (armeriaCtx != null) {
      // We do not ever restore the ROOT context when in the context of an Armeria request. The
      // context's lifecycle is effectively bound to the request, even through asynchronous flows,
      // so we do not ever need to clear it explicitly. It will disappear along with the request
      // when it's done.
      if (toRestore != null) {
        armeriaCtx.setAttr(CONTEXT, toRestore);
      }
    } else {
      LOCAL_CONTEXT.set(toRestore);
    }
  }

  @Override
  public Context current() {
    RequestContext armeriaCtx = RequestContext.currentOrNull();
    return current(armeriaCtx);
  }

  private static Context current(RequestContext armeriaCtx) {
    final Context current;
    if (armeriaCtx != null) {
      current = armeriaCtx.attr(CONTEXT);
    } else {
      current = LOCAL_CONTEXT.get();
    }
    return current != null ? current : Context.ROOT;
  }
}
