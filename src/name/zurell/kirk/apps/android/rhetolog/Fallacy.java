package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.graphics.drawable.Drawable;

public class Fallacy {
	private String SortOrder;
	private String Uuid;
	private String Name;
	private String Title;
	private String Description;
	private String Example;
	private Drawable Icon;
	private int Color;
	/**
	 * @return the name
	 */
	public String getName() {
		return Name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		Name = name;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return Description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		Description = description;
	}
	/**
	 * @return the icon
	 */
	public Drawable getIcon() {
		return Icon;
	}
	/**
	 * @param icon the icon to set
	 */
	public void setIcon(Drawable icon) {
		Icon = icon;
	}
	public int getColor() {
		return Color;
	}
	public void setColor(int color) {
		Color = color;
	}
	public String getSortOrder() {
		return SortOrder;
	}
	public void setSortOrder(String sortOrder) {
		SortOrder = sortOrder;
	}
	public String getUuid() {
		return Uuid;
	}
	public void setUuid(String uuid) {
		Uuid = uuid;
	}
	public String getExample() {
		return Example;
	}
	public void setExample(String example) {
		Example = example;
	}
	public String getTitle() {
		return Title;
	}
	public void setTitle(String title) {
		Title = title;
	}
	
	
}
