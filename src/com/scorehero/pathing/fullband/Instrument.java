package com.scorehero.pathing.fullband;

public enum Instrument {
    GUITAR (0),
    DRUMS (1),
    VOCALS (2),
    BASS (3),
    INSTRUMENT_COUNT (4);

    private int index;

    Instrument(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
