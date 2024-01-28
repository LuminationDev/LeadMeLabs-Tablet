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

    public static final String[] headsetAudioDeviceNames = new String[] {
            "Speakers (VIVE Pro Multimedia Audio)", // vive pro 2
            "Speakers (VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (HTC-VIVE)", // vive pro 1
            "Headphones (Galaxy Buds2 (AE10) Stereo)" // leaving this here for debugging as I doubt someone is going to put their earbuds onto a station
    };
    public static final String[] projectorAudioDeviceNames = new String[] {
            "Laser Proj (HD Audio Driver for Display Audio)", // xiaomi
            "Laser Proj (AMD High Definition Audio Device)", // xiaomi
            "Laser Proj", // xiaomi
            "EPSON PJ (AMD High Definition Audio Device)", // epson
            "EPSON PJ (HD Audio Driver for Display Audio)", // epson
            "EPSON PJ", // epson
            "1 - EPSON PJ (AMD High Definition Audio Device)",
            "2 - EPSON PJ (AMD High Definition Audio Device)",
            "3 - EPSON PJ (AMD High Definition Audio Device)",
            "4 - EPSON PJ (AMD High Definition Audio Device)",
            "5 - EPSON PJ (AMD High Definition Audio Device)",
            "6 - EPSON PJ (AMD High Definition Audio Device)",
            "7 - EPSON PJ (AMD High Definition Audio Device)",
            "8 - EPSON PJ (AMD High Definition Audio Device)",
            "9 - EPSON PJ (AMD High Definition Audio Device)",
            "10 - EPSON PJ (AMD High Definition Audio Device)",
            "Headset (Galaxy Buds2 (AE10) Hands-Free AG Audio)" // leaving this here for debugging as I doubt someone is going to put their earbuds onto a station
    };
}
