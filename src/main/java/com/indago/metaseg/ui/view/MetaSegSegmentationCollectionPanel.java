/**
 *
 */
package com.indago.metaseg.ui.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.indago.metaseg.MetaSegContext;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegSegmentationCollectionModel;
import com.indago.plugins.seg.IndagoSegmentationPlugin;


/**
 * @author jug
 */
public class MetaSegSegmentationCollectionPanel extends JPanel {

	private final MetaSegSegmentationCollectionModel model;

	private JTabbedPane tabs;

	public MetaSegSegmentationCollectionPanel( final MetaSegSegmentationCollectionModel segModel ) {
		super( new BorderLayout( 5, 5 ) );
		this.model = segModel;
		buildGui();
	}

	private void buildGui() {
		tabs = new JTabbedPane();

		for ( final String name : MetaSegContext.segPlugins.getPluginNames() ) {
			final IndagoSegmentationPlugin segPlugin =
					MetaSegContext.segPlugins.createPlugin(
							name,
							model.getProjectFolder(),
							model.getModel().getRawData(),
							MetaSegLog.segmenterLog );
			if ( segPlugin.isUsable() ) {
				model.addPlugin( segPlugin );
				tabs.add( segPlugin.getUiName(), segPlugin.getInteractionPanel() );
			}
		}

		add( tabs, BorderLayout.CENTER );
	}

}
