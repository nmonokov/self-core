/**
 * Copyright (c) 2020, Self XDSD Contributors
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"),
 * to read the Software only. Permission is hereby NOT GRANTED to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.selfxdsd.core.mock;

import com.selfxdsd.api.Project;
import com.selfxdsd.api.Wallet;
import com.selfxdsd.api.Wallets;
import com.selfxdsd.api.storage.Storage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory Wallets.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 0.0.21
 */
public final class InMemoryWallets implements Wallets {

    /**
     * Wallets "table".
     */
    private final Map<InMemoryWallets.WalletKey, Wallet> wallets;

    /**
     * Parent storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     *
     * @param storage Parent storage
     */
    public InMemoryWallets(final Storage storage) {
        this.storage = storage;
        this.wallets = new HashMap<>();
    }

    @Override
    public Wallet register(
        final Project project,
        final String type,
        final BigDecimal cash
    ) {
        return null;
    }

    @Override
    public Wallets ofProject(final Project project) {
        return null;
    }

    @Override
    public Wallet active() {
        throw new UnsupportedOperationException(
            "You cannot get the active wallet out of all wallets in Self. "
            + "Call #ofProject(...) first."
        );
    }

    @Override
    public Iterator<Wallet> iterator() {
        throw new UnsupportedOperationException(
            "You cannot iterate over all wallets in Self. "
            + "Call #ofProject(...) first."
        );
    }

    /**
     * Wallet primary key, formed by the project's foreign key and
     * the wallet type.
     */
    private static class WalletKey {

        /**
         * Full name of the Repo represented by the Project.
         */
        private final String repoFullName;

        /**
         * Contributor/Project's provider.
         */
        private final String provider;

        /**
         * Wallet's type.
         */
        private final String type;

        /**
         * Constructor.
         *
         * @param repoFullName Fullname of the Repo represented by the project.
         * @param provider Contributor/Project's provider.
         * @param type Wallet type.
         */
        WalletKey(
            final String repoFullName,
            final String provider,
            final String type
        ) {
            this.repoFullName = repoFullName;
            this.provider = provider;
            this.type = type;
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            final InMemoryWallets.WalletKey key =
                (InMemoryWallets.WalletKey) object;
            return this.repoFullName.equals(key.repoFullName)
                && this.provider.equals(key.provider)
                && this.type.equals(key.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                this.repoFullName,
                this.provider,
                this.type
            );
        }
    }
}