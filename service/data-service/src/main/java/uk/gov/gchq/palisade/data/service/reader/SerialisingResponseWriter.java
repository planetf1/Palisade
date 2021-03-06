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

package uk.gov.gchq.palisade.data.service.reader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Util;
import uk.gov.gchq.palisade.audit.service.AuditService;
import uk.gov.gchq.palisade.audit.service.request.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.data.service.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.data.service.reader.request.ResponseWriter;
import uk.gov.gchq.palisade.rule.Rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * The response writer for the {@link SerialisedDataReader} which will apply the record level rules for Palisade.
 */
public class SerialisingResponseWriter implements ResponseWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialisingResponseWriter.class);
    /**
     * The underlying data stream from the underlying data store.
     */
    private final InputStream stream;

    /**
     * Atomic flag to prevent double reading of the data.
     */
    private final AtomicBoolean written = new AtomicBoolean(false);
    /**
     * The serialiser for processing the input stream.
     */
    private final Serialiser<Object> serialiser;
    /**
     * The user data request.
     */
    private final DataReaderRequest request;
    /**
     * an audit service to send logs too once the steam has been read
     */
    private final AuditService auditService;
    /**
     * Atomic counter to know the number of records that have been processed
     */
    private final AtomicLong recordsProcessed = new AtomicLong(0);
    /**
     * Atomic counter to know the number of records that have been returned
     */
    private final AtomicLong recordsReturned = new AtomicLong(0);

    /**
     * Create a serialising response writer instance.
     *
     * @param stream     the underlying data stream
     * @param serialiser the serialiser for the request
     * @param request    the context for the request
     * @param auditService the audit service to send audit logs too
     */
    public SerialisingResponseWriter(final InputStream stream, final Serialiser<?> serialiser, final DataReaderRequest request, final AuditService auditService) {
        requireNonNull(stream, "stream");
        requireNonNull(serialiser, "serialiser");
        requireNonNull(request, "request");
        requireNonNull(auditService, "auditService");
        this.stream = stream;
        this.serialiser = (Serialiser<Object>) serialiser;
        this.request = request;
        this.auditService = auditService;
    }

    @Override
    public ResponseWriter write(final OutputStream output) throws IOException {
        requireNonNull(output, "output");

        //atomically get the previous value and set it to true
        boolean previousValue = written.getAndSet(true);

        if (previousValue) {
            throw new IOException("response already written");
        }

        final Rules rules = request.getRules();

        //if nothing to do, then just copy the bytes across
        try {
            if (isNull(rules) || isNull(rules.getRules()) || rules.getRules().isEmpty()) {
                LOGGER.debug("No rules to apply");
                IOUtils.copy(stream, output);
            } else {
                LOGGER.debug("Applying rules: {}", rules);
                //create stream of filtered objects
                final Stream<Object> deserialisedData = Util.applyRulesToStream(
                        serialiser.deserialise(stream),
                        request.getUser(),
                        request.getContext(),
                        rules,
                        recordsProcessed,
                        recordsReturned
                );
                //write this stream to the output
                serialiser.serialise(deserialisedData, output);
            }
            return this;
        } finally {
            this.close();
        }
    }

    @Override
    public void close() {
        // Audit log the number of results returned
        ReadRequestCompleteAuditRequest auditRequest = new ReadRequestCompleteAuditRequest()
                .user(request.getUser())
                .context(request.getContext())
                .rulesApplied(request.getRules())
                .resource(request.getResource())
                .numberOfRecordsProcessed(recordsProcessed.get())
                .numberOfRecordsReturned(recordsReturned.get());
        auditRequest.originalRequestId(request.getOriginalRequestId());
        auditService.audit(auditRequest);
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SerialisingResponseWriter that = (SerialisingResponseWriter) o;

        return new EqualsBuilder()
                .append(stream, that.stream)
                .append(written, that.written)
                .append(serialiser, that.serialiser)
                .append(request, that.request)
                .append(auditService, that.auditService)
                .append(recordsProcessed, that.recordsProcessed)
                .append(recordsReturned, that.recordsReturned)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("stream", stream)
                .append("written", written)
                .append("serialiser", serialiser)
                .append("request", request)
                .append("auditService", auditService)
                .append("recordsProcessed", recordsProcessed)
                .append("recordsReturned", recordsReturned)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 37)
                .append(stream)
                .append(written)
                .append(serialiser)
                .append(request)
                .append(auditService)
                .append(recordsProcessed)
                .append(recordsReturned)
                .toHashCode();
    }
}
