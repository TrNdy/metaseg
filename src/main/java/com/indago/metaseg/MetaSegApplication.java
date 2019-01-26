package com.indago.metaseg;
/**
 *
 */


import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.widget.WidgetService;

import com.apple.eawt.Application;
import com.indago.gurobi.GurobiInstaller;
import com.indago.metaseg.io.projectfolder.MetasegProjectFolder;
import com.indago.metaseg.ui.model.MetaSegModel;
import com.indago.metaseg.ui.view.MetaSegMainPanel;
import com.indago.plugins.seg.IndagoSegmentationPluginService;
import com.indago.ui.util.FrameProperties;
import com.indago.ui.util.UniversalFileChooser;
import com.indago.util.OSValidator;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import io.scif.codec.CodecService;
import io.scif.formats.qt.QTJavaService;
import io.scif.formats.tiff.TiffService;
import io.scif.img.ImgUtilityService;
import io.scif.img.converters.PlaneConverterService;
import io.scif.services.DatasetIOService;
import io.scif.services.FilePatternService;
import io.scif.services.FormatService;
import io.scif.services.InitializeService;
import io.scif.services.JAIIIOService;
import io.scif.services.LocationService;
import io.scif.services.TranslatorService;
import io.scif.xml.XMLService;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.ops.OpMatchingService;
import net.imagej.ops.OpService;
import net.imglib2.img.VirtualStackAdapter;
import weka.gui.ExtensionFileFilter;

/**
 * Starts the tr2d app.
 *
 * @author jug
 */
public class MetaSegApplication {

	/**
	 * true, iff this app is not started by the imagej2/fiji plugin (tr2d_)
	 */
	private final boolean isStandalone;

	private JFrame guiFrame;
	private MetaSegMainPanel mainPanel;

	private File inputStack;
	private MetasegProjectFolder projectFolder;

	private final OpService ops;
	private final IndagoSegmentationPluginService segPlugins;

	private final Logger log;

	public static void main( final String[] args ) {
		new MetaSegApplication().run(args);
	}

	public MetaSegApplication() {
		isStandalone = true;
		final ImageJ temp = IJ.getInstance();
		if ( temp == null ) {
			new ImageJ();
		}

		final Context context = new Context( FormatService.class, OpService.class, OpMatchingService.class,
				IOService.class, DatasetIOService.class, LocationService.class, DatasetService.class,
				ImgUtilityService.class, StatusService.class, TranslatorService.class, QTJavaService.class,
				TiffService.class, CodecService.class, JAIIIOService.class, LogService.class,
				IndagoSegmentationPluginService.class, PlaneConverterService.class, InitializeService.class,
				XMLService.class, FilePatternService.class, WidgetService.class);
		ops = context.getService( OpService.class );
		segPlugins = context.getService( IndagoSegmentationPluginService.class );
		log = context.getService( LogService.class ).subLogger( "metaseg" );
		log.info( "STANDALONE" );
	}

	public MetaSegApplication( final OpService opService, final IndagoSegmentationPluginService tr2dSegmentationPluginService, final Logger log )
	{
		isStandalone = false;
		if(tr2dSegmentationPluginService == null)
			log.error( "Metaseg failed to set the SegmentationPluginService!" );
		ops = opService;
		segPlugins = tr2dSegmentationPluginService;
		this.log = log;
		log.info( "PLUGIN" );
	}

	public void run( final String[] args ) {

		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		// GET THE APP SPECIFIC LOGGER
		// ---------------------------

		checkGurobiAvailability();
		parseCommandLineArgs( args );

		guiFrame = new JFrame( "tr2d" );
		if ( isStandalone ) setImageAppIcon();

		MetaSegContext.segPlugins = segPlugins;
		MetaSegContext.ops = ops;
		MetaSegContext.guiFrame = guiFrame;

		if(projectFolder == null || inputStack == null)
			openStackOrProjectUserInteraction();

		final ImgPlus imgPlus = openImageStack();

		if ( imgPlus != null ) {
			final MetaSegModel model = new MetaSegModel( projectFolder, imgPlus );
			mainPanel = new MetaSegMainPanel( guiFrame, model, log );

			guiFrame.getContentPane().add( mainPanel );
			setFrameSizeAndCloseOperation( model );
			guiFrame.setVisible( true );
			mainPanel.collapseLog();
		} else {
			guiFrame.dispose();
			if ( isStandalone ) System.exit( 0 );
		}
	}

