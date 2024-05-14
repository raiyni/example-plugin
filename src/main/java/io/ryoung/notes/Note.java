package io.ryoung.notes;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;
import java.awt.Color;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
public class Note implements Comparable<Note>
{
	private static final int SUBSTR_LEN = 15;

	@SerializedName("n")
	private String name;

	@Builder.Default
	@EqualsAndHashCode.Exclude
	@SerializedName("v")
	private boolean visible = true;

	@EqualsAndHashCode.Exclude
	@SerializedName("t")
	private String title;

	@EqualsAndHashCode.Exclude
	@SerializedName("b")
	private String body;

	@EqualsAndHashCode.Exclude
	@SerializedName("bc")
	private Color backgroundColor;

	@EqualsAndHashCode.Exclude
	@SerializedName("tc")
	private Color textColor;

	@EqualsAndHashCode.Exclude
	@SerializedName("tic")
	private Color titleColor;

	public String getMenuName()
	{
		String target = (!Strings.isNullOrEmpty(getTitle()) ? getTitle() : "");
		if (target.isEmpty())
		{
			target = MoreObjects.firstNonNull(getBody(), "");

			int len = Math.min(target.length(), SUBSTR_LEN);
			target = target.substring(0, len);
		}

		return target;
	}

	@Override
	public int compareTo(Note o)
	{
		return getMenuName().compareToIgnoreCase(o.getMenuName()) * -1;
	}
}
