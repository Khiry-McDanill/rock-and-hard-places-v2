package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.account.HomeownerRepository;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void persistsProjectWithGeneratedIdOwnerStatusAndLocation() {
        Homeowner homeowner = createHomeowner("owner@example.com");

        Project savedProject = projectRepository.saveAndFlush(new Project(
                "Kitchen remodel",
                "Update the kitchen cabinets and counters",
                ProjectStatus.PLANNING,
                "90210",
                homeowner));

        assertThat(savedProject.getId()).isNotNull();
        assertThat(projectRepository.findById(savedProject.getId()))
                .get()
                .satisfies(project -> {
                    assertThat(project.getTitle()).isEqualTo("Kitchen remodel");
                    assertThat(project.getDescription())
                            .isEqualTo("Update the kitchen cabinets and counters");
                    assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNING);
                    assertThat(project.getJobZip()).isEqualTo("90210");
                    assertThat(project.getHomeowner().getId()).isEqualTo(homeowner.getId());
                });
    }

    @Test
    void allowsOneHomeownerToOwnMultipleProjects() {
        Homeowner homeowner = createHomeowner("owner@example.com");

        projectRepository.saveAndFlush(new Project(
                "Bathroom remodel", "Replace the shower", ProjectStatus.IN_PROGRESS,
                "10001", homeowner));
        projectRepository.saveAndFlush(new Project(
                "Roof repair", "Repair the damaged shingles", ProjectStatus.COMPLETED,
                "10001", homeowner));

        assertThat(projectRepository.findAll())
                .extracting(Project::getHomeowner)
                .allMatch(projectOwner -> projectOwner.getId().equals(homeowner.getId()));
        assertThat(projectRepository.count()).isEqualTo(2);
    }

    private Homeowner createHomeowner(String email) {
        User user = userRepository.saveAndFlush(new User(email));
        return homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
    }
}