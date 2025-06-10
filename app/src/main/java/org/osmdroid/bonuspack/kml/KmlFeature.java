package org.osmdroid.bonuspack.kml;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.JsonObject;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public abstract class KmlFeature implements Parcelable, Cloneable {

	public String mId;
	public String mName;
	public String mDescription;
	public boolean mVisibility;
	public boolean mOpen;
	public String mStyle;
	public HashMap<String, String> mExtendedData;

	// Abstract methods
	public abstract BoundingBox getBoundingBox();
	public abstract Overlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument);
	public abstract void writeKMLSpecifics(Writer writer);
	public abstract JsonObject asGeoJSON(boolean isRoot);

	public interface Styler {
		void onFeature(Overlay overlay, KmlFeature kmlFeature);
		void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint);
		void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString);
		void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon);
		void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack);
	}

	public KmlFeature() {
		mVisibility = true;
		mOpen = true;
	}

	public boolean hasGeometry(Class<? extends KmlGeometry> C){
		if (!(this instanceof KmlPlacemark))
			return false;
		KmlPlacemark placemark = (KmlPlacemark)this;
		KmlGeometry geometry = placemark.mGeometry;
		if (geometry == null)
			return false;
		return C.isInstance(geometry);
	}

	public String getExtendedData(String name){
		if (mExtendedData == null)
			return null;
		else
			return mExtendedData.get(name);
	}

	public String getExtendedDataAsText(){
		if (mExtendedData == null)
			return null;
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, String> entry : mExtendedData.entrySet()) {
			result.append(entry.getKey()).append("=").append(entry.getValue()).append("<br>\n");
		}
		return result.length() > 0 ? result.toString() : null;
	}

	public void setExtendedData(String name, String value){
		if (mExtendedData == null)
			mExtendedData = new HashMap<>();
		mExtendedData.put(name, value);
	}

	protected boolean writeKMLExtendedData(Writer writer){
		if (mExtendedData == null)
			return true;
		try {
			writer.write("<ExtendedData>\n");
			for (Map.Entry<String, String> entry : mExtendedData.entrySet()) {
				String name = escapeXml(entry.getKey());
				String value = escapeXml(entry.getValue());
				writer.write("<Data name=\"" + name + "\"><value>" + value + "</value></Data>\n");
			}
			writer.write("</ExtendedData>\n");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean writeAsKML(Writer writer, boolean isDocument, KmlDocument kmlDocument){
		try {
			String objectType;
			if (this instanceof KmlFolder){
				objectType = isDocument ? "Document" : "Folder";
			} else if (this instanceof KmlPlacemark)
				objectType = "Placemark";
			else if (this instanceof KmlGroundOverlay)
				objectType = "GroundOverlay";
			else
				return false;

			writer.write('<' + objectType);
			if (mId != null)
				writer.write(" id=\"" + escapeXml(mId) + "\"");
			writer.write(">\n");

			if (mStyle != null){
				writer.write("<styleUrl>#" + escapeXml(mStyle) + "</styleUrl>\n");
			}
			if (mName != null){
				writer.write("<name>" + escapeXml(mName) + "</name>\n");
			}
			if (mDescription != null){
				writer.write("<description><![CDATA[" + mDescription + "]]></description>\n");
			}
			if (!mVisibility){
				writer.write("<visibility>0</visibility>\n");
			}

			writeKMLSpecifics(writer);
			writeKMLExtendedData(writer);

			if (isDocument){
				kmlDocument.writeKMLStyles(writer);
			}
			writer.write("</" + objectType + ">\n");
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// Helper: Escape XML entities (basic)
	private String escapeXml(String input) {
		if (input == null) return null;
		return input.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	public static KmlFeature parseGeoJSON(JsonObject json){
		if (json == null)
			return null;
		String type = json.get("type").getAsString();
		if ("FeatureCollection".equals(type)){
			return new KmlFolder(json);
		} else if ("Feature".equals(type)){
			return new KmlPlacemark(json);
		} else
			return null;
	}

	@Override
	public KmlFeature clone(){
		try {
			KmlFeature copy = (KmlFeature) super.clone();
			if (mExtendedData != null) {
				copy.mExtendedData = new HashMap<>(mExtendedData);
			}
			return copy;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		out.writeString(mId);
		out.writeString(mName);
		out.writeString(mDescription);
		out.writeInt(mVisibility ? 1 : 0);
		out.writeInt(mOpen ? 1 : 0);
		out.writeString(mStyle);
		// TODO: mExtendedData (optional)
	}

	public KmlFeature(Parcel in){
		mId = in.readString();
		mName = in.readString();
		mDescription = in.readString();
		mVisibility = in.readInt() == 1;
		mOpen = in.readInt() == 1;
		mStyle = in.readString();
		// TODO: mExtendedData (optional)
	}
}
