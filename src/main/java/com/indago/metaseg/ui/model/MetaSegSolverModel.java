/**
 *
 */
package com.indago.metaseg.ui.model;

import com.indago.metaseg.MetaSegLog;

/**
 * @author jug
 */
public class MetaSegSolverModel {

	private final MetaSegModel parentModel;

	public MetaSegSolverModel( final MetaSegModel metaSegModel ) {
		this.parentModel = metaSegModel;
	}

	public void run() {
		MetaSegLog.log.error( "Solver.run not yet implemented!" );
	}
}
