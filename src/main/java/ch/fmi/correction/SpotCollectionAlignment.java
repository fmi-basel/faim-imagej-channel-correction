
package ch.fmi.correction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import net.imglib2.realtransform.AffineGet;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.convert.ConvertService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.models.AffineModel3D;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel3D;
import mpicbg.models.TranslationModel3D;
import plugin.DescriptorParameters;
import process.ComparePair;
import process.Matching;

@Plugin(type = Command.class,
	menuPath = "FMI > Multi-Channel Image Correction > Measure Transform Between Point Clouds")
public class SpotCollectionAlignment extends ContextCommand {

	private static final String TRANSLATION_2D = "2d-translation";
	private static final String RIGID_2D = "2d-rigid";
	private static final String AFFINE_2D = "2d-affine";
	private static final String AFFINE_2D_TRANSLATION_3D =
		"2d-affine + 3d-translation";
	private static final String TRANSLATION_3D = "3d-translation";
	private static final String SIMILARITY_3D = "3d-similarity";
	private static final String RIGID_3D = "3d-rigid";
	private static final String AFFINE_3D = "3d-affine";

	@Parameter(label = "Channel 1 Spots (reference)")
	private SpotCollection spots1;

	@Parameter(label = "Channel 2 Spots")
	private SpotCollection spots2;

	@Parameter(label = "Include another channel (3)")
	private boolean process_ch3 = false;

	@Parameter(label = "Channel 3 Spots", required = false)
	private SpotCollection spots3;

	@Parameter(label = "Include another channel (4)")
	private boolean process_ch4 = false;

	@Parameter(label = "Channel 4 Spots", required = false)
	private SpotCollection spots4;

	@Parameter(choices = { TRANSLATION_2D, RIGID_2D, AFFINE_2D,
		AFFINE_2D_TRANSLATION_3D, TRANSLATION_3D, RIGID_3D, SIMILARITY_3D,
		AFFINE_3D })
	private String transformationType = SIMILARITY_3D;

	@Parameter(label = "Name to store transforms (will be pre-pended with C1-, C2- etc.")
	private String transformName;

	@Parameter(label = "Remember Transforms in current ImageJ instance")
	private Boolean registerObjects;

	@Parameter(label = "Save Transforms to Output Directory")
	private Boolean saveTransforms;

	@Parameter(style = "directory", required = false)
	private File outputDirectory;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private IOService ioService;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		// TODO ensure we have a single frame only (per SpotCollection)?

		List<SpotCollection> spotCollections = new ArrayList<>();
		spotCollections.add(spots1);
		spotCollections.add(spots2);
		if (process_ch3) spotCollections.add(spots3);
		if (process_ch4) spotCollections.add(spots4);

		// FIXME 2D cases not supported yet
		ArrayList<ArrayList<DifferenceOfGaussianPeak<FloatType>>>peakListList = convertSpotsToPeaks(spotCollections);
		logService.info(peakListList.size());
		DescriptorParameters params = defaultParameters(transformationType);
		Vector<ComparePair> comparePairs = Matching.descriptorMatching(peakListList, peakListList.size(), params, 1.0f);
		logService.info(comparePairs.size());

		/*
		// 2-channel case (required?)
		if (comparePairs.size() == 2) {
			affine_ch2 = convertService.convert(comparePairs.get(0).model, AffineGet.class); // FIXME			
		} else { // any number of channels
			ArrayList<InvertibleBoundable> models = Matching.globalOptimization(comparePairs, 0, params);
		}
		*/

		ArrayList<InvertibleBoundable> models = Matching.globalOptimization(comparePairs, peakListList.size(), params);
		if (models == null) {
			logService.error("No transformation models could be found.");
			return;
		}
		
		if (saveTransforms && !outputDirectory.exists()) {
			logService.error("Output folder doesn't exist.");
			saveTransforms = false;
		}

		for (int i = 0; i < models.size(); i++) {
			AffineGet affine = convertService.convert(models.get(i), AffineGet.class);
			if (registerObjects) {
				objectService.addObject(affine, "C" + (i+1) + "-" + transformName);
			}
			if (saveTransforms) {
				try {
					ioService.save(affine, new File(outputDirectory, "C" + (i+1) + "-" + transformName + ".transform").getAbsolutePath());
				} catch (IOException e) {
					logService.error("Error when saving transforms", e);
				}
			}
		}

		// Quality assessment? Map of abs?({dx,dy,dz}) over xy plane for correspondences...
		// => requires input image dimensions: separate plugin?
		// OUTPUT: one AffineGet for each non-reference channel
	}

	private DescriptorParameters defaultParameters(String transformType) {
		DescriptorParameters params = new DescriptorParameters();
		switch (transformType) {
			case TRANSLATION_2D:
			case RIGID_2D:
			case AFFINE_2D:
			case AFFINE_2D_TRANSLATION_3D:
				throw new RuntimeException("Not yet implemented.");
			case TRANSLATION_3D:
				params.model = new TranslationModel3D();
				break;
			case RIGID_3D:
				params.model = new RigidModel3D();
				break;
			case SIMILARITY_3D:
				params.model = new SimilarityModel3D();
				break;
			case AFFINE_3D:
			default:
				params.model = new AffineModel3D();
				break;
		}
		params.dimensionality = 3;
		params.numNeighbors = 3;
		params.significance = 3.0;
		params.similarOrientation = true;
		params.ransacThreshold = 5;
		params.redundancy = 1;
		//params.globalOpt = 0;
		return params;
	}

	private ArrayList<ArrayList<DifferenceOfGaussianPeak<FloatType>>> convertSpotsToPeaks(
		List<SpotCollection> spotCollections)
	{
		// Create List<Peak> for each spot collection
		ArrayList<ArrayList<DifferenceOfGaussianPeak<FloatType>>> peakListList = new ArrayList<>();
		double[] realPosition = new double[3];
		int[] position = new int[3];
		for (SpotCollection collection : spotCollections) {
			ArrayList<DifferenceOfGaussianPeak<FloatType>> peakList = new ArrayList<>();
			collection.iterable(false).forEach(spot -> peakList.add(createPeak(spot, realPosition, position)));
			peakListList.add(peakList);
		}
		return peakListList;
	}

	private DifferenceOfGaussianPeak<FloatType> createPeak(Spot spot,
		double[] realPos, int[] pos)
	{
		spot.localize(realPos);
		for (int i = 0; i < realPos.length; i++) {
			pos[i] = (int) realPos[i];
		}
		DifferenceOfGaussianPeak<FloatType> p = new DifferenceOfGaussianPeak<>(pos,
			new FloatType(), SpecialPoint.MAX);
		for (int d = 0; d < realPos.length; d++) {
			p.setSubPixelLocationOffset((float) (realPos[d] - pos[d]), d);
		}
		return p;
	}
}