	private void setFrameSizeAndCloseOperation( final MetaSegModel model ) {
		try {
			FrameProperties.load( projectFolder.getFile( MetasegProjectFolder.FRAME_PROPERTIES ).getFile(), guiFrame );
		} catch ( final IOException e ) {
			log.warn( "Frame properties not found. Will use default values." );
			guiFrame.setBounds( FrameProperties.getCenteredRectangle( 1200, 1024 ) );
		}

		guiFrame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		guiFrame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosing( final WindowEvent we ) {
				final Object[] options = { "Quit", "Cancel" };
				final int choice = JOptionPane.showOptionDialog(
						guiFrame,
						"Do you really want to quit MetaSeg?",
						"Quit?",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[ 0 ] );
				if ( choice == 0 ) {
					model.close();
					try {
						FrameProperties.save( projectFolder.getFile( MetasegProjectFolder.FRAME_PROPERTIES ).getFile(), guiFrame );
					} catch ( final Exception e ) {
						log.error( "Could not save frame properties in project folder!" );
						e.printStackTrace();
					}
					quit( 0 );
				}
			}
		} );
	}

	/**
	 * @param exit_value
	 */
	public void quit( final int exit_value ) {
		if(guiFrame != null)
			guiFrame.dispose();
		if ( isStandalone ) {
			System.exit( exit_value );
		}
	}

	/**
	 * @return
	 */
	private void openStackOrProjectUserInteraction() {
		UniversalFileChooser.showOptionPaneWithTitleOnMac = true;


		final Object[] options = { "MetaSeg Project...", "TIFF Stack..." };
		final int choice = JOptionPane.showOptionDialog(
				guiFrame,
				"Please choose an input type to be opened.",
				"Open...",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[ 0 ] );
		if ( choice == 0 ) { // ===== PROJECT =====
			openProjectUserInteraction();
		} else if ( choice == 1 ) { // ===== TIFF STACK =====
			openStackUserInteraction();
		}

		UniversalFileChooser.showOptionPaneWithTitleOnMac = false;
	}

	private void openStackUserInteraction()
	{
		chooseStackUserInteraction();

		boolean validSelection = false;
		while ( !validSelection ) {
			validSelection = chooseProjectFolderUserInteraction();
		}
	}

	private boolean chooseProjectFolderUserInteraction() {
		// Ask for folder...
		final File fileProjectFolder = UniversalFileChooser.showLoadFolderChooser(
				guiFrame,
				inputStack.getParent(),
				"Choose MetaSeg project folder..." );
		if ( fileProjectFolder == null ) {
			quit( 2 );
		}

		// Create the uninitialized project folder
		try {
			projectFolder = new MetasegProjectFolder( fileProjectFolder );
		} catch ( final IOException e ) {
			JOptionPane.showConfirmDialog(
					guiFrame,
					"Chosen project folder cannot be created.",
					"Error",
					JOptionPane.OK_OPTION );
			return false;
		}

		// If it IS a project folder... ask if it is ok to overwrite it...
		if ( MetasegProjectFolder.isValidProjectFolder( fileProjectFolder ) ) {
			final String msg = String.format(
					"Chosen project folder exists (%s).\nShould this project be overwritten?\nCurrent data in this project will be lost!",
					fileProjectFolder );
			final int overwrite = JOptionPane.showConfirmDialog( guiFrame, msg, "Project Folder Exists", JOptionPane.YES_NO_OPTION );
			if ( overwrite == JOptionPane.NO_OPTION ) return false;
		} else {
			// If it is NOT empty, point it out and refuse...
			// (NOTE: some OSes create hidden files right away into new folders. They start with '.'!)
			final File[] content = fileProjectFolder.listFiles();
			for ( final File f : content ) {
				if ( !f.getName().startsWith( "." ) ) {
					final int result = JOptionPane.showConfirmDialog(
							guiFrame,
							"Chosen project folder must be empty. Please choose another folder.",
							"Project Folder Not Empty",
							JOptionPane.OK_OPTION );
					return false;
				}
			}
		}

		try {
			projectFolder.initialize( this.inputStack );
		} catch ( final IOException e ) {
			log.error( String.format( "ERROR: Project folder (%s) could not be initialized.", fileProjectFolder.getAbsolutePath() ) );
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void chooseStackUserInteraction()
	{
		inputStack = UniversalFileChooser.showLoadFileChooser(
				guiFrame,
				"",
				"Load input tiff stack...",
				new ExtensionFileFilter( "tif", "TIFF Image Stack" ) );
		if ( inputStack == null ) {
			quit( 1 );
		}
	}

	private void openProjectUserInteraction()
	{
		UniversalFileChooser.showOptionPaneWithTitleOnMac = false;
		final File projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
				guiFrame,
				"",
				"Choose MetaSeg project folder..." );
		UniversalFileChooser.showOptionPaneWithTitleOnMac = true;
		if ( projectFolderBasePath == null ) {
			quit( 1 );
		}
		openProjectFolder(projectFolderBasePath);
	}

	private ImgPlus openImageStack() {
		ImagePlus imagePlus = null;
		if ( inputStack != null ) {
//			IJ.open( inputStack.getAbsolutePath() );
			imagePlus = IJ.openImage( inputStack.getAbsolutePath() );
			if ( imagePlus == null ) {
				IJ.error( "There must be an active, open window!" );
				quit( 4 );
			}
		}
		final ImgPlus< ? > imgPlus = VirtualStackAdapter.wrap( imagePlus );
		return imgPlus;
	}

	/**
	 *
	 */
	private void setImageAppIcon() {
		Image image = null;
		try {
			image = new ImageIcon( MetaSegApplication.class.getClassLoader().getResource( "metaseg_dais_icon_color.png" ) ).getImage();
		} catch ( final Exception e ) {
			try {
				image = new ImageIcon( MetaSegApplication.class.getClassLoader().getResource(
						"resources/metaseg_dais_icon_color.png" ) ).getImage();
			} catch ( final Exception e2 ) {
				log.error( "app icon not found..." );
			}
		}

		if ( image != null ) {
			if ( OSValidator.isMac() ) {
				log.info( "On a Mac! --> trying to set icons..." );
				Application.getApplication().setDockIconImage( image );
			} else {
				log.info( "Not a Mac! --> trying to set icons..." );
				guiFrame.setIconImage( image );
			}
		}
	}

	/**
	 * Check if GRBEnv can be instantiated. For this to work Gurobi has to be
	 * installed and a valid license has to be pulled.
	 */
	private void checkGurobiAvailability() {
		final String jlp = System.getProperty( "java.library.path" );
		if ( !GurobiInstaller.testGurobi() ) {
			final String msgs = "Initial Gurobi test threw exception... check your Gruobi setup!\n\nJava library path: " + jlp;
			JOptionPane.showMessageDialog(
					guiFrame,
					msgs,
					"Gurobi Error?",
					JOptionPane.ERROR_MESSAGE);
			quit(98);
		}
	}

	/**
	 * Parse command line arguments and set variables accordingly.
	 *
	 * @param args
	 */
	private void parseCommandLineArgs( final String[] args ) {
		final String helpMessageLine1 =
				"Metaseg:";
		final Options options = getOptions();

		// get the commands parsed
		final CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					helpMessageLine1,
					"",
					options,
					"Error: " + e1.getMessage() );
			quit( 0 );
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( helpMessageLine1, options );
			quit( 0 );
		}

		final File projectFolderBasePath = checkWritableFolderOption(cmd, "p", "project folder");

		inputStack = null;
		if ( cmd.hasOption( "i" ) ) {
			inputStack = new File( cmd.getOptionValue( "i" ) );
			if ( !inputStack.isFile() )
				showErrorAndExit(5, "Given input tiff stack could not be found!");
			if ( !inputStack.canRead() )
				showErrorAndExit(6, "Given input tiff stack is not readable!");
		} else if ( projectFolderBasePath != null ) { // if a project folder was given load data from there!
			openProjectFolder(projectFolderBasePath);
		}
	}

	private Options getOptions()
	{
		// create Options object & the parser
		final Options options = new Options();
		// defining command line options
		final Option help = new Option( "help", "print this message" );

		final Option projectfolder = new Option( "p", "projectfolder", true, "tr2d project folder" );
		projectfolder.setRequired( false );

		final Option instack = new Option( "i", "input", true, "tiff stack to be read" );
		instack.setRequired( false );

		options.addOption( help );
		options.addOption( instack );
		options.addOption( projectfolder );
		return options;
	}

	private File checkWritableFolderOption(final CommandLine cmd, final String shortOption, final String displayName) {
		File result = null;
		if ( cmd.hasOption( shortOption ) ) {
			result = new File( cmd.getOptionValue( shortOption ) );
			if ( !result.exists() )
				showErrorAndExit(1, "Given " + displayName + " does not exist!");
			if ( !result.isDirectory() )
				showErrorAndExit(2, "Given " + displayName + " is not a folder!");
			if ( !result.canWrite() )
				showErrorAndExit(3, "Given " + displayName + " cannot be written to!");
		}
		return result;
	}

	public void showWarning( final String msg, final Object... data ) {
		JOptionPane.showMessageDialog( guiFrame, String.format(msg, data),
				"Argument Warning", JOptionPane.WARNING_MESSAGE );
		log.warn( msg );
	}

	public void showErrorAndExit( final int exit_value, final String msg, final Object... data ) {
		JOptionPane.showMessageDialog(guiFrame, String.format(msg, data),
				"Argument Error", JOptionPane.ERROR_MESSAGE);
		log.error(msg);
		quit(exit_value);
	}

	private void openProjectFolder(final File projectFolderBasePath) {
		try {
			projectFolder = new MetasegProjectFolder( projectFolderBasePath );
			projectFolder.initialize();
			inputStack = projectFolder.getFile( MetasegProjectFolder.RAW_DATA ).getFile();
			if ( !inputStack.canRead() || !inputStack.exists() ) {
				showErrorAndExit(7, "Invalid project folder -- missing RAW data or read protected!");
			}
		} catch ( final IOException e ) {
			e.printStackTrace();
			showErrorAndExit(8, "Project folder (%s) could not be initialized.", projectFolderBasePath.getAbsolutePath() );
		}
	}

}
