/**
 *
 */
package com.indago.metaseg.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingBuilder;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.MinimalOverlapConflictGraph;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegSegmentationCollectionModel;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public class LabelingFrame {

	private final MetaSegSegmentationCollectionModel segModel;
	private boolean processedOrLoaded = false;

	private LabelingBuilder labelingBuilder;
	private ConflictGraph< LabelingSegment > conflictGraph;
	private Collection< ? extends Collection< LabelingSegment > > cliques;

	// Parameters for FilteredComponentTrees
	private final int minHypothesisSize = 0;
	private final int maxHypothesisSize = Integer.MAX_VALUE;
	private final Filter maxGrowthPerStep = new MaxGrowthPerStep( maxHypothesisSize );;
	private final boolean darkToBright = false;

	public LabelingFrame( final MetaSegSegmentationCollectionModel segModel ) {
		this.segModel = segModel;
	}

	public boolean processInput() {
		final List< RandomAccessibleInterval< IntType > > segmentHypothesesImages = segModel.getSumImages();

		if ( segmentHypothesesImages.size() == 0 ) { return false; }

		final RandomAccessibleInterval< IntType > firstSumImg = segmentHypothesesImages.get( 0 );
		labelingBuilder = new LabelingBuilder( firstSumImg );

		for ( final RandomAccessibleInterval< IntType > frame : segmentHypothesesImages ) {
			// build component tree on frame
			final FilteredComponentTree< IntType > tree =
					FilteredComponentTree.buildComponentTree(
							frame,
							new IntType(),
							minHypothesisSize,
							maxHypothesisSize,
							maxGrowthPerStep,
							darkToBright );
			labelingBuilder.buildLabelingForest( tree );
		}
		processedOrLoaded = true;
		return processedOrLoaded;
	}

	public ConflictGraph< LabelingSegment > getConflictGraph() {
		if ( conflictGraph == null ) {
			MetaSegLog.log.info( "...constructing conflict graph..." );
			conflictGraph = new MinimalOverlapConflictGraph( labelingBuilder );
		}
		return conflictGraph;
	}

	public Collection< ? extends Collection< LabelingSegment > > getConflictCliques() {
		if ( cliques == null ) {
			MetaSegLog.log.info( "...computing conflict cliques..." );
			cliques = getConflictGraph().getConflictGraphCliques();
		}
		return cliques;
	}

	public List< LabelingSegment > getSegments() {
		if ( labelingBuilder == null ) { return new ArrayList< LabelingSegment >(); }
		return labelingBuilder.getSegments();
	}

}
