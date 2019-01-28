/**
 *
 */
package com.indago.metaseg.ui.util;

import com.indago.fg.Assignment;
import com.indago.io.DataMover;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegSolverModel;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.SegmentNode;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public class SolutionVisualizer {

	public static RandomAccessibleInterval< IntType > drawSolutionSegmentImages( final MetaSegSolverModel msSolverModel ) {

		final RandomAccessibleInterval< IntType > ret =
				DataMover.createEmptyArrayImgLike( msSolverModel.getRawData(), new IntType() );

		final Assignment< IndicatorNode > solution = msSolverModel.getPgSolution();
		if ( solution != null ) {
			final int curColorId = 1;
			for ( final SegmentNode segVar : msSolverModel.getProblem().getSegments() ) {
				if ( solution.getAssignment( segVar ) == 1 ) {
					drawSegmentWithId( ret, solution, segVar, curColorId );
				}
			}
		}

		return ret;
	}

	private static void drawSegmentWithId(
			final RandomAccessibleInterval< IntType > imgSolution,
			final Assignment< IndicatorNode > solution,
			final SegmentNode segVar,
			final int curColorId ) {

		if ( solution.getAssignment( segVar ) == 1 ) {
			final int color = curColorId;

			final IterableRegion< ? > region = segVar.getSegment().getRegion();
			final int c = color;
			try {
				Regions.sample( region, imgSolution ).forEach( t -> t.set( c ) );
			} catch ( final ArrayIndexOutOfBoundsException aiaob ) {
				MetaSegLog.log.error( aiaob );
			}
		}
	}

}
