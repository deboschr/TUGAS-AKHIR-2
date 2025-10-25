package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.Link;
import com.unpar.brokenlinkchecker.model.CheckingStatus;
import com.unpar.brokenlinkchecker.model.FetchResult;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.application.Platform;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Crawler {

}
