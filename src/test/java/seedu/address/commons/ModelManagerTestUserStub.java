package seedu.address.commons;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.ComponentManager;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.model.AddressBookChangedEvent;
import seedu.address.commons.events.model.TimeTableChangedEvent;
import seedu.address.commons.events.security.LogoutEvent;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.User;
import seedu.address.model.UserPrefs;
import seedu.address.model.VersionedAddressBook;
import seedu.address.model.person.CombinedFriendPredicate;
import seedu.address.model.person.CombinedOtherPredicate;
import seedu.address.model.person.FriendListPredicate;
import seedu.address.model.person.OtherListPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.TimeTable;
import seedu.address.model.util.SampleDataUtil;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManagerTestUserStub extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManagerTestUserStub.class);

    private final VersionedAddressBook versionedAddressBook;
    private final FilteredList<Person> filteredPersons;
    private final FilteredList<Person> friendList;
    private final FilteredList<Person> otherList;
    private final TimeTable timeTable;
    private ObservableList<Person> list;
    private User user;
    private Person person;

    /**
     * Initializes a ModelManager with the given addressBook, userPrefs, timeTable.
     */
    public ModelManagerTestUserStub(ReadOnlyAddressBook addressBook, UserPrefs userPrefs) {
        super();
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        versionedAddressBook = new VersionedAddressBook(addressBook);
        friendList = new FilteredList<>(versionedAddressBook.getPersonList());
        otherList = new FilteredList<>(versionedAddressBook.getPersonList());
        filteredPersons = new FilteredList<>(versionedAddressBook.getPersonList());
        timeTable = new TimeTable();
        person = SampleDataUtil.getSamplePerson();
        user = new User(person.getData());
    }

    public ModelManagerTestUserStub() {
        this(new AddressBook(), new UserPrefs());
    }

    @Override
    public void resetData(ReadOnlyAddressBook newData) {
        versionedAddressBook.resetData(newData);
        indicateAddressBookChanged();
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return versionedAddressBook;
    }

    @Override
    public TimeTable getTimeTable() {
        return timeTable;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateAddressBookChanged() {
        //TODO Check whether this actually works when modifying your own data.
        if (user != null) {
            matchUserToPerson(user.getName().toString());
        }
        raise(new AddressBookChangedEvent(versionedAddressBook));
    }

    /** Raises an event to indicate the timetable has changed */
    private void indicateTimeTableChanged() {
        raise(new TimeTableChangedEvent(timeTable));
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return versionedAddressBook.hasPerson(person);
    }

    @Override
    public boolean hasPersonToRegister(Person person) {
        requireNonNull(person);
        return versionedAddressBook.hasPersonToRegister(person);
    }

    @Override
    public void deletePerson(Person target) {
        versionedAddressBook.removePerson(target);
        indicateAddressBookChanged();
    }

    @Override
    public void addPerson(Person person) {
        versionedAddressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        indicateAddressBookChanged();
    }

    @Override
    public void updatePerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);
        versionedAddressBook.updatePerson(target, editedPerson);
        indicateAddressBookChanged();
    }

    @Override
    public void updateTimeTable(TimeTable timeTable) {
        requireNonNull(timeTable);

        this.timeTable.updateTimeTable(timeTable);
        indicateTimeTableChanged();
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return FXCollections.unmodifiableObservableList(filteredPersons);
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public void updateFriendList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        friendList.setPredicate(combinedFriendPredicate(predicate, friendsPredicateFromPerson(user)));
    }

    @Override
    public void updateOtherList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        otherList.setPredicate(combinedOtherPredicate(predicate, othersPredicateFromPerson(user)));
    }

    @Override
    public ObservableList<Person> getMeList() {
        filteredPersons.setPredicate(p -> p.getName().equals(user.getName()));
        return FXCollections.unmodifiableObservableList(filteredPersons);
    }

    @Override
    public ObservableList<Person> getOtherList() {
        otherList.setPredicate(othersPredicateFromPerson(user));
        return FXCollections.unmodifiableObservableList(otherList);
    }

    public ObservableList<Person> getOtherList(Person person) {
        requireNonNull(person);
        otherList.setPredicate(othersPredicateFromPerson(person));
        return FXCollections.unmodifiableObservableList(otherList);
    }

    @Override
    public ObservableList<Person> getCurrentOtherList() {
        return FXCollections.unmodifiableObservableList(otherList);
    }

    @Override
    public ObservableList<Person> getCurrentFriendList() {
        return FXCollections.unmodifiableObservableList(friendList);
    }

    @Override
    public ObservableList<Person> getFriendList() {
        friendList.setPredicate(friendsPredicateFromPerson(user));
        return FXCollections.unmodifiableObservableList(friendList);
    }


    //=========== Undo/Redo =================================================================================

    @Override
    public boolean canUndoAddressBook() {
        return versionedAddressBook.canUndo();
    }

    @Override
    public boolean canRedoAddressBook() {
        return versionedAddressBook.canRedo();
    }

    @Override
    public void undoAddressBook() {
        versionedAddressBook.undo();
        indicateAddressBookChanged();

        // TODO: Implement after user comes online
        // indicateTimeTableChanged(user.getTimeTable());
    }

    @Override
    public void redoAddressBook() {
        versionedAddressBook.redo();
        indicateAddressBookChanged();

        // TODO: Implement after user comes online
        // indicateTimeTableChanged(user.getTimeTable());
    }

    @Override
    public void commitAddressBook() {
        versionedAddressBook.commit();
    }

    @Override
    public boolean equals(Object obj) {
        // short circuit if same object
        if (obj == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(obj instanceof ModelManagerTestUserStub)) {
            return false;
        }

        // state check
        ModelManagerTestUserStub other = (ModelManagerTestUserStub) obj;
        return versionedAddressBook.equals(other.versionedAddressBook)
                && filteredPersons.equals(other.filteredPersons)
                && timeTable.equals(other.timeTable);
    }

    public FriendListPredicate friendsPredicateFromPerson(Person person) {
        return new FriendListPredicate(person);
    }

    public OtherListPredicate othersPredicateFromPerson(Person person) {
        return new OtherListPredicate(person);
    }

    /**
     * Combines the predicates to allow SetPredicate to be called
     * @param predicate
     * @param friendListPredicate
     * @return
     */
    public CombinedFriendPredicate combinedFriendPredicate(Predicate<Person> predicate,
                                                           FriendListPredicate friendListPredicate) {
        return new CombinedFriendPredicate(predicate, friendListPredicate);
    }

    public CombinedOtherPredicate combinedOtherPredicate(Predicate<Person> predicate,
                                                         OtherListPredicate otherListPredicate) {
        return new CombinedOtherPredicate(predicate, otherListPredicate);
    }

    @Override
    public void matchUserToPerson(String name) {
        list = versionedAddressBook.getPersonList();
        //Loops through personlist to get matched name Person Class
        for (Person person : list) {
            if (name.equals(person.getName().toString())) {
                this.user = new User(person.getData());
            }
        }
    }

    @Override
    public void clearUser() {
        this.user = null;
    }

    @Override
    public User getUser() {
        //TODO Can you do this? Must you create a new object to be returned instead?
        return this.user;
    }

    @Override
    public void commandLogout() {
        raise(new LogoutEvent());
    }
}
