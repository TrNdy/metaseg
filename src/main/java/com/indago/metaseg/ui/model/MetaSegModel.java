/**
 *
 */
package com.indago.metaseg.ui.model;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.ProjectFolder;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.view.MetaSegMainPanel;
import com.indago.util.ImglibUtil;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class MetaSegModel implements AutoCloseable {

	private MetaSegMainPanel mainPanel;
	private final MetaSegSegmentationCollectionModel segModel;
	private final MetaSegCostPredictionTrainerModel costTrainerModel;

	private final ImgPlus< DoubleType > imgRaw;
	private final DoubleType minValInRaw;
	private final DoubleType maxValInRaw;

	private InputTriggerConfig defaultBdvInputTriggerConfig;

	private final ProjectFolder projectFolder;
	private final MetaSegMainPanel mainUiPanel;
	private final MetaSegSolverModel modelSolver;


	@SuppressWarnings( "unchecked" )
	public < T extends NumericType< T > > MetaSegModel( final ProjectFolder projectFolder, final ImgPlus< T > imgPlus ) {
		this.projectFolder = projectFolder;
		this.mainUiPanel = null;

		ImglibUtil.logImgPlusFacts( MetaSegLog.log, imgPlus );
		imgRaw = DoubleTypeImgLoader.wrapEnsureType( imgPlus );

		minValInRaw = new DoubleType();
		maxValInRaw = new DoubleType();
		ImglibUtil.computeMinMax( Views.iterable( imgRaw ), minValInRaw, maxValInRaw );

		segModel = new MetaSegSegmentationCollectionModel( this );
		costTrainerModel = new MetaSegCostPredictionTrainerModel( this );
		modelSolver = new MetaSegSolverModel( this );
	}

	public void setRefToMainPanel( final MetaSegMainPanel metaSegMainPanel ) {
		this.mainPanel = metaSegMainPanel;
	}

	public void setDefaultInputTriggerConfig( final InputTriggerConfig conf ) {
		this.defaultBdvInputTriggerConfig = conf;
	}

	public InputTriggerConfig getDefaultInputTriggerConfig() {
		return defaultBdvInputTriggerConfig;
	}

	public ImgPlus< DoubleType > getRawData() {
		return imgRaw;
	}

	public double getMaxRawValue() {
		return maxValInRaw.get();
	}

	public double getMinRawValue() {
		return minValInRaw.get();
	}

	public MetaSegSegmentationCollectionModel getSegmentationModel() {
		return this.segModel;
	}

	public MetaSegCostPredictionTrainerModel getCostTrainerModel() {
		return this.costTrainerModel;
	}

	public ProjectFolder getProjectFolder() {
		return this.projectFolder;
	}

	@Override
	public void close() {
		segModel.close();
	}

	public MetaSegMainPanel getMainPanel() {
		return this.mainUiPanel;
	}

	public MetaSegSolverModel getSolutionModel() {
		return this.modelSolver;
	}
}
