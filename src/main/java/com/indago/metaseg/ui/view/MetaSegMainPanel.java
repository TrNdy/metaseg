/**
 *
 */
package com.indago.metaseg.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.log.Logger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.InputActionBindings;

import com.indago.IndagoLog;
import com.indago.log.LoggingPanel;
import com.indago.metaseg.MetaSegContext;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegModel;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;

/**
 * @author jug
 */
public class MetaSegMainPanel extends JPanel implements ActionListener, ChangeListener {

	private final Frame frame;

	private final MetaSegModel model;

	private JTabbedPane tabs;
	private JPanel tabData;
	private JPanel tabSegmentation;
	private JPanel tabTraining;
	private JPanel tabSolution;

	private BdvHandlePanel bdvData;

	private JSplitPane splitPane;

	private final LoggingPanel logPanel;

	public MetaSegMainPanel( final Frame frame, final MetaSegModel model, final Logger logger ) {
		super( new BorderLayout( 5, 5 ) );
		logPanel = new LoggingPanel( MetaSegContext.ops.context() );
		model.setRefToMainPanel( this );

		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.model = model;

		buildGui( logger );
	}

	private void buildGui( final Logger logger ) {
		// --- INPUT TRIGGERS ---------------------------------------------------------------------
		model.setDefaultInputTriggerConfig( loadInputTriggerConfig() );

		// --- LOGGING PANEL ----------------------------------------------------------------------
		IndagoLog.log = setupLogger( logger, "indago" );
		MetaSegLog.log = setupLogger( logger, "metaseg" );
		MetaSegLog.segmenterLog = setupLogger( logger, "seg" );
		MetaSegLog.solverlog = setupLogger( logger, "gurobi" );

		// === TAB DATA ===========================================================================
		tabs = new JTabbedPane();
		tabData = new JPanel( new BorderLayout() );
		bdvData = new BdvHandlePanel( frame, Bdv.options().is2D().inputTriggerConfig( model.getDefaultInputTriggerConfig() ) );
		tabData.add( bdvData.getViewerPanel(), BorderLayout.CENTER );
		final BdvSource source = BdvFunctions.show(
				model.getRawData(),
				"RAW",
				Bdv.options().addTo( bdvData ) );
		source.setDisplayRangeBounds( 0, model.getMaxRawValue() );
		source.setDisplayRange( model.getMinRawValue(), model.getMaxRawValue() );

		// === TAB SEGMENTATION ===================================================================
		tabSegmentation = new MetaSegSegmentationCollectionPanel( model.getSegmentationModel() );

		// === TAB META-TRAINING ==================================================================
		tabTraining = new MetaSegCostPredictionTrainerPanel( model.getCostTrainerModel() );

		// === TAB SOLUTION =======================================================================
		tabSolution = new MetaSegSolutionPanel( model.getSolutionModel() );

		// --- ASSEMBLE PANEL ---------------------------------------------------------------------

		tabs.add( "data", tabData );
		tabs.add( "segments", tabSegmentation );
		tabs.add( "meta training", tabTraining );
		tabs.add( "solution", tabSolution );
		tabs.setSelectedComponent( tabTraining );

		splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, tabs, logPanel );
		splitPane.setResizeWeight( .5 ); // 1.0 == extra space given to left (top) component alone!
		splitPane.setOneTouchExpandable( true );

		this.add( splitPane, BorderLayout.CENTER );
	}

	private Logger setupLogger( final Logger logger, final String name ) {
		final Logger log = logger.subLogger( name );
		log.addLogListener( logPanel );
		return log;
	}

	/**
	 * @return the loaded <code>InputTriggerConfig</code>, or <code>null</code>
	 *         if none was found.
	 *
	 */
	private InputTriggerConfig loadInputTriggerConfig() {
		try {

			MetaSegLog.log.info( "Try to fetch yaml from " + ClassLoader.getSystemResource( "metaseg.yaml" ) );
			URL yamlURL = ClassLoader.getSystemResource( "metaseg.yaml" );
			if ( yamlURL == null ) {
				MetaSegLog.log.info( "Try to fetch yaml from " + getClass().getClassLoader().getResource( "metaseg.yaml" ) );
				yamlURL = getClass().getClassLoader().getResource( "metaseg.yaml" );
			}
			if ( yamlURL != null ) {
				final BufferedReader in = new BufferedReader( new InputStreamReader( yamlURL.openStream() ) );
				final InputTriggerConfig conf = new InputTriggerConfig( YamlConfigIO.read( in ) );

				final InputActionBindings bindings = new InputActionBindings();
				SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
				SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

//				final AbstractActions a = new AbstractActions( bindings, "tabs", conf, new String[] { "tr2d" } );
//
//				a.runnableAction(
//						() -> tabs.setSelectedIndex( Math.min( tabs.getSelectedIndex() + 1, tabs.getTabCount() - 1 ) ),
//						"next tab",
//						"COLON" );
//				a.runnableAction(
//						() -> tabs.setSelectedIndex( Math.max( tabs.getSelectedIndex() - 1, 0 ) ),
//						"previous tab",
//						"COMMA" );

				return conf;
			} else {
				MetaSegLog.log.info( "Falling back to default BDV action settings." );
				final InputTriggerConfig conf = new InputTriggerConfig();
				final InputActionBindings bindings = new InputActionBindings();
				SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
				SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );
				return conf;
			}
		} catch ( IllegalArgumentException | IOException e ) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {}

	public void collapseLog() {
		splitPane.setDividerLocation( 1.0 );
	}

	/**
	 * @return the logPanel
	 */
	public LoggingPanel getLogPanel() {
		return logPanel;
	}

	public void selectTab( final JPanel tab ) {
		for ( int i = 0; i < tabs.getTabCount(); ++i ) {
			if ( tabs.getComponentAt( i ) == tab ) {
				tabs.setSelectedIndex( i );
				break;
			}
		}
	}
}
