package com.needhamsoftware.securesrc.search;

import java.util.List;
import org.apache.lucene.document.Document;

public class SearchResult {
  public List<Document> resultPage;
  public long totalHits;

  public SearchResult(List<Document> resultPage, long totalHits) {
    this.resultPage = resultPage;
    this.totalHits = totalHits;
  }
}
