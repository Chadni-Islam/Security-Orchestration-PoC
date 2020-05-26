import java.io.IOException;
import java.util.HashMap;

public class Classifier {

    // - call the classifier using the ontology

    // then 'upload' the data to Splunk using 'runreport'

//    Command 1: ’trains’ the classifier
//
//    python3 classifier.py "asn owner country attr_date comment netRatio payRatio extRatio artRatio miscRatio blacklist" rid tools
//
//    Command 2: ‘predicts’ using the classifier
//
//    python3 classify_data.py output/asn_owner_country_attrdate_comment_netRatio_payRatio_extRatio_artRatio_miscRatio_blacklist-rid-threat_actor.sav example_input.csv
//
//    $features = features to train (csv columns)
//    $algorithm = algo to train with
//    $label = feature to predict
//
//    $input = path to input file
//    $output = path to output file
//
//    python3 classifier.py $features $algorithm $label
//    python3 classify_data.py $output $input

    // container for fields which needs to be fetched from NEW_PROCESS data
    // Keys are exactly as CSV headers, Values are normalized versions
    private static final HashMap<String, String> train_fields = new HashMap<>();
    private static final HashMap<String, String> predict_fields = new HashMap<>();
    private static final HashMap<String, String> upload_fields = new HashMap<>();

    private String features       = "asn owner country attr_date comment netRatio payRatio extRatio artRatio miscRatio blacklist";
    private String algorithm      = "rid";
    private String label          = "tools";
    private String inputFilePath  = "~/MISP-Classifier/MISP-Classifier/example_input.csv";
    private String outputFilePath = "~/MISP-Classifier/MISP-Classifier/output/" + features.replace(" ", "_") + "-" + algorithm + "-" + label + ".sav";

    private void populateHashMaps() {

        System.out.println("Classifier.populateHashMaps()");

        train_fields.put("action", "ClassifierTrain");
        train_fields.put("message", "success");

        train_fields.put("features", features); // the csv file headers
        train_fields.put("algorithm",algorithm);
        train_fields.put("label",    label); // the feature to predict for

        predict_fields.put("action", "ClassifierPredict");
        predict_fields.put("message", "success");

        predict_fields.put("inputFilePath", inputFilePath); // path to the .sav file created by 'ClassifierTrain'
        predict_fields.put("outputFilePath", outputFilePath); // path to the .sav file created by 'ClassifierTrain'

        upload_fields.put("action", "UploadFile");
        upload_fields.put("message", "success");
        upload_fields.put("inputFilePath", outputFilePath); // path to the .sav file created by 'ClassifierTrain'
    }

    public void invoke() throws IOException {

        // set up the headers
        populateHashMaps();

        Ontology ontology = new Ontology();

        // train the data on the OSINT scraped from MISP
        ontology.invoke(train_fields);

        // run the prediction on the input data
        ontology.invoke(predict_fields);

        // upload the result to all SIEM tools
        ontology.invoke(upload_fields);

    }



}
