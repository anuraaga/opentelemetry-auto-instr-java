package library;

public class IncorrectKeyClassUsageKeyClass {
  public boolean isInstrumented() {
    return false;
  }

  public int incorrectKeyClassUsage() {
    // instrumentation will not apply to this class because advice incorrectly uses context api
    return -1;
  }
}
