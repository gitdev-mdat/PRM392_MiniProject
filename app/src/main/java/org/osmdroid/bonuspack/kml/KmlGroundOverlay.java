package org.osmdroid.bonuspack.kml;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonObject;

import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KmlGroundOverlay extends KmlFeature implements Cloneable, Parcelable {

	public String mIconHref;
	public Bitmap mIcon;
	public int mColor;
	public float mRotation;
	public ArrayList<GeoPoint> mCoordinates;

	public KmlGroundOverlay() {
		super();
		mColor = 0xFF000000;
	}

	public KmlGroundOverlay(GroundOverlay overlay) {
		this();
		mCoordinates = overlay.getAllBounds();
		mIcon = overlay.getImage();
		mRotation = -overlay.getBearing();
		mColor = 255 - Color.alpha((int)(overlay.getTransparency() * 255));
		mVisibility = overlay.isEnabled();
	}

	@Override
	public BoundingBox getBoundingBox() {
		return BoundingBox.fromGeoPoints(mCoordinates);
	}

	public void setIcon(String href, File containerFile, ZipFile kmzContainer) {
		mIconHref = href;
		if (mIconHref.startsWith("http://") || mIconHref.startsWith("https://")) {
			mIcon = BonusPackHelper.loadBitmap(mIconHref);
		} else if (kmzContainer == null) {
			if (containerFile != null) {
				String actualFullPath = containerFile.getParent() + '/' + mIconHref;
				mIcon = BitmapFactory.decodeFile(actualFullPath);
			} else
				mIcon = null;
		} else {
			try {
				final ZipEntry fileEntry = kmzContainer.getEntry(href);
				InputStream stream = kmzContainer.getInputStream(fileEntry);
				mIcon = BitmapFactory.decodeStream(stream);
			} catch (Exception e) {
				mIcon = null;
			}
		}
	}

	public void setLatLonBox(double north, double south, double east, double west) {
		mCoordinates = new ArrayList<>(2);
		mCoordinates.add(new GeoPoint(north, west));
		mCoordinates.add(new GeoPoint(south, east));
	}

	public void setLatLonQuad(ArrayList<GeoPoint> coords) {
		mCoordinates = new ArrayList<>(coords.size());
		for (GeoPoint g : coords)
			mCoordinates.add(g.clone());
	}

	@Override
	public Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument) {
		GroundOverlay overlay = new GroundOverlay();
		if (mCoordinates.size() == 2) {
			GeoPoint pNW = mCoordinates.get(0);
			GeoPoint pSE = mCoordinates.get(1);
			overlay.setPositionFromBounds(pNW, pSE);
		} else if (mCoordinates.size() == 4) {
			overlay.setPositionFromBounds(mCoordinates.get(3), mCoordinates.get(2),
					mCoordinates.get(1), mCoordinates.get(0));
		}

		if (mIcon != null) {
			overlay.setImage(mIcon);
			float transparency = 1.0f - Color.alpha(mColor) / 255.0f;
			overlay.setTransparency(transparency);
		} else {
			Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
			bitmap.eraseColor(mColor);
			overlay.setImage(bitmap);
		}

		overlay.setBearing(-mRotation);
		if (styler == null)
			overlay.setEnabled(mVisibility);
		else
			styler.onFeature(overlay, this);

		return overlay;
	}

	@Override
	public void writeKMLSpecifics(Writer writer) {
		try {
			writer.write("<color>" + ColorStyle.colorAsKMLString(mColor) + "</color>\n");
			writer.write("<Icon><href>" + escapeXml(mIconHref) + "</href></Icon>\n");
			if (mCoordinates.size() == 2) {
				writer.write("<LatLonBox>");
				GeoPoint pNW = mCoordinates.get(0);
				GeoPoint pSE = mCoordinates.get(1);
				writer.write("<north>" + pNW.getLatitude() + "</north>");
				writer.write("<south>" + pSE.getLatitude() + "</south>");
				writer.write("<east>" + pSE.getLongitude() + "</east>");
				writer.write("<west>" + pNW.getLongitude() + "</west>");
				writer.write("<rotation>" + mRotation + "</rotation>");
				writer.write("</LatLonBox>\n");
			} else {
				writer.write("<gx:LatLonQuad>");
				KmlGeometry.writeKMLCoordinates(writer, mCoordinates);
				writer.write("</gx:LatLonQuad>\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Helper to escape XML
	private String escapeXml(String input) {
		if (input == null) return null;
		return input.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	@Override
	public JsonObject asGeoJSON(boolean isRoot) {
		// TODO: GroundOverlay is not supported by GeoJSON. Output enclosing polygon with mColor?
		return null;
	}

	@Override
	public KmlGroundOverlay clone() {
		KmlGroundOverlay kmlGroundOverlay = (KmlGroundOverlay) super.clone();
		kmlGroundOverlay.mCoordinates = KmlGeometry.cloneArrayOfGeoPoint(mCoordinates);
		return kmlGroundOverlay;
	}

	// Parcelable implementation
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(mIconHref);
		out.writeParcelable(mIcon, flags);
		out.writeInt(mColor);
		out.writeFloat(mRotation);
		out.writeList(mCoordinates);
	}

	public static final Creator<KmlGroundOverlay> CREATOR = new Creator<KmlGroundOverlay>() {
		@Override
		public KmlGroundOverlay createFromParcel(Parcel source) {
			return new KmlGroundOverlay(source);
		}

		@Override
		public KmlGroundOverlay[] newArray(int size) {
			return new KmlGroundOverlay[size];
		}
	};

	public KmlGroundOverlay(Parcel in) {
		super(in);
		mIconHref = in.readString();
		mIcon = in.readParcelable(Bitmap.class.getClassLoader());
		mColor = in.readInt();
		mRotation = in.readFloat();
		mCoordinates = in.readArrayList(GeoPoint.class.getClassLoader());
	}
}
