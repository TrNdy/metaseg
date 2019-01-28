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
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class MetaSegSolverModel implements BdvWithOverlaysOwner {

	private final MetaSegModel model;
	private final MetaSegCostPredictionTrainerModel costModel;

	private BdvHandlePanel bdvHandlePanel;
	private final List< RandomAccessibleInterval< IntType > > imgs = new ArrayList<>();;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	private final List< MetaSegProblem > msProblems = new ArrayList<>();
	private final List< MappedFactorGraph > msFactorGraphs = new ArrayList<>();
	private SolveGurobi gurobiFGsolver;
	private final List< Assignment< Variable > > fgSolutions = new ArrayList<>();
	private final List< Assignment< IndicatorNode > > pgSolutions = new ArrayList<>();

	public MetaSegSolverModel( final MetaSegModel metaSegModel ) {
		this.model = metaSegModel;
		this.costModel = metaSegModel.getCostTrainerModel();
	}

	public void run() {
		msProblems.clear();
		msFactorGraphs.clear();

		MetaSegLog.solverLog.info( "Building PGs..." );
		if ( model.hasFrames() ) {
			for ( int t=0; t<model.getNumberOfFrames(); t++ ) {
				MetaSegLog.solverLog.info( String.format( "...t = %d...", t ) );
				msProblems.add(
						new MetaSegProblem(
								costModel.getLabelings().getSegments( t ),
								costModel,
								costModel.getConflictGraph( t ) ) );
			}
		} else {
			msProblems.add(
					new MetaSegProblem(
							costModel.getLabelings().getSegments( 0 ),
							costModel,
							costModel.getConflictGraph( 0 ) ) );
		}
		MetaSegLog.solverLog.info( "...done!" );

		MetaSegLog.solverLog.info( "Building FGs..." );
		if ( model.hasFrames() ) {
			for ( int t = 0; t < model.getNumberOfFrames(); t++ ) {
				MetaSegLog.solverLog.info( String.format( "...t = %d...", t ) );
				msFactorGraphs.add( FactorGraphFactory.createFactorGraph( msProblems.get( t ) ) );
			}
		} else {
			msFactorGraphs.add( FactorGraphFactory.createFactorGraph( msProblems.get( 0 ) ) );
		}
		MetaSegLog.solverLog.info( "...done!" );

		MetaSegLog.solverLog.info( "Solve using GUROBI..." );
		solveFactorGraphInternally();
		MetaSegLog.solverLog.info( "...done!" );
	}

	private void solveFactorGraphInternally() {
		pgSolutions.clear();
		fgSolutions.clear();

		if ( model.hasFrames() ) {
			for ( int t = 0; t < model.getNumberOfFrames(); t++ ) {
				MetaSegLog.solverLog.info( String.format( "Solving t = %d with GUROBI...", t ) );
				//			final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();
				final Pair< Assignment< IndicatorNode >, Assignment< Variable > > assmnts = solveFactorGraphInternally( msFactorGraphs.get( t ) );
				pgSolutions.add( assmnts.getA() );
				fgSolutions.add( assmnts.getB() );
			}
		} else {
			//			final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();
			final Pair< Assignment< IndicatorNode >, Assignment< Variable > > assmnts = solveFactorGraphInternally( msFactorGraphs.get( 0 ) );
			pgSolutions.add( assmnts.getA() );
			fgSolutions.add( assmnts.getB() );
		}
	}

	private Pair< Assignment< IndicatorNode >, Assignment< Variable > > solveFactorGraphInternally( final MappedFactorGraph mappedFactorGraph ) {
		final UnaryCostConstraintGraph fg = mappedFactorGraph.getFg();
		final AssignmentMapper< Variable, IndicatorNode > assMapper = mappedFactorGraph.getAssmntMapper();
		try {
			SolveGurobi.GRB_PRESOLVE = 0;
			gurobiFGsolver = new SolveGurobi();
			final Assignment< Variable > fgSolution = gurobiFGsolver.solve( fg, new DefaultLoggingGurobiCallback( MetaSegLog.solverLog ) );
			final Assignment< IndicatorNode > pgSolution = assMapper.map( fgSolution );
			return new ValuePair<>( pgSolution, fgSolution );
		} catch ( final GRBException e ) {
			e.printStackTrace();
		} catch ( final IllegalStateException ise ) {
			MetaSegLog.solverLog.error( "Model is now infeasible and needs to be retracked!" );
		}
		return new ValuePair<>( null, null );
	}

	public void populateBdv() {
		bdvRemoveAll();
		bdvRemoveAllOverlays();

		imgs.clear();
		overlays.clear();

		bdvAdd( model.getRawData(), "RAW" );

		final int bdvTime = bdvHandlePanel.getViewerPanel().getState().getCurrentTimepoint();
		if ( pgSolutions != null && pgSolutions.size() > bdvTime && pgSolutions.get( bdvTime ) != null ) {
			final RandomAccessibleInterval< IntType > imgSolution = SolutionVisualizer.drawSolutionSegmentImages( this );
			bdvAdd( imgSolution, "solution", 0, 2, new ARGBType( 0x00FF00 ), true );
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
		return model.getRawData();
	}

	public List< Assignment< IndicatorNode > > getPgSolutions() {
		return this.pgSolutions;
	}

	public Assignment< IndicatorNode > getPgSolution( final int t ) {
		return this.pgSolutions.get( t );
	}

	public List< Assignment< Variable > > getFgSolutions() {
		return this.fgSolutions;
	}

	public Assignment< Variable > getFgSolution( final int t ) {
		return this.fgSolutions.get( t );
	}

	public List< MetaSegProblem > getProblems() {
		return this.msProblems;
	}

	public MetaSegProblem getProblem( final int t ) {
		return this.msProblems.get( t );
	}

	public MetaSegModel getModel() {
		return model;
	}
}
