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
            "Speakers (1- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (2- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (3- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (4- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (5- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (6- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (7- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (8- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (9- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (10- VIVE Pro Mutimedia Audio)", // vive pro 2 typo
            "Speakers (HTC-VIVE)", // vive pro 1
            "Speakers (VIVE Virtual Audio Device)", // xr elite
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
