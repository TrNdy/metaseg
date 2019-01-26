/**
 *
 */
package com.indago.metaseg.ui.view;

import javax.swing.JPanel;

import com.indago.metaseg.ui.model.MetaSegCostPredictionTrainerModel;

/**
 * @author jug
 */
public class MetaSegCostPredictionTrainerPanel extends JPanel {

	private static final long serialVersionUID = 3940247743127023839L;

	MetaSegCostPredictionTrainerModel model;

	public MetaSegCostPredictionTrainerPanel( final MetaSegCostPredictionTrainerModel costTrainerModel ) {
		this.model = costTrainerModel;
	}

}
