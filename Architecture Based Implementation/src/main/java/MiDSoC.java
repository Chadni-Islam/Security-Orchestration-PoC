import java.io.IOException;
import java.nio.file.NotDirectoryException;

/**
 * Driver class for MiDSoC.
 */
public class MiDSoC {

  /**
   * Driver method for MiDSoC
   * @param args Expects args[0] to be LC output path and args[1] to be Splunk output path
   */
    public static void main(String[] args) {

      StaticInterpreter staticInterpreter = new StaticInterpreter();

      try {
        new CollectorLC    (args[0], staticInterpreter);
        new CollectorSplunk(args[1], staticInterpreter);
      } catch (NotDirectoryException e) {
        e.printStackTrace();
      }

      Classifier classifier = new Classifier();
      try {
        classifier.invoke();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
}
