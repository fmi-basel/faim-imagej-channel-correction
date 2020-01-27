
package ch.fmi.correction;

import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Computes displacement maps of dx, dy and dz for a given affine transformation
 * when applied to all positions in an image.
 * 
 * @author Jan Eglinger
 */
@Plugin(type = Command.class,
	menuPath = "FMI > Multi-Channel Image Correction > Compute Displacement Map")
public class DisplacementMap extends ContextCommand {

	@Parameter
	private long dimX = 512;

	@Parameter
	private long dimY = 512;

	@Parameter(required = false)
	private long dimZ = 3;

	@Parameter
	private AffineGet transform;

	@Parameter
	private OpService opService;

	@Parameter(type = ItemIO.OUTPUT)
	private Img<DoubleType> dxImg;

	@Parameter(type = ItemIO.OUTPUT)
	private Img<DoubleType> dyImg;

	@Parameter(type = ItemIO.OUTPUT)
	private Img<DoubleType> dzImg;

	private double[] srcLoc;
	private double[] dstLoc;

	@Override
	public void run() {
		if (transform.numDimensions() == 3) {
			dxImg = opService.create().img(new long[] { dimX, dimY, dimZ });
			dyImg = dxImg.copy();
			dzImg = dxImg.copy();
			srcLoc = new double[3];
			dstLoc = new double[3];

			LoopBuilder.setImages(Intervals.positions(dxImg), dxImg, dyImg, dzImg)
				.forEachPixel((pos, x, y, z) -> {
					pos.localize(srcLoc);
					transform.apply(srcLoc, dstLoc);
					x.set(dstLoc[0] - srcLoc[0]);
					y.set(dstLoc[1] - srcLoc[1]);
					z.set(dstLoc[2] - srcLoc[2]);
				});
		}
		else if (transform.numDimensions() == 2) {
			dxImg = opService.create().img(new long[] { dimX, dimY });
			dyImg = dxImg.copy();
			srcLoc = new double[2];
			dstLoc = new double[2];

			LoopBuilder.setImages(Intervals.positions(dxImg), dxImg, dyImg)
				.forEachPixel((pos, x, y) -> {
					pos.localize(srcLoc);
					transform.apply(srcLoc, dstLoc);
					x.set(dstLoc[0] - srcLoc[0]);
					y.set(dstLoc[1] - srcLoc[1]);
				});
		}
		else {
			throw new RuntimeException("Cannot handle number of dimensions: " +
				transform.numDimensions());
		}
		// TODO create datasets, adjust minMax to be symmetric around 0, set colormap
		// TODO consider creating multi-channel image as output (one channel per dimension x,y,z)
	}
}
