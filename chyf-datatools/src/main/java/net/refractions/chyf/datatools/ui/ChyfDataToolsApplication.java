/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.datatools.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.refractions.chyf.datatools.ChyfDistance2Water2DProcessor;
import net.refractions.chyf.datatools.ChyfSEAProcessor;
import net.refractions.chyf.datatools.processor.ProgressMonitor;

/**
 * UI for chyf data tools.
 * 
 * @author Emily
 *
 */
public class ChyfDataToolsApplication extends Application {

	public static final String HEADER_STYLE = "-fx-background-color: #444466;-fx-font-size: 1.1em; -fx-text-fill: white; -fx-font-weight:bold;";
	
	private FileChooser.ExtensionFilter FILTER_GEOPACKAGE = new FileChooser.ExtensionFilter("GeoPackage (*.gpkg)","*.gpkg");
	private FileChooser.ExtensionFilter FILTER_SHP = new FileChooser.ExtensionFilter("Shapefile (*.shp)","*.shp");
	private FileChooser.ExtensionFilter FILTER_TIFF = new FileChooser.ExtensionFilter("tif Files (*.tif, *.tiff, *.geotiff)","*.tif", "*.tiff", "*.geotiff");
	private FileChooser.ExtensionFilter FILTER_SUPPORTED = new FileChooser.ExtensionFilter("Supported Formats (*.shp, *.gpkg)","*.gpkg", "*.shp");
	private FileChooser.ExtensionFilter FILTER_ALL = new FileChooser.ExtensionFilter("All Files (*.*)","*.*");
	
	private File lastDir = null;
	
	private ProgressBar pbardistance;
	private Label plbldistance;
	private ProgressBar pbarsea;
	private Label plblsea;
	
	private List<Node> nodes;
	
	@Override
	public void start(Stage stage) throws Exception {
		nodes = new ArrayList<>();
		
		StackPane root = new StackPane();
		root.getChildren().add(buildUi());
		
		Scene scene = new Scene(root, 1040,570);
		stage.setTitle("CHyF Data Processing Tools");
		
		stage.setScene(scene);
		
		stage.show();
	}
	
	private Pane buildUi() {
		
		VBox outer = new VBox(10);
		outer.setFillWidth(true);
		outer.setStyle("-fx-background-color: #FFFFFF");

		Pane sea = buildSEAPane();
		outer.getChildren().add(sea);
		
		Pane distance = buildDistance2WaterPane();
		outer.getChildren().add(distance);
		
		VBox spacer = new VBox();
		outer.getChildren().add(spacer);
		VBox.setVgrow(spacer, Priority.ALWAYS);
		
		Label warn = new Label("Warning: Depending on the data size these computations may take many hours to complete.");
		outer.getChildren().add(warn);
		
		return outer;
	}
	
