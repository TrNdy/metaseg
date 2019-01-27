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
import com.indago.metaseg.ui.model.MetaSegSolverModel;

import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class MetaSegSolutionPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -2148493794258482336L;

	private final MetaSegSolverModel model;

	private JSplitPane splitPane;
	private JButton btnRun;

	public MetaSegSolutionPanel( final MetaSegSolverModel solutionModel ) {
		super( new BorderLayout() );
		this.model = solutionModel;
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

		btnRun = new JButton( "run" );
		btnRun.addActionListener( this );

		controls.add( btnRun, "span, growx, wrap" );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( btnRun ) ) {
			actionRun();
		}
	}

	private void actionRun() {
		MetaSegLog.log.info( "Starting MetaSeg optimization..." );
		model.run();
		MetaSegLog.log.info( "Done!" );
	}
}