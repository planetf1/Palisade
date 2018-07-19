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
package uk.gov.gchq.palisade.resource;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.gchq.palisade.ToStringBuilder;

import javax.rmi.CORBA.Stub;

public class StubResource implements Resource {

    private String type;
    private String id;
    private String format;

    public StubResource() {

    }

    public StubResource(String type, String id, String format) {
        this.type = type;
        this.id = id;
        this.format = format;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StubResource stub = (StubResource) o;

        return new EqualsBuilder()
                .append(format, stub.format)
                .append(id, stub.id)
                .append(type, stub.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 23)
                .append(format)
                .append(id)
                .append(type)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("format", format)
                .append("id", id)
                .append("type", type)
                .toString();
    }
}

