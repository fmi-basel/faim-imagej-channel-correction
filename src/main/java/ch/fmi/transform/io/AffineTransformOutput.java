package ch.fmi.transform.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.imglib2.realtransform.AffineGet;

import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;

@Plugin(type = IOPlugin.class)
public class AffineTransformOutput extends AbstractIOPlugin<AffineGet> {
	
	private static String suffix = ".transform";

	@Override
	public Class<AffineGet> getDataType() {
		return AffineGet.class;
	}

	@Override
	public boolean supportsSave(String destination) {
		return destination.endsWith(suffix);
	}

	@Override
	public void save(AffineGet affine, String destination) throws IOException {
		File file = new File(destination);
		try (FileOutputStream fos = new FileOutputStream(file); DataOutputStream dos = new DataOutputStream(fos)) {
			// number of dimensions
			dos.writeInt(affine.numDimensions());
			// matrix
			double[] matrix = affine.getRowPackedCopy();
			for (double d : matrix) {
				dos.writeDouble(d);
			}
		}
	}
}
