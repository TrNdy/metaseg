/**
 *
 */
package com.indago.metaseg.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.indago.io.ProjectFolder;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.io.projectfolder.MetasegProjectFolder;
import com.indago.plugins.seg.IndagoSegmentationPlugin;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public class MetaSegSegmentationCollectionModel implements AutoCloseable {

	private final MetaSegModel model;
	private final ProjectFolder projectFolder;

	private final List< IndagoSegmentationPlugin > plugins = new ArrayList<>();

	public MetaSegSegmentationCollectionModel( final MetaSegModel model ) {
		this.model = model;
		projectFolder = model.getProjectFolder().getFolder( MetasegProjectFolder.SEGMENTATION_FOLDER );
	}

	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	public MetaSegModel getModel() {
		return model;
	}

	public void addPlugin( final IndagoSegmentationPlugin segPlugin ) {
		this.plugins.add( segPlugin );
	}

	public List< IndagoSegmentationPlugin > getPlugins() {
		return plugins;
	}

	public List< RandomAccessibleInterval< IntType > > getSumImages() {
		final List< RandomAccessibleInterval< IntType > > ret = new ArrayList< RandomAccessibleInterval< IntType > >();
		for ( final IndagoSegmentationPlugin plugin : plugins ) {
			ret.addAll( plugin.getOutputs() );
		}
		return ret;
	}

	@Override
	public void close() {
		for ( final IndagoSegmentationPlugin plugin : this.plugins )
			try {
				plugin.close();
			} catch ( final Exception e ) {
				MetaSegLog.log.warn( "Exception while closing: " + plugin.getUiName(), e );
			}
	}

}
