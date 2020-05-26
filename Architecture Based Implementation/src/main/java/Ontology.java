import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Ontology {

    // input:
    // returns: False if invocation failed, true if no errors
    boolean invoke(HashMap<String, String> action) throws IOException {

        // it will still be null if the KEY does not exist: add in 'StaticInterpreter'
        String errorString = "ERROR";
        // replace all null values with 'not available' values
        for (String key : action.keySet()) {
            System.out.println(key);
            action.putIfAbsent(key, "NA");
        }

//        String function   = errorString;
//        String process_id = errorString;
//        String sid        = errorString;
//        String algorithm  = errorString;
//        String features   = errorString;
//        String label      = errorString;

        // TODO: add all parameters
        String function   = action.get("action");
        String process_id = action.get("process_id");
        String sid        = action.get("sid");
        String algorithm  = action.get("algorithm");
        String features   = action.get("features");
        String label      = action.get("label");

        // TODO: slim this code down: https://stackoverflow.com/questions/31412294/java-check-not-null-empty-else-assign-default-value
        if (function == null) {
            function = errorString;
        }
        if (process_id == null) {
            process_id = errorString;
        }
        if (sid == null) {
            sid = errorString;
        }
        if (algorithm == null) {
            algorithm = errorString;
        }
        if (features == null) {
            features = errorString;
        }
        if (label == null) {
            label = errorString;
        }

//        System.out.println(function);
//        System.out.println(process_id);
//        System.out.println(sid);
//        System.out.println(filePath);

        System.out.println("action=" + action);
        System.out.println("process_id=" + process_id);
        System.out.println("sid=" + sid);
        System.out.println("algorithm=" + algorithm);
        System.out.println("features=" + features);
        System.out.println("label=" + label);

        Process process = null;
        process = Runtime.getRuntime().exec(new String[]{
                "python3", "src/main/python/ontology.py",
                "--function", function,
                "--pid",      process_id,
                "--sensorId", sid,
                "--algorithm",algorithm,
                "--features", features,
                "--label",    label,

//                    "--filePath", filePath
        });

        // ------- https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program
        assert process != null;
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));
        // read the output from the command
        System.out.println("Here is the standard output of the command:\n");

        while (true)
        {
            String s;
            try
            {
                if ((s = stdInput.readLine()) == null) break;
                System.out.println(s);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        // read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while (true)
        {
            String s = null;
            try
            {
                if ((s = stdError.readLine()) == null) break;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            System.out.println(s);
        }
        // --------
        return false;
    }

}
