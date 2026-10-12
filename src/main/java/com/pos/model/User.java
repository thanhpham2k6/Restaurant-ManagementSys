package com.pos.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private boolean active;

    public User() {}
    public User(int id, String username, String passwordHash, String fullName, Role role, boolean active) {
        this.id = id; this.username = username; this.passwordHash = passwordHash;
        this.fullName = fullName; this.role = role; this.active = active;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    @Override public String toString() { return fullName + " [" + role + "]"; }
}
