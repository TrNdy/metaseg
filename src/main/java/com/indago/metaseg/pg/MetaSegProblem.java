package com.indago.metaseg.pg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.metaseg.pg.levedit.EditState;
import com.indago.metaseg.ui.model.MetaSegCostPredictionTrainerModel;
import com.indago.pg.SegmentationProblem;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.util.Bimap;

public class MetaSegProblem implements SegmentationProblem {

	private final MetaSegCostPredictionTrainerModel costsModel;

	private final Collection< SegmentNode > segments = new ArrayList<>();
	private final ConflictGraph< LabelingSegment > conflictGraph;

	// LEVERAGED EDITING STATE
	private EditState edits = new EditState();

	private final Bimap< SegmentNode, LabelingSegment > segmentBimap;

	public MetaSegProblem(
			final List< LabelingSegment > labelingSegments,
			final MetaSegCostPredictionTrainerModel costModel,
			final ConflictGraph< LabelingSegment > conflictGraph ) {
		segmentBimap = new Bimap<>();

		this.costsModel = costModel;
		this.conflictGraph = conflictGraph;

		createSegmentVars( labelingSegments );
	}

	/**
	 * @param labelingSegments
	 */
	private void createSegmentVars( final List< LabelingSegment > labelingSegments ) {
		for ( final LabelingSegment labelingSegment : labelingSegments ) {
			final SegmentNode segVar =
					new SegmentNode( labelingSegment, costsModel.getCost( labelingSegment ) );
			segments.add( segVar );
			segmentBimap.add( segVar, labelingSegment );
		}
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getSegments()
	 */
	@Override
	public Collection< SegmentNode > getSegments() {
		return segments;
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getConflictSets()
	 */
	@Override
	public Collection< ConflictSet > getConflictSets() {
		final ArrayList< ConflictSet > ret = new ArrayList< ConflictSet >();
		for ( final Collection< LabelingSegment > clique : conflictGraph.getConflictGraphCliques() ) {
			final ConflictSet cs = new ConflictSet();
			for ( final LabelingSegment ls : clique ) {
				cs.add( segmentBimap.getA( ls ) );
			}
			ret.add( cs );
		}
		return ret;
	}

	public ConflictSet getConflictSetFor( final SegmentNode node ) {
		for ( final Collection< LabelingSegment > clique : conflictGraph.getConflictGraphCliques() ) {
			if ( clique.contains( node.getSegment() ) ) {
				final ConflictSet cs = new ConflictSet();
				for ( final LabelingSegment ls : clique ) {
					cs.add( segmentBimap.getA( ls ) );
				}
				return cs;
			}
		}
		return new ConflictSet();
	}

	public SegmentNode getSegmentVar( final LabelingSegment segment ) {
		return segmentBimap.getA( segment );
	}

	public LabelingSegment getLabelingSegment( final SegmentNode segment ) {
		return segmentBimap.getB( segment );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution.
	 * This method is smart enough to avoid obvious problems by removing
	 * all potentially conflicting constraints (on segment and assignment
	 * level).
	 *
	 * @see com.indago.pg.SegmentationProblem#force(com.indago.pg.segments.SegmentNode)
	 */
	@Override
	public void force( final SegmentNode segNode ) {
		forceAndClearConflicts( segNode );
	}

	/**
	 * Forces the given <code>SegmentNode</code> and removes all forces of
	 * conflicting nodes and (in-)assignments.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 */
	private void forceAndClearConflicts( final SegmentNode segNode ) {
		// ensure not also being avoided
		edits.getAvoidedSegmentNodes().remove( segNode );

		// un-force all conflicting segment nodes
		final Collection< ? extends Collection< LabelingSegment > > cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segmentBimap.getB( segNode ) ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					edits.getForcedSegmentNodes().remove( segmentBimap.getA( labelingSegment ) );
				}
			}
		}

		// force the one!
		edits.getForcedSegmentNodes().add( segNode );
	}

	/**
	 * Avoids the given SegmentNode in any found solution.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 * @see com.indago.pg.SegmentationProblem#avoid(com.indago.pg.segments.SegmentNode)
	 */
	@Override
	public void avoid( final SegmentNode segNode ) {
		// ensure not also being forced
		edits.getForcedSegmentNodes().remove( segNode );

		// avoid the one!
		edits.getAvoidedSegmentNodes().add( segNode );
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedNodes() {
		return edits.getForcedSegmentNodes();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getAvoidedNodes()
	 */
	@Override
	public Set< SegmentNode > getAvoidedNodes() {
		return edits.getAvoidedSegmentNodes();
	}

	/**
	 * @return the current EditState
	 */
	public EditState getEditState() {
		return edits;
	}

	/**
	 * @param state
	 *            the EditState to be set
	 */
	public void setEditState( final EditState state ) {
		this.edits = state;
	}
}
