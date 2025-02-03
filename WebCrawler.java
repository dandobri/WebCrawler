package info.kgeorgiy.ja.dobris.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements NewCrawler {
    private final Downloader downloader;

    public final ExecutorService downloadersExecutor;

    private final ExecutorService extractorsExecutor;

    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersExecutor = Executors.newFixedThreadPool(downloaders);
        this.extractorsExecutor = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("You have an invalid input");
            return;
        }
        try {
            final String url = args[0];
            final int depth = Integer.parseInt(args[1]) > 0 ? Integer.parseInt(args[1]) : 1;
            final int downloads = Integer.parseInt(args[2]) > 0 ? Integer.parseInt(args[2]) : 1;
            final int extractors = Integer.parseInt(args[3]) > 0 ? Integer.parseInt(args[3]) : 1;
            final int perHost = Integer.parseInt(args[4]) > 0 ? Integer.parseInt(args[4]) : 1;
            Set<String> exludes = new HashSet<>();
            exludes.add("hello");
            exludes.add("world");
            Downloader down = new CachingDownloader(52);
            WebCrawler webCrawler = new WebCrawler(down, downloads, extractors, perHost);
            Result result = webCrawler.download(url, depth, exludes);
            webCrawler.close();
            System.out.println(result.getDownloaded() + " " + result.getErrors());
        } catch (NumberFormatException e) {
            System.err.println("Your input can not be cast");
        } catch (IOException e) {
            System.err.println("You have an IOException in downloader" + e.getMessage());
        }
    }
    @Override
    public Result download(final String url, final int depth, final Set<String> excludes) {
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Set<String> links = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        Queue<Future<List<String>>> queueFuture = new ConcurrentLinkedQueue<>();
        if (excludes.stream().noneMatch(url::contains)) {
            queue.add(url);
            links.add(url);
        }
        for(int i = 0; i < depth; i++) {
            while(!queue.isEmpty()) {
                String link = queue.poll();
                Future<Document> doc = downloadersExecutor.submit(() -> {
                    try {
                        Document document = downloader.download(link);
                        downloaded.add(link);
                        return document;
                    } catch (IOException e) {
                        errors.put(link, e);
                    }
                    return null;
                });
                // NullPointerException
                queueFuture.add(extractorsExecutor.submit(() -> doc.get().extractLinks()));
            }
            queueFuture.forEach(future -> {
                try {
                    queue.addAll(future.get().stream()
                            // Move to extractor
                            .filter(link -> excludes.stream().noneMatch(link::contains) && links.add(link)).toList());
                } catch (InterruptedException | ExecutionException ignored) { }
            });
            queueFuture.clear();
        }
        queue.clear();
        links.clear();
        return new Result(new ArrayList<>(downloaded), errors);
    }

    @Override
    public void close() {
        downloadersExecutor.shutdownNow();
        extractorsExecutor.shutdownNow();
    }
}
