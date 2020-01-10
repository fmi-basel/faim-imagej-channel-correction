package ch.fmi.transform.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.scijava.Context;
import org.scijava.io.IOService;


public class AffineTransformIOTest {
	
	private Context context;
	private IOService ioService;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setUp() {
		context = new Context();
		ioService = context.service(IOService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testAffineTransform3DIO() throws IOException {
		AffineTransform3D affine = new AffineTransform3D();
		affine.translate(2.0, 3.0, 4.0);
		affine.scale(0.5);
		affine.rotate(2, 0.5);
		File tmpFile = folder.newFile("testAffine3D.transform");
		ioService.save(affine, tmpFile.getAbsolutePath());
		Object saved = ioService.open(tmpFile.getAbsolutePath());
		assertTrue(saved instanceof AffineTransform3D);
		assertArrayEquals(affine.getRowPackedCopy(), ((AffineTransform3D) saved).getRowPackedCopy(), 0.0);
	}

	@Test
	public void testAffineTransform2DIO() throws IOException {
		AffineTransform2D affine = new AffineTransform2D();
		affine.translate(2.0, 3.0);
		affine.scale(0.5);
		affine.rotate(0.5);
		File tmpFile = folder.newFile("testAffine2D.transform");
		ioService.save(affine, tmpFile.getAbsolutePath());
		Object saved = ioService.open(tmpFile.getAbsolutePath());
		assertTrue(saved instanceof AffineTransform2D);
		assertArrayEquals(affine.getRowPackedCopy(), ((AffineTransform2D) saved).getRowPackedCopy(), 0.0);
	}
}
