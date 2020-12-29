package csveditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;

public class Controller {

    @FXML
    private TextField wikiUrl;
    @FXML
    private ChoiceBox<String> extrChoice;
    @FXML
    private TableView<String> csvViewer;
    @FXML
    private ChoiceBox<Integer> numTable;
    @FXML
    private Button btnRegister;
    @FXML
    private Button btnExtract;

    private String javaHtml = "Java - HTML/Wikitext";
    private String python = "Python";

    private final String javaExtractorFolder = System.getProperty("user.dir")+
            File.separator+"javaExtractor"+File.separator;
    private final String javaScript = "java_script.sh";
    private final String javaWinScript = "java_script.bat";

    ObservableList<String> extractor =
            FXCollections.observableArrayList(javaHtml, python);

    @FXML
    public void initialize(){
        extrChoice.setItems(extractor);
        btnExtract.setDisable(true);
        btnRegister.setDisable(true);
        //detectOS();
    }
    @FXML
    public void enableExtractBtn(){
        if(wikiUrl.getText().length() > 0 && !wikiUrl.getText().isBlank()){
            btnExtract.setDisable(false);
        }else {
            btnExtract.setDisable(true);
        }
    }

    public void extractCsv(){
        String pageTile = wikiUrl.getText().trim();
        try {
            writeInFile(pageTile);

            // Run java extractor since bash script
            this.runJavaExtractor();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInFile(String fileName) throws IOException {
        File file = new File(this.javaExtractorFolder+"wikiurls.txt");

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(fileName);
        fileWriter.flush();
        fileWriter.close();
    }

    private void runJavaExtractor() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        // Unix Linux OS

        if (detectOS().equals("Linux") || detectOS().equals("Mac")) {
            runtime.exec(this.javaExtractorFolder+this.javaScript);
        } else {
            runtime.exec("cmd.exe /C "+this.javaExtractorFolder+this.javaWinScript);
        }
        /*final Process process = runtime.exec(this.javaExtractorFolder+this.javaScript);

        new Thread(){
            @Override
            public void run() {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = "";
                        int i = 0;
                        while ((line = reader.readLine()) != null) {
                            // Traitement du flux de sortie de l'application si besoin est
                            System.out.println("Sortie " + i + " : " + line);
                            i++;
                        }
                    }
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }.start();

        new Thread(){
            @Override
            public void run() {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line = "";
                        int i = 0;
                        while ((line = reader.readLine()) != null) {
                            // Traitement du flux d'erreur de l'application si besoin est
                            System.out.println("Entree "+i+" : "+ line);
                            i++;
                        }
                    }
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }.start();*/
    }

    private String detectOS(){
        String os = System.getProperty("os.name");
        //System.out.println("L'os est : "+ os);
        return os;
    }
}
