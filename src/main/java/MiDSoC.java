import java.nio.file.NotDirectoryException;

/**
 * Driver class for middleware MiDSoC. Enables interoperability between the SIEM tool, Splunk
 * Enterprise, and the EDR tool, LimaCharlie.
 */
public class MiDSoC {

  /**
   * Driver method for MiDSoC
   *
   * @param args Expects args[0] to be LC output path and args[1] to be Splunk output path
   */
  public static void main(String[] args) {
    String lcPath = args[0];
    String splunkPath = args[1];

    StaticInterpreter staticInterpreter = new StaticInterpreter();

    // Setup collectors
    LimaCharlieCollector limaCharlieCollector;
    SplunkCollector splunkCollector;

    try {
      limaCharlieCollector = new LimaCharlieCollector(lcPath, staticInterpreter);
      splunkCollector = new SplunkCollector(splunkPath, staticInterpreter);
    } catch (NotDirectoryException e) {
      e.printStackTrace();
    }
  }
}
