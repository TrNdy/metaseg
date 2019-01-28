/**
 *
 */
package com.indago.metaseg.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.data.LabelingFrames;

/**
 * @author jug
 */
public class MetaSegCostPredictionTrainerModel implements CostFactory< LabelingSegment > {

	private final MetaSegModel parentModel;
	private LabelingFrames labelingFrames;

	private Map< LabelingSegment, Double > costs;

	public MetaSegCostPredictionTrainerModel( final MetaSegModel metaSegModel ) {
		parentModel = metaSegModel;
		costs = new HashMap<>();
	}

	public LabelingFrames getLabelings() {
		if ( this.labelingFrames == null ) {
			labelingFrames = new LabelingFrames( parentModel.getSegmentationModel(), 1, Integer.MAX_VALUE );
			MetaSegLog.log.info( "...processing LabelFrame inputs..." );
			labelingFrames.processFrames();
		}
		return labelingFrames;
	}

	public ConflictGraph< LabelingSegment > getConflictGraph( final int t ) {
		return labelingFrames.getConflictGraph( t );
	}

	public List< ConflictGraph< LabelingSegment > > getConflictGraphs() {
		final List< ConflictGraph< LabelingSegment > > ret = new ArrayList<>();
		for ( int t = 0; t < labelingFrames.getNumFrames(); t++ ) {
			ret.add( getConflictGraph( t ) );
		}
		return ret;
	}

	public Collection< ? extends Collection< LabelingSegment > > getConflictCliques( final int t ) {
		return labelingFrames.getConflictGraph( t ).getConflictGraphCliques();
	}

	public List< Collection< ? extends Collection< LabelingSegment > > > getConflictCliques() {
		final List< Collection< ? extends Collection< LabelingSegment > > > ret = new ArrayList<>();
		for ( int t = 0; t < labelingFrames.getNumFrames(); t++ ) {
			ret.add( getConflictCliques( t ) );
		}
		return ret;
	}

	public void setRandomSegmentCosts() {
		costs = new HashMap<>();
		final Random r = new Random();
		for ( int t = 0; t < labelingFrames.getNumFrames(); t++ ) {
			for ( final LabelingSegment segment : labelingFrames.getSegments( t ) ) {
				costs.put( segment, -r.nextDouble() );
			}
			MetaSegLog.log.info( String.format( "Random costs set for %d LabelingSegments at t=%d.", labelingFrames.getSegments( t ).size(), t ) );
		}
	}

	public boolean hasCost( final LabelingSegment ls ) {
		return costs.containsKey( ls );
	}

	/**
	 * @see com.indago.costs.CostFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment ls ) {
		return costs.get( ls );
	}

	/**
	 * @see com.indago.costs.CostFactory#getParameters()
	 */
	@Override
	public CostParams getParameters() {
		return null;
	}

	/**
	 * @see com.indago.costs.CostFactory#setParameters(com.indago.costs.CostParams)
	 */
	@Override
	public void setParameters( final CostParams p ) {
		MetaSegLog.solverLog.error( "No parameters are accepted for this CostFactory!" );
	}
}
