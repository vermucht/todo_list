package todolist.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import todolist.model.Item;
import todolist.model.TaskBean;

import java.util.List;
import java.util.function.Function;

/**
 * Database storage api using Hibernate.
 *
 * @author Aleksei Sapozhnikov (vermucht@gmail.com)
 * @version 0.1
 * @since 0.1
 */
public class ItemDatabaseStorage implements ItemStorage {

    /**
     * Instance variable.
     */
    private static volatile ItemStorage instance;

    /**
     * Double-checked locking getter.
     *
     * @return Singleton instance.
     */
    public static ItemStorage getInstance() {
        if (instance == null) {
            synchronized (ItemDatabaseStorage.class) {
                if (instance == null) {
                    instance = new ItemDatabaseStorage();
                }
            }
        }
        return instance;
    }

    /**
     * Logger.
     */
    @SuppressWarnings("unused")
    static final Logger LOG = LogManager.getLogger(ItemDatabaseStorage.class);

    /**
     * Hibernate session factory to store items.
     */
    private final SessionFactory factory;

    /**
     * Constructor.
     */
    private ItemDatabaseStorage() {
        this.factory = new Configuration().configure().buildSessionFactory();
    }

    /**
     * Finds item by id and return Item object.
     *
     * @param task Item object with search information (id).
     * @return Item with given id from database.
     */
    @Override
    public TaskBean get(TaskBean task) {
        return this.performTransaction(
                session -> session.get(Item.class, task.getId()));
    }

    /**
     * If id of given item found in database, updates item.
     * Otherwise adds given item to database with id given by database.
     *
     * @param task Item to store or to use as update.
     * @return Item object stored in database, with id given by database.
     */
    @Override
    public TaskBean merge(TaskBean task) {
        return this.performTransaction(
                session -> (TaskBean) session.merge(task));
    }

    /**
     * Returns all items stored in database.
     *
     * @return List of Item objects.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<TaskBean> getAll() {
        return this.performTransaction(
                session -> session.createQuery("from Item").list());
    }

    /**
     * Performs transaction: creates session, performs given operations, commits and closes.
     *
     * @param operations Function: operations to perform.
     * @param <T>        Result type.
     * @return Operation result.
     */
    private <T> T performTransaction(final Function<Session, T> operations) {
        T result;
        try (final var session = this.factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                result = operations.apply(session);
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw e;
            }
        }
        return result;
    }

    /**
     * Closes resources.
     */
    @Override
    public void close() {
        this.factory.close();
    }
}
