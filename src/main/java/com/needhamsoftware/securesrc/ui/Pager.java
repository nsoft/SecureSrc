package com.needhamsoftware.securesrc.ui;

import java.io.IOException;
import com.needhamsoftware.securesrc.search.SearchFunction;
import com.needhamsoftware.securesrc.search.SearchResult;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * Encapsulate paging logic and state. Methods are synchronized to guard against
 * double and fast clicking in the UI.
 */
public class Pager {

  private int pageSize = 10;
  private int currentPage = 1;
  private long hits;

  /**
   * Run the search and return the results for the next page.
   *
   * @param request a function that accepts a request size and returns a SearchResult. It is important
   *                that this function not perform other stateful actions as it may be repeated in some cases
   * @return The search result for the next page.
   */
  public synchronized SearchResult nextPage(SearchFunction<Integer, SearchResult> request) throws IOException, ParseException {
    currentPage++;
    SearchResult result = request.apply(docsToSkip());
    hits = result.totalHits;
    if (docsToSkip() - hits > pageSize) {
      // we didn't need another page after all. If we don't do this and allow
      // the pages to increase beyond the amount required, then the back
      // button will have to be pressed many times to retrace "phantom" pages.
      currentPage = (int) (hits / pageSize) + 1;
    }
    return result;
  }

  /**
   * Run the search for the previous page
   *
   * @see #nextPage(SearchFunction) for details about functions given to methods on this class
   */
  public synchronized SearchResult prevPage(SearchFunction<Integer, SearchResult> request) throws IOException, ParseException {
    if (currentPage > 1) {
      currentPage--;
    }
    SearchResult result = request.apply(docsToSkip());
    hits = result.totalHits;
    if (result.resultPage.isEmpty() && result.totalHits > 0) {
      // back button should never produce zero results, and next
      // after back should always produce a new page of results
      // last page decreased more than one page, docs were deleted
      currentPage = (int) (hits / pageSize) + 1; // find the actual last page
      // re-run the request to get the second to last page or page 1 if only one page.
      result = prevPage(request);
    }
    return result;
  }

  /**
   * Run the search for the first page
   *
   * @see #nextPage(SearchFunction) for details about functions given to methods on this class
   */
  public synchronized SearchResult firstPage(SearchFunction<Integer, SearchResult> request) throws IOException, ParseException {
    currentPage = 0;
    return nextPage(request);
  }

  /**
   * Run the search for the last page
   *
   * @see #nextPage(SearchFunction) for details about functions given to methods on this class
   */
  public synchronized SearchResult lastPage (SearchFunction<Integer,SearchResult> request) throws IOException, ParseException {
    currentPage = (int) (hits / pageSize) + 1; // guess the last page
    SearchResult result = nextPage(request);
    if (result.totalHits > docsToSkip()) {
      // many docs added, try again, this will recurse till we get it right.
      // breaks with StackOverflow if we are indexing docs faster than we can iterate this request.
      // can also break if total hits is varying by more than a page size faster than we can iterate.
      // neither case is important for this application.
      hits = result.totalHits;
      result = nextPage(request);
    }
    return result;
  }

  public synchronized void reset() {
    currentPage = 1;
  }

  public int docsToSkip() {
    return pageSize * (currentPage - 1);
  }


  @SuppressWarnings("unused")
  public synchronized void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public synchronized int getPageSize() {
    return pageSize;
  }

  public synchronized int getCurrentPage() {
    return currentPage;
  }

}
