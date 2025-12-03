package com.unpar.brokenlinkscanner.utils;

import com.unpar.brokenlinkscanner.models.Link;

public interface LinkReceiver {
    void receive(Link link);
}
