
package ch.fmi.util;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import ij.ImagePlus;

public class Beads {

	private Beads() {
		// avoid instantiation of utility class
	}

	public static SpotCollection getSpotCollection(ImagePlus imp, int channel, double radius,
		double threshold)
	{
		return getSpotModel(imp, channel, radius, threshold).getSpots();
	}

	public static <T extends RealType<T> & NativeType<T>> Model getSpotModel(ImagePlus imp, int channel, double radius,
		double threshold)
	{
		Model model = new Model();

		Settings settings = new Settings();
		settings.setFrom(imp);
		settings.detectorFactory = new LogDetectorFactory<T>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.detectorSettings.put(DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION,
			true);
		settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL, channel);
		settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, radius);
		settings.detectorSettings.put(DetectorKeys.KEY_THRESHOLD, threshold);

		TrackMate trackmate = new TrackMate(model, settings);

		if (trackmate.execDetection() && trackmate.execInitialSpotFiltering() && trackmate.computeSpotFeatures(false) && trackmate.execSpotFiltering(false)) {
			return model;
		}
		throw new RuntimeException(trackmate.getErrorMessage());
	}
}
