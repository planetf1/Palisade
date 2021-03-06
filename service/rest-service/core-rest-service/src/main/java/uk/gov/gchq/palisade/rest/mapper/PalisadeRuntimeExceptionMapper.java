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

package uk.gov.gchq.palisade.rest.mapper;

import uk.gov.gchq.palisade.exception.Error;
import uk.gov.gchq.palisade.exception.ErrorFactory;
import uk.gov.gchq.palisade.exception.PalisadeRuntimeException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static uk.gov.gchq.palisade.rest.ServiceConstants.PALISADE_MEDIA_TYPE;
import static uk.gov.gchq.palisade.rest.ServiceConstants.PALISADE_MEDIA_TYPE_HEADER;

/**
 * Jersey {@link javax.ws.rs.ext.ExceptionMapper} to be used to handle
 * {@link uk.gov.gchq.palisade.exception.PalisadeRuntimeException}s.
 */
@Provider
public class PalisadeRuntimeExceptionMapper implements ExceptionMapper<PalisadeRuntimeException> {
    @Override
    public Response toResponse(final PalisadeRuntimeException gre) {
        final Error error = ErrorFactory.from(gre);
        return Response.status(error.getStatusCode())
                .header(PALISADE_MEDIA_TYPE_HEADER, PALISADE_MEDIA_TYPE)
                .entity(error)
                .build();
    }
}
