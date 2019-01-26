import javax.swing.JOptionPane;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.indago.gurobi.GurobiInstaller;
import com.indago.metaseg.MetaSegApplication;
import com.indago.plugins.seg.IndagoSegmentationPluginService;

import net.imagej.ops.OpService;

/**
 * MetaSeg Plugin for Fiji/ImageJ2
 *
 * @author Florian Jug
 */

@Plugin( type = ContextCommand.class, headless = false, menuPath = "Plugins > Tracking > Tr2d" )
public class MetasegPlugin implements Command {

	@Parameter
	private OpService opService;

	@Parameter
	private IndagoSegmentationPluginService tr2dSegmentationPluginService;

	@Parameter
	private Logger log;

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final boolean gurobiWorks = GurobiInstaller.install();

		if(gurobiWorks) {
			final MetaSegApplication app = new MetaSegApplication();
			try {
				app.run( null );
			} catch ( final NoClassDefFoundError err ) {
				showGurobiErrorMessage( err );
				app.quit( 100 );
			}
		}
		else
			log.warn( "Abort start of Tr2d, because Gurobi is not working properly." );
	}

	private void showGurobiErrorMessage( final NoClassDefFoundError err )
	{
		final String jlp = System.getProperty( "java.library.path" );
		final String msgs = "Gurobi seems to be not installed on your system.\n" +
						"Please visit 'www.gurobi.com' for further information.\n\n" +
						"Java library path: " + jlp;
		JOptionPane.showMessageDialog(
				null,
				msgs,
				"Gurobi not installed?",
				JOptionPane.ERROR_MESSAGE );
		err.printStackTrace();
	}
}
