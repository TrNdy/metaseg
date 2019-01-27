/**
 *
 */
package com.indago.metaseg.pg.levedit;

import java.util.HashSet;
import java.util.Set;

import com.indago.pg.segments.SegmentNode;

/**
 * @author jug
 */
public class EditState {

	protected final Set< SegmentNode > forcedSegmentNodes = new HashSet<>();
	protected final Set< SegmentNode > avoidedSegmentNodes = new HashSet<>();

	public EditState() {}

	public EditState( final EditState state ) {
		this.forcedSegmentNodes.addAll( state.forcedSegmentNodes );
		this.avoidedSegmentNodes.addAll( state.avoidedSegmentNodes );
	}

	public Set< SegmentNode > getForcedSegmentNodes() {
		return forcedSegmentNodes;
	}

	public Set< SegmentNode > getAvoidedSegmentNodes() {
		return avoidedSegmentNodes;
	}

	/**
	 * @return A string showing how many edits are stored per type.
	 */
	public String getDebugString() {
		return String.format(
				"Number of edits stored: %d,%d",
				forcedSegmentNodes.size(),
				avoidedSegmentNodes.size() );
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode represents an avoided segment
	 */
	public boolean isAvoided( final SegmentNode segnode ) {
		return avoidedSegmentNodes.contains( segnode );
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode represents a forced segment
	 */
	public boolean isForced( final SegmentNode segnode ) {
		return forcedSegmentNodes.contains( segnode );
	}

}
