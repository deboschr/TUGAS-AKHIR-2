package com.unpar.brokenlinkchecker.models;

public enum Status {
    // Lagi nganggur, belum ngapa-ngapain.
    IDLE,

    // Lagi ngecek link, proses sedang berlansung.
    CHECKING,

    // Proses dihentikan user.
    STOPPED,

    // Proses selesai tanpa dihentikan user.
    COMPLETED
}
