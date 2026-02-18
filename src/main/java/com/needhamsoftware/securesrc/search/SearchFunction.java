package com.needhamsoftware.securesrc.search;

import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

public interface SearchFunction<I extends Number, S> {

  public S apply(I o) throws IOException, ParseException;

}
