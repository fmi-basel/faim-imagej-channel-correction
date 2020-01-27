
package ch.fmi.correction;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.ops.OpService;
import net.imglib2.KDTree;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPointSampleList;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import process.ComparePair;

@Plugin(type = Command.class,
	menuPath = "FMI > Multi-Channel Image Correction > Compute Residual Distance Map")
public class ResidualDistanceMap extends ContextCommand {

	@Parameter
	private Dataset dataset;

	@Parameter
	private ComparePair comparePair;

	@Parameter
	private Boolean specifyTransform = false;

	@Parameter(required = false)
	private AffineGet transform;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset result;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private OpService opService;

	@Parameter
	private LogService logService;

	private double[] targetLoc;
	private double[] calibratedLoc;
	private double distX;
	private double distY;
	private double distZ;

	@Override
	public void run() {
		// TODO initial dimensions?
		RealPointSampleList<DoubleType> dx = new RealPointSampleList<>(3);
		RealPointSampleList<DoubleType> dy = new RealPointSampleList<>(3);
		RealPointSampleList<DoubleType> dz = new RealPointSampleList<>(3);
		targetLoc = new double[3];
		calibratedLoc = new double[3];

		AffineTransform3D calibrationTransform = new AffineTransform3D();
		calibrationTransform.scale(dataset.averageScale(0), dataset.averageScale(1),
			dataset.averageScale(2));

		comparePair.inliers.forEach(match -> {
			double[] loc1 = match.getP1().getL();
			double[] loc2 = match.getP2().getL();
			if (specifyTransform && transform != null) {
				transform.apply(loc2, targetLoc);
				distX = loc1[0] - targetLoc[0];
				distY = loc1[1] - targetLoc[1];
				distZ = loc1[2] - targetLoc[2];
			}
			else {
				targetLoc = comparePair.model.apply(loc1);
				distX = targetLoc[0] - loc2[0];
				distY = targetLoc[1] - loc2[1];
				distZ = targetLoc[2] - loc2[2];
			}
			calibrationTransform.inverse().apply(loc2, calibratedLoc);
			dx.add(new RealPoint(calibratedLoc), new DoubleType(distX));
			dy.add(new RealPoint(calibratedLoc), new DoubleType(distY));
			dz.add(new RealPoint(calibratedLoc), new DoubleType(distZ));
		});

		// Create image and interpolate from sample list.
		// TODO add KNearestNeighborSampling and InverseDistanceWeighting interpolation
		NearestNeighborSearchOnKDTree<DoubleType> searchX = new NearestNeighborSearchOnKDTree<>(new KDTree<>(dx));
		IntervalView<DoubleType> imgX = Views.interval(//
			Views.raster(//
				Views.interpolate(searchX,
					new NearestNeighborSearchInterpolatorFactory<DoubleType>())),
			dataset);

		NearestNeighborSearchOnKDTree<DoubleType> searchY = new NearestNeighborSearchOnKDTree<>(new KDTree<>(dy));
		IntervalView<DoubleType> imgY = Views.interval(//
			Views.raster(//
				Views.interpolate(searchY,
					new NearestNeighborSearchInterpolatorFactory<DoubleType>())),
			dataset);

		NearestNeighborSearchOnKDTree<DoubleType> searchZ = new NearestNeighborSearchOnKDTree<>(new KDTree<>(dz));
		IntervalView<DoubleType> imgZ = Views.interval(//
			Views.raster(//
				Views.interpolate(searchZ,
					new NearestNeighborSearchInterpolatorFactory<DoubleType>())),
			dataset);

		RandomAccessibleInterval<DoubleType> resultImg = Views.stack(imgX, imgY, imgZ);

		result = datasetService.create(resultImg);
		result.setAxis(new DefaultLinearAxis(Axes.CHANNEL), result.numDimensions() - 1);

		// Views.interpolate dx|dy|dz on interval
		// Views.stack dx,dy,dz into channels
	}
}
