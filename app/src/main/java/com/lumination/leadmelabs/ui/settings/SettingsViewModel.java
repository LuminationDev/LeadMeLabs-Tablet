package com.lumination.leadmelabs.ui.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Only responsible for setting the address as it is saved in shared
 * preferences afterwards which can be loaded at the application start.
 */
public class SettingsViewModel extends AndroidViewModel {
    private MutableLiveData<String> nucAddress;
    private MutableLiveData<String> nucMacAddress;
    private MutableLiveData<String> pinCode;
    private MutableLiveData<String> encryptionKey;
    private MutableLiveData<String> labLocation;
    private MutableLiveData<String> licenseKey;
    private MutableLiveData<String> ipAddress;
    private MutableLiveData<Boolean> hideStationControls;
    private MutableLiveData<Boolean> enableAnalyticsCollection;
    private MutableLiveData<Boolean> additionalExitPrompts;
    private MutableLiveData<Boolean> internalTraffic;
    private MutableLiveData<Boolean> developerTraffic;
    private MutableLiveData<Boolean> enableRoomLock;
    private MutableLiveData<HashSet<String>> lockedRooms;
    private MutableLiveData<Boolean> updateAvailable = new MutableLiveData<>(false);

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Get the IP Address that is saved for the NUC.
     */
    public LiveData<String> getNuc() {
        if (NetworkService.getNUCAddress() == null || NetworkService.getNUCAddress().equals("")) {
            nucAddress = new MutableLiveData<>();
        } else {
            nucAddress = new MutableLiveData<>(NetworkService.getNUCAddress());
        }

        return nucAddress;
    }

