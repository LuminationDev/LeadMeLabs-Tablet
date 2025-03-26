package com.lumination.leadmelabs.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Notification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationViewModel extends ViewModel {
    private final int itemsPerPage = 5;
    private final MutableLiveData<Integer> pages = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(1);
    private final MutableLiveData<String> currentSearch = new MutableLiveData<>("");
    private final MutableLiveData<CopyOnWriteArrayList<Notification>> allNotifications = new MutableLiveData<>(new CopyOnWriteArrayList<>());
    private final MutableLiveData<CopyOnWriteArrayList<Notification>> viewableNotifications = new MutableLiveData<>();
    private final CopyOnWriteArrayList<Notification> filteredNotifications = new CopyOnWriteArrayList<>();

    public LiveData<Integer> getCurrentPage() {
        return this.currentPage;
    }

    public void setCurrentPage(int value) {
        this.currentPage.setValue(value);
    }

    public void setPages(int value) {
        this.pages.setValue(value);
    }

    public void setCurrentSearch(String value) {
        this.currentSearch.setValue(value);
    }

    /**
     * Clear the current list of notifications to avoid any doubles when reloading the information.
     */
    public void clearNotifications() {
        CopyOnWriteArrayList<Notification> none = new CopyOnWriteArrayList<>();
        this.allNotifications.setValue(none);
    }

    /**
     * Adds a new notification to the existing list of notifications.
     * If the current list is null, initializes a new ArrayList.
     *
     * @param newNotification Notification object to add to the list.
     */
    public void addNotification(Notification newNotification) {
        CopyOnWriteArrayList<Notification> currentNotifications = allNotifications.getValue();

        if (currentNotifications == null) {
            currentNotifications = new CopyOnWriteArrayList<>();
        }

        currentNotifications.add(newNotification);

        // Sort the notifications by _timeStamp before setting the value
        currentNotifications.sort(new Comparator<Notification>() {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

            @Override
            public int compare(Notification n1, Notification n2) {
                try {
                    Date date1 = dateFormat.parse(n1.get_timeStamp());
                    Date date2 = dateFormat.parse(n2.get_timeStamp());
                    if (date2 != null) {
                        return date2.compareTo(date1); // Reverse order
                    }
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid date format", e);
                }
                return 0;
            }
        });

        allNotifications.setValue(currentNotifications);
        loadObjects();
    }

    /**
     * Updates the status of a notification that matches the given title and timestamp. Searches
     * through the current list of notifications for a notification that matches the specified title
     * and timestamp. If a matching notification is found, its status is updated to the provided status,
     * before updating the allNotifications list.
     *
     * @param title The title of the notification to be updated.
     * @param timeStamp The timestamp of the notification to be updated.
     * @param status The new status to be set for the matching notification.
     */
    public void updateNotification(String title, String timeStamp, String status) {
        CopyOnWriteArrayList<Notification> currentNotifications = allNotifications.getValue();

        if (currentNotifications != null) {
            for (Notification notification : currentNotifications) {
                if (notification.getTitle().equals(title) && notification.get_timeStamp().equals(timeStamp)) {
                    notification.setStatus(status);
                    allNotifications.setValue(currentNotifications);
                    return;
                }
            }
        }
    }

    public LiveData<CopyOnWriteArrayList<Notification>> getViewableNotifications() {
        return viewableNotifications;
    }

    public LiveData<CopyOnWriteArrayList<Notification>> getObjects() {
        loadObjects();
        return viewableNotifications;
    }

    /**
     * Retrieves the total number of pages based on the number of notifications and items per page.
     *
     * @return a LiveData object containing the total number of pages.
     *         If there are no notifications, returns a LiveData with value 1.
     */
    public LiveData<Integer> getPages() {
        List<Notification> allObjects = allNotifications.getValue();
        if (allObjects == null) return new MutableLiveData<>(1);

        int totalObjects = allObjects.size();
        this.pages.setValue((int) Math.ceil((double) totalObjects / itemsPerPage));

        return this.pages;
    }

    /**
     * Loads the current page of notifications into the viewableNotifications LiveData.
     * Calculates the indices for pagination and updates the viewableNotifications.
     */
    private void loadObjects() {
        CopyOnWriteArrayList<Notification> allObjects = allNotifications.getValue();
        if (allObjects == null) return;
        if (currentPage.getValue() == null) return;

        // Use the filtered list if it is not empty, otherwise use the original list
        List<Notification> objectsToLoad = currentSearch.getValue() != null &&
                currentSearch.getValue().isEmpty() ? allObjects : filteredNotifications;

        //Update the number of pages
        int totalObjects = objectsToLoad.size();
        this.pages.setValue((int) Math.ceil((double) totalObjects / itemsPerPage));

        // Calculate pagination indices
        int startIndex = (currentPage.getValue()-1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, objectsToLoad.size());

        CopyOnWriteArrayList<Notification> visibleObjects = new CopyOnWriteArrayList<>(objectsToLoad.subList(startIndex, endIndex));
        viewableNotifications.setValue(visibleObjects);
    }

    /**
     * Increments the current page value and loads the next page of notifications.
     * If the currentPage is null, the function does nothing.
     */
    public void loadNextPage() {
        if (currentPage.getValue() == null) return;

        int current = currentPage.getValue();
        current++;
        currentPage.setValue(current);
        loadObjects();
    }

    /**
     * Decrements the current page value and loads the previous page of notifications.
     * If the currentPage is null or already at the first page, the function does nothing.
     */
    public void loadPreviousPage() {
        if (currentPage.getValue() == null) return;

        int current = currentPage.getValue();
        if (current > 1) {
            current--;
            currentPage.setValue(current);
            loadObjects();
        }
    }

    /**
     * Filters the notifications based on the provided query and reloads the viewable notifications.
     *
     * @param query the search query to filter notifications.
     */
    public void searchNotifications(String query) {
        CopyOnWriteArrayList<Notification> allObjects = allNotifications.getValue();
        if (allObjects == null) return;
        setCurrentSearch(query);

        filteredNotifications.clear();
        for (Notification notification : allObjects) {
            String message = String.join(" ", notification.getMessages());

            //Check if the title or any of the messages contain the search query
            if (notification.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    message.toLowerCase().contains(query.toLowerCase())) {
                filteredNotifications.add(notification);
            }
        }

        // Reset to the first page and load the objects
        currentPage.setValue(1);
        loadObjects();
    }
}
