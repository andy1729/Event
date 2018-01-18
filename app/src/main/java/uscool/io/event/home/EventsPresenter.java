package uscool.io.event.home;

import android.app.Activity;
import android.support.annotation.NonNull;

import uscool.io.event.addeditevent.AddEditEventActivity;
import uscool.io.event.data.Event;
import uscool.io.event.data.source.EventsDataSource;
import uscool.io.event.data.source.EventsRepository;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andy1729 on 18/01/18.
 *
 * Listens to user actions from the UI ({@link EventsFragment}), retrieves the data and updates the
 * UI as required.
 */
public class EventsPresenter implements EventsContract.Presenter {

    private final EventsRepository mEventsRepository;

    private final EventsContract.View mEventsView;

    private EventsFilterType mCurrentFiltering = EventsFilterType.ALL_TASKS;

    private boolean mFirstLoad = true;

    public EventsPresenter(@NonNull EventsRepository eventsRepository, @NonNull EventsContract.View eventsView) {
        mEventsRepository = checkNotNull(eventsRepository, "eventsRepository cannot be null");
        mEventsView = checkNotNull(eventsView, "eventsView cannot be null!");

        mEventsView.setPresenter(this);
    }

    @Override
    public void start() {
        loadEvents(false);
    }

    @Override
    public void result(int requestCode, int resultCode) {
        // If a event was successfully added, show snackbar
        if (AddEditEventActivity.REQUEST_ADD_TASK == requestCode && Activity.RESULT_OK == resultCode) {
            mEventsView.showSuccessfullySavedMessage();
        }
    }

    @Override
    public void loadEvents(boolean forceUpdate) {
        // Simplification for sample: a network reload will be forced on first load.
        loadEvents(forceUpdate || mFirstLoad, true);
        mFirstLoad = false;
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the {@link EventsDataSource}
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private void loadEvents(boolean forceUpdate, final boolean showLoadingUI) {
        if (showLoadingUI) {
            mEventsView.setLoadingIndicator(true);
        }
        if (forceUpdate) {
            mEventsRepository.refreshEvents();
        }


        mEventsRepository.getEvents(new EventsDataSource.LoadEventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                List<Event> eventsToShow = new ArrayList<Event>();

                // This callback may be called twice, once for the cache and once for loading
                // the data from the server API, so we check before decrementing, otherwise
                // it throws "Counter has been corrupted!" exception.


                // We filter the events based on the requestType
                for (Event event : events) {
                    switch (mCurrentFiltering) {
                        case ALL_TASKS:
                            eventsToShow.add(event);
                            break;
                        case ACTIVE_TASKS:
                            if (event.isActive()) {
                                eventsToShow.add(event);
                            }
                            break;
                        case COMPLETED_TASKS:
                            if (event.isCompleted()) {
                                eventsToShow.add(event);
                            }
                            break;
                        default:
                            eventsToShow.add(event);
                            break;
                    }
                }
                // The view may not be able to handle UI updates anymore
                if (!mEventsView.isActive()) {
                    return;
                }
                if (showLoadingUI) {
                    mEventsView.setLoadingIndicator(false);
                }

                processEvents(eventsToShow);
            }

            @Override
            public void onDataNotAvailable() {
                // The view may not be able to handle UI updates anymore
                if (!mEventsView.isActive()) {
                    return;
                }
                mEventsView.showLoadingEventsError();
            }
        });
    }

    private void processEvents(List<Event> events) {
        if (events.isEmpty()) {
            // Show a message indicating there are no events for that filter type.
            processEmptyEvents();
        } else {
            // Show the list of events
            mEventsView.showEvents(events);
            // Set the filter label's text.
            showFilterLabel();
        }
    }

    private void showFilterLabel() {
        switch (mCurrentFiltering) {
            case ACTIVE_TASKS:
                mEventsView.showActiveFilterLabel();
                break;
            case COMPLETED_TASKS:
                mEventsView.showCompletedFilterLabel();
                break;
            default:
                mEventsView.showAllFilterLabel();
                break;
        }
    }

    private void processEmptyEvents() {
        switch (mCurrentFiltering) {
            case ACTIVE_TASKS:
                mEventsView.showNoActiveEvents();
                break;
            case COMPLETED_TASKS:
                mEventsView.showNoCompletedEvents();
                break;
            default:
                mEventsView.showNoEvents();
                break;
        }
    }

    @Override
    public void addNewEvent() {
        mEventsView.showAddEvent();
    }

    @Override
    public void openEventDetails(@NonNull Event requestedEvent) {
        checkNotNull(requestedEvent, "requestedEvent cannot be null!");
        mEventsView.showEventDetailsUi(requestedEvent.getId());
    }

    @Override
    public void completeEvent(@NonNull Event completedEvent) {
        checkNotNull(completedEvent, "completedEvent cannot be null!");
        mEventsRepository.completeEvent(completedEvent);
        mEventsView.showEventMarkedComplete();
        loadEvents(false, false);
    }

    @Override
    public void activateEvent(@NonNull Event activeEvent) {
        checkNotNull(activeEvent, "activeEvent cannot be null!");
        mEventsRepository.activateEvent(activeEvent);
        mEventsView.showEventMarkedActive();
        loadEvents(false, false);
    }

    @Override
    public void clearCompletedEvents() {
        mEventsRepository.clearCompletedEvents();
        mEventsView.showCompletedEventsCleared();
        loadEvents(false, false);
    }

    /**
     * Sets the current event filtering type.
     *
     * @param requestType Can be {@link EventsFilterType#ALL_TASKS},
     *                    {@link EventsFilterType#COMPLETED_TASKS}, or
     *                    {@link EventsFilterType#ACTIVE_TASKS}
     */
    @Override
    public void setFiltering(EventsFilterType requestType) {
        mCurrentFiltering = requestType;
    }

    @Override
    public EventsFilterType getFiltering() {
        return mCurrentFiltering;
    }

}
