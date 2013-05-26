package org.netbeans.gradle.project.api.property;

import org.netbeans.gradle.project.api.event.NbListenerRef;

/**
 * Defines the value of an arbitrary property. The value of this property might
 * change in an implementation dependent way. Some property might be changed by
 * client code, some might change due to external (and uncontrollable) events.
 * <P>
 * For example, the value of the property can be derived from the content of a
 * file and might get updated after the content of that file changes.
 * <P>
 * Note that this interface defines the same methods as the
 * {@code org.jtrim.property.PropertySource} (of the <I>JTrim</I> library)
 * interface with exactly the same contract. This is so, that later
 * (in NetBeans 7.4) this interface can extend from the interface of <I>JTrim</I>.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> and may be called from any context.
 *
 * @param <ValueType> the type of the value of the property
 */
public interface NbPropertySource<ValueType> {
    /**
     * Returns the current value of this property. Implementations of this
     * method must not do any expensive computations or otherwise block. That
     * is, this method might be called from threads where responsiveness is
     * necessary, such as the <I>AWT Event Dispatch Thread</I>.
     * <P>
     * Implementation note:
     * Implementations are recommended to only return the value of a
     * {@code volatile} field (and possibly make defensive copy of the value).
     * If the property is read from some other sources then a separate thread
     * should read the value and update the volatile field.
     *
     * @return the current value of this property. This method may return
     *   {@code null} if the implementation allows {@code null} values for a
     *   property.
     */
    public ValueType getValue();

    /**
     * Registers a listener to be notified after the value of this property
     * changes. In what context the listener is called is implementation
     * dependent.
     * <P>
     * Once a listener is notified, it needs to get the current value of this
     * property by calling the {@link #getValue() getValue()} method. Note that,
     * it is allowed for implementations to notify the listener even if the
     * property does not change. Also, implementations may merge listener
     * notifications. That is, if a value is changed multiple times before it is
     * notified, implementations may decide to only notify the listener once.
     *
     * @param listener the listener whose {@code run()} method is to be called
     *   whenever the value of this property changes. This argument cannot be
     *   {@code null}.
     * @return the {@code NbListenerRef} which can be used to unregister the
     *   currently added listener, so that it will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     */
    public NbListenerRef addChangeListener(Runnable listener);
}