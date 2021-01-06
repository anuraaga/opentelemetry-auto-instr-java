package io.opentelemetry.javaagent.instrumentation.opentelemetryapi.baggage;

import application.io.opentelemetry.api.baggage.Baggage;
import application.io.opentelemetry.api.baggage.BaggageBuilder;
import application.io.opentelemetry.api.baggage.BaggageConsumer;
import application.io.opentelemetry.api.baggage.BaggageEntry;
import java.util.Map;

public class ApplicationBaggage implements Baggage {

  private final io.opentelemetry.api.baggage.Baggage agentBaggage;

  ApplicationBaggage(io.opentelemetry.api.baggage.Baggage agentBaggage) {
    this.agentBaggage = agentBaggage;
  }

  @Override
  public int size() {
    return agentBaggage.size();
  }

  @Override
  public void forEach(BaggageConsumer baggageConsumer) {
    agentBaggage.forEach(
        (key, value, metadata) ->
            baggageConsumer.accept(
                key, value, BridgedBaggageEntryMetadata.toApplication(metadata)));
  }

  @Override
  public Map<String, BaggageEntry> asMap() {
    return null;
  }

  @Override
  public String getEntryValue(String key) {
    return agentBaggage.getEntryValue(key);
  }

  @Override
  public BaggageBuilder toBuilder() {
    return new ApplicationBaggageBuilder(agentBaggage.toBuilder());
  }
}
