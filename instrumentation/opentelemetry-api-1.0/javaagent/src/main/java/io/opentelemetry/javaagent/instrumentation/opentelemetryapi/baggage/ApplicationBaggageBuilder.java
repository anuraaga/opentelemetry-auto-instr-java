package io.opentelemetry.javaagent.instrumentation.opentelemetryapi.baggage;

import application.io.opentelemetry.api.baggage.Baggage;
import application.io.opentelemetry.api.baggage.BaggageBuilder;
import application.io.opentelemetry.api.baggage.BaggageEntryMetadata;
import application.io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.AgentContextStorage;

public class ApplicationBaggageBuilder implements BaggageBuilder {

  private final io.opentelemetry.api.baggage.BaggageBuilder agentBaggageBuilder;

  public ApplicationBaggageBuilder(
      io.opentelemetry.api.baggage.BaggageBuilder agentBaggageBuilder) {
    this.agentBaggageBuilder = agentBaggageBuilder;
  }

  @Override
  public BaggageBuilder setParent(Context context) {
    agentBaggageBuilder.setParent(AgentContextStorage.getAgentContext(context));
    return this;
  }

  @Override
  public BaggageBuilder setNoParent() {
    agentBaggageBuilder.setNoParent();
    return this;
  }

  @Override
  public BaggageBuilder put(String key, String value,
      BaggageEntryMetadata baggageEntryMetadata) {
    agentBaggageBuilder.put(key, value, BridgedBaggageEntryMetadata.toAgent(baggageEntryMetadata));
    return this;
  }

  @Override
  public BaggageBuilder put(String key, String value) {
    agentBaggageBuilder.put(key, value);
    return this;
  }

  @Override
  public BaggageBuilder remove(String key) {
    agentBaggageBuilder.remove(key);
    return this;
  }

  @Override
  public Baggage build() {
    return new ApplicationBaggage(agentBaggageBuilder.build());
  }
}
