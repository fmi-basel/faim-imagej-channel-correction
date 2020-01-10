
package ch.fmi.correction;

import java.util.HashMap;
import java.util.Map;

import net.imagej.ImageJ;
import net.imagej.command.InteractiveImageCommand;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import ch.fmi.util.Beads;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.DummySpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.measure.Measurements;

@Plugin(type = Command.class,
	menuPath = "FMI > Multi-Channel Image Correction > Interactive Bead Detection")
public class ChannelBeadDetection extends InteractiveImageCommand implements Initializable {

	// We use ImageJ1 objects here (because of TrackMate and Overlay)
	@Parameter
	private ImagePlus inputImp;

	@Parameter(min = "1", max = "9", style = NumberWidget.SCROLL_BAR_STYLE)
	private Integer channel = 1;

	@Parameter(min = "0", max = "10", stepSize = "0.1", style = NumberWidget.SLIDER_STYLE)
	private Double radius = 1.0;

	@Parameter(min = "0", max = "1000", stepSize = "0.1", style = NumberWidget.SLIDER_STYLE)
	private Double threshold;

	@Parameter(required = false)
	private String name; // for saving the output to ObjectService

	@Parameter(label = "Keep Detections", callback = "saveCurrentDetections")
	private Button keepButton;

	@Parameter(label = "Reset Stored Detections", callback = "removeStoredDetections")
	private Button resetButton;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private LogService logService;

	private SpotCollection spots;
	private boolean initialized = false;

	@Override
	public void run() {
		// TODO check which parameters changed
		// if channel or radius or threshold changed: update model
		// TODO safeguard against too unreasonable parameters (e.g. radius << pixel spacing...)
		Model model = Beads.getSpotModel(inputImp, channel, radius, threshold);
		setOverlay(model);
		spots = model.getSpots();
		logService.info("Number of spots in current collection: " + spots.getNSpots(false));
		if (!initialized) {
			initialized = true;
		}
	}

	private void setOverlay(Model model) {
		SpotOverlay ovl = new SpotOverlay(model, inputImp, defaultDisplaySettings());
		inputImp.setOverlay(new Overlay(ovl));
		inputImp.updateAndDraw();
	}

	private Map<String, Object> defaultDisplaySettings() {
		final Map< String, Object > settings = new HashMap<>(6);
		settings.put( TrackMateModelView.KEY_COLOR, TrackMateModelView.DEFAULT_SPOT_COLOR );
		settings.put( TrackMateModelView.KEY_HIGHLIGHT_COLOR, TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR );
		settings.put( TrackMateModelView.KEY_SPOTS_VISIBLE, true );
		settings.put( TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, false );
		settings.put( TrackMateModelView.KEY_SPOT_COLORING, new DummySpotColorGenerator() );
		settings.put( TrackMateModelView.KEY_SPOT_RADIUS_RATIO, 1.0d );

		// required for displayer, even if no tracks present
		settings.put( TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE );
		settings.put( TrackMateModelView.KEY_LIMIT_DRAWING_DEPTH, TrackMateModelView.DEFAULT_LIMIT_DRAWING_DEPTH );
		settings.put( TrackMateModelView.KEY_DRAWING_DEPTH, TrackMateModelView.DEFAULT_DRAWING_DEPTH );

		return settings;
	}

	// -- Callback methods --

	@SuppressWarnings("unused") // keepButton callback
	private void saveCurrentDetections() {
		objectService.addObject(spots, name);
	}

	@SuppressWarnings("unused") // keepButton callback
	private void removeStoredDetections() {
		for (SpotCollection collection : objectService.getObjects(SpotCollection.class)) {
			objectService.removeObject(collection);
		}
	}

	//-- Initializable methods --

	@Override
	public void initialize() {
		logService.info("initialize called");
		// TODO adapt parameters to image bit depth and dimensionality
		if (inputImp == null) {
			logService.error("image not present during initialization");
		}
		else {
			double smallestScale = Math.min(Math.min(inputImp
				.getCalibration().pixelWidth, inputImp.getCalibration().pixelHeight),
				inputImp.getCalibration().pixelDepth);
			if (radius != null && radius < smallestScale) {
				radius = smallestScale * 3;
			}
			if (threshold == null) {
				switch (inputImp.getBitDepth()) {
					case 8:
						threshold = 10.0;
						break;
					case 16:
						threshold = 1000.0;
						break;
					default:
						threshold = 0.0;
				}
			}
			// adjust max of threshold slider
			final MutableModuleItem<Double> thresholdItem = getInfo().getMutableInput("threshold", Double.class);
			thresholdItem.setMaximumValue(inputImp.getStatistics(Measurements.MIN_MAX).max * 2);
			// adjust max of channel slider
			final MutableModuleItem<Integer> channelItem = getInfo().getMutableInput("channel", Integer.class);
			channelItem.setMaximumValue(inputImp.getNChannels());
			name = "Beads (Channel " + channel + "): " + inputImp.getTitle();
		}
	}

	// -- main method for testing from IDE --

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
