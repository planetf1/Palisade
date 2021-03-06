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

package uk.gov.gchq.palisade.redirect.service;

import uk.gov.gchq.palisade.service.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;


/**
 * A marshall co-ordinates the redirection of requests to a Palisade {@link Service} using a {@link Redirector} that
 * encapsulates the business logic of how to redirect certain requests.
 * <p>
 * The {@link RedirectionMarshall#host(String)} method call is optional, but if called before calling a proxy method,
 * the host string will be passed to the {@code redirector} object. Some {@link Redirector} implementations may use
 * the originating host argument aid them in their decision. If the host is not set, then {@code null} is passed as the
 * host to the redirector object. <strong>The host is reset after each call to a {@code redirect} method overload.</strong>
 *
 * <b>Sample Usage:</b> Assuming a {@linkplain Service} named {@code MyService} exists as defined below, we set up a simple
 * redirector and show the use of the proxy.
 *
 * <pre>{@code
 * interface MyService extends Service {
 *
 *     void someVoidMethod(int a);
 *
 *     int someIntMethod(String a);
 * }}</pre>
 * <p>
 * Assuming some instances of {@code MyService} are running, then a redirection request may look like this:
 * <pre>{@code
 * class Client {
 *
 *     private final RedirectionMarshall<String> marshall = new RedirectionMarshall<>(...);
 *
 *     private final MyService fake = marshall.createProxyFor(MyService.class);
 *
 *     public void someMethod() {
 *          //we wish to work out where to redirect a method call to someIntMethod:
 *          StringRedirectionResult result = marshall.redirect(fake.someIntMethod("some string"));
 *
 *          //the result now contains the destination computed by the redirector instance
 *          String destination = result.get();
 *
 *          ...
 *
 *          //optionally, we can set a host that originated the request for the next request, this will be passed
 *          //to the redirector object to help it make a decision
 *          marshall.host("localhost");
 *
 *          //to redirect a void method, we use a slightly different syntax:
 *          StringRedirectionResult result = marshall.redirect(() -> fake.someVoidMethod(5));
 *          ...
 *     }
 * }}</pre>
 *
 * @param <T> the return type of the redirector
 */
public class RedirectionMarshall<T> {

    /**
     * Method reference for hashCode.
     */
    private static final Method HASH_CODE_OBJECT;

    /**
     * Method reference for equals.
     */
    private static final Method EQUALS_OBJECT;

    static {
        Method hashCode = null;
        Method equals = null;
        try {
            hashCode = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
        }
        try {
            equals = Object.class.getMethod("equals", Object.class);
        } catch (NoSuchMethodException e) {

        }
        HASH_CODE_OBJECT = hashCode;
        EQUALS_OBJECT = equals;
    }

    /**
     * The object that contains the logic on how to redirect requests.
     */
    private final Redirector<T> redirector;

    /**
     * Stores the redirection result of the latest call to a proxy method. As multiple requests may happen, we use
     * a {@link ThreadLocal} to protect against clashes.
     */
    private final ThreadLocal<RedirectionResult<T>> recentRedirect = new ThreadLocal<>();

    /**
     * Stores the hostname/address for the client making the call. This may be optionally set and can be used as a hint
     * by redirectors when routing a request.
     */
    private final ThreadLocal<String> recentHost = new ThreadLocal<>();

    /**
     * Create a marshall.
     *
     * @param redirector the redirector for requests on this marshall
     */
    public RedirectionMarshall(final Redirector<T> redirector) {
        requireNonNull(redirector, "redirector");
        this.redirector = redirector;
    }

    /**
     * Get the {@link Redirector} for this instance.
     *
     * @return the redirector
     */
    public Redirector<?> getRedirector() {
        return redirector;
    }

    /**
     * Compute where to redirect the contained method call. This method should be used as directed in the class API usage
     * above. The {@code} call parameter should be the method call to a proxy generated by this instance using {@link RedirectionMarshall#createProxyFor(Class)}.
     *
     * @param call the method call to redirect
     * @param <S>  ignored return type of {@code call}
     * @return the result value retrieved by the redirector
     * @throws IllegalStateException if no redirection result could be retrieved, may happen if a call to the proxy
     *                               was not made before calling this method
     * @see RedirectionMarshall#createProxyFor(Class)
     */
    public <S> T redirect(final S call) {
        //result is ignored
        //we should be able to retrieve the redirection result
        try {
            RedirectionResult<T> result = recentRedirect.get();
            if (isNull(result)) {
                throw new IllegalStateException("no redirection result is present, was a valid method call made via the object returned from createProxyFor() from this instance?");
            }
            return result.get();
        } finally {
            recentRedirect.remove();
            recentHost.remove();
        }
    }

