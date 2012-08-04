package org.netbeans.gradle.project.query;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

public final class CachedAsyncNoArgQuery<ResultType> implements AsyncNoArgQuery<ResultType> {
    // This executor is required to execute tasks in the order they were submitted.
    private final Executor queryExecutor;
    private final Callable<ResultType> syncQuery;
    private final NotificationRegistry changeRegistry;
    private final AtomicReference<ChangeListener> changeListenerRef;

    private final ChangeSupport changes;
    private final Runnable fireChangeEventTask;

    private final AtomicReference<QueryResult<ResultType>> lastResult;
    private final AtomicBoolean queryInProgress;

    private final AtomicBoolean closed;

    public CachedAsyncNoArgQuery(
            Executor queryExecutor,
            ResultType initialResult,
            Callable<ResultType> syncQuery,
            NotificationRegistry changeRegistry) {
        if (queryExecutor == null) throw new NullPointerException("queryExecutor");
        if (syncQuery == null) throw new NullPointerException("query");
        if (changeRegistry == null) throw new NullPointerException("changeRegistry");

        this.queryExecutor = queryExecutor;
        this.syncQuery = syncQuery;
        this.changeRegistry = changeRegistry;
        this.changeListenerRef = new AtomicReference<ChangeListener>(null);

        this.changes = new ChangeSupport(this);
        this.fireChangeEventTask = new Runnable() {
            @Override
            public void run() {
                changes.fireChange();
            }
        };

        this.lastResult = new AtomicReference<QueryResult<ResultType>>(
                new QueryResult<ResultType>(initialResult));
        this.queryInProgress = new AtomicBoolean(false);

        this.closed = new AtomicBoolean(true);
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private void fireChangeEvent() {
        SwingUtilities.invokeLater(fireChangeEventTask);
    }

    private void doSubmitQuery() {
        queryExecutor.execute(new Runnable() {
            @Override
            public void run() {
                queryInProgress.set(false);
                QueryResult<ResultType> queryResult;
                try {
                    ResultType result = syncQuery.call();
                    queryResult = new QueryResult<ResultType>(result);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    queryResult = new QueryResult<ResultType>(ex);
                } catch (Exception ex) {
                    queryResult = new QueryResult<ResultType>(ex);
                }

                lastResult.set(queryResult);
                fireChangeEvent();
            }
        });
    }

    private void submitQuery() {
        if (queryInProgress.compareAndSet(false, true)) {
            // invokeLater is not required for correctness but only here to
            // possibly improve performance when submitQuery is invoked from
            // the EDT. In this case if subsequent events on the EDT invoke
            // submitQuery (indirectly), then the queries will be merged
            // avoiding multiple unnecessary queries.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    doSubmitQuery();
                }
            });
        }
    }

    private void needRefresh() {
        if (!closed.get()) {
            // Otherwise we will execute the query in the open() method.
            submitQuery();
        }
    }

    @Override
    public ResultType query() throws QueryException {
        return lastResult.get().get();
    }

    @Override
    public void open() {
        if (closed.compareAndSet(true, false)) {
            ChangeListener newListener = new QueryChangeListener();
            ChangeListener oldListener = changeListenerRef.getAndSet(newListener);
            if (oldListener != null) {
                changeRegistry.removeChangeListener(oldListener);
            }

            changeRegistry.addChangeListener(newListener);
            if (closed.get()) {
                close();
            }

            submitQuery();
        }
    }

    @Override
    public void close() {
        closed.set(true);

        ChangeListener listener = changeListenerRef.getAndSet(null);
        if (listener != null) {
            changeRegistry.removeChangeListener(listener);
        }
    }

    private class QueryChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            needRefresh();
        }
    }

    private static class QueryResult<ResultType> {
        private final ResultType result;
        private final Exception exception;

        public QueryResult(ResultType result) {
            this.result = result;
            this.exception = null;
        }

        public QueryResult(Exception exception) {
            this.result = null;
            this.exception = exception;
        }

        public ResultType get() throws QueryException {
            if (exception != null) {
                throw new QueryException(exception.getMessage(), exception);
            }
            return result;
        }
    }
}
