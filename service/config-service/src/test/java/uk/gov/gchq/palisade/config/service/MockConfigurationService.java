/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.config.service;

import org.mockito.Mockito;

import uk.gov.gchq.palisade.config.service.request.AddConfigRequest;
import uk.gov.gchq.palisade.config.service.request.GetConfigRequest;
import uk.gov.gchq.palisade.service.request.InitialConfig;

import java.util.concurrent.CompletableFuture;

public class MockConfigurationService implements InitialConfigurationService {
    private static InitialConfigurationService mock = Mockito.mock(InitialConfigurationService.class);

    public static InitialConfigurationService getMock() {
        return mock;
    }

    public static void setMock(final InitialConfigurationService mock) {
        if (null == mock) {
            MockConfigurationService.mock = Mockito.mock(InitialConfigurationService.class);
        }
        MockConfigurationService.mock = mock;
    }

    @Override
    public CompletableFuture<InitialConfig> get(final GetConfigRequest request) {
        return mock.get(request);
    }

    @Override
    public CompletableFuture<Boolean> add(final AddConfigRequest request) {
        return mock.add(request);
    }
}
