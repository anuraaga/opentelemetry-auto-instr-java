/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_1.server;

import static io.opentelemetry.javaagent.instrumentation.netty.v4_1.server.NettyHttpServerTracer.TRACER;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.trace.Span;

public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel channel = ctx.channel();

    if (!(msg instanceof HttpRequest)) {
      Context serverContext = TRACER.getServerContext(channel);
      if (serverContext == null) {
        ctx.fireChannelRead(msg);
      } else {
        try (Scope ignored = serverContext.makeCurrent()) {
          ctx.fireChannelRead(msg);
        }
      }
      return;
    }

    Context context = TRACER.startSpan((HttpRequest) msg, channel, "netty.request");
    Span span = Java8BytecodeBridge.spanFromContext(context);
    try (Scope ignored = TRACER.startScope(span, channel)) {
      ctx.fireChannelRead(msg);
    } catch (Throwable throwable) {
      TRACER.endExceptionally(span, throwable);
      throw throwable;
    }
  }
}
