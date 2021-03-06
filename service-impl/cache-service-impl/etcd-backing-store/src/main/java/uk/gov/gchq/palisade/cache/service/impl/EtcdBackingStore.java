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
package uk.gov.gchq.palisade.cache.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class EtcdBackingStore implements BackingStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdBackingStore.class);
    /**
     * Flag to indicate boolean false.
     */
    public static final ByteSequence FALSE_FLAG = ByteSequence.from(new byte[]{0});
    /**
     * Flag to indicate boolean true.
     */
    public static final ByteSequence TRUE_FLAG = ByteSequence.from(new byte[]{1});
    /**
     * Default charset
     */
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    private Collection<URI> connectionDetails;
    private Client etcdClient;
    private KV keyValueClient;
    private Lease leaseClient;

    public EtcdBackingStore() {
    }

    @Override
    public void close() {
        if (null != keyValueClient) {
            keyValueClient.close();
            keyValueClient = null;
        }
        if (null != leaseClient) {
            leaseClient.close();
            leaseClient = null;
        }
        if (null != etcdClient) {
            etcdClient.close();
            etcdClient = null;
        }
    }

    public Collection<URI> getConnectionDetails() {
        requireNonNull(connectionDetails, "The etcd connection details have not been set.");
        return connectionDetails;
    }

    public void setConnectionDetails(final Collection<URI> connectionDetails) {
        connectionDetails(connectionDetails);
    }

    public EtcdBackingStore connectionDetails(final Collection<URI> connectionDetails) {
        return connectionDetails(connectionDetails, true);
    }

    public EtcdBackingStore connectionDetails(final Collection<URI> connectionDetails, final boolean connect) {
        requireNonNull(connectionDetails, "The etcd connection details have not been set.");
        if (connectionDetails.isEmpty()) {
            throw new IllegalArgumentException("connection details must not be empty");
        }
        this.connectionDetails = connectionDetails;
        if (connect) {
            this.etcdClient = Client.builder().endpoints(connectionDetails).build();
            this.keyValueClient = etcdClient.getKVClient();
            this.leaseClient = etcdClient.getLeaseClient();
        }
        return this;
    }

    @JsonIgnore
    public void setEtcdClient(final Collection<URI> connectionDetails) {
        connectionDetails(connectionDetails);
    }

    @JsonIgnore
    public Client getEtcdClient() {
        requireNonNull(etcdClient, "No connection is open to the etcd cluster.");
        return etcdClient;
    }

    @JsonIgnore
    private KV getKeyValueClient() {
        requireNonNull(keyValueClient, "No connection is open to the etcd cluster.");
        return keyValueClient;
    }

    @JsonIgnore
    private Lease getLeaseClient() {
        requireNonNull(leaseClient, "No connection is open to the etcd cluster.");
        return leaseClient;
    }

    @Override
    public boolean add(final String key, final Class<?> valueClass, final byte[] value, final Optional<Duration> timeToLive) {
        String cacheKey = BackingStore.validateAddParameters(key, valueClass, value, timeToLive);
        long leaseID = 0;
        if (timeToLive.isPresent()) {
            long ttl = timeToLive.get().getSeconds();
            if (ttl > 0) {
                leaseID = getLeaseClient().grant(ttl).join().getID();
            } else {
                // if it has a TTL < 1 second then it is not worth inserting as etcd can not deal with a TTL < 1 second
                return true;
            }
        }
        CompletableFuture<PutResponse> response1 = getKeyValueClient().put(
                ByteSequence.from(cacheKey + ".class", UTF8),
                ByteSequence.from(valueClass.getTypeName(), UTF8),
                PutOption.newBuilder().withLeaseId(leaseID).build());
        CompletableFuture<PutResponse> response2 = getKeyValueClient().put(
                ByteSequence.from(cacheKey + ".value", UTF8),
                ByteSequence.from(value),
                PutOption.newBuilder().withLeaseId(leaseID).build());
        CompletableFuture.allOf(response1, response2).join();
        return true;
    }

    @Override
    public SimpleCacheObject get(final String key) {
        String cacheKey = BackingStore.keyCheck(key);
        CompletableFuture<GetResponse> futureValueClass = getKeyValueClient().get(ByteSequence.from(cacheKey + ".class", UTF8));
        CompletableFuture<GetResponse> futureValue = getKeyValueClient().get(ByteSequence.from(cacheKey + ".value", UTF8));
        List<KeyValue> valueClassKV = futureValueClass.join().getKvs();
        if (valueClassKV.size() == 0) {
            return new SimpleCacheObject(Object.class, Optional.empty());
        }
        try {
            return new SimpleCacheObject(Class.forName(futureValueClass.join().getKvs().get(0).getValue().toString(UTF8)), Optional.of(futureValue.join().getKvs().get(0).getValue().getBytes()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Get request failed due to the class of the value not being found.", e);
        }
    }

    @Override
    public Stream<String> list(final String prefix) {
        requireNonNull(prefix, "prefix");
        return getKeyValueClient().get(ByteSequence.from(prefix, UTF8), GetOption.newBuilder().withRange(ByteSequence.from(prefix + "~", UTF8)).withKeysOnly(true).build())
                .join()
                .getKvs()
                .stream()
                .map(keyValue -> keyValue.getKey().toString(UTF8))
                .map(key -> key.substring(0, key.lastIndexOf('.')))
                .distinct();
    }

    @Override
    public boolean remove(final String key) {
        String cacheKey = BackingStore.keyCheck(key);
        CompletableFuture<DeleteResponse> removedValue = getKeyValueClient().delete(ByteSequence.from(cacheKey + ".value", UTF8));
        CompletableFuture<DeleteResponse> removedClass = getKeyValueClient().delete(ByteSequence.from(cacheKey + ".class", UTF8));
        CompletableFuture.allOf(removedClass).join();
        return (removedValue.join().getDeleted() != 0);
    }
}
