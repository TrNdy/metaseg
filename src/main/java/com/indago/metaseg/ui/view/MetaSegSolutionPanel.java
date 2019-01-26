/**
 *
 */
package com.indago.metaseg.ui.view;

import javax.swing.JPanel;

import com.indago.metaseg.ui.model.MetaSegSolverModel;


/**
 * @author jug
 */
public class MetaSegSolutionPanel extends JPanel {

	private static final long serialVersionUID = -2148493794258482336L;

	private final MetaSegSolverModel model;

	public MetaSegSolutionPanel( final MetaSegSolverModel solutionModel ) {
		this.model = solutionModel;
	}

}
