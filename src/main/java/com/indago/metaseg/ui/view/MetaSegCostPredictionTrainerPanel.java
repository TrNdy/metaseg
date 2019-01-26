/**
 *
 */
package com.indago.metaseg.ui.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegCostPredictionTrainerModel;

import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class MetaSegCostPredictionTrainerPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 3940247743127023839L;

	MetaSegCostPredictionTrainerModel model;

	private JSplitPane splitPane;

	private JButton btnFetch;

	private JButton btnRandCosts;

	public MetaSegCostPredictionTrainerPanel( final MetaSegCostPredictionTrainerModel costTrainerModel ) {
		super( new BorderLayout() );
		this.model = costTrainerModel;
		buildGui();
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
//		model.bdvSetHandlePanel(
//				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
//						.options()
//						.is2D()
//						.inputTriggerConfig( model.getModel().getDefaultInputTriggerConfig() ) ) );
//		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
//
//		// show loaded image
//		final RandomAccessibleInterval< FloatType > flowImg = model.getFlowImage();
//		model.bdvAdd( model.getModel().getRawData(), "RAW" );
//		if ( flowImg != null ) {
////			model.bdvAdd( Views.hyperSlice( flowImg, 2, 0 ), "r", false );
////			model.bdvAdd( Views.hyperSlice( flowImg, 2, 1 ), "phi", false );
//			model.bdvAdd( new Tr2dFlowOverlay( model ), "overlay_flow" );
//		}

		final MigLayout layout = new MigLayout( "", "[][grow]", "" );
		final JPanel controls = new JPanel( layout );

		btnFetch = new JButton( "fetch segments" );
		btnFetch.addActionListener( this );
		btnRandCosts = new JButton( "set random costs" );
		btnRandCosts.addActionListener( this );

		controls.add( btnFetch, "span, growx, wrap" );
		controls.add( btnRandCosts, "span, growx, wrap" );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if (e.getSource().equals( btnFetch )) {
			actionFetch();
		} else
		if (e.getSource().equals( btnRandCosts )) {
			actionSetRandomCosts();
		}
	}

	private void actionFetch() {
		MetaSegLog.log.info( "Fetching segmentation results..." );
		model.getLabeling();
		model.getConflictGraph();
		model.getConflictCliques();
		MetaSegLog.log.info( "Segmentation results fetched!" );
	}

	private void actionSetRandomCosts() {
		MetaSegLog.log.info( "Setting random cost values." );
		model.setRandomSegmentCosts();
	}
}
