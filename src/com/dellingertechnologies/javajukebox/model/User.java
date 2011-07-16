package com.dellingertechnologies.javajukebox.model;

public class User {

	public static final User DEFAULT = new User("default");
	
	private String username;
	private String gravatarId;
	private boolean enabled = true;

	public User(String username) {
		this.username = username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setGravatarId(String gravatarId) {
		this.gravatarId = gravatarId;
	}

	public String getGravatarId() {
		return gravatarId;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean equals(Object obj) {
		User user = (User) obj;
		return getUsername().equals(user.getUsername());
	}

	@Override
	public int hashCode() {
		return getUsername().hashCode();
	}
	
}
