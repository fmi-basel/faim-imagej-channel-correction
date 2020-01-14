
package ch.fmi.transform.convert;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import mpicbg.models.Affine2D;
import mpicbg.models.TranslationModel2D;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.Translation2D;

@SuppressWarnings("rawtypes")
@Plugin(type = Converter.class)
public class MPICBGAffine2DToAffineGetConverter extends
	AbstractConverter<Affine2D, AffineGet>
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		AffineGet affine;
		if (src instanceof TranslationModel2D) {
			double[] m = new double[6];
			((TranslationModel2D) src).toArray(m);
			affine = new Translation2D(m[4], m[5]);
		} else if (src instanceof Affine2D) {
			affine = new AffineTransform2D();
			double[] m = new double[6];
			((Affine2D) src).toArray(m);
			((AffineTransform2D) affine).set(m[0], m[2], m[4], m[1], m[3], m[5]);
		} else {
			throw new RuntimeException("AffineGet: Cannot convert object" + src.toString());
		}
		return (T) affine;
		
	}

	@Override
	public Class<AffineGet> getOutputType() {
		return AffineGet.class;
	}

	@Override
	public Class<Affine2D> getInputType() {
		return Affine2D.class;
	}
}
