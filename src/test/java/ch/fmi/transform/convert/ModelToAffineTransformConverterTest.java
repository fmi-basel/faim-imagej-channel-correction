package ch.fmi.transform.convert;

import static org.junit.Assert.*;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.SimilarityModel3D;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;

public class ModelToAffineTransformConverterTest {

	private Context context;
	private ConvertService convertService;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void test3DModels() {
		double[] expected = new double[12];
		double[] actual = new double[12];

		AffineModel3D affine3d = new AffineModel3D();
		affine3d.rotate(2, 0.5);
		assertTrue(convertService.supports(affine3d, AffineGet.class));
		AffineGet convertedAffine3d = convertService.convert(affine3d, AffineGet.class);
		assertTrue(convertedAffine3d instanceof AffineTransform3D);
		affine3d.getMatrix(expected);
		((AffineTransform3D) convertedAffine3d).toArray(actual);
		assertArrayEquals(expected, actual, 0);

		double[] testLocation = { 2.0, 3.0, 4.0 };
		double[] target = new double[3];
		((AffineTransform3D) convertedAffine3d).apply(testLocation, target);
		assertArrayEquals(affine3d.apply(testLocation), target, 0);

		SimilarityModel3D similarity3d = new SimilarityModel3D();
		assertTrue(convertService.supports(similarity3d, AffineGet.class));
		AffineGet convertedSimilarity3d = convertService.convert(similarity3d, AffineGet.class);
		assertTrue(convertedSimilarity3d instanceof AffineTransform3D);
		similarity3d.getMatrix(expected);
		((AffineTransform3D) convertedSimilarity3d).toArray(actual);
		assertArrayEquals(expected, actual, 0);

		((AffineTransform3D) convertedSimilarity3d).apply(testLocation, target);
		assertArrayEquals(similarity3d.apply(testLocation), target, 0);

		RigidModel3D rigid3d = new RigidModel3D();
		rigid3d.rotate(2, 0.3);
		assertTrue(convertService.supports(rigid3d, AffineGet.class));
		AffineGet convertedRigid3d = convertService.convert(rigid3d, AffineGet.class);
		assertTrue(convertedRigid3d instanceof AffineTransform3D);
		rigid3d.getMatrix(expected);
		((AffineTransform3D) convertedRigid3d).toArray(actual);
		assertArrayEquals(expected, actual, 0);

		((AffineTransform3D) convertedRigid3d).apply(testLocation, target);
		assertArrayEquals(rigid3d.apply(testLocation), target, 0);

		TranslationModel3D translation3d = new TranslationModel3D();
		translation3d.set(2.5, 3.6, 4.2);
		assertTrue(convertService.supports(translation3d, AffineGet.class));
		AffineGet convertedTranslation3d = convertService.convert(translation3d, AffineGet.class);
		assertTrue(convertedTranslation3d instanceof Translation3D);
		translation3d.getMatrix(expected);
		assertArrayEquals(expected, convertedTranslation3d.getRowPackedCopy(), 0);

		((Translation3D) convertedTranslation3d).apply(testLocation, target);
		assertArrayEquals(translation3d.apply(testLocation), target, 0);
	}

	@Test
	public void test2DModels() {
		double[] expected = new double[6];
		double[] actual = new double[6];

		AffineModel2D affine2d = new AffineModel2D();
		affine2d.set(0, -2, 2, 0, 2.5, 3.6);
		assertTrue(convertService.supports(affine2d, AffineGet.class));
		AffineGet convertedAffine2d = convertService.convert(affine2d, AffineGet.class);
		assertTrue(convertedAffine2d instanceof AffineTransform2D);
		//affine2d.toArray(expected);
		//assertArrayEquals(expected, convertedAffine2d.getRowPackedCopy(), 0);

		double[] testLocation = { 2.0, 3.6 };
		double[] target = new double[2];
		((AffineTransform2D) convertedAffine2d).apply(testLocation, target);
		assertArrayEquals(affine2d.apply(testLocation), target, 0);

		SimilarityModel2D similarity2d = new SimilarityModel2D();
		similarity2d.setScaleRotationTranslation(0.5, 0.2, 2.5, 4.6);
		assertTrue(convertService.supports(similarity2d, AffineGet.class));
		AffineGet convertedSimilarity2d = convertService.convert(similarity2d, AffineGet.class);
		assertTrue(convertedSimilarity2d instanceof AffineTransform2D);
		//similarity2d.toArray(expected);
		//((AffineTransform2D) convertedSimilarity2d).toArray(actual);
		//assertArrayEquals(expected, actual, 0);

		((AffineTransform2D) convertedSimilarity2d).apply(testLocation, target);
		assertArrayEquals(similarity2d.apply(testLocation), target, 0);

		RigidModel2D rigid2d = new RigidModel2D();
		rigid2d.set(0.5, 3.6, 4.2);
		assertTrue(convertService.supports(rigid2d, AffineGet.class));
		AffineGet convertedRigid2d = convertService.convert(rigid2d, AffineGet.class);
		assertTrue(convertedRigid2d instanceof AffineTransform2D);
		//rigid2d.toArray(expected);
		//((AffineTransform2D) convertedRigid2d).toArray(actual);
		//assertArrayEquals(expected, actual, 0);

		((AffineTransform2D) convertedRigid2d).apply(testLocation, target);
		assertArrayEquals(rigid2d.apply(testLocation), target, 0);

		TranslationModel2D translation2d = new TranslationModel2D();
		translation2d.set(2.5, 3.6);
		assertTrue(convertService.supports(translation2d, AffineGet.class));
		AffineGet convertedTranslation2d = convertService.convert(translation2d, AffineGet.class);
		assertTrue(convertedTranslation2d instanceof Translation2D);
		//translation2d.toArray(expected);
		//assertArrayEquals(expected, convertedTranslation2d.getRowPackedCopy(), 0);

		((Translation2D) convertedTranslation2d).apply(testLocation, target);
		assertArrayEquals(translation2d.apply(testLocation), target, 0);
	}
}
