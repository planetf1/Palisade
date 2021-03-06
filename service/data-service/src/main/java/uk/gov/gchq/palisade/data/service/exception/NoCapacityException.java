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
package uk.gov.gchq.palisade.data.service.exception;

import uk.gov.gchq.palisade.exception.RequestFailedException;

/**
 * An exception thrown when a request is made to a {@link uk.gov.gchq.palisade.data.service.DataService} via a
 * {@link uk.gov.gchq.palisade.data.service.request.ReadRequest} or to a {@link uk.gov.gchq.palisade.data.service.reader.DataReader}
 * via a {@link uk.gov.gchq.palisade.data.service.reader.request.DataReaderRequest} for a resource that cannot currently
 * be served due to a lack of capacity to serve the request.
 */
public class NoCapacityException extends RequestFailedException {

    public NoCapacityException(final String e) {
        super(e);
    }

    public NoCapacityException(final Throwable cause) {
        super(cause);
    }

    public NoCapacityException(final String e, final Throwable cause) {
        super(e, cause);
    }
}
