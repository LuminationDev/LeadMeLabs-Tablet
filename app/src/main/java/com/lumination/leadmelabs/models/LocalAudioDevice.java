package com.lumination.leadmelabs.models;

public class LocalAudioDevice {
    private final String Name;
    private final String Id;
    private int Volume;

    private boolean Muted;

    public LocalAudioDevice(String name, String id)
    {
        Name = name;
        Id = id;
    }

    public void SetVolume(int volume)
    {
        Volume = volume;
    }
    public int GetVolume()
    {
        return this.Volume;
    }

    public void SetMuted(boolean isMuted)
    {
        Muted = isMuted;
    }
    public boolean GetMuted()
    {
        return this.Muted;
    }

    public String getName() {
        return this.Name;
    }

    public String getId() {
        return this.Id;
    }
}
