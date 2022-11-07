package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;

import com.udacity.webcrawler.parser.PageParserFactory;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {

  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount, PageParserFactory pageParserFactory,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.parserFactory = pageParserFactory;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    ParallelWebCrawlerRA parallelWebCrawlerRA = new ParallelWebCrawlerRA.Builder()
        .setClock(clock)
        .setTimeout(timeout)
        .setDeadline(clock.instant().plus(timeout))
        .setStartingUrls(startingUrls)
        .setMaxDepth(maxDepth)
        .setIgnoredUrls(ignoredUrls)
        .setParserFactory(parserFactory).build();

    pool.invoke(parallelWebCrawlerRA);

    Map<String, Integer> counts = parallelWebCrawlerRA.getCounts();
    Set<String> visitedUrls = parallelWebCrawlerRA.getVisitedUrls();

    pool.shutdown();

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
          .setWordCounts(counts)
          .setUrlsVisited(visitedUrls.size())
          .build();
    }

    return new CrawlResult.Builder()
        .setWordCounts(WordCounts.sort(counts, popularWordCount))
        .setUrlsVisited(visitedUrls.size())
        .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
