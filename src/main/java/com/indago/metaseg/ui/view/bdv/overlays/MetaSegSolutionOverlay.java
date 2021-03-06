/**
 *
 */
package com.indago.metaseg.ui.view.bdv.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.indago.fg.Assignment;
import com.indago.metaseg.pg.MetaSegProblem;
import com.indago.metaseg.ui.model.MetaSegSolverModel;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.SegmentNode;

import bdv.util.BdvOverlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class MetaSegSolutionOverlay extends BdvOverlay {

	private final MetaSegSolverModel model;

	public MetaSegSolutionOverlay( final MetaSegSolverModel model ) {
		this.model = model;
	}

	/**
	 * @see bdv.util.BdvOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	protected void draw( final Graphics2D g ) {
		if ( model.getPgSolutions() == null || model.getPgSolutions().size() == 0 ) return;

		final int bdvTime = model.bdvGetHandlePanel().getViewerPanel().getState().getCurrentTimepoint();
		final Assignment< IndicatorNode > pgSolution = model.getPgSolutions().get( bdvTime );

		if ( pgSolution != null ) {
			drawCOMs( g );
		}
	}

	private void drawCOMs( final Graphics2D g ) {
		final Color theRegularColor = Color.RED.darker();
		final Color theForcedColor = Color.RED.brighter();
		final Color theAvoidedColor = Color.GRAY.brighter().brighter();

		final Graphics2D g2 = g;
		int len = 3;

		final int bdvTime = model.bdvGetHandlePanel().getViewerPanel().getState().getCurrentTimepoint();
		final MetaSegProblem problem = model.getProblems().get( bdvTime );
		final Assignment< IndicatorNode > pgSolution = model.getPgSolutions().get( bdvTime );

		// exit if pointless
		if ( pgSolution == null ) return;
		// otherwise...
		try {
			final AffineTransform2D trans = new AffineTransform2D();
			getCurrentTransform2D( trans );

			RandomAccessibleInterval< DoubleType > frame = model.getRawData();
			long t = 0;
			if ( model.getModel().hasFrames() ) {
				t = info.getTimePointIndex();
				frame = model.getModel().getFrame( t );
			}

			for ( final SegmentNode segvar : problem.getSegments() ) {
				if ( pgSolution.getAssignment( segvar ) == 1 || problem.getEditState().isAvoided( segvar ) ) {
					final RealLocalizable com = segvar.getSegment().getCenterOfMass();
					final int nSpatialDims = model.getModel().getNumberOfSpatialDimensions();
					final double[] lpos = new double[ nSpatialDims ];
					final double[] gpos = new double[ nSpatialDims ];
					com.localize( lpos );
					trans.apply( lpos, gpos );

					double zDistToCOM = 0;
					if ( nSpatialDims == 3 ) {
						zDistToCOM = Math.abs( lpos[ 2 ] - t );
					}

					// Only show nearby stuff...
					if ( zDistToCOM > 1. ) {
						continue;
					}

					if ( problem.getEditState().isForced( segvar ) ) {
						g.setColor( theForcedColor );
						g2.setStroke( new BasicStroke( ( float ) 3.5 ) );
						len = 8;
						g.drawOval( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, len * 2, len * 2 );
					} else if ( problem.getEditState().isAvoided( segvar ) ) {
						g.setColor( theAvoidedColor );
						g2.setStroke( new BasicStroke( 4 ) );
						len = 8;
						g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] + len );
						g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] + len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] - len );
					} else {
						g.setColor( theRegularColor );
						g2.setStroke( new BasicStroke( ( float ) 2.0 ) );
						len = 4;
						g.drawOval( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, len * 2, len * 2 );
					}
				}
			}
		} catch ( final ArrayIndexOutOfBoundsException aioobe ) {
			// do not bother, this happens only during threaded re-computations while the UI would love to point something that is currently invalid
//			System.out.println( "" );
		}
	}

}
