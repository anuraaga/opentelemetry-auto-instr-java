package io.opentelemetry.javaagent.instrumentation.opentelemetryapi.baggage;

import application.io.opentelemetry.api.baggage.BaggageEntry;

public class BridgedBaggageEntry implements BaggageEntry,
    io.opentelemetry.api.baggage.BaggageEntry {

  public static io.opentelemetry.api.baggage.BaggageEntry toAgent(BaggageEntry applicationEntry) {
    if (applicationEntry instanceof io.opentelemetry.api.baggage.BaggageEntry) {
      return (io.opentelemetry.api.baggage.BaggageEntry) applicationEntry;
    }
    return new BridgedBaggageEntry(applicationEntry.getValue(), new BridgedBaggageEntryMetadata(applicationEntry.getEntryMetadata().getValue()));
  }

  public static BaggageEntry toApplication(io.opentelemetry.api.baggage.BaggageEntry agentEntry) {
    if (agentEntry instanceof BaggageEntry) {
      return (BaggageEntry) agentEntry;
    }
    return new BridgedBaggageEntry(agentEntry.getValue(), BridgedBaggageEntryMetadata.toApplication(agentEntry.getEntryMetadata()));
  }

  private final String value;
  private final BridgedBaggageEntryMetadata metadata;

  public BridgedBaggageEntry(
      String value,
      BridgedBaggageEntryMetadata metadata) {
    this.value = value;
    this.metadata = metadata;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public BridgedBaggageEntryMetadata getEntryMetadata() {
    return metadata;
  }
}
