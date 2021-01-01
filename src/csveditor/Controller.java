package csveditor;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
            File.separator + "output" + File.separator;
    private final String groundTruthFolder = System.getProperty("user.dir")+
            File.separator + "groundTruth" + File.separator;

    private final String javaScript = "java_script.sh";
    private final String javaWinScript = "java_script.bat";

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

        //csvViewer.getItems().addAll(dataList);
        //System.out.println("Extracteur : "+extrChoice.getValue());
        numTable.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Integer>() {
                    @Override
                    public void changed(ObservableValue<? extends Integer> observableValue, Integer oldValue, Integer newValue) {
                        if (newValue != null) {
                            System.out.println("Number is changed");
                            fileName = fetchFile(newValue);
                            System.out.println("The file name : " + fileName);
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
        //btnExtract.setDisable(false);
    }

    /**
     * Run extraction
     */
    public void extractCsv() {
        if ((!wikiUrl.getText().isEmpty() && !wikiUrl.getText().isBlank()) &&
                extrChoice.getValue() != null) {
            String pageTile = wikiUrl.getText().trim();
            //System.out.println("Extracteur : "+extrChoice.getValue());
            deleteFiles(javaExtractorOutput);
            System.out.println("Fin de la suppression");

            choicedExtractor = extractor(extrChoice.getValue());

            try {
                writeInFile(pageTile);

                // Run java extractor since bash script
                this.runJavaExtractor();

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
        try (
                Reader reader = Files.newBufferedReader(Paths.get(javaExtractorOutput + choicedExtractor + csvFile));
                CSVReader csvReader = new CSVReader(reader);
        ) {
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
                //String[] value = csvData.get(line);
                //System.out.println("Line : "+ Arrays.toString(value));
                for (int cols = 0; cols < value.length; cols++) {
                    row.put("Column "+cols, value[cols]);

                }//System.out.println("Map: "+row);
                dataList.add(row);
            }
            csvViewer.setEditable(true);
            //csvViewer.getItems().clear();
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
                //FileWriter writer = new FileWriter(new File(groundTruthFolder+fileName));

                Writer writer = Files.newBufferedWriter(Paths.get(groundTruthFolder+fileName));
                CSVWriter csvWriter = new CSVWriter(writer,
                        CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.NO_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
        ) {
            System.out.println("File path: "+groundTruthFolder+fileName);
            for (int i = 0; i < numberOfLineInCsvFile; i++) {
                HashMap contents = (HashMap) csvViewer.getItems().get(i);
                String[] values = new String[contents.size()];
                for (int j = 0; j < contents.size(); j++){
                    values[j] = (String) contents.get("Column " + j);
                    if (values[j].contains(",")){ // Escape comma character
                        values[j] = "\""+values[j]+"\"";
                    }
                        /*if (j == contents.size() - 1) { // Case it's the last column
                            writer.write(values[j]);
                        }else { // Adding separator
                            writer.write(values[j]+separator);
                        }*/

                }
                //writer.write(System.lineSeparator());
                //tableData.add(values);
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
        File file = new File(javaExtractorOutput + choicedExtractor + File.separator);
        String[] list = file.list();

        for (String s : list) {
            int begin = s.lastIndexOf("_");
            int end = s.indexOf(".");
            int numFile = Integer.parseInt(s.substring(begin + 1, end));

            //System.out.println("Begin: " + begin + " end: " + end + " numFile: " + numFile);

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
        File file = new File(this.javaExtractorFolder + "wikiurls.txt");

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
    private void runJavaExtractor() throws IOException {
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
            } else if (extrChoice.getValue().equals(python)) {
                System.out.println("Extracteur python");
            }
        } else { // Windows OS
            if (extrChoice.getValue().contains("Java")) {
                process = runtime.exec("cmd.exe /C " + this.javaExtractorFolder + this.javaWinScript);
                executionTrace(process);

                while (process.isAlive()){}
                processExtraction();

            }else if (extrChoice.getValue().equals(python)){
                System.out.println("Python extractor");
            }
        }
    }

    private void processExtraction(){
        int numberOfTables = 0;
        //See in the right folder to count number of tables
        if (extrChoice.getValue().equals(javaHtml)){
            numberOfTables = numberOfJavaTables("html");
        }else if(extrChoice.getValue().equals(javaWikitext)){
            numberOfTables = numberOfJavaTables("wikitext");
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
        String os = System.getProperty("os.name");
        //System.out.println("L'os est : "+ os);
        return os;
    }

    /**
     * Provide the number of tables on page browsed
     * @param folder the destination folder of extracted files
     * @return the number of tables
     */
    public int numberOfJavaTables(String folder){
        String path = this.javaExtractorOutput+folder+File.separator;
        File file = new File(path);

        String[] numberOfFiles = file.list();

        if (numberOfFiles == null){
            return 0;
        }

        return numberOfFiles.length;
    }

    /**
     * Delete old extracted files
     * @param javaFolder path of folder
     */
    private static void deleteFiles(String javaFolder){
        String htmlPath = javaFolder+File.separator+"html"+File.separator;
        String wikiPath = javaFolder+File.separator+"wikitext"+File.separator;
        File htmlFile = new File(htmlPath);
        File wikiFile = new File(wikiPath);
        File[] htmlContents = htmlFile.listFiles();
        File[] wikiContents = wikiFile.listFiles();

        // Empty the html folder
        if (htmlContents != null) {
            for (File file: htmlContents){
                file.delete();
            }
        }
        // Empty the wikitext folder...
        if (wikiContents != null){
            for (File file : wikiContents){
                file.delete();
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
