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

package uk.gov.gchq.palisade.service.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.resource.Resource;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to request the {@link DataRequestConfig}.
 */
public class GetDataRequestConfig extends Request {
    private String token;
    private Resource resource;

    public String getToken() {
        requireNonNull(token, "The token has not been set.");
        return token;
    }

    public GetDataRequestConfig token(final String token) {
        requireNonNull(token, "The token cannot be set to null.");
        this.token = token;
        return this;
    }

    public void setToken(final String token) {
        token(token);
    }

    public Resource getResource() {
        requireNonNull(resource, "The resource has not been set.");
        return resource;
    }

    public GetDataRequestConfig resource(final Resource resource) {
        requireNonNull(resource, "The resource cannot be set to null.");
        this.resource = resource;
        return this;
    }

    public void setResource(final Resource resource) {
        resource(resource);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GetDataRequestConfig that = (GetDataRequestConfig) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(token, that.token)
                .append(resource, that.resource)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 37)
                .appendSuper(super.hashCode())
                .append(token)
                .append(resource)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("token", token)
                .append("resource", resource)
                .toString();
    }
}