	private Pane buildSEAPane() {
		VBox seaPane = new VBox(10);
		seaPane.setFillWidth(true);
		seaPane.setPadding(new Insets(0,0,10,0));
		Label ll = new Label("Compute Elevation/Slope/Aspect Statistics");
		ll.setStyle(HEADER_STYLE);
		ll.setPadding(new Insets(5,5,5,5));
		ll.setMaxWidth(Double.MAX_VALUE);
		VBox.setVgrow(ll, Priority.ALWAYS);

		seaPane.getChildren().add(ll);
		
		GridPane grid = new GridPane();
		grid.setVgap(2);
		grid.setPadding(new Insets(0,10,0,10));

		seaPane.getChildren().add(grid);
		ll = new Label("Input Dataset*:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 0);
		
		TextField txtInFile = new TextField ();
		grid.add(txtInFile, 1, 0);
		GridPane.setHgrow(txtInFile, Priority.ALWAYS);
		nodes.add(txtInFile);
		
		Button btnSelection = new Button("...");
		grid.add(btnSelection, 2, 0);
		nodes.add(btnSelection);
		btnSelection.setOnAction(e->{
			getFile("Select Input Dataset", txtInFile, true, null, 
					FILTER_SUPPORTED, FILTER_GEOPACKAGE, FILTER_SHP, FILTER_ALL);
		});
		
		
		ll = new Label("DEM**:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 1);
		
		TextField txtDemFile = new TextField();
		grid.add(txtDemFile, 1, 1);
		nodes.add(txtDemFile);
		
		Button btnDemSelection = new Button("...");
		btnDemSelection.setOnAction(e->{
			getFile("Select DEM File", txtDemFile, 	true, 
					null, FILTER_TIFF, FILTER_ALL);
		});
		nodes.add(btnDemSelection);
		grid.add(btnDemSelection, 2, 1);

		
		ll = new Label("Output Dataset***:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 2);
		
		TextField txtOutFile = new TextField();
		grid.add(txtOutFile, 1, 2);
		nodes.add(txtOutFile);
		
		Button btnOutSelection = new Button("...");
		grid.add(btnOutSelection, 2, 2);
		nodes.add(btnOutSelection);
		btnOutSelection.setOnAction(e->{
			FileChooser.ExtensionFilter  sel = null;
			if (txtInFile.getText().endsWith("shp")) {
				sel = FILTER_SHP;
			}else if (txtInFile.getText().endsWith("gpkg")) {
				sel = FILTER_GEOPACKAGE;
			}
			getFile("Select Output File", txtOutFile, false, sel,
					FILTER_GEOPACKAGE, FILTER_SHP, FILTER_ALL);
					
		});
		
		ll = new Label("*Supported input datasets include shapefile or geopackage. For shapefiles pick the Catchment.shp file. Geopackage files require an ElementaryCatchment layer.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		grid.add(ll, 1, 3);
		nodes.add(ll);
		
		ll = new Label("**The .tif file must be a GeoTIFF. A conformal projection is recommended. See the documentation for details.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		grid.add(ll, 1, 4);
		nodes.add(ll);
		
		ll = new Label("***For shapefile inputs the output must be a shapefile.  For geopackage inputs the output must be a geopackage file.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		grid.add(ll, 1, 5);
		nodes.add(ll);
		
		HBox box = new HBox();
		box.setMaxWidth(Double.MAX_VALUE);
		
		plblsea = new Label();
		plblsea.setText("Processing ...");
		plblsea.setVisible(false);
		box.getChildren().add(plblsea);
		
		pbarsea = new ProgressBar(0);
		pbarsea.setVisible(false);
		pbarsea.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(pbarsea, Priority.ALWAYS);
		box.getChildren().add(pbarsea);
		
		grid.add(box, 1, 6);
		
		Button btnDoIt = new Button("Compute Elevation/Slope/Aspect Statistics");
		btnDoIt.setOnAction(e->{
			String txtIn = txtInFile.getText();
			String txtOut = txtOutFile.getText();
			String txtDem = txtDemFile.getText();
			computeSeaValues(txtIn, txtDem, txtOut);
		});
		
		
		nodes.add(btnDoIt);
		seaPane.setAlignment(Pos.CENTER);
		seaPane.getChildren().add(btnDoIt);
		
		return seaPane;
	}
	
	
	private Pane buildDistance2WaterPane() {
		VBox distancePane = new VBox(10);
		distancePane.setFillWidth(true);
		distancePane.setPadding(new Insets(0,0,10,0));
		distancePane.setAlignment(Pos.CENTER);
		
		Label ll = new Label("Compute 2D Distance To Water Statistics");
		ll.setStyle(HEADER_STYLE);
		ll.setPadding(new Insets(5,5,5,5));
		ll.setMaxWidth(Double.MAX_VALUE);
		VBox.setVgrow(ll, Priority.ALWAYS);

		distancePane.getChildren().add(ll);
		
		GridPane grid = new GridPane();
		grid.setVgap(2);
		grid.setPadding(new Insets(0,10,0,10));

		distancePane.getChildren().add(grid);
		ll = new Label("Input Dataset*:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 0);
		
		TextField txtInFile = new TextField ();
		grid.add(txtInFile, 1, 0);
		nodes.add(txtInFile);
		GridPane.setHgrow(txtInFile, Priority.ALWAYS);
		
		Button btnSelection = new Button("...");
		grid.add(btnSelection, 2, 0);
		nodes.add(btnSelection);
		btnSelection.setOnAction(e->{
			getFile("Select Input Dataset", txtInFile, true,
					null, FILTER_SUPPORTED, FILTER_GEOPACKAGE, FILTER_SHP, FILTER_ALL);
		});
		

		ll = new Label("Processing EPSG**:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 1);
		
		TextField txtSrid = new TextField("EPSG:3978");
		nodes.add(txtSrid);
		grid.add(txtSrid, 1, 1);
		
				
		ll = new Label("Output Dataset***:");
		ll.setMinWidth(Region.USE_PREF_SIZE);
		nodes.add(ll);
		grid.add(ll, 0, 2);
		
		TextField txtOutFile = new TextField();
		nodes.add(txtOutFile);
		grid.add(txtOutFile, 1, 2);
		
		Button btnOutSelection = new Button("...");
		grid.add(btnOutSelection, 2, 2);
		nodes.add(btnOutSelection);
		btnOutSelection.setOnAction(e->{
			FileChooser.ExtensionFilter  sel = null;
			if (txtInFile.getText().endsWith("shp")) {
				sel = FILTER_SHP;
			}else if (txtInFile.getText().endsWith("gpkg")) {
				sel = FILTER_GEOPACKAGE;
			}
			getFile("Select Output File", txtOutFile, false, sel,  	
					FILTER_GEOPACKAGE,FILTER_SHP, FILTER_ALL);
			
		});
		
		ll = new Label("*Supported input datasets include shapefiles or geopackage. For shapefiles pick the Catchment.shp file (Flowpath.shp and Waterbody.shp must also exist in the same directory).  Geopackage files require ElementaryCatchment, Waterbody and Flowpath layers.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		nodes.add(ll);
		grid.add(ll, 1, 3);
		
		ll = new Label("**Data is projected to this projection during processing. A conformal projection is recommended. The default is EPSG:3978. See the documentation for details.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		nodes.add(ll);
		grid.add(ll, 1, 4);
		
		
		ll = new Label("***For shapefile inputs the output must be a shapeilfe.  For geopackage inputs the output must be a geopackage file.");
		ll.setWrapText(true);
		ll.prefWidthProperty().bind(grid.widthProperty());
		nodes.add(ll);
		grid.add(ll, 1, 5);
		
		HBox box = new HBox();
		box.setMaxWidth(Double.MAX_VALUE);
		
		plbldistance = new Label();
		plbldistance.setText("Processing ...");
		plbldistance.setVisible(false);
		box.getChildren().add(plbldistance);
		
		pbardistance = new ProgressBar(0);
		pbardistance.setVisible(false);
		pbardistance.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(pbardistance, Priority.ALWAYS);
		box.getChildren().add(pbardistance);
		
		grid.add(box, 1, 6);
		
		Button btnDoIt = new Button("Compute Distance to Water 2D Statistics");
		nodes.add(btnDoIt);
		btnDoIt.setOnAction(e->{
			String txtIn = txtInFile.getText();
			String txtOut = txtOutFile.getText();
			String epsg = txtSrid.getText();
			computeDistance2Water(txtIn, epsg, txtOut);
		});
		
		
		distancePane.getChildren().add(btnDoIt);
		
		return distancePane;
	}
	
	private void enableControls(boolean enable) {
		for (Node n : nodes) n.setDisable(!enable);
	}
	
	private void getFile(String name,  TextField text, boolean open, FileChooser.ExtensionFilter selection, FileChooser.ExtensionFilter... items) {
		FileChooser fileChooser = new FileChooser();
		if (text.getText().trim().isEmpty()) {
			if (lastDir != null) {
				fileChooser.setInitialDirectory(lastDir);
			}else {
				fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
			}			
		}else {
			File f= new File(text.getText());
			fileChooser.setInitialDirectory(f.getParentFile());
		}
		fileChooser.setTitle("Select Output File");
		fileChooser.getExtensionFilters().addAll(items);
		if (selection != null) fileChooser.setSelectedExtensionFilter(selection);
		File selectedFile = null;
		if (open) {
			selectedFile = fileChooser.showOpenDialog(text.getScene().getWindow());
			if (selectedFile == null) return;
		}else {
			selectedFile = fileChooser.showSaveDialog(text.getScene().getWindow());
			if (selectedFile == null) return;
		}
		lastDir = selectedFile.getParentFile();
		text.setText(selectedFile.toString());
	}
	
	private void computeDistance2Water(String inFile, String srid, String outFile) {
		Path input = Paths.get(inFile);
		if (!Files.exists(input)) {
			showError("Input file doesn't exist");
			return;
		}
		boolean isshp = false;
		if (input.toString().endsWith("shp")) {
			input = input.getParent();
			isshp = true;
		}
		
		Path output = Paths.get(outFile);
		if (Files.exists(output)) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Overwrite");
			alert.setHeaderText(null);
			alert.setContentText("The file " + output.toString() + "exists.  Do you want to overwrite?");
			final boolean[] delete = new boolean[] {false};
			alert.showAndWait().ifPresent(rs -> {
			    if (rs == ButtonType.OK) {
			        delete[0] = true;
			    }
			});
			if (delete[0]) {
				try {
					Files.delete(output);
				} catch (IOException e) {
					showError("Unable to delete file: " + e.getMessage(), e);
					return;
				}
			}else{
				return;
			};
		}
		
		if (isshp) {
			if (!output.toString().endsWith("shp")) {
				showError("Output file must be shapefile for shapefile input");
				return;
			}
		}
		if (input.toString().endsWith("gpkg") && !output.toString().endsWith("gpkg")) {
			showError("Output file must be geopackage for geopackage input");
			return;
		}
		
		final String inputString = input.toString();
		final String outputString = output.toString();
		final String sridString = srid;
		
		ProgressMonitor monitor = new ProgressMonitor() {
			public void worked(int amount) {
				super.worked(amount);
				pbardistance.setProgress(getPercentage() / 100.0);
			}
		};
		
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				(new ChyfDistance2Water2DProcessor()).compute(inputString, outputString, sridString, monitor);
				return null;
			}
		};
		
		enableControls(false);
		task.setOnSucceeded(e->{
			showInformation("Processing Complete.  Output written to " + output.toString() + ".");
			pbardistance.setVisible(false);
			plbldistance.setVisible(false);
			enableControls(true);
		});
		task.setOnFailed(e->{
			showError("Processing Error: " + task.getException().getMessage(), task.getException());
			pbardistance.setVisible(false);
			plbldistance.setVisible(false);
			enableControls(true);
		});
		task.setOnCancelled(e->{
			showError("Processing Cancelled by User");
			pbardistance.setVisible(false);
			plbldistance.setVisible(false);
			enableControls(true);
		});
		pbardistance.setProgress(0);
		pbardistance.setVisible(true);
		plbldistance.setVisible(true);
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();	
		
	}
	
	
	private void computeSeaValues(String inFile, String demFile, String outFile) {
		Path input = Paths.get(inFile);
		if (!Files.exists(input)) {
			showError("Input file doesn't exist");
			return;
		}
		Path dem = Paths.get(demFile);
		if (!Files.exists(dem)) {
			showError("Input DEM doesn't exist");
			return;
		}
		boolean isshp = false;
		if (input.toString().endsWith("shp")) {
			input = input.getParent();
			isshp = true;
		}
		
		Path output = Paths.get(outFile);
		if (Files.exists(output)) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Overwrite");
			alert.setHeaderText(null);
			alert.setContentText("The file " + output.toString() + "exists.  Do you want to overwrite?");
			final boolean[] delete = new boolean[] {false};
			alert.showAndWait().ifPresent(rs -> {
			    if (rs == ButtonType.OK) {
			        delete[0] = true;
			    }
			});
			if (delete[0]) {
				try {
					Files.delete(output);
				} catch (IOException e) {
					showError("Unable to delete file: " + e.getMessage(), e);
					return;
				}
			}else{
				return;
			};
		}
		
		if (isshp) {
			if (!output.toString().endsWith("shp")) {
				showError("Output file must be shapefile for shapefile input");
				return;
			}
		}
		if (input.toString().endsWith("gpkg") && !output.toString().endsWith("gpkg")) {
			showError("Output file must be geopackage for geopackage input");
			return;
		}
		
		final String inputString = input.toString();
		final String outputString = output.toString();
		final String demString = dem.toString();
		
		ProgressMonitor monitor = new ProgressMonitor() {
			public void worked(int amount) {
				super.worked(amount);
				pbarsea.setProgress(getPercentage() / 100.0);
			}
		};
		
		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				(new ChyfSEAProcessor()).compute(inputString, demString, outputString, monitor);
				return null;
			}
		};
		
		enableControls(false);
		task.setOnSucceeded(e->{
			showInformation("Processing Complete.  Output written to " + output.toString() + ".");
			pbarsea.setVisible(false);
			plblsea.setVisible(false);
			enableControls(true);
		});
		task.setOnFailed(e->{
			showError("Processing Error: " + task.getException().getMessage(), task.getException());
			pbarsea.setVisible(false);
			plblsea.setVisible(false);
			enableControls(true);
		});
		task.setOnCancelled(e->{
			showError("Processing Cancelled by User");
			pbarsea.setVisible(false);
			plblsea.setVisible(false);
			enableControls(true);
		});
		pbarsea.setProgress(0);
		pbarsea.setVisible(true);
		plblsea.setVisible(true);
		Thread th = new Thread(task);
		th.setDaemon(true);
		th.start();	
	}
	
	private void showError(String error, Throwable ex) {
		Alert a = new Alert(AlertType.ERROR);
		a.setTitle("Error");
		a.setHeaderText(error);
		
		StringWriter stackTraceWriter = new StringWriter();
		ex.printStackTrace(new PrintWriter(stackTraceWriter));

		String exceptionText = stackTraceWriter.toString();
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(false);
		
		a.getDialogPane().setContent(textArea);
		
		a.showAndWait();
	}
	private void showError(String error) {
		
		Alert a = new Alert(AlertType.ERROR);
		a.setTitle("Error");
		a.setHeaderText(null);
		a.setContentText(error);
		a.showAndWait();
	}
	
	private void showInformation(String info) {
		
		Alert a = new Alert(AlertType.INFORMATION);
		a.setTitle("CHyF Data Tools");
		a.setHeaderText(null);
		a.setContentText(info);
		a.showAndWait();
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
