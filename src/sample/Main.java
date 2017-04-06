package sample;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class Main extends Application {
    /** matrix */
    private Character[][] matrix = new Character[22][80];

    /** unicode -> ASCII bidirectional map */
    private BiMap<Character, Character> BiASCIIUnicodeMap = HashBiMap.create();

    private void mapInit(){
        BiASCIIUnicodeMap.put((char)219, (char)0x2588);
        BiASCIIUnicodeMap.put((char)186, (char)0x2551);
        BiASCIIUnicodeMap.put((char)187, (char)0x2557);
        BiASCIIUnicodeMap.put((char)188, (char)0x255D);
        BiASCIIUnicodeMap.put((char)200, (char)0x255A);
        BiASCIIUnicodeMap.put((char)201, (char)0x2554);
        BiASCIIUnicodeMap.put((char)205, (char)0x2550);
        BiASCIIUnicodeMap.put((char)254, (char)0x25A0);
    }

    private Character convert(Character c) {
        Character character = BiASCIIUnicodeMap.get(c);
        if (character == null)
            character = BiASCIIUnicodeMap.inverse().get(c);
        if (character == null)
            return c;
        return character;
    }

    private void makeFrame() {
        final int rows = matrix.length;
        final int cols = matrix[0].length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++)
                if (i == 0)
                    if (j == 0)
                        matrix[i][j] = 0xC9;
                    else if (j == cols - 1)
                        matrix[i][j] = 0xBB;
                    else
                        matrix[i][j] = 0xCD;
                else if (i == rows - 1)
                    if (j == 0)
                        matrix[i][j] = 0xC8;
                    else if (j == cols - 1)
                        matrix[i][j] = 0xBC;
                    else
                        matrix[i][j] = 0xCD;
                else
                    if (j == 0 || j == cols - 1)
                        matrix[i][j] = 0xBA;
                    else
                        matrix[i][j] = 0x20;
        }
    }

    private void writeToFileAsSource(File file) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(file);

        final int rows = matrix.length;
        final int cols = matrix[0].length;

        for (int i = 0; i < rows; i++) {
            out.print("db ");
            for (int j = 0; j < cols; j++) {
                out.printf("0%Xh,0Fh", (byte) matrix[i][j].charValue());
                if (j != cols - 1)
                    out.print(",");
            }
            out.println();
        }
        out.close();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Painter-2.0");
        mapInit();
        makeFrame();

        // perform save
        Button action = new Button("Save");
        action.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            // set suitable
//            fileChooser.setInitialDirectory(new File(""));
            fileChooser.setInitialFileName("map.txt");
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null)
                try {
                    writeToFileAsSource(file);
                } catch (IOException e) {
                    // what a pity!
                    e.printStackTrace();
                }
        });

        // choice box
        ChoiceBox<Character> choiceBox = new ChoiceBox<>();
        for (Map.Entry<Character, Character> entry : BiASCIIUnicodeMap.entrySet()) {
            choiceBox.getItems().add(entry.getValue());
        }
        choiceBox.getSelectionModel().clearAndSelect(0);

        TableView<Character[]> table = new TableView<>();

        ObservableList<Character[]> data = FXCollections.observableArrayList();
        data.addAll(Arrays.asList(matrix));

        for (int i = 0; i < matrix[0].length; i++) {
            TableColumn<Character[], String> tc = new TableColumn<>(String.valueOf(i + 1));
            final int colNo = i;
            tc.setCellValueFactory(p -> new SimpleStringProperty(
                    Character.toString(convert(p.getValue()[colNo]))
            ));
            // cell factory
            Callback<TableColumn<Character[], String>, TableCell<Character[], String>> cellFactory = new Callback<TableColumn<Character[], String>, TableCell<Character[], String>>() {
                @Override
                public TableCell<Character[], String> call(TableColumn<Character[], String> param) {
                    TableCell<Character[], String> tableCell = new TextFieldTableCell<Character[], String>() {
                        @Override
                        public void updateItem(String item, boolean empty) {
                            if (Objects.equals(item, getItem())) return;
                            super.updateItem(item, empty);
                            super.setText(item);
                            super.setGraphic(null);
                        }
                    };
                    tableCell.setOnMouseClicked(event -> {
                        Character character = choiceBox.getValue();
                        if (tableCell.getText().charAt(0) == character)
                            character = ' ';
                        matrix[tableCell.getTableRow().getIndex()][colNo] = convert(character);
                        tableCell.setText(String.valueOf(character));
                    });
                    tableCell.setPadding(new Insets(0, 0, 0, 0));
                    tableCell.setAlignment(Pos.CENTER);
                    tableCell.setEditable(true);
                    return tableCell;
                }
            };
            tc.setCellFactory(cellFactory);
            // view
            tc.setPrefWidth(20);
            tc.setSortable(false);
            table.getColumns().add(tc);
        }

        table.setEditable(true);
        table.setItems(data);
        table.setSelectionModel(null);

        VBox root = new VBox();
        root.setPadding(new Insets(6, 0, 0,0));
        root.setSpacing(12);
        root.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(choiceBox, action, table);

        primaryStage.setScene(new Scene(root));
        primaryStage.setMinHeight(768);
        primaryStage.setMinWidth(1024);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
