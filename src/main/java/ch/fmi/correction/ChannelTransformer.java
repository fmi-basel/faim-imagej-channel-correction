
package ch.fmi.correction;

import java.util.ArrayList;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.process.LUT;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "FMI > Multi-Channel Image Correction > Apply Channel Transformation")
public class ChannelTransformer<T extends RealType<T>> extends DynamicCommand implements Initializable {
	// TODO decide if we should add an Identity transform (if absent)
	// to ObjectService in an initialize() method (DynamicCommand)

	@Parameter
	private ImagePlus imp;

	@Parameter
	private Boolean transformCalibrated = true;

	@Parameter
	private Boolean transformChannel1 = false;

	@Parameter(required = false)
	private AffineGet affineChannel1;

	@Parameter
	private Boolean transformChannel2 = true;

	@Parameter(required = false)
	private AffineGet affineChannel2;

	@Parameter
	private Boolean transformChannel3 = true;

	@Parameter(required = false)
	private AffineGet affineChannel3;

	@Parameter(type = ItemIO.OUTPUT)
	private ImagePlus resultImp;

	@Parameter
	private OpService opService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		// use ImagePlus input and convertService(imp, Dataset.class)
		Dataset dataset = convertService.convert(imp, Dataset.class);

		ArrayList<RandomAccessibleInterval<T>> transformedChannels = new ArrayList<>();
		AffineTransform3D calibrationTransform = new AffineTransform3D();
		if (transformCalibrated) {
			calibrationTransform.scale(dataset.averageScale(0), dataset.averageScale(1), dataset.averageScale(2));
		}
		transformedChannels.add(transformChannel1 ? //
				transform(dataset, 0, affineChannel1, calibrationTransform) : extract(dataset, 0));
		transformedChannels.add(transformChannel2 ? //
				transform(dataset, 1, affineChannel2, calibrationTransform) : extract(dataset, 1));
		if (dataset.dimension(Axes.CHANNEL) > 2) {
			transformedChannels.add(transformChannel3 ? //
					transform(dataset, 2, affineChannel3, calibrationTransform) : extract(dataset, 2));
		}
		RandomAccessibleInterval<T> stacked = Views.stack(transformedChannels);

		// use datasetService.create(rai) for output, then set axes, then convert back to ImagePlus and set LUTs
		Dataset resultDataset = datasetService.create(stacked);
		CalibratedAxis[] originalAxes = new CalibratedAxis[dataset.numDimensions()];
		dataset.axes(originalAxes);
		CalibratedAxis[] newAxes = new CalibratedAxis[resultDataset.numDimensions()];
		int o = 0;
		for (int i=0; i<originalAxes.length; i++) {
			if (originalAxes[i].type() == Axes.CHANNEL) {
				newAxes[newAxes.length - 1] = originalAxes[i];
				o++;
				continue;
			}
			newAxes[i-o] = originalAxes[i];
		}
		resultDataset.setAxes(newAxes);

		resultImp = convertService.convert(resultDataset, ImagePlus.class);
		LUT[] luts = imp.getLuts();
		logService.info("Number of LUTs: " + luts.length);

		for (int i=1; i<= imp.getNChannels(); i++) {
			resultImp.setPositionWithoutUpdate(i, 1, 1);
			resultImp.setLut(luts[i-1]);
		}
	}

	@SuppressWarnings("unchecked")
	private RandomAccessibleInterval<T> extract(Dataset d, int channelIndex) {
		return (RandomAccessibleInterval<T>) Views.hyperSlice(d, d.dimensionIndex(Axes.CHANNEL), channelIndex);
	}

	private RandomAccessibleInterval<T> transform(Dataset d, int channelIndex, AffineGet affine, AffineTransform3D calibration) {
		RandomAccessibleInterval<T> singleChannel = extract(d, channelIndex);
		AffineGet transform = calibration.copy().preConcatenate(affine).preConcatenate(calibration.inverse());
		logService.info("Applying effective transform to channel index " + channelIndex + ": " + transform.toString());
		AffineRandomAccessible<T, ?> transformed = RealViews
				.affine(Views.interpolate(Views.extendZero(singleChannel), new NLinearInterpolatorFactory<T>()), transform);
		return Views.interval(transformed, singleChannel);
	}

	@Override
	public void initialize() {
		if (imp.getNChannels() < 3) {
			transformChannel3 = false;
		}
	}

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
