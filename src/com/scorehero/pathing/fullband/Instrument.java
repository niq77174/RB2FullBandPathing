package com.scorehero.pathing.fullband;

public enum Instrument {
    GUITAR (0),
    BASS (1),
    DRUMS (2),
    VOCALS (3),
    INSTRUMENT_COUNT (4);

    private int index;

    Instrument(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
