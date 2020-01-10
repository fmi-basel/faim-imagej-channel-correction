package ch.fmi.transform.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.imglib2.realtransform.AffineTransform3D;

import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;

@Plugin(type = IOPlugin.class)
public class AffineTransform3DInput extends AbstractIOPlugin<AffineTransform3D> {
	
	private static String suffix = ".transform";

	@Override
	public Class<AffineTransform3D> getDataType() {
		return AffineTransform3D.class;
	}

	@Override
	public boolean supportsOpen(String source) {
		if (!source.endsWith(suffix)) return false;
		File file = new File(source);
		try (FileInputStream fis = new FileInputStream(file); DataInputStream dis = new DataInputStream(fis)) {
			return (dis.readInt() == 3);
		}
		catch (FileNotFoundException exc) {
			return false;
		}
		catch (IOException exc) {
			return false;
		}
	}

	@Override
	public AffineTransform3D open(String source) throws IOException {
		File file = new File(source);
		try (FileInputStream fis = new FileInputStream(file); DataInputStream dis = new DataInputStream(fis)) {
			dis.readInt(); // Ignore number of dimensions here
			AffineTransform3D affine = new AffineTransform3D();
			double[] values = new double[12];
			for (int i=0; i<values.length; i++) {
				values[i] = dis.readDouble();
			}
			affine.set(values);
			return affine;
		}
	}
}
