/**
 * Copyright (c) 2020, Self XDSD Contributors
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permission is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.selfxdsd.core.contributors;

import com.selfxdsd.api.*;
import com.selfxdsd.api.storage.Storage;
import com.selfxdsd.core.contracts.ContributorContracts;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unit tests for {@link ProjectContributors}.
 *
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 0.0.4
 */
public final class ProjectContributorsTestCase {

    /**
     * ProjectContributors should be iterable.
     */
    @Test
    public void canBeIterated() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class))::stream,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(contributors, Matchers.iterableWithSize(3));
    }

    /**
     * Returns null when the specified Contributor is not found.
     */
    @Test
    public void getByIdFindsNothing() {
        final Contributors contributors = new ProjectContributors(
            "john/test", "github",
            Stream::empty,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.getById("george", Provider.Names.GITHUB),
            Matchers.nullValue()
        );
    }

    /**
     * Returns the found Contributor.
     */
    @Test
    public void getByIdFindReturnsFound() {
        final Contributor mihai = Mockito.mock(Contributor.class);
        Mockito.when(mihai.username()).thenReturn("mihai");
        Mockito.when(mihai.provider()).thenReturn(Provider.Names.GITHUB);
        final Contributor vlad = Mockito.mock(Contributor.class);
        Mockito.when(vlad.username()).thenReturn("vlad");
        Mockito.when(vlad.provider()).thenReturn(Provider.Names.GITHUB);

        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(vlad, mihai)::stream,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.getById("mihai", Provider.Names.GITHUB),
            Matchers.is(mihai)
        );
    }

    /**
     * Method ofProject should return the same instance if the ID is a match.
     */
    @Test
    public void ofProjectReturnsSelfIfSameId() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class))::stream,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.ofProject("john/test", Provider.Names.GITHUB),
            Matchers.is(contributors)
        );
    }

    /**
     * Method ofProject should complain if the ID of another project is given as
     * input.
     */
    @Test(expected = IllegalStateException.class)
    public void ofProjectComplainsIfDifferentId() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class))::stream,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.ofProject("george/test", Provider.Names.GITLAB),
            Matchers.is(contributors)
        );
    }

    /**
     * We should only be able to register contributors from the same provider.
     */
    @Test(expected = IllegalArgumentException.class)
    public void registerComplainsWhenDiffProvider() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class),
                Mockito.mock(Contributor.class))::stream,
            Mockito.mock(Storage.class)
        );
        contributors.register("mihai", Provider.Names.GITLAB);
    }

    /**
     * If the contributor is already registered, just return it.
     */
    @Test
    public void contributorAlreadyRegistered() {
        final Contributor vlad = Mockito.mock(Contributor.class);
        Mockito.when(vlad.username()).thenReturn("vlad");
        Mockito.when(vlad.provider()).thenReturn(Provider.Names.GITHUB);
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(vlad)::stream,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.register("vlad", Provider.Names.GITHUB),
            Matchers.is(vlad)
        );
    }

    /**
     * A new contributor is registered and a DEV Contract with hourly rate 0 is
     * created.
     */
    @Test
    public void registersNewContributor() {
        final Contributor mihai = Mockito.mock(Contributor.class);
        final List<Contributor> allContributorsSrc = new ArrayList<>();
        final Contributors allContributors = Mockito.mock(Contributors.class);
        Mockito.when(
            allContributors.register("mihai", Provider.Names.GITHUB)
        ).thenAnswer(invocation -> {
            allContributorsSrc.add(mihai);
            return mihai;
        });
        Mockito.when(allContributors.spliterator())
            .thenReturn(allContributorsSrc.spliterator());

        final Contracts allContracts = Mockito.mock(Contracts.class);
        Mockito.when(
            allContracts.addContract(
                "john/test", "mihai", Provider.Names.GITHUB,
                BigDecimal.valueOf(0), Contract.Roles.DEV
            )
        ).thenReturn(Mockito.mock(Contract.class));
        final Storage storage = Mockito.mock(Storage.class);
        Mockito.when(storage.contributors()).thenReturn(allContributors);
        Mockito.when(storage.contracts()).thenReturn(allContracts);

        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            allContributorsSrc::stream,
            storage
        );
        MatcherAssert.assertThat(contributors, Matchers.emptyIterable());
        MatcherAssert.assertThat(
            contributors.register("mihai", Provider.Names.GITHUB),
            Matchers.is(mihai)
        );
        MatcherAssert.assertThat(contributors, Matchers.iterableWithSize(1));
    }

    /**
     * Elect(...) returns null if ProjectContributors is empty.
     */
    @Test
    public void electsReturnsNullWhenNoContributors() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            Stream::empty,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(
            contributors.elect(Mockito.mock(Task.class)),
            Matchers.nullValue()
        );
    }

    /**
     * Elect(...) returns a new Contributor for an already assigned Task. The
     * elected contributor cannot be the same as the one already assigned.
     */
    @Test
    public void electsNewContributorForAssignedTask() {
        final Contributor assignee = this.mockContributor(
            "mihai", "DEV", "REV", "QA"
        );
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(
                assignee,
                this.mockContributor("vlad", "DEV"),
                this.mockContributor("mary", "REV", "QA"),
                this.mockContributor("george", "DEV", "ARCH"),
                this.mockContributor("karen", "DEV")
            )::stream,
            Mockito.mock(Storage.class)
        );
        final Task task = Mockito.mock(Task.class);
        Mockito.when(task.assignee()).thenReturn(assignee);
        Mockito.when(task.role()).thenReturn("DEV");
        final Contributor elected = contributors.elect(task);

        MatcherAssert.assertThat(
            elected.username(),
            Matchers.not(Matchers.equalTo("mihai"))
        );
        MatcherAssert.assertThat(
            elected.username(),
            Matchers.not(Matchers.equalTo("mary"))
        );
        MatcherAssert.assertThat(
            elected.username(),
            Matchers.isOneOf("vlad", "george", "karen")
        );
    }

    /**
     * Elect(...) returns a new Contributor for an unassigned Task.
     */
    @Test
    public void electsNewContributorForUnassignedTask() {
        final Contributors contributors = new ProjectContributors(
            "john/test", Provider.Names.GITHUB,
            List.of(
                this.mockContributor("mihai", "DEV", "REV", "QA"),
                this.mockContributor("vlad", "DEV"),
                this.mockContributor("mary", "REV", "QA"),
                this.mockContributor("george", "DEV", "ARCH"),
                this.mockContributor("karen", "DEV")
            )::stream,
            Mockito.mock(Storage.class)
        );
        final Task task = Mockito.mock(Task.class);
        Mockito.when(task.assignee()).thenReturn(null);
        Mockito.when(task.role()).thenReturn("DEV");
        final Contributor elected = contributors.elect(task);

        MatcherAssert.assertThat(
            elected.username(),
            Matchers.not(Matchers.equalTo("mary"))
        );
        MatcherAssert.assertThat(
            elected.username(),
            Matchers.isOneOf("mihai", "vlad", "george", "karen")
        );
    }

    /**
     * Can compare two StoredContributor objects.
     */
    @Test
    public void comparesStoredContributorObjects() {
        final Contributor contributor = new StoredContributor(
            "mihai",
            Provider.Names.GITHUB,
            Mockito.mock(Storage.class)
        );
        final Contributor contributorTwo = new StoredContributor(
            "mihai",
            Provider.Names.GITHUB,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(contributor, Matchers.equalTo(contributorTwo));
    }

    /**
     * Verifies HashCode generation from StoredContributor.
     */
    @Test
    public void verifiesStoredContributorHashcode() {
        final Contributor contributor = new StoredContributor(
            "mihai",
            Provider.Names.GITHUB,
            Mockito.mock(Storage.class)
        );
        final Contributor contributorTwo = new StoredContributor(
            "mihai",
            Provider.Names.GITHUB,
            Mockito.mock(Storage.class)
        );
        MatcherAssert.assertThat(contributor.hashCode(),
            Matchers.equalTo(contributorTwo.hashCode()));
    }

    /**
     * Mock a Contributor.
     *
     * @param username Username.
     * @param roles Roles.
     * @return Contributor.
     */
    public Contributor mockContributor(final String username,
        final String... roles) {
        final Contributor contributor = Mockito.mock(Contributor.class);
        Mockito.when(contributor.username()).thenReturn(username);

        final List<Contract> contracts = new ArrayList<>();
        for (final String role : roles) {
            final Contract mock = Mockito.mock(Contract.class);
            Mockito.when(mock.role()).thenReturn(role);
            contracts.add(mock);
        }

        Mockito.when(contributor.contracts()).thenReturn(
            new ContributorContracts(
                contributor,
                contracts::stream,
                Mockito.mock(Storage.class))
        );
        return contributor;
    }
}
