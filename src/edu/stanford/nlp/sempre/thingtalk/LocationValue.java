package edu.stanford.nlp.sempre.thingtalk;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

public class LocationValue extends Value {
	public enum RelativeTag {
		ABSOLUTE, REL_CURRENT_LOCATION, REL_HOME, REL_WORK
	}

	private final RelativeTag relativeTag;
	private final double latitude;
	private final double longitude;

	public LocationValue(double latitude, double longitude) {
		this.relativeTag = RelativeTag.ABSOLUTE;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public LocationValue(RelativeTag relativeTag) {
		this.relativeTag = relativeTag;
		this.latitude = -1;
		this.longitude = -1;
	}

	public LocationValue(LispTree tree) {
		this.relativeTag = RelativeTag.valueOf(tree.child(1).value.toUpperCase());
		if (this.relativeTag == RelativeTag.ABSOLUTE) {
			this.latitude = Double.parseDouble(tree.child(2).value);
			this.longitude = Double.parseDouble(tree.child(3).value);
		} else {
			this.latitude = -1;
			this.longitude = -1;
		}
	}

	@Override
	public LispTree toLispTree() {
		LispTree tree = LispTree.proto.newList();
		tree.addChild("location");
		tree.addChild(relativeTag.toString().toLowerCase());
		tree.addChild(Double.toString(latitude));
		tree.addChild(Double.toString(longitude));
		return tree;
	}

	@Override
	public Map<String, Object> toJson() {
		Map<String, Object> json = new HashMap<>();
		json.put("relativeTag", relativeTag.toString().toLowerCase());
		json.put("latitude", latitude);
		json.put("longitude", longitude);
		return json;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((relativeTag == null) ? 0 : relativeTag.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationValue other = (LocationValue) obj;
		if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
			return false;
		if (relativeTag != other.relativeTag)
			return false;
		return true;
	}

}
