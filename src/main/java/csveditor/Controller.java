package csveditor;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Controller {

    @FXML
    private TextField wikiUrl;
    @FXML
    private ChoiceBox<String> extrChoice;
    @FXML
    private TableView csvViewer;
    @FXML
    private ChoiceBox<Integer> numTable;
    @FXML
    private Button btnRegister;
    @FXML
    private Button btnExtract;
    @FXML
    private Button btnReset;

    private final String javaHtml = "Java - HTML";
    private final String javaWikitext = "Java - Wikitext";
    private final String python = "Python";

    private final String javaExtractorFolder = System.getProperty("user.dir") +
            File.separator + "javaExtractor" + File.separator;
    private final String javaExtractorOutput = javaExtractorFolder +
            "output" + File.separator;
    private final String groundTruthFolder = System.getProperty("user.dir")+
            File.separator + "groundTruth" + File.separator;
    private final String pythonExtractorFolder = System.getProperty("user.dir")+
            File.separator+"pythonExtractor"+File.separator;
    private final String pythonExtractorOutput = pythonExtractorFolder+
            "output"+File.separator;

    private final String javaScript = "java_script.sh";
    private final String javaWinScript = "java_script.bat";
    private final String pythonScript = "python_extractor.sh";
    private final String pythonWinScript = "python_extractor.bat";

    private String choicedExtractor;

    private int maxColumns, numberOfLineInCsvFile;

    private String fileName;

    // populate list of extractors
    ObservableList<String> extractor =
            FXCollections.observableArrayList(javaHtml, javaWikitext, python);

    //populate list of tables
    ObservableList<Integer> tableNumber;

    //data of tableview
    ObservableList<Map<String, String>> dataList = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        extrChoice.setItems(extractor);
        btnExtract.setDisable(true);
        btnRegister.setDisable(true);

        numTable.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Integer>() {
                    @Override
                    public void changed(ObservableValue<? extends Integer> observableValue, Integer oldValue, Integer newValue) {
                        if (newValue != null) {
                            //System.out.println("Number is changed");
                            fileName = fetchFile(newValue);
                            //System.out.println("The file name : " + fileName);
                            csvViewer.getColumns().clear();
                            csvViewer.getItems().clear();
                            csvReader(fileName);
                        }
                    }
                }
        );

        EventHandler<ActionEvent> confirmSaveDialog = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Enregistrement du fichier");
                alert.setContentText("Le contenu du tableau va être enregistrer. \nÊtes-vous sûre ?");

                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(((Node) actionEvent.getSource()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && (result.get() == ButtonType.OK)){
                    saveCsv();
                } else {
                    alert.close();
                }
            }
        };
        // Adding confirm event on register button
        btnRegister.setOnAction(confirmSaveDialog);
        // Reset button handle
        EventHandler<ActionEvent> reset = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                wikiUrl.clear();
                extrChoice.getSelectionModel().clearSelection();
                numTable.getItems().clear();
                csvViewer.getItems().clear();
                csvViewer.getColumns().clear();
                btnExtract.setDisable(true);
                btnRegister.setDisable(true);
            }
        };
        btnReset.setOnAction(reset);

    }

    @FXML
    public void enableExtractBtn() {
        // Set an event on wikiurl textfield
        wikiUrl.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (!t1.isBlank()) {
                    btnExtract.setDisable(false);
                } else {
                    btnExtract.setDisable(true);
                }
            }
        });
        // Set an event on extractor choice
        extrChoice.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                        if (s != null || t1 != null) {
                            btnExtract.setDisable(false);
                        } else {
                            btnExtract.setDisable(true);
                        }
                    }
                });
    }

    /**
     * Run extraction
     */
    public void extractCsv() {
        if ((!wikiUrl.getText().isEmpty() && !wikiUrl.getText().isBlank()) &&
                extrChoice.getValue() != null) {
            String pageTile = wikiUrl.getText().trim();
            if (extrChoice.getValue().contains("Java")) {
                deleteFiles(javaExtractorOutput);
            }else {
                deleteFiles(pythonExtractorOutput);
            }
            System.out.println("Fin de la suppression");

            choicedExtractor = extractor(extrChoice.getValue());

            try {
                writeInFile(pageTile);

                // Run java extractor since bash script
                this.runExtractor();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            new EventHandler<ActionEvent>(){
                @Override
                public void handle(ActionEvent actionEvent) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Attention");
                    alert.setHeaderText("Extraction lancer");
                    alert.setContentText("Veuillez saisir le titre de votre page " +
                            "\net sélection un extracteur avant d'exécuter");

                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(((Node) actionEvent.getSource()).getScene().getWindow());
                    alert.showAndWait();
                }
            };
        }
    }

    /**
     * Read csv file and write his contents in tableView
     * @param csvFile csv file to read
     */
    private void csvReader(String csvFile) {
        try {
            Reader reader;

            if (extrChoice.getValue().equals(javaHtml) ||
                    extrChoice.getValue().equals(javaWikitext)){
                reader = Files.newBufferedReader(Paths.get(javaExtractorOutput + choicedExtractor + csvFile));
            }else { // Case extractor is python
                reader = Files.newBufferedReader(Paths.get(pythonExtractorOutput + csvFile));
            }

            CSVReader csvReader = new CSVReader(reader);
            List<String[]> csvData = csvReader.readAll();
            numberOfLineInCsvFile = csvData.size();
            maxColumns = 0;
            for (String[] s: csvData){
                if (maxColumns < s.length){
                    maxColumns = s.length;
                }
            }

            for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
                TableColumn<Map, String> tableColumn = new TableColumn("Column "+colIndex);
                //Binding data in column
                tableColumn.setCellValueFactory(new MapValueFactory<>("Column "+colIndex));
                //Authorized modifications
                tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
                //Commit the edits
                int finalColIndex = colIndex;
                tableColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Map, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<Map, String> cell) {
                        cell.getTableView().getItems().get(
                                cell.getTablePosition().getRow()
                        ).put("Column "+ finalColIndex, cell.getNewValue());
                    }
                });
                csvViewer.getColumns().addAll(tableColumn);
            }

            for (String[] value: csvData) {
                Map<String, String> row = new HashMap<>();

                for (int cols = 0; cols < value.length; cols++) {
                    row.put("Column "+cols, value[cols]);
                }
                dataList.add(row);
            }
            csvViewer.setEditable(true);
            csvViewer.setItems(dataList);

            btnRegister.setDisable(false);
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save tableView contents as a csv file
     */
    private void saveCsv(){
        try(
                Writer writer = Files.newBufferedWriter(Paths.get(groundTruthFolder+fileName));
                CSVWriter csvWriter = new CSVWriter(writer,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.NO_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
        ) {
            //System.out.println("File path: "+groundTruthFolder+fileName);
            for (int i = 0; i < numberOfLineInCsvFile; i++) {
                HashMap contents = (HashMap) csvViewer.getItems().get(i);
                String[] values = new String[contents.size()];
                for (int j = 0; j < contents.size(); j++){
                    values[j] = (String) contents.get("Column " + j);
                    if (values[j].contains(",")){ // Escape comma character
                        values[j] = "\""+values[j]+"\"";
                    }
                }
                csvWriter.writeNext(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Provide csv file corresponding of a number
     * @param fileNumber number
     * @return csv file name
     */
    private String fetchFile(int fileNumber) {
        File file;

        if (extrChoice.getValue().equals(javaHtml) ||
                extrChoice.getValue().equals(javaWikitext)){
            file = new File(javaExtractorOutput + choicedExtractor + File.separator);
        }else { // Case extractor is python
           file = new File(pythonExtractorOutput + File.separator);
        }

        String[] list = file.list();

        for (String s : list) {
            int begin = s.lastIndexOf("_");
            int end = s.indexOf(".");
            int numFile = Integer.parseInt(s.substring(begin + 1, end));

            if (numFile == fileNumber) {
                return s;
            }
        }
        return null;
    }

    /**
     * Provide the selected extractor
     * @param choice choice
     * @return the extractor type
     */
    private String extractor(String choice) {
        String extractor;
        switch (choice) {
            case javaHtml:
                extractor = "html" + File.separator;
                break;
            case javaWikitext:
                extractor = "wikitext" + File.separator;
                break;
            case python:
                extractor = "python" + File.separator;
                break;
            default:
                extractor = null;
        }
        return extractor;
    }

    /**
     * Write the page title in urls file (wikiurls.txt)
     *
     * @param fileName title of the page
     * @throws IOException exception
     */
    private void writeInFile(String fileName) throws IOException {
        File file;

        if (extrChoice.getValue().equals(javaHtml) ||
                extrChoice.getValue().equals(javaWikitext)){
            file = new File(this.javaExtractorFolder + "wikiurls.txt");
        }else { // Case extractor is python
            file = new File(pythonExtractorFolder + "wikiurls.txt");
        }

        if (fileName.contains("https://")){
            int lastSlash = fileName.lastIndexOf("/");
            fileName = fileName.substring(lastSlash + 1);
        }

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(fileName);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Run java extractor in command line
     *
     * @throws IOException exception
     */
    private void runExtractor() throws IOException {
        Runtime runtime = Runtime.getRuntime();

        Process process;

        // Unix | Linux | Mac OS
        if (detectOS().equals("Linux") || detectOS().equals("Mac")) {
            if (extrChoice.getValue().contains("Java")) {
                process = runtime.exec(this.javaExtractorFolder + this.javaScript);
                executionTrace(process);

                while (process.isAlive()) {
                }
                processExtraction();
            } else {// Case it's python
                //System.out.println("Extracteur python");
                process = runtime.exec(this.pythonExtractorFolder+this.pythonScript);
                executionTrace(process);
                while (process.isAlive()){}
                processExtraction();
            }
        } else { // Windows OS

            if (extrChoice.getValue().contains("Java")) {
                String path = "\""+this.javaExtractorFolder+this.javaWinScript+"\"";
                process = runtime.exec("cmd.exe /C " + path);
                executionTrace(process);

                while (process.isAlive()){}
                processExtraction();

            }else { //Case python selected
                String path = "\""+this.pythonExtractorFolder+this.pythonWinScript+"\"";
                //System.out.println("Python extractor");
                process = runtime.exec("cmd.exe /C " + path);
                executionTrace(process);

                while (process.isAlive()){}
                processExtraction();
            }
        }
    }

    /**
     * Fill the list of number of tables extracted
     */
    private void processExtraction(){
        int numberOfTables = 0;
        //See in the right folder to count number of tables
        if (extrChoice.getValue().equals(javaHtml)){
            numberOfTables = numberOfTables("html");
        }else if(extrChoice.getValue().equals(javaWikitext)){
            numberOfTables = numberOfTables("wikitext");
        }else {
            numberOfTables = numberOfTables("output");
        }

        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= numberOfTables; i++) {
            list.add(i);
        }
        tableNumber = FXCollections.observableArrayList(list);
        numTable.setItems(tableNumber);
    }

    /**
     * Provide the host OS
     * @return the OS name
     */
    private String detectOS(){
        return System.getProperty("os.name");
    }

    /**
     * Provide the number of tables on page browsed
     * @param folder the destination folder of extracted files
     * @return the number of tables
     */
    public int numberOfTables(String folder){
        String path;

        if (extrChoice.getValue().equals(javaHtml) ||
                extrChoice.getValue().equals(javaWikitext)){
            path = this.javaExtractorOutput+folder+File.separator;
        }else { // Case extractor is python
            path = this.pythonExtractorFolder+folder+File.separator;
        }

        File file = new File(path);

        String[] numberOfFiles = file.list();

        if (numberOfFiles == null){
            return 0;
        }

        return numberOfFiles.length;
    }

    /**
     * Delete old extracted files
     * @param outputFolder path of folder
     */
    private void deleteFiles(String outputFolder){
        if (outputFolder.equals(javaExtractorOutput)) {
            String htmlPath = outputFolder + "html" + File.separator;
            String wikiPath = outputFolder + "wikitext" + File.separator;
            File htmlFile = new File(htmlPath);
            File wikiFile = new File(wikiPath);

            // Empty the html folder
            if (htmlFile.exists()) {
                for (File file : Objects.requireNonNull(htmlFile.listFiles())) {
                    file.delete();
                }
            }
            // Empty the wikitext folder...
            if (wikiFile.exists()) {
                for (File file : Objects.requireNonNull(wikiFile.listFiles())) {
                    file.delete();
                }
            }
        }else { // Case extractor is python
            File pyFile = new File(pythonExtractorOutput);
            // Empty the html folder
            if (pyFile.exists()) {
                for (File file : pyFile.listFiles()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Catch and show log message during process
     * @param process the process
     */
    private static void executionTrace(Process process){
        new Thread(){
            @Override
            public void run() {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            // process output flow
                            System.out.println("Output : " + line);
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
                        while ((line = reader.readLine()) != null) {
                            // process errors flow
                            System.out.println("Error : "+line);
                        }
                    }
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }.start();
    }
}