    /**
     * Set the IP address for the local NUC, after setting the tablet requests the most up to date
     * information about the Stations list and the Appliance list. Essentially refreshing the current
     * status of the application.
     */
    public void setNucAddress(String newValue) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nuc_address", newValue);
        editor.apply();
        NetworkService.setNUCAddress(newValue);
        nucAddress.setValue(newValue);
        NetworkService.sendMessage("NUC", "Stations", "List");
        NetworkService.sendMessage("NUC", "Appliances", "List");
    }

    /**
     * Get the tablets IP address provided by the Network service. This is used to identify message
     * communications and displayed on the settings page.
     */
    public LiveData<String> getIpAddress() {
        if (ipAddress == null) {
            ipAddress = new MutableLiveData<>(NetworkService.getIPAddress());
        }

        return ipAddress;
    }

    /**
     * Get the MAC Address that is saved for the NUC.
     */
    public LiveData<String> getNucMac() {
        if (nucMacAddress == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_mac_address", Context.MODE_PRIVATE);
            nucMacAddress = new MutableLiveData<>(sharedPreferences.getString("nuc_mac_address", null));
        }

        return nucMacAddress;
    }

    /**
     * Set the MAC address of the NUC, this is used for a wake on lan command in case the NUC is
     * ever down.
     */
    public void setNucMacAddress(String newValue) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_mac_address", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nuc_mac_address", newValue);
        editor.apply();
        nucMacAddress.setValue(newValue);
    }

    /**
     * Determine whether the tablet should be in WallMode. WallMode only shows the
     * automation controls and settings while Normal Mode gives the user full access.
     */
    public LiveData<Boolean> getHideStationControls() {
        if (hideStationControls == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
            hideStationControls = new MutableLiveData<>(sharedPreferences.getBoolean("hide_station_controls", true));
        }
        return hideStationControls;
    }

    /**
     * Set the tablet in to WallMode or Normal Mode.
     * @param value A boolean to represent if WallMode is active (true) or not (false).
     */
    public void setHideStationControls(Boolean value) {
        hideStationControls.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hide_station_controls", value);
        editor.apply();
    }

    /**
     * Check to see if the user has disabled analytics. This value represents if the user has opted
     * out of all analytic collection.
     */
    public LiveData<Boolean> getAnalyticsEnabled() {
        if (enableAnalyticsCollection == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("enable_analytics_collection", Context.MODE_PRIVATE);
            enableAnalyticsCollection = new MutableLiveData<>(sharedPreferences.getBoolean("enable_analytics_collection", true));
        }
        return enableAnalyticsCollection;
    }

    /**
     * Set whether analytics can be collected or not.
     */
    public void setAnalyticsEnabled(Boolean value) {
        enableAnalyticsCollection.setValue(value);
        FirebaseManager.toggleAnalytics(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("enable_analytics_collection", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enable_analytics_collection", value);
        editor.apply();
    }

    /**
     * Determine whether the tablet should ask the user if they are sure they want to exit the
     * current application as a VR user may need to save their progress.
     */
    public LiveData<Boolean> getAdditionalExitPrompts() {
        if (additionalExitPrompts == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("additional_exit_prompts", Context.MODE_PRIVATE);
            additionalExitPrompts = new MutableLiveData<>(sharedPreferences.getBoolean("additional_exit_prompts", false));
        }
        return additionalExitPrompts;
    }

    /**
     * Ask the user if they are sure they want to exit an experience.
     * @param value A boolean to represent if Additional prompts is active (true) or not (false).
     */
    public void setAdditionalExitPrompts(Boolean value) {
        additionalExitPrompts.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("additional_exit_prompts", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("additional_exit_prompts", value);
        editor.apply();
    }

    /**
     * Get the internally saved License key, if none is found this returns null meaning the key has
     * not been entered yet.
     * @return A String representing the key present on the device.
     */
    public LiveData<String> getLicenseKey() {
        if (licenseKey == null) {
            licenseKey = new MutableLiveData<>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("license_key", Context.MODE_PRIVATE);
            licenseKey.setValue(sharedPreferences.getString("license_key", null));
        }
        return licenseKey;
    }

    /**
     * Save the license key internally. This key enables the device to be used with the LeadMe Labs
     * software suite. It is checked against a firebase record to determine if it is valid.
     */
    public void setLicenseKey(String value) {
        getLicenseKey(); // to initialize if not already done
        licenseKey.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("license_key", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("license_key", value);
        editor.apply();
    }

    /**
     * Retrieve the pin code used to enter into the settings page.
     */
    public LiveData<String> getPinCode() {
        if (pinCode == null) {
            pinCode = new MutableLiveData<>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("pin_code", Context.MODE_PRIVATE);
            pinCode.setValue(sharedPreferences.getString("pin_code", null));
        }
        return pinCode;
    }

    /**
     * Set the pin code that locks the settings page from regular users.
     */
    public void setPinCode(String value) {
        getPinCode(); // to initialize if not already done
        pinCode.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("pin_code", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pin_code", value);
        editor.apply();
    }

    /**
     * Get the encryption key that is used to encrypt messages travelling between a tablet and
     * the NUC.
     */
    public LiveData<String> getEncryptionKey() {
        if (encryptionKey == null) {
            encryptionKey = new MutableLiveData<>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("encryption_key", Context.MODE_PRIVATE);
            encryptionKey.setValue(sharedPreferences.getString("encryption_key", null));
        }
        return encryptionKey;
    }

    /**
     * Set a new encryption key, WARNING: the key must be the same as present on the local NUC and
     * all Stations otherwise messages will not be correctly interpreted.
     */
    public void setEncryptionKey(String value) {
        getEncryptionKey(); // to initialize if not already done
        encryptionKey.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("encryption_key", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("encryption_key", value);
        editor.apply();
    }

    /**
     * Get the lab location
     */
    public LiveData<String> getLabLocation() {
        if (labLocation == null) {
            labLocation = new MutableLiveData<>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("lab_location", Context.MODE_PRIVATE);
            labLocation.setValue(sharedPreferences.getString("lab_location", null));
            FirebaseManager.setDefaultAnalyticsParameter("lab_location", labLocation.getValue());
        }
        return labLocation;
    }

    /**
     * Set the lab location
     */
    public void setLabLocation(String value) {
        getLabLocation(); // to initialize if not already done
        labLocation.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("lab_location", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lab_location", value);
        editor.apply();
    }

    /**
     *
     */
    public LiveData<Boolean> getUpdateAvailable() {
        if (updateAvailable == null) {
            updateAvailable = new MutableLiveData<>(false);
        }

        return updateAvailable;
    }

    /**
     *
     */
    public void setUpdateAvailable(Boolean newValue) {
        updateAvailable.setValue(newValue);
    }

    /**
     * Check to see if the user has enabled the room lock. This value represents if the user has
     * selected to only control one room from the tablet, ignoring all other information from the
     * NUC about different rooms.
     */
    public LiveData<Boolean> getRoomLockEnabled() {
        if (enableRoomLock == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("enable_room_lock", Context.MODE_PRIVATE);
            enableRoomLock = new MutableLiveData<>(sharedPreferences.getBoolean("enable_room_lock", true));
        }
        return enableRoomLock;
    }

    /**
     * Set whether the room lock is engaged.
     */
    public void setRoomLockEnabled(Boolean value) {
        enableRoomLock.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("enable_room_lock", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enable_room_lock", value);
        editor.apply();
    }

    /**
     * This value represents that the user has selected to only control the one specific room from
     * the tablet, ignoring all other information from the NUC about different rooms.
     */
    public LiveData<HashSet<String>> getLockedRooms() {
        if (lockedRooms == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("locked_rooms", Context.MODE_PRIVATE);
            String hashSetString = sharedPreferences.getString("locked_rooms", "None");

            if(hashSetString.equals("None")) {
                lockedRooms = new MutableLiveData<>(new HashSet<>());
            } else {
                lockedRooms = new MutableLiveData<>(StringToHashSet(hashSetString));
            }
        }

        return lockedRooms;
    }

    /**
     * Detect if the locked toggle is set to on or off in the settings and respond to the locked
     * room query appropriately.
     * @return An empty hashset if the toggle is off and the hashset of rooms if the toggle is on.
     */
    public LiveData<HashSet<String>> getLockedIfEnabled() {
        //Early exit if the toggle is off
        if(Boolean.FALSE.equals(getRoomLockEnabled().getValue())) {
            return new MutableLiveData<>(new HashSet<>());
        }

        return getLockedRooms();
    }

    /**
     * Set which room name is locked.
     */
    public void setLockedRooms(String value) {
        String entry = value;

        if(value.equals("")) {
            entry = "None";
            lockedRooms.setValue(new HashSet<>());
        } else {
            lockedRooms.setValue(StringToHashSet(value));
        }

        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("locked_rooms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("locked_rooms", entry);
        editor.apply();
    }

    /**
     * Transform a String into a HashSet.
     * @param value A toString() version of a HashSet.
     * @return A HashSet with the appropriate values as per the supplied string.
     */
    private HashSet<String> StringToHashSet(String value) {
        return new HashSet<>(Arrays.asList(value
                .replace("[", "")
                .replace("]", "")
                .replace(", ", ",")
                .split(",")));
    }

    /**
     * Check if the user tablet is Lumination internal traffic. Used for analytics
     */
    public LiveData<Boolean> getInternalTrafficValue() {
        if (internalTraffic == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("internal_traffic", Context.MODE_PRIVATE);
            internalTraffic = new MutableLiveData<>(sharedPreferences.getBoolean("internal_traffic", false));
        }
        return internalTraffic;
    }

    /**
     * Set if the user tablet is Lumination internal traffic. Used for analytics
     */
    public void setInternalTrafficValue(Boolean value) {
        internalTraffic.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("internal_traffic", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("internal_traffic", value);
        editor.apply();
    }

    /**
     * Set if the user tablet is Lumination developer traffic. Used for analytics
     */
    public LiveData<Boolean> getDeveloperTrafficValue() {
        if (developerTraffic == null) {
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("developer_traffic", Context.MODE_PRIVATE);
            developerTraffic = new MutableLiveData<>(sharedPreferences.getBoolean("developer_traffic", false));
        }
        return developerTraffic;
    }

    /**
     * Check if the user tablet is Lumination developer traffic. Used for analytics
     */
    public void setDeveloperTrafficValue(Boolean value) {
        developerTraffic.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("developer_traffic", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("developer_traffic", value);
        editor.apply();
    }
}