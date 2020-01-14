
package ch.fmi.transform.convert;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import mpicbg.models.Affine3D;
import mpicbg.models.TranslationModel3D;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Translation3D;

@SuppressWarnings("rawtypes")
@Plugin(type = Converter.class)
public class MPICBGAffine3DToAffineGetConverter extends
	AbstractConverter<Affine3D, AffineGet>
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		AffineGet affine;
		if (src instanceof TranslationModel3D) {
			affine = new Translation3D(((TranslationModel3D) src).getTranslation());
		} else if (src instanceof Affine3D) {
			affine = new AffineTransform3D();
			double[] m = new double[12];
			((Affine3D) src).toArray(m);
			((AffineTransform3D) affine).set(m[0], m[3], m[6], m[9], m[1], m[4], m[7], m[10], m[2], m[5], m[8], m[11]);
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
	public Class<Affine3D> getInputType() {
		return Affine3D.class;
	}
}
