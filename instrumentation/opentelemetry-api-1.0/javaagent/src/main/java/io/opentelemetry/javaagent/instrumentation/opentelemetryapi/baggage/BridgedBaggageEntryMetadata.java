package io.opentelemetry.javaagent.instrumentation.opentelemetryapi.baggage;

import application.io.opentelemetry.api.baggage.BaggageEntryMetadata;

public class BridgedBaggageEntryMetadata implements BaggageEntryMetadata,
    io.opentelemetry.api.baggage.BaggageEntryMetadata {

  public static BridgedBaggageEntryMetadata toAgent(BaggageEntryMetadata metadata) {
    if (metadata instanceof BridgedBaggageEntryMetadata) {
      return (BridgedBaggageEntryMetadata) metadata;
    }
    return new BridgedBaggageEntryMetadata(metadata.getValue());
  }

  public static BridgedBaggageEntryMetadata toApplication(io.opentelemetry.api.baggage.BaggageEntryMetadata metadata) {
    if (metadata instanceof BridgedBaggageEntryMetadata) {
      return (BridgedBaggageEntryMetadata) metadata;
    }
    return new BridgedBaggageEntryMetadata(metadata.getValue());
  }

  private final String value;

  public BridgedBaggageEntryMetadata(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
