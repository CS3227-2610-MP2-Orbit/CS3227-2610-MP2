package seedu.eventmanager.service;

import java.util.function.Supplier;

public interface TransactionManager {
    <T> T execute(Supplier<T> work);
}
