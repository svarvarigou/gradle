/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.catalog;

import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.internal.artifacts.ImmutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.plugin.use.PluginDependency;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.gradle.api.internal.catalog.AliasNormalizer.normalize;

public abstract class AbstractExternalDependencyFactory implements ExternalModuleDependencyFactory {
    protected final DefaultVersionCatalog config;
    protected final ProviderFactory providers;

    @SuppressWarnings("unused")
    public static abstract class SubDependencyFactory implements ExternalModuleDependencyFactory {
        protected final AbstractExternalDependencyFactory owner;

        protected SubDependencyFactory(AbstractExternalDependencyFactory owner) {
            this.owner = owner;
        }

        protected Provider<MinimalExternalModuleDependency> create(String alias) {
            return owner.create(alias);
        }

        @Override
        public Optional<Provider<MinimalExternalModuleDependency>> findDependency(String alias) {
            return owner.findDependency(alias);
        }

        @Override
        public Optional<Provider<ExternalModuleDependencyBundle>> findBundle(String bundle) {
            return owner.findBundle(bundle);
        }

        @Override
        public Optional<VersionConstraint> findVersion(String name) {
            return owner.findVersion(name);
        }

        @Override
        public Optional<Provider<PluginDependency>> findPlugin(String alias) {
            return owner.findPlugin(alias);
        }

        @Override
        public String getName() {
            return owner.getName();
        }

        @Override
        public List<String> getDependencyAliases() {
            return owner.getDependencyAliases();
        }

        @Override
        public List<String> getBundleAliases() {
            return owner.getBundleAliases();
        }

        @Override
        public List<String> getVersionAliases() {
            return owner.getVersionAliases();
        }

        @Override
        public List<String> getPluginAliases() {
            return owner.getPluginAliases();
        }
    }

    @Inject
    protected AbstractExternalDependencyFactory(DefaultVersionCatalog config,
                                                ProviderFactory providers) {
        this.config = config;
        this.providers = providers;
    }

    protected Provider<MinimalExternalModuleDependency> create(String alias) {
        return providers.of(DependencyValueSource.class,
            spec -> spec.getParameters().getDependencyData().set(config.getDependencyData(alias)))
            .forUseAtConfigurationTime();
    }

    @Override
    public final Optional<Provider<MinimalExternalModuleDependency>> findDependency(String alias) {
        String normalizedAlias = normalize(alias);
        if (config.getDependencyAliases().contains(normalizedAlias)) {
            return Optional.of(create(normalizedAlias));
        }
        return Optional.empty();
    }

    @Override
    public final Optional<Provider<ExternalModuleDependencyBundle>> findBundle(String bundle) {
        String normalizedBundle = normalize(bundle);
        if (config.getBundleAliases().contains(normalizedBundle)) {
            return Optional.of(new BundleFactory(providers, config).createBundle(normalizedBundle));
        }
        return Optional.empty();
    }

    @Override
    public final Optional<VersionConstraint> findVersion(String name) {
        String normalizedName = normalize(name);
        if (config.getVersionAliases().contains(normalizedName)) {
            return Optional.of(new VersionFactory(providers, config).findVersionConstraint(normalizedName));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Provider<PluginDependency>> findPlugin(String alias) {
        String normalizedPlugin = normalize(alias);
        if (config.getPluginAliases().contains(normalizedPlugin)) {
            return Optional.of(new PluginFactory(providers, config).createPlugin(normalizedPlugin));
        }
        return Optional.empty();
    }

    @Override
    public final String getName() {
        return config.getName();
    }

    @Override
    public List<String> getDependencyAliases() {
        return config.getDependencyAliases();
    }

    @Override
    public List<String> getBundleAliases() {
        return config.getBundleAliases();
    }

    @Override
    public List<String> getVersionAliases() {
        return config.getVersionAliases();
    }

    @Override
    public List<String> getPluginAliases() {
        return config.getPluginAliases();
    }

    public static class VersionFactory {
        protected final ProviderFactory providers;
        protected final DefaultVersionCatalog config;

        public VersionFactory(ProviderFactory providers, DefaultVersionCatalog config) {
            this.providers = providers;
            this.config = config;
        }

        /**
         * Returns a single version string from a rich version
         * constraint, assuming the user knows what they are doing.
         *
         * @param name the name of the version alias
         * @return a single version string or an empty string
         */
        protected Provider<String> getVersion(String name) {
            return providers.provider(() -> doGetVersion(name));
        }

        private String doGetVersion(String name) {
            ImmutableVersionConstraint version = findVersionConstraint(name);
            String requiredVersion = version.getRequiredVersion();
            if (!requiredVersion.isEmpty()) {
                return requiredVersion;
            }
            String strictVersion = version.getStrictVersion();
            if (!strictVersion.isEmpty()) {
                return strictVersion;
            }
            return version.getPreferredVersion();
        }

        private ImmutableVersionConstraint findVersionConstraint(String name) {
            return config.getVersion(name).getVersion();
        }
    }

    public static class BundleFactory {
        protected final ProviderFactory providers;
        protected final DefaultVersionCatalog config;

        public BundleFactory(ProviderFactory providers, DefaultVersionCatalog config) {
            this.providers = providers;
            this.config = config;
        }

        protected Provider<ExternalModuleDependencyBundle> createBundle(String name) {
            return providers.of(DependencyBundleValueSource.class,
                spec -> spec.parameters(params -> {
                    params.getConfig().set(config);
                    params.getBundleName().set(name);
                }))
                .forUseAtConfigurationTime();
        }
    }

    public static class PluginFactory {
        protected final ProviderFactory providers;
        protected final DefaultVersionCatalog config;

        public PluginFactory(ProviderFactory providers, DefaultVersionCatalog config) {
            this.providers = providers;
            this.config = config;
        }

        protected Provider<PluginDependency> createPlugin(String name) {
            return providers.of(PluginDependencyValueSource.class,
                spec -> spec.parameters(params -> {
                    params.getConfig().set(config);
                    params.getPluginName().set(name);
                }))
                .forUseAtConfigurationTime();
        }
    }
}
