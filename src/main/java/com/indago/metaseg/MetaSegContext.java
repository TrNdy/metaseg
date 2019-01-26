/**
 *
 */
package com.indago.metaseg;

import javax.swing.JFrame;

import com.indago.plugins.seg.IndagoSegmentationPluginService;

import net.imagej.ops.OpService;

/**
 * @author jug
 */
public class MetaSegContext {

	public static OpService ops = null;
	public static IndagoSegmentationPluginService segPlugins = null;
	public static JFrame guiFrame = null;

}
