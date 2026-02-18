package com.needhamsoftware.securesrc.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import com.needhamsoftware.securesrc.model.NamedObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * A class providing basic in-memory search with Apache Lucene. This class strongly favors
 * simplicity over performance because the size of the dataset involved is absolutely
 * miniscule by Lucene standards. Lucene is built to handle millions of docs
 * with millisecond searches, and index docs at thousands per second. The case where
 * a user has over a thousand login records will be very rare. So searchers are
 * not cached, the index is rebuilt at the drop of a hat, etc. We will optimize later
 * (if it is ever needed!). In other words, don't use this as an example for large
 * applications!
 */
public class LuceneSearch {

  Directory index;

  public LuceneSearch() {
    this.index = new ByteBuffersDirectory();
  }

  /**
   * Since the point of the search index is to allow selection of a node in the display tree we
   * want to index using the TreeModel not our list of model objects.
   *
   * @param model
   */
  public void indexTreeModel(TreeModel model) {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    try {
      try (IndexWriter indexWriter = new IndexWriter(index, config)) {
        indexWriter.deleteAll();
        indexTree((DefaultMutableTreeNode) model.getRoot(), indexWriter);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void indexTree(DefaultMutableTreeNode root, IndexWriter writer) {
    int childCount = root.getChildCount();
    for (int i = 0; i < childCount; i++) {
      indexTree((DefaultMutableTreeNode) root.getChildAt(i), writer);
    }
    Document doc = new Document();
    List<DefaultMutableTreeNode> path = Arrays.stream(root.getPath())
        .map(n -> (DefaultMutableTreeNode) n)
        .toList();
    String pathStr = path.stream()
        .map(n -> {
          if (n.getUserObject() instanceof String) {
            return n.getUserObject().toString();
          } else {
            return ((NamedObject) n.getUserObject()).getUuid();
          }
        })
        .collect(Collectors.joining("/"));

    String breadCrumb = path.stream()
        .map(n -> {
          if (n.getUserObject() instanceof String) {
            return n.getUserObject().toString();
          } else {
            return ((NamedObject) n.getUserObject()).getName();
          }
        })
        .collect(Collectors.joining(" > "));

    StringBuilder text = new StringBuilder();
    text.append(pathStr).append(" ");
    doc.add(new StringField("path", pathStr, Field.Store.YES));
    doc.add(new StringField("breadcrumb", breadCrumb, Field.Store.YES));

    Object obj = root.getUserObject();
    if (obj instanceof NamedObject userObject) {
      String name = userObject.getName();
      text.append(name).append(" ");
      doc.add(new TextField("name", name, Field.Store.YES));

      String description = userObject.getDescription();
      text.append(description);
      doc.add(new TextField("description", description, Field.Store.YES));
      doc.add(new TextField("text", text.toString(), Field.Store.YES));
      doc.add(new StringField("id",((NamedObject) obj).getUuid(), Field.Store.YES));
      try {
        writer.updateDocument(new Term("id",userObject.getUuid()),doc);
      } catch (IOException e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace(); // don't throw and fail the other docs
      }
    }
  }

  public SearchResult search(String queryStr, int hits, int offset) throws IOException, ParseException {
    try (IndexReader reader = DirectoryReader.open(index)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      Analyzer analyzer = new StandardAnalyzer();
      QueryParser parser = new QueryParser("text", analyzer);
      Query query = parser.parse(queryStr);
      System.out.println(query);

      TopDocs topDocs = searcher.search(query, hits+offset);
      try {
        List<Document> list = Arrays.stream(topDocs.scoreDocs).skip(offset).map(sd -> {
          try {
            return reader.storedFields().document(sd.doc);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).toList();
        SearchResult result = new SearchResult(list,topDocs.totalHits.value());
        return result;
      } catch (RuntimeException r) {
        if (r.getCause() instanceof IOException) {
          throw (IOException) r.getCause();
        } else {
          throw r;
        }
      }
    }
  }

}
