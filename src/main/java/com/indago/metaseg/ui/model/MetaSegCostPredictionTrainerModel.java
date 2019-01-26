/**
 *
 */
package com.indago.metaseg.ui.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.data.LabelingFrame;

/**
 * @author jug
 */
public class MetaSegCostPredictionTrainerModel {

	private final MetaSegModel parentModel;
	private LabelingFrame labelingFrame;

	private Map< LabelingSegment, Double > costs;

	public MetaSegCostPredictionTrainerModel( final MetaSegModel metaSegModel ) {
		parentModel = metaSegModel;
		costs = new HashMap<>();
	}

	public LabelingFrame getLabeling() {
		if ( this.labelingFrame == null ) {
			labelingFrame = new LabelingFrame( parentModel.getSegmentationModel() );
			MetaSegLog.log.info( "...processing LabelFrame inputs..." );
			labelingFrame.processInput();
		}
		return labelingFrame;
	}

	public ConflictGraph< LabelingSegment > getConflictGraph() {
		return labelingFrame.getConflictGraph();
	}

	public Collection< ? extends Collection< LabelingSegment > > getConflictCliques() {
		return labelingFrame.getConflictGraph().getConflictGraphCliques();
	}

	public void setRandomSegmentCosts() {
		costs = new HashMap<>();
		final Random r = new Random();
		for ( final LabelingSegment segment : labelingFrame.getSegments() ) {
			costs.put( segment, -r.nextDouble() );
		}
		MetaSegLog.log.info( String.format( "Random costs set for %d LabelingSegments.", labelingFrame.getSegments().size() ) );
	}

	public boolean hasCost( final LabelingSegment ls ) {
		return costs.containsKey( ls );
	}

	public Double getCost( final LabelingSegment ls ) {
		return costs.get( ls );
	}
}
