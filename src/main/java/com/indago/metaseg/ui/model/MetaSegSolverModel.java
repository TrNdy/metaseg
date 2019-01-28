/**
 *
 */
package com.indago.metaseg.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.Assignment;
import com.indago.fg.AssignmentMapper;
import com.indago.fg.FactorGraphFactory;
import com.indago.fg.MappedFactorGraph;
import com.indago.fg.UnaryCostConstraintGraph;
import com.indago.fg.Variable;
import com.indago.ilp.DefaultLoggingGurobiCallback;
import com.indago.ilp.SolveGurobi;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.pg.MetaSegProblem;
import com.indago.metaseg.ui.util.SolutionVisualizer;
import com.indago.metaseg.ui.view.bdv.overlays.MetaSegSolutionOverlay;
import com.indago.pg.IndicatorNode;
import com.indago.ui.bdv.BdvWithOverlaysOwner;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import gurobi.GRBException;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class MetaSegSolverModel implements BdvWithOverlaysOwner {

	private final MetaSegModel parentModel;
	private final MetaSegCostPredictionTrainerModel costModel;

	private BdvHandlePanel bdvHandlePanel;
	private final List< RandomAccessibleInterval< IntType > > imgs = new ArrayList<>();;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	private MetaSegProblem msProblem;
	private MappedFactorGraph msFactorGraph;
	private SolveGurobi gurobiFGsolver;
	private Assignment< Variable > fgSolution;
	private Assignment< IndicatorNode > pgSolution;

	public MetaSegSolverModel( final MetaSegModel metaSegModel ) {
		this.parentModel = metaSegModel;
		this.costModel = metaSegModel.getCostTrainerModel();
	}

	public void run() {
		MetaSegLog.solverLog.info( "Building PG..." );
		msProblem = new MetaSegProblem( costModel.getLabeling().getSegments(), costModel, costModel.getConflictGraph() );
		MetaSegLog.solverLog.info( "...done!" );

		MetaSegLog.solverLog.info( "Building FG..." );
		msFactorGraph = FactorGraphFactory.createFactorGraph( msProblem );
		MetaSegLog.solverLog.info( "...done!" );

		MetaSegLog.solverLog.info( "Solve using GUROBI..." );
		solveFactorGraphInternally();
		MetaSegLog.solverLog.info( "...done!" );
	}

	private void solveFactorGraphInternally() {
		final UnaryCostConstraintGraph fg = msFactorGraph.getFg();
		final AssignmentMapper< Variable, IndicatorNode > assMapper = msFactorGraph.getAssmntMapper();
//			final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();

		fgSolution = null;
		try {
			SolveGurobi.GRB_PRESOLVE = 0;
			gurobiFGsolver = new SolveGurobi();
			fgSolution = gurobiFGsolver.solve( fg, new DefaultLoggingGurobiCallback( MetaSegLog.solverLog ) );
			pgSolution = assMapper.map( fgSolution );
		} catch ( final GRBException e ) {
			e.printStackTrace();
		} catch ( final IllegalStateException ise ) {
			fgSolution = null;
			pgSolution = null;
			MetaSegLog.solverLog.error( "Model is now infeasible and needs to be retracked!" );
		}
	}

	public void populateBdv() {
		bdvRemoveAll();
		bdvRemoveAllOverlays();

		imgs.clear();
		overlays.clear();

		bdvAdd( parentModel.getRawData(), "RAW" );

		if ( this.pgSolution != null ) {
			final RandomAccessibleInterval< IntType > imgSolution = SolutionVisualizer.drawSolutionSegmentImages( this );
			bdvAdd( imgSolution, "solution", 0, 2, new ARGBType( 0x0000FF ), true );
			imgs.add( imgSolution );
		}

		bdvAdd( new MetaSegSolutionOverlay( this ), "overlay" );
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvSetHandlePanel(bdv.util.BdvHandlePanel)
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return this.bdvSources;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {		// TODO Auto-generated method stub
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
	 */
	@Override
	public List< BdvSource > bdvGetOverlaySources() {
		return this.bdvOverlaySources;
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
	 */
	@Override
	public List< BdvOverlay > bdvGetOverlays() {
		return this.overlays;
	}

	public ImgPlus< DoubleType > getRawData() {
		return parentModel.getRawData();
	}

	public Assignment< IndicatorNode > getPgSolution() {
		return this.pgSolution;
	}

	public Assignment< Variable > getFgSolution() {
		return this.fgSolution;
	}

	public MetaSegProblem getProblem() {
		return this.msProblem;
	}
}