    /**
     * Tests if a redirection has occurred but the result not retrieved yet. This can happen if a redirectory proxy
     * method has been called, but no {@code redirect} method overload in this class has been called yet. This is a perfectly
     * valid state for the system to be in. This method allows for the testing of that condition.
     *
     * @return true if a proxy method has been called, but the result not yet retrieved
     */
    public boolean isRedirectPending() {
        return nonNull(recentRedirect.get());
    }

    /**
     * Compute where to redirect the contained method call. This method should be used as directed in the class API usage
     * above. The {@code} call parameter should be the method call to a proxy generated by this instance using {@link RedirectionMarshall#createProxyFor(Class)}.
     * <p>
     * This overload is provided to allow void methods to be redirected. It can be used by any method call however.
     *
     * @param call the call to redirect
     * @return the result value retrieved by the redirector
     * @throws IllegalStateException if no redirection result could be retrieved, may happen if a call to the proxy
     *                               was not made before calling this method
     * @see RedirectionMarshall#createProxyFor(Class)
     */
    public T redirect(final Runnable call) {
        call.run(); //ensure void method proxy is run
        return redirect((Object) null);
    }

    /**
     * Set the originating host for the incoming request. This is an optional operation and can be used by redirectors
     * as a hint for redirecting. The host field is cleared when {@link RedirectionMarshall#redirect(Object)} (or a variant)
     * is called.
     *
     * @param host the hostname/address that originated the request
     * @return this object
     * @throws IllegalArgumentException if {@code host} is {@code null} or empty
     */
    public RedirectionMarshall<T> host(final String host) {
        requireNonNull(host, "host");
        if (host.trim().isEmpty()) {
            throw new IllegalArgumentException("host cannot be empty");
        }
        recentHost.set(host);
        return this;
    }

    /**
     * The {@link java.lang.reflect.InvocationHandler} for all proxy instances. This method provides the "glue" between
     * the proxy and the retrieval of the redirection. This method calls the redirector object and passes in the method
     * and parameters to be redirected. It then stores the result in a thread local object for later retrieval.
     *
     * @param proxy  the proxy instance that intercepted this request, specified by {@link java.lang.reflect.InvocationHandler}
     * @param method the actual method to be redirected
     * @param args   the method arguments
     * @return a dummy object
     * @throws Throwable to comply with interface
     */
    private Object delegateRedirection(final Object proxy, final Method method, final Object... args) throws Throwable {
        if (method.equals(HASH_CODE_OBJECT)) {
            return this.hashCode();
        } else if (method.equals(EQUALS_OBJECT) && args.length > 0) {
            return proxy == args[0];
        }
        //work out where to send this request
        RedirectionResult<T> result = redirector.redirectionFor(recentHost.get(), method, args);
        //stash this result
        recentRedirect.set(result);
        //Don't care about the actual method result
        return RedirectionUtils.safeReturnTypeFor(method);
    }

    /**
     * Creates a redirection proxy for the given {@link Service} class. This should be called as shown in the class API usage
     * above. The returned object is used by clients to "call" methods with the same parameters the real {@code Service} method
     * should be called with so the redirector can perform its function.
     *
     * @param redirectClass the {@code Service} class to be redirected
     * @param <S>           the type of service
     * @return the proxy
     * @throws IllegalArgumentException if {@code redirectClass} is not an instance of {@link Service}, or it cannot be proxied,
     *                                  e.g. it is a class not an interface
     */
    @SuppressWarnings("unchecked")
    public <S extends Service> S createProxyFor(final Class<S> redirectClass) {
        requireNonNull(redirectClass, "redirectClass");
        if (!Service.class.isAssignableFrom(redirectClass)) {
            throw new IllegalArgumentException("class does not implement Service interface");
        }
        return (S) Proxy.newProxyInstance(redirectClass.getClassLoader(), new Class[]{redirectClass}, this::delegateRedirection);
    }
}
