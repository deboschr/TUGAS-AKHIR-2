package com.unpar.brokenlinkscanner.utils;

import com.unpar.brokenlinkscanner.models.Link;

/**
 * Antarmuka untuk menerima objek Link.
 *
 * Antarmuka ini digunakan sebagai mekanisme komunikasi (menerima/mengirim) antara kelas yang melakukan proses pemeriksaan tautan (Crawler) dengan kelas yang menerima dan mengolah hasilnya (MainController).
 */
public interface LinkReceiver {

    /**
     * Method yang dipanggil setiap kali sebuah Link berhasil diperiksa.
     *
     * @param link objek Link hasil pemeriksaan
     */
    void receive(Link link);
}