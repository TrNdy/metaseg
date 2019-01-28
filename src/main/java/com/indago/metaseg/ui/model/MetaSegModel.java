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
import net.imglib2.RandomAccessibleInterval;
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

	private boolean is2D;
	private final long numChannels;
	private final long numFrames;


	@SuppressWarnings( "unchecked" )
	public < T extends NumericType< T > > MetaSegModel( final ProjectFolder projectFolder, final ImgPlus< T > imgPlus ) {
		this.projectFolder = projectFolder;
		this.mainUiPanel = null;

		ImglibUtil.logImgPlusFacts( MetaSegLog.log, imgPlus );
		imgRaw = DoubleTypeImgLoader.wrapEnsureType( imgPlus );
		switch ( ImglibUtil.getNumberOfSpatialDimensions( imgPlus ) ) {
		case 2:
			this.is2D = true;
			break;
		case 3:
			this.is2D = false;
			break;
		default:
			final String msg = "MetaSeg requires an input image containing 2 or 3 spatial dimensions!";
			MetaSegLog.log.error( msg );
			throw new IllegalArgumentException( msg );
		}
		this.numChannels = ImglibUtil.getNumChannels( imgPlus );
		this.numFrames = ImglibUtil.getNumFrames( imgPlus );

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

	public boolean hasChannels() {
		return (this.numChannels == -1)?false:true;
	}

	public boolean hasFrames() {
		return ( this.numFrames == -1 ) ? false : true;
	}

	/**
	 * Returns a hyperslice of the RAW image at a given zero-indexed time.
	 *
	 * @param t
	 *            time slice to be returned (note: this index always starts at
	 *            0, even if min of time dimension would be != 0)
	 * @return the frame at time t
	 */
	public RandomAccessibleInterval< DoubleType > getFrame( final long t ) {
		final int timeIdx = ImglibUtil.getTimeDimensionIndex( this.imgRaw );
		return Views.hyperSlice( this.imgRaw, timeIdx, this.imgRaw.min( timeIdx ) + t );
	}

	public int getNumberOfSpatialDimensions() {
		return ImglibUtil.getNumberOfSpatialDimensions( this.imgRaw );
	}

	public long getNumberOfFrames() {
		final int timeIdx = ImglibUtil.getTimeDimensionIndex( this.imgRaw );
		return this.imgRaw.dimension( timeIdx );
	}

	public int getTimeDimensionIndex() {
		return ImglibUtil.getTimeDimensionIndex( this.imgRaw );
	}

	public long getNumberOfChannels() {
		final int timeIdx = ImglibUtil.getTimeDimensionIndex( this.imgRaw );
		return this.imgRaw.dimension( timeIdx );
	}

	public int getChannelDimensionIndex() {
		return ImglibUtil.getTimeDimensionIndex( this.imgRaw );
	}

	public long getTimeIndex( final int t ) {
		return t + imgRaw.min( ImglibUtil.getTimeDimensionIndex( this.imgRaw ) );
	}
}
