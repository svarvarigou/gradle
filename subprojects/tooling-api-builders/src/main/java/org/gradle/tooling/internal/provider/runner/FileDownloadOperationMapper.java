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

package org.gradle.tooling.internal.provider.runner;

import org.gradle.internal.UncheckedException;
import org.gradle.internal.build.event.BuildEventSubscriptions;
import org.gradle.internal.build.event.types.DefaultFileDownloadDescriptor;
import org.gradle.internal.build.event.types.DefaultOperationFinishedProgressEvent;
import org.gradle.internal.build.event.types.DefaultOperationStartedProgressEvent;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationStartEvent;
import org.gradle.internal.resource.ExternalResourceReadBuildOperationType;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.internal.protocol.events.InternalOperationFinishedProgressEvent;
import org.gradle.tooling.internal.protocol.events.InternalOperationStartedProgressEvent;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

public class FileDownloadOperationMapper implements BuildOperationMapper<ExternalResourceReadBuildOperationType.Details, DefaultFileDownloadDescriptor> {
    @Override
    public boolean isEnabled(BuildEventSubscriptions subscriptions) {
        return subscriptions.isRequested(OperationType.FILE_DOWNLOAD);
    }

    @Override
    public Class<ExternalResourceReadBuildOperationType.Details> getDetailsType() {
        return ExternalResourceReadBuildOperationType.Details.class;
    }

    @Override
    public DefaultFileDownloadDescriptor createDescriptor(ExternalResourceReadBuildOperationType.Details details, BuildOperationDescriptor buildOperation, @Nullable OperationIdentifier parent) {
        try {
            return new DefaultFileDownloadDescriptor(buildOperation.getId(), buildOperation.getName(), buildOperation.getDisplayName(), parent, new URI(details.getLocation()));
        } catch (URISyntaxException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    @Override
    public InternalOperationStartedProgressEvent createStartedEvent(DefaultFileDownloadDescriptor descriptor, ExternalResourceReadBuildOperationType.Details details, OperationStartEvent startEvent) {
        return new DefaultOperationStartedProgressEvent(startEvent.getStartTime(), descriptor);
    }

    @Override
    public InternalOperationFinishedProgressEvent createFinishedEvent(DefaultFileDownloadDescriptor descriptor, ExternalResourceReadBuildOperationType.Details details, OperationFinishEvent finishEvent) {
        return new DefaultOperationFinishedProgressEvent(finishEvent.getEndTime(), descriptor, ClientForwardingBuildOperationListener.toOperationResult(finishEvent));
    }
}
