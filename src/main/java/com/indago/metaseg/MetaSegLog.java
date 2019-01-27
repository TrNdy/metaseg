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

	public static Logger log = IndagoLog.stdLogger().subLogger( "metaseg" );
	public static Logger solverLog = log.subLogger( "sol" );
	public static Logger segmenterLog = log.subLogger( "seg" );

}
