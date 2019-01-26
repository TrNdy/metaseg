package com.indago.metaseg;
/**
 *
 */

import org.scijava.log.Logger;

import com.indago.IndagoLog;

/**
 * @author jug
 */
public class MetaSegLog {

	public static Logger log = IndagoLog.log.subLogger( "metaseg" );
	public static Logger solverlog = IndagoLog.log.subLogger( "gurobi" );
	public static Logger segmenterLog = IndagoLog.log.subLogger( "seg" );

}
