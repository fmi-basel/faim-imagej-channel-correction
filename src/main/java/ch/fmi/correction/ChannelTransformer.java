
package ch.fmi.correction;

import java.util.ArrayList;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "FMI > Multi-Channel Image Correction > Apply Channel Transformation")
public class ChannelTransformer<T extends RealType<T>> extends DynamicCommand implements Initializable {
	// TODO decide if we should add an Identity transform (if absent)
	// to ObjectService in an initialize() method (DynamicCommand)

	@Parameter
	private Dataset dataset;

	@Parameter(required = false)
	private AffineGet affineChannel1;

	@Parameter
	private Boolean transformChannel1 = false;

	@Parameter(required = false)
	private AffineGet affineChannel2;

	@Parameter
	private Boolean transformChannel2 = true;

	@Parameter(required = false)
	private AffineGet affineChannel3;

	@Parameter
	private Boolean transformChannel3 = true;

	@Parameter(type = ItemIO.OUTPUT)
	private ImgPlus<T> outputImgPlus;

	@Parameter
	private OpService opService;

	@Parameter
	private DatasetService datasetService;

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		ArrayList<RandomAccessibleInterval<T>> transformedChannels = new ArrayList<>();
		transformedChannels.add(transformChannel1 ? //
				transform(dataset, 0, affineChannel1) : extract(dataset, 0));
		transformedChannels.add(transformChannel2 ? //
				transform(dataset, 1, affineChannel2) : extract(dataset, 1));
		if (dataset.dimension(Axes.CHANNEL) > 2) {
			transformedChannels.add(transformChannel3 ? //
					transform(dataset, 2, affineChannel3) : extract(dataset, 2));
		}
		RandomAccessibleInterval<T> stacked = Views.stack(transformedChannels);
		ImgPlus<T> imgPlus = (ImgPlus<T>) dataset.getImgPlus();
		Img<T> img = ImgView.wrap(stacked, imgPlus.factory());
		outputImgPlus = new ImgPlus<>(img);
		// TODO transfer ImgPlus metadata?
		// TODO add compatibility for higher-dimensional images, using slicewise...
		outputImgPlus.setAxis(dataset.axis(Axes.CHANNEL).get(), outputImgPlus.numDimensions() - 1);	
	}

	@SuppressWarnings("unchecked")
	private RandomAccessibleInterval<T> extract(Dataset d, int channelIndex) {
		return (RandomAccessibleInterval<T>) Views.hyperSlice(d, d.dimensionIndex(Axes.CHANNEL), channelIndex);
	}

	private RandomAccessibleInterval<T> transform(Dataset d, int channelIndex, AffineGet affine) {
		RandomAccessibleInterval<T> singleChannel = extract(d, channelIndex);
		AffineRandomAccessible<T, ?> transformed = RealViews
				.affine(Views.interpolate(Views.extendZero(singleChannel), new NLinearInterpolatorFactory<T>()), affine);
		return Views.interval(transformed, singleChannel);
	}

	@Override
	public void initialize() {
		if (dataset.dimension(Axes.CHANNEL) < 3) {
			transformChannel3 = false;
		}
	}

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
