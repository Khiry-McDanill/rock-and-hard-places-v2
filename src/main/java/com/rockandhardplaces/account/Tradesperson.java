package com.rockandhardplaces.account;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;

import com.rockandhardplaces.catalog.PersonSpecialty;
import com.rockandhardplaces.project.ProjectTeam;
import com.rockandhardplaces.project.TaskAssignment;
import com.rockandhardplaces.project.Bid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tradespeople")
public class Tradesperson {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private TradespersonVerificationStatus verificationStatus;

    @Column(name = "profile_image_reference")
    private String profileImageReference;

    @NotBlank
    @Column(name = "base_zip", nullable = false)
    private String baseZip;

    @Column(name = "service_radius", nullable = false)
    private Integer serviceRadius;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "available_start_date")
    private LocalDate availableStartDate;

    @OneToMany(mappedBy = "tradesperson")
    private List<PersonTrade> personTrades = new ArrayList<>();

    @OneToMany(mappedBy = "tradesperson")
    private List<PersonSpecialty> personSpecialties = new ArrayList<>();

    @OneToMany(mappedBy = "tradesperson")
    private List<TaskAssignment> taskAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "tradesperson")
    private List<ProjectTeam> projectTeams = new ArrayList<>();

    @OneToMany(mappedBy = "tradesperson")
    private List<Bid> bids = new ArrayList<>();

    protected Tradesperson() {
    }

    public Tradesperson(User user, String displayName) {
        this(user, displayName, "00000", 0, AvailabilityStatus.AVAILABLE_NOW, null);
    }

    public Tradesperson(User user, String displayName, String baseZip, Integer serviceRadius,
            AvailabilityStatus availabilityStatus, LocalDate availableStartDate) {
        this.user = user;
        this.displayName = displayName;
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationStatus = TradespersonVerificationStatus.NOT_SUBMITTED;
        this.baseZip = baseZip;
        this.serviceRadius = serviceRadius;
        this.availabilityStatus = availabilityStatus;
        this.availableStartDate = availableStartDate;
    }

    public void updateProfessionalIdentity(String name, String zip, Integer radius, AvailabilityStatus availability) {
        this.displayName = name; this.baseZip = zip; this.serviceRadius = radius; this.availabilityStatus = availability;
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

    public String getBaseZip() {
        return baseZip;
    }

    public Integer getServiceRadius() {
        return serviceRadius;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public LocalDate getAvailableStartDate() {
        return availableStartDate;
    }

    public List<PersonTrade> getPersonTrades() {
        return personTrades;
    }

    public List<PersonSpecialty> getPersonSpecialties() {
        return personSpecialties;
    }

    public List<TaskAssignment> getTaskAssignments() {
        return taskAssignments;
    }

    public List<ProjectTeam> getProjectTeams() {
        return projectTeams;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public AccountStatus getAccountStatus() { return accountStatus; }

    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public TradespersonVerificationStatus getVerificationStatus() { return verificationStatus; }

    public void setVerificationStatus(TradespersonVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getProfileImageReference() { return profileImageReference; }

    public void setProfileImageReference(String profileImageReference) {
        this.profileImageReference = profileImageReference;
    }
}
