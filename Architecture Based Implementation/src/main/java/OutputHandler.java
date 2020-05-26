import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class OutputHandler implements Runnable {

  private String outPath;
  private String toolFrom;
  private StaticInterpreter staticInterpreter;

  /**
   * Constructor for OutputHandler
   *
   * @param outPath path to output file to read and handle
   */
  OutputHandler(String outPath, String toolFrom, StaticInterpreter staticInterpreter) {

    System.out.println("OutputHandler(outPath=" + outPath + ", toolFrom=" + toolFrom + ")");

    this.toolFrom = toolFrom;
    this.outPath = outPath;
    this.staticInterpreter = staticInterpreter;
    Thread thread = new Thread(this);
    thread.start();
  }

  /**
   * Creates a thread for each arraylist of action
   * uses 'toolTo' and 'toolFrom' to execute the appropriate methods for each action in arraylist
   */
  @Override
  public void run() {

    // Get list of actions from output
    try {
      ArrayList<HashMap<String, String>> actions = staticInterpreter.interpret(this.outPath, this.toolFrom);

      for (HashMap<String, String> curAction : actions) {

        // Execute action based on information returned from interpreter
        Ontology ontology = new Ontology();
        ontology.invoke(curAction);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
