package com.unpar.brokenlinkchecker.model;

import org.jsoup.nodes.Document;

public record FetchResult(Link link, Document document) {
}
