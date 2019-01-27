/**
 *
 */
package com.indago.metaseg.ui.model;

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
import com.indago.pg.IndicatorNode;

import gurobi.GRBException;

/**
 * @author jug
 */
public class MetaSegSolverModel {

	private final MetaSegModel parentModel;
	private final MetaSegCostPredictionTrainerModel costModel;

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
}
