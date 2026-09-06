package com.rockandhardplaces.account;

import java.util.ArrayList;
import java.util.List;

import com.rockandhardplaces.project.Project;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "homeowners")
public class Homeowner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "profile_image_reference")
    private String profileImageReference;

    @OneToMany(mappedBy = "homeowner")
    private List<Project> projects = new ArrayList<>();

    protected Homeowner() {
    }

    public Homeowner(User user, String displayName) {
        this.user = user;
        this.displayName = displayName;
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public AccountStatus getAccountStatus() { return accountStatus; }

    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public String getProfileImageReference() { return profileImageReference; }

    public void setProfileImageReference(String profileImageReference) {
        this.profileImageReference = profileImageReference;
    }
}
